var Cohort = (function () {

  var cls = function () {

  };

  cls.render = function () {
    var self = this;

    var cohort;
    var params = $.deparam(window.location.search.substring(1));
    if (params.type === 'cohort') {
      cohort = params;
    }
    else {
      cohort = {};
      history.pushState("", document.title, window.location.pathname);
    }

    $('.body-container').html(Mustache.render(cohortTemplate));

    this.initializeCohortShowMe(cohort);
    this.initializeCohortDatePickers(cohort);
    this.bindInputListeners();
  };

  cls.getCohort = function () {
    var self = this;

    var cohort = {
      start_date: Utils.formatDate($('#cohortStartDate').val()),
      end_date: Utils.formatDate($('#cohortEndDate').val()),
      row_event_type: $('.show-me .event-type--input').eq(0).val(),
      column_event_type: $('.show-me .event-type--input').eq(1).val(),
      num_days_per_row: $('#daysLater').val(),
      num_columns: 11, //$('#numColumns').val()... Why make things more complicated...
      type: 'cohort'
    };

    $('.event-container').each(function (i, event) {
      $(event).find('.filters-container .filters').each(function (j, filters) {
        var $filterValue = $(filters).find('.filter-value--input');
        var $filterKey = $(filters).find('.filter-key--input');
        if ($filterValue.length) {
          var axis = i === 1 ? 'c' : 'r';
          cohort[axis + "efk"] = cohort[axis + "efk"] || [];
          cohort[axis + "efk"] = cohort[axis + "efk"].concat($filterKey.val());
          cohort[axis + "efv"] = cohort[axis + "efv"] || [];
          cohort[axis + "efv"] = cohort[axis + "efv"].concat($filterValue.val());
        }
      });
    });

    window.history.replaceState({}, '', '/?' + $.param(cohort));

    $.ajax({
      type: "GET",
      url: "/events/cohort",
      data: cohort
    }).done(function(cohort) {
        cohort = JSON.parse(cohort);
        self.renderGraph(cohort);
    });
  };

  cls.renderGraph = function (cohort) {
    this.resetCohortGraph();
    $('.table-container').addClass('rendered');

    $('.date-title').text('Date');
    $('.event-title').text('People');

    var count = 0;
    var currentDate = new Date($('#cohortStartDate').val());
    for (var i = 0; i < cohort.length; i++) {
       $('.events').append('<div>' + cohort[i][0] + '</div>');

       var date = (currentDate.getMonth() + 1) + '/' + currentDate.getDate() + '/' + currentDate.getFullYear();
       $('.dates').append('<div>' + date + '</div>');
       var daysLater = parseInt($('#daysLater').val(), 10);
       currentDate.setDate(currentDate.getDate() + daysLater); // change

       $('.cohort').append('<div class="row' + i + '"></div>');
       for (var j = 1; j < cohort[i].length; j++) {
          if (i === 0) $('.axis').append('<div>' + j + '</div>');
          var percentage = cohort[i][j] === 0 ? 0 : cohort[i][j] / cohort[i][0] * 100;
          percentage = percentage === 100 ? percentage : percentage.toFixed(2);
          var boxClass = 'gradient-' + parseInt(percentage / 10, 10);
          $('.row' + i).append('<div class="box ' + boxClass + '">' + percentage + '%</div>');
       }
    }

    $('.range-container .spinner').removeClass('rendered');
  };

  cls.resetCohortGraph = function () {
    $('.events').empty();
    $('.dates').empty();
    $('.cohort').empty();
    $('.axis').empty();
  };

  cls.initializeCohortDaysLater = function (cohort) {
    $('#daysLater').val(cohort.num_days_per_row || 7);
  };

  cls.initializeCohortDatePickers = function (cohort) {
    var start_date = cohort.start_date ? Utils.unFormatDate(cohort.start_date) : '01/01/2014';
    var end_date = cohort.end_date ? Utils.unFormatDate(cohort.end_date) : '01/30/2014';
    $( "#cohortStartDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                           .datepicker('setValue', start_date)
                                           .on('keydown', function (e) {
                                             var keyCode = e.keyCode || e.which;
                                             if (keyCode === 9) {
                                               $(this).datepicker('hide');
                                             }
                                           });
    $( "#cohortEndDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                         .datepicker('setValue', end_date)
                                         .on('keydown', function (e) {
                                           var keyCode = e.keyCode || e.which;
                                           if (keyCode === 9) {
                                             $(this).datepicker('hide');
                                           }
                                         });
  };

  cls.initializeCohortShowMe = function(cohort) {
    var self = this;

    Utils.getEventTypes(function(eventTypes) {
      EVENT_TYPES = JSON.parse(eventTypes);
      Utils.getEventKeys(function () {
        self.renderShowMe(cohort);

        var rowEventType = cohort.row_event_type || EVENT_TYPES[0];
        $('.show-me .event-type--input').eq(0).last().val(rowEventType);

        var columnEventType = cohort.column_event_type || EVENT_TYPES[1];
        $('.show-me .event-type--input').eq(1).last().val(columnEventType);

        ['r', 'c'].forEach(function (axis, i) {
          if (cohort[axis + 'efv']) {
            cohort[axis + 'efv'].forEach(function (filterValue, j) {
              var $eventContainer = $('.event-container').eq(i);
              self.renderFilterKey($eventContainer);

              var filterKey = cohort[axis + 'efk'][j];
              var $filterKey = $eventContainer.find('.filter-key--input').last();
              $filterKey.val(filterKey);

              self.renderFilterValue($filterKey, function ($filterValue) {
                $filterValue.val(filterValue);
              });
            });
          }
        });
      });
    });
  };

  cls.renderShowMe = function (cohort) {
    var self = this;

    var view = {
      eventTypes: EVENT_TYPES,
      daysLater: cohort.num_days_per_row || 7
    };

    var partials = {
      "event" : eventTemplate
    };

    $('.cohort-definition').html(Mustache.render(showMeTemplate, view, partials));

    $('.event-container').each(function (i, el) {
      self.bindAddFilterListener($(el));
      self.bindEventSelectorListeners($(el));
    });

    $('.event-type--input').typeahead({
      source: EVENT_TYPES,
      items: 10000
    });
  };

  cls.renderFilterValue= function ($filterKey, cb) {
    var $eventContainer = $filterKey.parents().eq(2);
    var $eventSelector = $eventContainer.find('.event-type--input');

    var params = {
      event_type: $eventSelector.val(),
      event_key: $filterKey.val()
    }

    $filterKey.parent().append(Mustache.render(filterValueTemplate));

    var $filterValue = $eventContainer.find('.filter-value--input').last();

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

  cls.renderFilterKey = function ($eventContainer) {
    var $filtersContainer = $eventContainer.find('.filters-container');
    var $eventSelector = $eventContainer.find('.event-type--input');

    $filtersContainer.append(Mustache.render(filterKeyTemplate));
    var $filterKey = $filtersContainer.find('.filter-key--input').last();
    $filterKey.typeahead({
      source: EVENT_TYPE_KEYS[$eventSelector.val()],
      items: 10000
    });

    this.bindFilterKeyListeners($eventContainer);
    this.bindRemoveFilterListener($eventContainer);

    return $filterKey
  };

  cls.bindInputListeners = function () {
    var self = this;

    $('.calculate-cohort').click(function () {
      $('.range-container .spinner').addClass('rendered');
      self.getCohort();
    });
  };

  cls.bindFilterKeyListeners = function ($eventContainer) {
    var self = this;

    $eventContainer.find('.filter-key--input').last().change(function () {
      $(this).parent().find('.filter-value').remove();
      self.renderFilterValue($(this));
    });
  };

  cls.bindAddFilterListener = function ($eventContainer) {
    var self = this;
    $eventContainer.find('.add-filter').click(function () {
      var $filterKey = self.renderFilterKey($eventContainer);
      $filterKey.focus();
    });
  };

  cls.bindRemoveFilterListener = function ($eventContainer) {
    var self = this;
    $eventContainer.find('.remove-filter').last().click(function () {
      var $filters = $(this).parent();
      $filters.remove();
    });
  };

  cls.bindEventSelectorListeners = function ($eventContainer) {
    var self = this;
    $eventContainer.find('.event-type--input').change(function () {
      $eventContainer.find('.filters-container').empty();
    });
  };

  return cls;

});
