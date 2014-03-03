var barTemplate = '<div class="bar" style="height: {{height}}%; width: 85px;"><div class="numEvents">{{numEvents}}</div><div class="eventName" style="width: 80px;">{{eventName}}</div></div>';
var spaceTemplate = '<div class="space"><div class="conversion-container"><div class="conversion">{{conversion}}%</div></div></div>';
var stepTemplate ='<div class="step-container">{{> eventType}}<div class="remove-step"><span class="glyphicon glyphicon-remove"></span></div><div class="next-step"><span class="glyphicon glyphicon-arrow-right"></div></div>';
var showMeTemplate = '<div class="show-me">Show me people who did &nbsp {{> eventType}} &nbsp then came back and did &nbsp {{> eventType}} &nbsp within &nbsp <div class="two-digits-container"><input class="two-digits" id="daysLater" type="text" name="daysLater" value="{{daysLater}}"></div> &nbsp days.';
var eventTypeTemplate = '<select class="selectpicker" name="events">\n{{#eventTypes}}<option value="{{.}}">{{.}}</option>{{/eventTypes}}\n</select>';

//===============================================================================

var EVENT_TYPES;

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
  window.history.replaceState({}, '', '/?' + $.param(retention));
  $.ajax({
    type: "GET",
    url: "/events/retention",
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
    $( "#retentionStartDate" ).datepicker().val(start_date);
    $( "#retentionEndDate" ).datepicker().val(end_date);
}

function initializeRetentionShowMe(retention) {
    if (!EVENT_TYPES) {
      getEventTypes(function(eventTypes) {
          EVENT_TYPES = JSON.parse(eventTypes);
          renderShowMe(retention);
      });
    } else {
      renderShowMe(retention);
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

function bindRetentionInputListeners() {
    $('.calculate-retention').click(function () {
        $('.range-container .spinner').addClass('rendered');
        getRetention();
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
    bindAddStepListener();
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
  var funnelSteps = $('.funnel-steps select').map(function(i, el) {
    return $(el).val();
  }).toArray();
  return funnelSteps;
}

function renderFunnelName(funnel) {
    $('.funnel-name').text(funnel.name || 'Unnamed funnel');
}

function bindAddStepListener() {
  $('.add-step').off().click(function () {
    addStep();
  });
}

function bindRemoveStepListener() {
  $(document.body).off().on('click', '.remove-step', function () {
    $(this).parent().remove();
  });
}

function bindFunnelInputListeners() {
    $('.calculate-funnel').off().click(function () {
        $('.funnel-inputs .spinner').addClass('rendered');
        getFunnel();
    });
}

function addStep() {
  view = {
    eventTypes: EVENT_TYPES
  };
  var partials = { "eventType": eventTypeTemplate };
  $('.funnel-show .funnel-steps').append(Mustache.render(stepTemplate, view, partials));
  $('.selectpicker').selectpicker('render');
}

function initializeFunnelSteps(funnel) {
  $('.funnel-show .funnel-steps').empty();
  getEventTypes(function (eventTypes) {
    EVENT_TYPES = JSON.parse(eventTypes);
    funnel.steps = funnel.funnel_steps || [EVENT_TYPES[0], EVENT_TYPES[1]];
    funnel.steps.forEach(function (v, i) {
      addStep();
      $('.funnel-show select').last().val(v);
      $('.funnel-show select').selectpicker('refresh');
    });
  });
}

function initializeDaysToComplete(funnel) {
  $('#daysToComplete').val(funnel.num_days_to_complete_funnel || 7);
}

function initializeFunnelDatePickers(funnel) {
    var start_date = funnel.start_date ? unFormatDate(funnel.start_date) : '01/01/2013';
    var end_date = funnel.end_date ? unFormatDate(funnel.end_date) : '01/30/2013';
    $( "#funnelStartDate" ).datepicker().val(start_date);
    $( "#funnelEndDate" ).datepicker().val(end_date);
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

function formatDate(date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
}

function unFormatDate(date) {
  return date.substring(4,6) + '/' + date.substring(6,8) + '/' + date.substring(0,4);
}
