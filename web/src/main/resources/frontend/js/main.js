var barTemplate = '<div class="bar" style="height: {{height}}%; width: 85px;"><div class="numEvents">{{numEvents}}</div><div class="eventName" style="width: 80px;">{{eventName}}</div></div>';
var spaceTemplate = '<div class="space"><div class="conversion-container"><div class="conversion">{{conversion}}%</div></div></div>';
var stepTemplate ='<div class="step-container">{{> eventType}}<div class="remove-step"><span class="glyphicon glyphicon-remove"></span></div><div class="next-step"><span class="glyphicon glyphicon-arrow-right"></div>{{> filters}}</div>';
var showMeTemplate = '<div class="show-me">Show me people who did &nbsp {{> eventType}} &nbsp then came back and did &nbsp {{> eventType}} &nbsp using &nbsp <div class="two-digits-container"><input class="two-digits" id="daysLater" type="text" name="daysLater" value="{{daysLater}}"></div> &nbsp day cohorts.';
var eventTypeTemplate = '<select class="selectpicker" name="events">\n{{#eventTypes}}<option value="{{.}}">{{.}}</option>{{/eventTypes}}\n</select>';
var filterKeyTemplate = '<div class="filters"><select class="selectpicker" name="filterKey">\n{{#filterKeys}}<option value="{{.}}">{{.}}</option>{{/filterKeys}}\n</select></div>'
var filterValueTemplate = '<div class="filter-value"><select class="selectpicker" name="filterValue">\n{{#filterValues}}<option value="{{.}}">{{.}}</option>{{/filterValues}}\n</select></div>'
var addStepTemplate = '<div class="add-step"><span class="glyphicon glyphicon-plus"></span></div>'

//===============================================================================

var EVENT_TYPES;
var EVENT_TYPE_KEYS = {};

//===============================================================================

$(document).ready(function() {
    bindNavBar();

    var params = document.location.search;

    if (params.indexOf('type=retention') > -1) {
      $('.nav-retention').click();
    } else {
      $('.nav-funnel').click();
    }
});

function bindNavBar() {
   $('.nav li').click(function () {
      $('.nav li').removeClass('active');
      $(this).addClass('active');
   });
   $('.nav-funnel').click(function () {
      initFunnelShow();
   });
   $('.nav-retention').click(function () {
      initRetentionShow();
   });
}

//===============================================================================

function initRetentionShow() {
    $('.frame').removeClass('show');
    $('.retention-show').addClass('show');
    $('.container').removeClass('small');

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

//===============================================================================

// var funnel = {
//     name: $('input[name="name"]').val(),
//     steps: steps.toArray(),
//     type: 'funnel'
// };
// window.location.href = '?' + $.param(funnel);

//===============================================================================

function initFunnelShow() {
    $('.frame').removeClass('show');
    $('.funnel-show').addClass('show');
    $('.container').removeClass('small');
    var params = $.deparam(window.location.search.substring(1));
    var funnel = params.type === 'funnel' ? params : {};
    initializeFunnelSteps(funnel);
    initializeFunnelDatePickers(funnel);
    initializeDaysToComplete(funnel)
    bindFunnelInputListeners();
    bindRemoveStepListener();
}

function getFunnel() {
    var funnel = {
      start_date: formatDate($('#funnelStartDate').val()),
      end_date: formatDate($('#funnelEndDate').val()),
      funnel_steps: getFunnelSteps(),
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
        renderCompletionRate(eventVolumes);
        renderFunnelGraph(eventVolumes);
    });
}

function getFunnelSteps() {
  var funnelSteps = $('.funnel-steps select[name="events"]').map(function(i, el) {
    return $(el).val();
  }).toArray();
  return funnelSteps;
}

function bindFunnelAddFiltersListener() {
  $('.funnel-filters-toggle').click(function () {
    $(this).addClass('hide');
    $('.funnel-steps').addClass('show-filters');
    bindFunnelFilterKeyListeners();
  });
}

function bindAddStepListener() {
  $('.add-step').off().click(function () {
    addStep();
  });
}

function bindRemoveStepListener() {
  $(document.body).off().on('click', '.remove-step', function () {
    $('.add-step').css('display', 'inline-block');
    $(this).parent().remove();
  });
}

function bindFunnelInputListeners() {
  $('.calculate-funnel').off().click(function () {
    $('.funnel-inputs .spinner').addClass('rendered');
    getFunnel();
  });
}

