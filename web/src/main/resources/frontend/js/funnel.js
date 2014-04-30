var Funnel = (function () {

  var cls = function () {

  };

  cls.render = function () {
    $('.body-container').html(Mustache.render(funnelTemplate));

    var funnel;
    var params = $.deparam(window.location.search.substring(1));
    if (params.type === 'funnel') {
      funnel = params;
    }
    else {
      funnel = {};
      history.pushState("", document.title, window.location.pathname);
    }

    this.initializeFunnelSteps(funnel);
    this.initializeDatePickers(funnel);
    this.initializeDaysToComplete(funnel);
    this.bindInputListeners();
    this.bindRemoveStepListener();
  };

  cls.getFunnel = function () {
    var self = this;

    var funnel = {
      start_date: Utils.formatDate($('#funnelStartDate').val()),
      end_date: Utils.formatDate($('#funnelEndDate').val()),
      num_days_to_complete_funnel: $('input[name="days"]').val(),
      funnel_steps: this.getFunnelSteps(),
      type: 'funnel'
    };

    $('.step-container').each(function(i, step) {
      $(step).find('.filters-container .filters').each(function (j, filters) {
        var $filterValue = $(filters).find('.filter-value--input');
        var $filterKey = $(filters).find('.filter-key--input');
        if ($filterValue.length) {
          funnel["efv" + i] = funnel["efv" + i] || [];
          funnel["efv" + i] = funnel["efv" + i].concat($filterValue.val());

          funnel["efk" + i] = funnel["efk" + i] || [];
          funnel["efk" + i] = funnel["efk" + i].concat($filterKey.val());
        }
      });
    });

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
    var funnelSteps = $('.funnel-steps .event-type--input').map(function(i, el) {
      return $(el).val();
    }).toArray();
    return funnelSteps;
  };

  cls.bindAddFilterListener = function ($step) {
    var self = this;
    $step.find('.add-filter').click(function () {
      self.renderFilterKey($step);
    });
  }

  cls.bindRemoveFilterListener = function ($step) {
    var self = this;
    $step.find('.remove-filter').last().click(function () {
      var $filters = $(this).parent();
      $filters.remove();
    });
  }

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

  cls.bindInputListeners = function () {
    var self = this;
    $('.calculate-funnel').off().click(function () {
      $('.funnel-inputs .spinner').addClass('rendered');
      self.getFunnel();
    });
  };

  cls.bindFilterKeyListeners = function ($step) {
    var self = this;
    $step.find('.filter-key--input').last().change(function () {
      $(this).parent().find('.filter-value').remove();
      self.renderFilterValue($(this));
    });
  };

  cls.bindEventSelectorListeners = function ($step) {
    var self = this;
    $step.find('.event-type--input').change(function () {
      var $filtersContainer = $(this).parent().find('.filters-container');
      $filtersContainer.empty();
    });
  };

  cls.addStep = function () {
    var view = {
      eventTypes: EVENT_TYPES,
    };

    $('.steps-container').append(Mustache.render(stepTemplate, view));

    $step = $('.steps-container .step-container').last();

    $step.find('.event-type--input').typeahead({
      source: EVENT_TYPES,
      items: 10000
    });

    this.bindEventSelectorListeners($step);
    this.bindAddFilterListener($step);
    this.bindFilterKeyListeners($step);

    if ($('.step-container').length === 5) $('.add-step').css('display', 'none');
  };

  cls.initializeFunnelSteps = function (funnel) {
    var self = this;

    $('.steps-container').empty();

    Utils.getEventTypes(function (eventTypes) {
      EVENT_TYPES = JSON.parse(eventTypes);
      Utils.getEventKeys(function () {
        self.renderAddFunnelStep();
        self.bindAddStepListener();

        funnel.steps = funnel.funnel_steps || [EVENT_TYPES[0], EVENT_TYPES[1]];
        funnel.steps.forEach(function (v, i) {
          self.addStep();
          $('.step-container .event-type--input').last().val(v);
          if (funnel['efv' + i]) {
            funnel['efv' + i].forEach(function (filterValue, j) {
              var $step = $('.step-container').last();
              self.renderFilterKey($step);

              var filterKey = funnel['efk' + i][j];
              var $filterKey = $step.find('.filter-key--input').last();
              $filterKey.val(filterKey)

              self.renderFilterValue($filterKey, function ($filterValue) {
                $filterValue.val(filterValue);
              });
            });
          }
        });
      });
    });
  };

  cls.initializeDaysToComplete = function (funnel) {
    $('#daysToComplete').val(funnel.num_days_to_complete_funnel || 7);
  };

  cls.initializeDatePickers = function (funnel) {
    var start_date = funnel.start_date ? Utils.unFormatDate(funnel.start_date) : '01/01/2014';
    var end_date = funnel.end_date ? Utils.unFormatDate(funnel.end_date) : '01/30/2014';
    $("#funnelStartDate").datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                        .datepicker('setValue', start_date)
                                        .on('keydown', function (e) {
                                          var keyCode = e.keyCode || e.which;
                                          if (keyCode === 9) {
                                            $(this).datepicker('hide');
                                          }
                                        });
    $("#funnelEndDate").datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                      .datepicker('setValue', end_date)
                                      .on('keydown', function (e) {
                                        var keyCode = e.keyCode || e.which;
                                        if (keyCode === 9) {
                                          $(this).datepicker('hide');
                                        }
                                      });
  };

  cls.renderFilterKey = function ($step) {
    var $filtersContainer = $step.find('.filters-container');
    var $eventsSelector = $step.find('.event-type--input');

    $filtersContainer.append(Mustache.render(filterKeyTemplate));
    $step.find('.filter-key--input').last().typeahead({
      source: EVENT_TYPE_KEYS[$eventsSelector.val()],
      items: 10000
    });

    this.bindFilterKeyListeners($step);
    this.bindRemoveFilterListener($step);
  };

  cls.renderFilterValue = function ($filterKey, cb) {
    var $stepContainer = $filterKey.parents().eq(2);
    var $eventsSelector = $stepContainer.find('.event-type--input');
    var $filters = $filterKey.parent();

    var params = {
      event_type: $eventsSelector.val(),
      event_key: $filterKey.val()
    };

    $filters.append(Mustache.render(filterValueTemplate));

    var $filterValue = $stepContainer.find('.filter-value--input');

    $.ajax({
      type: "GET",
      url: "/events/values?" + $.param(params)
    }).done(function(values) {
      values = JSON.parse(values);

      $filterValue.typeahead({
        source: values,
        items: 10000
      });

      if (cb) cb($filterValue);
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
        } else {
          $('.graph').append(Mustache.render(heightExpanderTemplate));
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

});
