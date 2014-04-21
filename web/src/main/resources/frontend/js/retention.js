var Retention = (function () {

  var cls = function () {

  };

  cls.render = function () {
    $('.body-container').html(Mustache.render(retentionTemplate));

    var params = $.deparam(window.location.search.substring(1));
    var retention = params.type === 'retention' ? params : {};

    this.initializeRetentionShowMe(retention);
    this.initializeRetentionDatePickers(retention);
    this.bindInputListeners();
  };

  cls.getRetention = function () {
    var self = this;

    var retention = {
      start_date: Utils.formatDate($('#retentionStartDate').val()),
      end_date: Utils.formatDate($('#retentionEndDate').val()),
      row_event_type: $('.show-me select[name="events"]').eq(0).val(),
      column_event_type: $('.show-me select[name="events"]').eq(1).val(),
      num_days_per_row: $('#daysLater').val(),
      num_columns: 9, //$('#numColumns').val()... Why make things more complicated...
      type: 'retention'
    };

    $('.event-container').each(function (i, event) {
      $(event).find('.filters-container .filters').each(function (j, filters) {
        var $filterValue = $(filters).find('select[name="filterValue"]');
        var $filterKey = $(filters).find('select[name="filterKey"]');
        if ($filterValue.length) {
          var axis = i === 1 ? 'c' : 'r';
          retention[axis + "efk"] = retention[axis + "efk"] || [];
          retention[axis + "efk"] = retention[axis + "efk"].concat($filterKey.val());
          retention[axis + "efv"] = retention[axis + "efv"] || [];
          retention[axis + "efv"] = retention[axis + "efv"].concat($filterValue.val());
        }
      });
    });

    window.history.replaceState({}, '', '/?' + $.param(retention));

    $.ajax({
      type: "GET",
      url: "/events/cohort",
      data: retention
    }).done(function(retention) {
        retention = JSON.parse(retention);
        self.renderGraph(retention);
    });
  };

  cls.renderGraph = function (retention) {
    this.resetRetentionGraph();
    $('.table-container').addClass('rendered');

    $('.date-title').text('Date');
    $('.event-title').text('People');

    var count = 0;
    var currentDate = new Date($('#retentionStartDate').val());
    for (var i = 0; i < retention.length; i++) {
       if (retention[i][0] === 0) break;
       $('.events').append('<div>' + retention[i][0] + '</div>');

       var date = (currentDate.getMonth() + 1) + '/' + currentDate.getDate() + '/' + currentDate.getFullYear();
       $('.dates').append('<div>' + date + '</div>');
       var daysLater = parseInt($('#daysLater').val(), 10);
       currentDate.setDate(currentDate.getDate() + daysLater); // change

       $('.retention').append('<div class="row' + i + '"></div>');
       for (var j = 1; j < retention[i].length; j++) {
          if (retention[i][j] === 0) break;
          if (i === 0) $('.axis').append('<div>' + j + '</div>');
          var percentage = retention[i][j] / retention[i][0] * 100;
          percentage = percentage === 100 ? percentage : percentage.toFixed(2);
          var boxClass = 'gradient-' + parseInt(percentage / 10, 10);
          $('.row' + i).append('<div class="box ' + boxClass + '">' + percentage + '%</div>');
       }
    }

    $('.range-container .spinner').removeClass('rendered');
  };

  cls.resetRetentionGraph = function () {
    $('.events').empty();
    $('.dates').empty();
    $('.retention').empty();
    $('.axis').empty();
  };

  cls.initializeRetentionDaysLater = function (retention) {
    $('#daysLater').val(retention.num_days_per_row || 7);
  };

  cls.initializeRetentionDatePickers = function (retention) {
    var start_date = retention.start_date ? Utils.unFormatDate(retention.start_date) : '01/01/2014';
    var end_date = retention.end_date ? Utils.unFormatDate(retention.end_date) : '01/30/2014';
    $( "#retentionStartDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                           .datepicker('setValue', start_date);
    $( "#retentionEndDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                         .datepicker('setValue', end_date);
  };

  cls.initializeRetentionShowMe = function(retention) {
    var self = this;

    Utils.getEventTypes(function(eventTypes) {
      EVENT_TYPES = JSON.parse(eventTypes);
      Utils.getEventKeys(function () {
        self.bindShowFiltersListener();
        self.renderShowMe(retention);

        var rowEventType = retention.row_event_type || EVENT_TYPES[0];
        $('.show-me select[name="events"]').eq(0).last().val(rowEventType);

        var columnEventType = retention.column_event_type || EVENT_TYPES[1];
        $('.show-me select[name="events"]').eq(1).last().val(columnEventType);

        var showFilters = false;

        ['r', 'c'].forEach(function (axis, i) {
          if (retention[axis + 'efv']) {
            showFilters = true;
            retention[axis + 'efv'].forEach(function (filterValue, j) {
              var $eventContainer = $('.event-container').eq(i);
              self.renderKeyFilter($eventContainer);

              var filterKey = retention[axis + 'efk'][j];
              var $filterKey = $eventContainer.find('select[name="filterKey"]').last();
              $filterKey.val(filterKey);

              self.renderValueFilter($filterKey);
              var $filterValue = $eventContainer.find('select[name="filterValue"]').last();
              $filterValue.val(filterValue);
            });
          }
        });

        $('.event-container select').selectpicker('refresh');
        if (showFilters) $('.retention-filters-toggle').click();
      });
    });
  };

  cls.renderShowMe = function (retention) {
    var view = {
      eventTypes: EVENT_TYPES,
      daysLater: retention.num_days_per_row || 7
    };

    var partials = {
      "event" : eventTemplate,
      "eventType": eventTypeTemplate
    };
    $('.cohort-definition').html(Mustache.render(showMeTemplate, view, partials));
    $('.selectpicker').selectpicker('render');
  };

  cls.renderValueFilter = function ($keyFilter) {
    $eventContainerSelector = $keyFilter.parents().eq(2).find('select[name="events"]')

    var params = {
      event_type: $eventContainerSelector.val(),
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

  cls.renderKeyFilter = function ($eventContainer) {
    var $filtersContainer = $eventContainer.find('.filters-container');
    var $eventContainersSelector = $eventContainer.find('select[name="events"]');

    var view = {
      filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[$eventContainersSelector.val()])
    };
    $filtersContainer.append(Mustache.render(filterKeyTemplate, view));

    this.bindFilterKeyListeners($eventContainer);
    this.bindRemoveFilterListener($eventContainer);
    $('.selectpicker').selectpicker('render');
  };

  cls.bindInputListeners = function () {
    var self = this;

    $('.calculate-retention').click(function () {
      $('.range-container .spinner').addClass('rendered');
      self.getRetention();
    });
  };

  cls.bindShowFiltersListener = function () {
    var self = this;

    $('.retention-filters-toggle').click(function () {
      $(this).addClass('hide');
      $('.cohort-definition').addClass('show-filters');
      $('.event-container').each(function (i, el) {
        self.bindAddFilterListener($(el));
        self.bindEventSelectorListeners($(el));
      });
    });
  };

  cls.bindFilterKeyListeners = function ($eventContainer) {
    var self = this;

    $eventContainer.find('select[name="filterKey"]').last().change(function () {
      $(this).parent().find('.filter-value').remove();
      if ($(this).val() !== 'no filter') {
        self.renderValueFilter($(this));
      }
    });
  };

  cls.bindAddFilterListener = function ($eventContainer) {
    var self = this;
    $eventContainer.find('.add-filter').click(function () {
      self.renderKeyFilter($eventContainer);
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
    $eventContainer.find('select[name="events"]').change(function () {
      $eventContainer.find('.filters-container').empty();
    });
  };

  return cls;

});
