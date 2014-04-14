function initRetentionShow() {
  $('.body-container').html(Mustache.render(retentionTemplate));

  $('.retention-show').addClass('show');

  var params = $.deparam(window.location.search.substring(1));
  var retention = params.type === 'retention' ? params : {};

  initializeRetentionShowMe(retention);
  initializeRetentionDatePickers(retention);
  bindRetentionInputListeners();
}

function getRetention() {
  var retention = {
    start_date: formatDate($('#retentionStartDate').val()),
    end_date: formatDate($('#retentionEndDate').val()),
    row_event_type: $('.show-me select[name="events"]').eq(0).val(),
    column_event_type: $('.show-me select[name="events"]').eq(1).val(),
    num_days_per_row: $('#daysLater').val(),
    num_columns: 9, //$('#numColumns').val()... Why make things more complicated...
    type: 'retention'
  };

  $('.retention-filters select[name="filterValue"]').each(function (i, select) {
    var selectorIndex = $(select).data('index');
    if (selectorIndex === 0) {
      retention["cefk"] = $('.retention-filters select[name="filterKey"]').eq(selectorIndex).val();
      retention["cefv"] = $(this).val();
    } else {
      retention["refk"] = $('.retention-filters select[name="filterKey"]').eq(selectorIndex).val();
      retention["refv"] = $(this).val();
    }
  });

  window.history.replaceState({}, '', '/?' + $.param(retention));
  $.ajax({
    type: "GET",
    url: "/events/cohort",
    data: retention
  }).done(function(retention) {
      retention = JSON.parse(retention);
      renderRetentionGraph(retention);
  });
}

function renderRetentionGraph(retention) {
  resetRetentionGraph();
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
}

function resetRetentionGraph() {
  $('.events').empty();
  $('.dates').empty();
  $('.retention').empty();
  $('.axis').empty();
}

function initializeRetentionDaysLater(retention) {
  $('#daysLater').val(retention.num_days_per_row || 7);
}

function initializeRetentionDatePickers(retention) {
  var start_date = retention.start_date ? unFormatDate(retention.start_date) : '01/01/2013';
  var end_date = retention.end_date ? unFormatDate(retention.end_date) : '01/30/2013';
  $( "#retentionStartDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                         .datepicker('setValue', start_date);
  $( "#retentionEndDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                       .datepicker('setValue', end_date);
}

function initializeRetentionShowMe(retention) {
  if (!EVENT_TYPES) {
    getEventTypes(function(eventTypes) {
        EVENT_TYPES = JSON.parse(eventTypes);
        renderShowMe(retention);
        getEventKeys(bindRetentionAddFiltersListener);
    });
  } else {
    renderShowMe(retention);
    bindRetentionAddFiltersListener();
  }
}

function renderShowMe(retention) {
  var view = {
    eventTypes: EVENT_TYPES,
    daysLater: retention.num_days_per_row || 7
  };
  var partials = { "eventType": eventTypeTemplate };
  $('.eventType-container').html(Mustache.render(showMeTemplate, view, partials));

  var rowEventType = retention.row_event_type || EVENT_TYPES[0];
  var columnEventType = retention.column_event_type || EVENT_TYPES[1];
  $('.show-me select[name="events"]').eq(0).last().val(rowEventType);
  $('.show-me select[name="events"]').eq(1).last().val(columnEventType);
  $('.selectpicker').selectpicker('render');
}

function renderRetentionFilters() {
  var firstEventType = $('.selectpicker[name="events"]').eq(0).val();
  var secondEventType = $('.selectpicker[name="events"]').eq(1).val();

  var view = {
    filterValues: ['filterValue']
  };
  view.filterKeys = ['no filter'].concat(EVENT_TYPE_KEYS[firstEventType])
  var firstFilter = Mustache.render(filterKeyTemplate, view);

  view.filterKeys = ['no filter'].concat(EVENT_TYPE_KEYS[secondEventType])
  var secondFilter = Mustache.render(filterKeyTemplate, view);
  $('.retention-filters').html(firstFilter + secondFilter)
                         .addClass('show-filters');

  $('.retention-filters .filters').eq(0).find('select[name="filterKey"]').data('type', firstEventType);
  $('.retention-filters .filters').eq(1).find('select[name="filterKey"]').data('type', secondEventType);
  $('.show-me select[name="events"]').eq(0).data('index', 0);
  $('.show-me select[name="events"]').eq(1).data('index', 1);
  $('.selectpicker').selectpicker('render');
}

function renderRetentionValueFilter($keyFilter) {
  var params = {
    event_type: $keyFilter.data('type'),
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
    $('.retention-filters select[name="filterValue"]').eq(0).data('index', 0);
    $('.retention-filters select[name="filterValue"]').eq(1).data('index', 1); // refactor this later.
  });
}

function bindRetentionInputListeners() {
  $('.calculate-retention').click(function () {
    $('.range-container .spinner').addClass('rendered');
    getRetention();
  });
}

function bindRetentionAddFiltersListener() {
  $('.retention-filters-toggle').click(function () {
    $(this).addClass('hide');
    renderRetentionFilters();
    bindRetentionFilterKeyListeners();
    bindRetentionEventListeners();
  });
}

function bindRetentionFilterKeyListeners() {
  $('select[name="filterKey"]').off().change(function () {
    $(this).parent().find('.filter-value').remove();
    if ($(this).val() !== 'no filter') {
      renderRetentionValueFilter($(this));
    }
  });
}

function bindRetentionEventListeners() {
  $('select[name="events"]').change(function () {
    var view = {
      filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[$(this).val()])
    };
    var selectorIndex = $(this).data('index');
    if (selectorIndex === 1) {
      $('.retention-filters .filters').eq(1).remove();
      $('.retention-filters').append(Mustache.render(filterKeyTemplate, view));
    } else {
      $('.retention-filters .filters').eq(0).remove();
      $('.retention-filters').prepend(Mustache.render(filterKeyTemplate, view));
    }
    $('.selectpicker').selectpicker('render');
    $('.retention-filters .filters').eq(selectorIndex).find('select[name="filterKey"]').data('type', $(this).val());
    bindRetentionFilterKeyListeners();
  });
}