function bindFunnelFilterKeyListeners() {
  $('select[name="filterKey"]').off().change(function () {
    $(this).parent().find('.filter-value').remove();
    if ($(this).val() !== 'no filter') {
      renderFunnelValueFilter($(this));
    }
  });
}

function bindFunnelEventListeners() {
  $('select[name="events"]').change(function () {
    $(this).parent().find('.filters').remove();
    var view = {
      filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[$(this).val()])
    };
    $(this).parent().append(Mustache.render(filterKeyTemplate, view));
    $('.selectpicker').selectpicker('render');
    bindFunnelFilterKeyListeners();
  });
}

function addStep() {
  view = {
    eventTypes: EVENT_TYPES,
    filterKeys: ['no filter'].concat(EVENT_TYPE_KEYS[EVENT_TYPES[0]]),
  };
  var partials = {
    "eventType": eventTypeTemplate,
    "filters": filterKeyTemplate
  };
  $('.steps-container').append(Mustache.render(stepTemplate, view, partials));
  $('.selectpicker').selectpicker('render');
  bindFunnelEventListeners();
  if ($('.step-container').length === 5) $('.add-step').css('display', 'none');
}

function initializeFunnelSteps(funnel) {
  $('.steps-container').empty();
  $('.add-step').remove();
  getEventTypes(function (eventTypes) {
    EVENT_TYPES = JSON.parse(eventTypes);
    getEventKeys(function () {
      funnel.steps = funnel.funnel_steps || [EVENT_TYPES[0], EVENT_TYPES[1]];
      funnel.steps.forEach(function (v, i) {
        addStep();
        $('.funnel-show select').last().val(v);
        $('.funnel-show select').selectpicker('refresh');
      });
      renderAddFunnelStep();
      bindAddStepListener();
      bindFunnelAddFiltersListener();
    });
  });
}

function initializeDaysToComplete(funnel) {
  $('#daysToComplete').val(funnel.num_days_to_complete_funnel || 7);
}

function initializeFunnelDatePickers(funnel) {
    var start_date = funnel.start_date ? unFormatDate(funnel.start_date) : '01/01/2013';
    var end_date = funnel.end_date ? unFormatDate(funnel.end_date) : '01/30/2013';
    $( "#funnelStartDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                        .datepicker('setValue', start_date);
    $( "#funnelEndDate" ).datepicker().on('changeDate', function () { $(this).datepicker('hide'); })
                                      .datepicker('setValue', end_date);
}

function renderFunnelValueFilter($keyFilter) {
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
}

function renderAddFunnelStep() {
  $('.funnel-steps').append(Mustache.render(addStepTemplate));
}

function renderCompletionRate(eventVolumes) {
    var eventLength = eventVolumes.length;
    var completionRate = (eventVolumes[eventLength - 1] / eventVolumes[0] * 100).toFixed(2);
    $('.completion-rate').html('<span style="font-weight: bold">' + completionRate + '%</span> Completion Rate');
}

function renderFunnelGraph(eventVolumes) {
    $('.middle').addClass('rendered');
    $('.graph').empty();
    var maxEventVolume = Math.max.apply(Math, eventVolumes);
    var diviser = Math.pow(10, (maxEventVolume.toString().length - 2));
    var Y_AXIS_MAX = Math.ceil(maxEventVolume / diviser) * diviser;

    $('.y-value').each(function (i, el) {
        $(el).text(parseInt(Y_AXIS_MAX / 6 * (i + 1), 10));
    });

    var funnelSteps = getFunnelSteps();
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

//===============================================================================

function getEventTypes(cb) {
    $.ajax({
      type: "GET",
      url: "/events/types",
    }).done(cb);
}

function getEventKey(type) {
  var deferred = $.Deferred();
  $.ajax({
    type: "GET",
    url: 'http://localhost:8080/events/keys?event_type=' + type
  }).done(function (keys) {
    keys = JSON.parse(keys);
    typeKeys = {};
    typeKeys[type] = keys; //I have no idea why {a: [1,2,3]} becomes [1,2,3]. Visit later...
    deferred.resolve(typeKeys);
  })
  return deferred.promise();
}

function getEventKeys(cb) {
  var promises = [];
  EVENT_TYPES.forEach(function (type) {
    promises.push(getEventKey(type))
  });
  $.when.apply($,promises).done(function () {
    [].forEach.call(arguments, function (typeKeys) {
        $.extend(EVENT_TYPE_KEYS, typeKeys)
    });
    cb();
  });
}

function formatDate(date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
}

function unFormatDate(date) {
  return date.substring(4,6) + '/' + date.substring(6,8) + '/' + date.substring(0,4);
}
