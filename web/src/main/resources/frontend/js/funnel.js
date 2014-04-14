var Funnel = (function () {

  var cls = function () {

  };

  cls.render = function () {
    $('.body-container').html(Mustache.render(funnelTemplate));

    var params = $.deparam(window.location.search.substring(1));
    var funnel = params.type === 'funnel' ? params : {};

    this.initializeFunnelSteps(funnel);
    this.initializeFunnelDatePickers(funnel);
    this.initializeDaysToComplete(funnel)
    this.bindFunnelInputListeners();
    this.bindRemoveStepListener();
  };

  cls.getFunnel = function () {
    var self = this;

    var funnel = {
      start_date: Utils.formatDate($('#funnelStartDate').val()),
      end_date: Utils.formatDate($('#funnelEndDate').val()),
      funnel_steps: this.getFunnelSteps(),
      num_days_to_complete_funnel: $('input[name="days"]').val(),
      type: 'funnel'
    };

    $('.step-container').each(function(i, step) {
      var filterValue = $(step).find('select[name="filterValue"]');
      if (filterValue.length) {
        funnel["efv" + i] = $(filterValue).val();
        funnel["efk" + i] = $(step).find('select[name="filterKey"]').val();
      }
    })

    window.history.replaceState({}, '', '/?' + $.param(funnel));
    $.ajax({
      type: "GET",
      url: "/events/funnel",
      data: funnel
    }).done(function(eventVolumes) {
      eventVolumes = JSON.parse(eventVolumes);
      self.renderCompletionRate(eventVolumes);
      self.renderFunnelGraph(eventVolumes);
    });
  };

  cls.getFunnelSteps = function () {
    var funnelSteps = $('.funnel-steps select[name="events"]').map(function(i, el) {
      return $(el).val();
    }).toArray();
    return funnelSteps;
  };

  cls.bindFunnelAddFiltersListener = function () {
    var self = this;
    $('.funnel-filters-toggle').click(function () {
      $(this).addClass('hide');
      $('.funnel-steps').addClass('show-filters');
      self.bindFunnelFilterKeyListeners();
    });
  };

  cls.bindAddStepListener = function () {
    var self = this;
    $('.add-step').off().click(function () {
      self.addStep();
    });
  };

  cls.bindRemoveStepListener = function () {
    $(document.body).off().on('click', '.remove-step', function () {
      $('.add-step').css('display', 'inline-block');
      $(this).parent().remove();
    });
  };

  cls.bindFunnelInputListeners = function () {
    var self = this;
    $('.calculate-funnel').off().click(function () {
      $('.funnel-inputs .spinner').addClass('rendered');
      self.getFunnel();
    });
  };

  cls.bindFunnelFilterKeyListeners = function () {
    var self = this;
    $('select[name="filterKey"]').off().change(function () {
      $(this).parent().find('.filter-value').remove();
      if ($(this).val() !== 'no filter') {
        self.renderFunnelValueFilter($(this));
      }
    });
  };

  cls.bindFunnelEventListeners = function () {
    var self = this;
    $('select[name="events"]').change(function () {
      $(this).parent().find('.filters').remove();
      var view = {
        filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[$(this).val()])
      };
      $(this).parent().append(Mustache.render(filterKeyTemplate, view));
      $('.selectpicker').selectpicker('render');
      self.bindFunnelFilterKeyListeners();
    });
  };

  cls.addStep = function () {
    var view = {
      eventTypes: EVENT_TYPES,
      filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[EVENT_TYPES[0]]),
    };

    var partials = {
      "eventType": eventTypeTemplate,
      "filters": filterKeyTemplate
    };

    $('.steps-container').append(Mustache.render(stepTemplate, view, partials));
    $('.selectpicker').selectpicker('render');

    this.bindFunnelEventListeners();

    if ($('.step-container').length === 5) $('.add-step').css('display', 'none');
  };

  cls.initializeFunnelSteps = function (funnel) {
    var self = this;

    $('.steps-container').empty();
    $('.add-step').remove();

    Utils.getEventTypes(function (eventTypes) {
      EVENT_TYPES = JSON.parse(eventTypes);
      Utils.getEventKeys(function () {
        funnel.steps = funnel.funnel_steps || [EVENT_TYPES[0], EVENT_TYPES[1]];
        funnel.steps.forEach(function (v, i) {
          self.addStep();
          $('.funnel-show select').last().val(v);
          $('.funnel-show select').selectpicker('refresh');
        });
        self.renderAddFunnelStep();
        self.bindAddStepListener();
        self.bindFunnelAddFiltersListener();
      });
    });
  };

  cls.initializeDaysToComplete = function (funnel) {
    $('#daysToComplete').val(funnel.num_days_to_complete_funnel || 7);
  };

  cls.initializeFunnelDatePickers = function (funnel) {
    var start_date = funnel.start_date ? Utils.unFormatDate(funnel.start_date) : '01/01/2013';
    var end_date = funnel.end_date ? Utils.unFormatDate(funnel.end_date) : '01/30/2013';
    $( "#funnelStartDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                        .datepicker('setValue', start_date);
    $( "#funnelEndDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                      .datepicker('setValue', end_date);
  };

  cls.renderFunnelValueFilter = function () {
    var params = {
      event_type: $keyFilter.parent().parent().find('select[name="events"]').val(),
      event_key: $keyFilter.val()
    }

    $.ajax({
      type: "GET",
      url: "/events/values?" + $.param(params)
    }).done(function(values) {
      values = JSON.parse(values);

      var view = {
        filterValues: values
      };

      $keyFilter.parent().append(Mustache.render(filterValueTemplate, view));
      $('.selectpicker').selectpicker('render');
    });
  };

  cls.renderAddFunnelStep = function () {
    $('.funnel-steps').append(Mustache.render(addStepTemplate));
  };

  cls.renderCompletionRate = function(eventVolumes) {
    var eventLength = eventVolumes.length;
    var completionRate = (eventVolumes[eventLength - 1] / eventVolumes[0] * 100).toFixed(2);
    $('.completion-rate').html('<span style="font-weight: bold">' + completionRate + '%</span> Completion Rate');
  };

  cls.renderFunnelGraph = function(eventVolumes) {
    $('.middle').addClass('rendered');
    $('.graph').empty();
    var maxEventVolume = Math.max.apply(Math, eventVolumes);
    var diviser = Math.pow(10, (maxEventVolume.toString().length - 2));
    var Y_AXIS_MAX = Math.ceil(maxEventVolume / diviser) * diviser;

    $('.y-value').each(function (i, el) {
        $(el).text(parseInt(Y_AXIS_MAX / 6 * (i + 1), 10));
    });

    var funnelSteps = this.getFunnelSteps();
    var previousVolume;
    eventVolumes.forEach(function (v, i) {
        if (i > 0) {
            view = {
                conversion: (v / previousVolume * 100).toFixed(2)
            };
            $('.graph').append(Mustache.render(spaceTemplate, view));
        }
        var view = {
            height: (v / Y_AXIS_MAX * 100),
            numEvents: v,
            eventName: funnelSteps[i]
        };
        previousVolume = v;
        $('.graph').append(Mustache.render(barTemplate, view));
    });

    $('.funnel-inputs .spinner').removeClass('rendered');
  }

  return cls;

})();
