var barTemplate = '<div class="bar" style="height: {{height}}%; width: 80px;"><div class="numEvents">{{numEvents}}</div><div class="eventName" style="width: 80px;">{{eventName}}</div></div>';
var spaceTemplate = '<div class="space"><div class="conversion">{{conversion}}%</div></div>';
var stepTemplate ='<div class="step-container cf"><div class="step-index">Step {{index}}</div>{{> eventType}}</div>';
var showMeTemplate = '<div class="show-me">Show me people who did &nbsp {{> eventType}} &nbsp then came back and did &nbsp {{> eventType}} &nbsp within &nbsp <div class="two-digits-container"><input class="two-digits" id="daysLater" type="text" name="daysLater" value="7"></div> &nbsp days.';
var eventTypeTemplate = '<select class="selectpicker" name="events">\n{{#eventTypes}}<option value="{{.}}">{{.}}</option>{{/eventTypes}}\n</select>'

//===============================================================================

$(document).ready(function() {
    bindNavBar();

    var params = document.location.search;

    if (params.indexOf('type=retention') > -1) {
      $('.nav-retention').click();
    } else {
        if (params.indexOf('type=funnel') > -1) {
            initFunnelShow();
        } else {
            $('.nav-funnel').click();
        }
    }
});

function bindNavBar() {
   $('.nav li').click(function () {
      $('.nav li').removeClass('active');
      $(this).addClass('active');
   })
   $('.nav-funnel').click(function () {
      initFunnelCreate();
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

    initializeRetentionDatePickers();
    initializeRetentionEventTypes();
    bindRetentionInputListeners();
}

function getRetention() {
  $.ajax({
    type: "GET",
    url: "http://localhost:8080/events/retention",
    data: {
      start_date: formatDate($('#retentionStartDate').val()),
      end_date: formatDate($('#retentionEndDate').val()),
      row_event_type: $('.show-me select[name="events"]').eq(0).val(),
      column_event_type: $('.show-me select[name="events"]').eq(1).val(),
      num_days_per_row: $('#daysLater').val(),
      num_columns: $('#numColumns').val()
    }
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
     $('.events').append('<div>' + retention[i][0] + '</div>')

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
}

function resetRetentionGraph() {
  $('.events').empty();
  $('.dates').empty();
  $('.retention').empty();
  $('.axis').empty();
}

function initializeRetentionDatePickers() {
    $( "#retentionStartDate" ).datepicker().val('01/01/2013');
    $( "#retentionEndDate" ).datepicker().val('01/30/2013');
}

function initializeRetentionEventTypes() {
    getEventTypes(function(eventTypes) {
        var view = { eventTypes: JSON.parse(eventTypes) };
        var partials = { "eventType": eventTypeTemplate };
        $('.eventType-container').html(Mustache.render(showMeTemplate, view, partials));
        $('.selectpicker').selectpicker('render');
    });
}

function bindRetentionInputListeners() {
    $('.calculate-retention').click(function () {
        getRetention();
    });
}

//===============================================================================

function initFunnelCreate() {
    $('.frame').removeClass('show');
    $('.funnel-create').addClass('show');
    $('.container').addClass('small');

    getEventTypes(function(eventTypes) {
        initializeSteps(eventTypes);
        $('.add-step').off().click(function (e) {
            e.preventDefault();
            addStep(eventTypes);
        });
    });

    $('input[type="submit"]').click(function(e){
        e.preventDefault();
        var steps =  $('select[name="events"]').map(function(i, el) {
            return $(el).val();
        });
        var funnel = {
            name: $('input[name="name"]').val(),
            steps: steps.toArray(),
            type: 'funnel'
        }
        window.location.href = '?' + $.param(funnel);
    });
}


var index = 1;
function addStep(eventTypes) {
    var view = {
        eventTypes: JSON.parse(eventTypes),
        index: index++
    };
    var partials = { "eventType": eventTypeTemplate };
    $('.steps').append(Mustache.render(stepTemplate, view, partials));
    $('.selectpicker').selectpicker('render');
}

function initializeSteps(eventTypes) {
    index = 1;
    $('.steps').empty();
    for (var i = 0; i < 2; i++) {
        addStep(eventTypes);
    }
}

//===============================================================================

function initFunnelShow() {
    $('.frame').removeClass('show');
    $('.funnel-show').addClass('show');
    $('.container').removeClass('small');
    var funnel = $.deparam(window.location.search.substring(1));
    initializeFunnelDatePickers();
    getFunnel(funnel);
}

function getFunnel(funnel) {
    $.ajax({
      type: "GET",
      url: "http://localhost:8080/events/funnel",
      data: {
        start_date: formatDate($('#funnelStartDate').val()),
        end_date: formatDate($('#funnelEndDate').val()),
        funnel_steps: funnel.steps,
        num_days_to_complete_funnel: $('input[name="days"]').val(),
        eck: "event_property_1",
        ecv: 1
      }
    }).done(function(eventVolumes) {
        eventVolumes = JSON.parse(eventVolumes);
        renderCompletionRate(eventVolumes);
        renderFunnelGraph(funnel, eventVolumes);
        bindFunnelInputListeners(funnel);
        renderFunnelName(funnel);
    });
}

function renderFunnelName(funnel) {
    $('.funnel-name').text(funnel.name || 'Unnamed funnel');
}

function bindFunnelInputListeners(funnel) {
    $('.calculate-funnel').click(function () {
        getFunnel(funnel);
    });
}

function initializeFunnelDatePickers() {
    $( "#funnelStartDate" ).datepicker().val('01/01/2013');
    $( "#funnelEndDate" ).datepicker().val('01/30/2013');
}

function renderCompletionRate(eventVolumes) {
    var eventLength = eventVolumes.length;
    var completionRate = (eventVolumes[eventLength - 1] / eventVolumes[0] * 100).toFixed(2);
    $('.completion-rate').html('<span style="font-weight: bold">' + completionRate + '%</span> Completion Rate');
}

function renderFunnelGraph(funnel, eventVolumes) {
    $('.graph').empty();
    var maxEventVolume = Math.max.apply(Math, eventVolumes);
    var diviser = Math.pow(10, (maxEventVolume.toString().length - 2));
    var Y_AXIS_MAX = Math.ceil(maxEventVolume / diviser) * diviser;

    $('.y-value').each(function (i, el) {
        $(el).text(parseInt(Y_AXIS_MAX / 6 * (i + 1), 10));
    });

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
            eventName: funnel.steps[i]
        };
        previousVolume = v;
        $('.graph').append(Mustache.render(barTemplate, view));
    });
}

//===============================================================================

function getEventTypes(cb) {
    $.ajax({
      type: "GET",
      url: "http://localhost:8080/events/types",
    }).done(cb);
}

function formatDate(date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
}