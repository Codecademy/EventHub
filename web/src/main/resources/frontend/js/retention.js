var Retention = (function () {

  var cls = function () {

  };

  cls.render = function () {
    var self = this;

    var params = $.deparam(window.location.search.substring(1));
    var retention = params.type === 'retention' ? params : {};

    $('.body-container').html(Mustache.render(retentionTemplate));

    this.initializeRetentionShowMe(retention);
    this.initializeRetentionDatePickers(retention);
    this.bindInputListeners();
  };

  cls.getRetention = function () {
    var self = this;

    var retention = {
      start_date: Utils.formatDate($('#retentionStartDate').val()),
      end_date: Utils.formatDate($('#retentionEndDate').val()),
      row_event_type: $('.show-me .event-type--input').eq(0).val(),
      column_event_type: $('.show-me .event-type--input').eq(1).val(),
      num_days_per_row: $('#daysLater').val(),
      num_columns: 11, //$('#numColumns').val()... Why make things more complicated...
      type: 'retention'
    };

    $('.event-container').each(function (i, event) {
      $(event).find('.filters-container .filters').each(function (j, filters) {
        var $filterValue = $(filters).find('.filter-value--input');
        var $filterKey = $(filters).find('.filter-key--input');
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
       $('.events').append('<div>' + retention[i][0] + '</div>');

       var date = (currentDate.getMonth() + 1) + '/' + currentDate.getDate() + '/' + currentDate.getFullYear();
       $('.dates').append('<div>' + date + '</div>');
       var daysLater = parseInt($('#daysLater').val(), 10);
       currentDate.setDate(currentDate.getDate() + daysLater); // change

       $('.retention').append('<div class="row' + i + '"></div>');
       for (var j = 1; j < retention[i].length; j++) {
          if (i === 0) $('.axis').append('<div>' + j + '</div>');
          var percentage = retention[i][j] === 0 ? 0 : retention[i][j] / retention[i][0] * 100;
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
        self.renderShowMe(retention);

        var rowEventType = retention.row_event_type || EVENT_TYPES[0];
        $('.show-me .event-type--input').eq(0).last().val(rowEventType);

        var columnEventType = retention.column_event_type || EVENT_TYPES[1];
        $('.show-me .event-type--input').eq(1).last().val(columnEventType);

        ['r', 'c'].forEach(function (axis, i) {
          if (retention[axis + 'efv']) {
            retention[axis + 'efv'].forEach(function (filterValue, j) {
              var $eventContainer = $('.event-container').eq(i);
              self.renderFilterKey($eventContainer);

              var filterKey = retention[axis + 'efk'][j];
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

  cls.renderShowMe = function (retention) {
    var self = this;

    var view = {
      eventTypes: EVENT_TYPES,
      daysLater: retention.num_days_per_row || 7
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

    $filterValue.typeahead({
      source: function (query, process) {
        if (query) {
          params.prefix = query;
          $.ajax({
            type: "GET",
            url: "/events/values?" + $.param(params)
          }).done(function(values) {
            values = JSON.parse(values);
            process(values);
          });
        } else {
          process([]);
        }
      },
      items: 10000
    });

    if (cb) cb($filterValue);
  };

  cls.renderFilterKey = function ($eventContainer) {
    var $filtersContainer = $eventContainer.find('.filters-container');
    var $eventSelector = $eventContainer.find('.event-type--input');

    $filtersContainer.append(Mustache.render(filterKeyTemplate));
    $filtersContainer.find('.filter-key--input').last().typeahead({
      source: ['no filter'].concat(EVENT_TYPE_KEYS[$eventSelector.val()]),
      items: 10000
    });

    this.bindFilterKeyListeners($eventContainer);
    this.bindRemoveFilterListener($eventContainer);
  };

  cls.bindInputListeners = function () {
    var self = this;

    $('.calculate-retention').click(function () {
      $('.range-container .spinner').addClass('rendered');
      self.getRetention();
    });
  };

  cls.bindFilterKeyListeners = function ($eventContainer) {
    var self = this;

    $eventContainer.find('.filter-key--input').last().change(function () {
      $(this).parent().find('.filter-value').remove();
      if ($(this).val() !== 'no filter') {
        self.renderFilterValue($(this));
      }
    });
  };

  cls.bindAddFilterListener = function ($eventContainer) {
    var self = this;
    $eventContainer.find('.add-filter').click(function () {
      self.renderFilterKey($eventContainer);
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
