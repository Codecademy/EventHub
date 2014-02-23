var barTemplate = '<div class="bar" style="height: {{height}}%; width: 50px;"><div class="numEvents">{{numEvents}}</div><div class="eventName" style="width: 50px;">{{eventName}}</div></div>';
var spaceTemplate = '<div class="space"><div class="conversion">{{conversion}}%</div></div>';
var stepTemplate ='<div><select name="events">\n{{#eventTypes}}<option value="{{.}}">{{.}}</option>{{/eventTypes}}\n</select></div>';

//===============================================================================

$(document).ready(function() {
    if (document.location.search.length) {
        initFunnelShow();
    } else {
        initFunnelCreate();
    }
});

//===============================================================================

function initFunnelCreate() {
    $('.funnel-create').addClass('show');
    $.ajax({
      type: "GET",
      url: "http://localhost:8080/events/types",
    }).done(function(eventTypes) {
        initializeSteps(eventTypes);
        $('.add-step').click(function (e) {
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
            steps: steps.toArray()
        }
        window.location.href = '?' + $.param(funnel);
    });
}


function addStep(eventTypes) {
    var view = {
        eventTypes: JSON.parse(eventTypes)
    };
    $('.steps').append(Mustache.render(stepTemplate, view));
}

function initializeSteps(eventTypes) {
    for (var i = 0; i < 3; i++) {
        addStep(eventTypes);
    }
}

//===============================================================================

function initFunnelShow() {
    $('.funnel-show').addClass('show');
    var funnel = $.deparam(window.location.search.substring(1));
    initializeDatePickers();
    getFunnel(funnel);
}

function getFunnel(funnel) {
    $.ajax({
      type: "GET",
      url: "http://localhost:8080/events/funnel",
      data: {
        start_date: formatDate($('#startDate').val()),
        end_date: formatDate($('#endDate').val()),
        funnel_steps: funnel.steps,
        num_days_to_complete_funnel: $('input[name="days"]').val(),
        eck: "event_property_1",
        ecv: 1
      }
    }).done(function(eventVolumes) {
        eventVolumes = JSON.parse(eventVolumes);
        renderCompletionRate(eventVolumes);
        renderFunnelGraph(funnel, eventVolumes);
        bindInputListeners(funnel);
    });
}

function bindInputListeners(funnel) {
    $('input').change(function () {
        getFunnel(funnel);
    });
}

function initializeDatePickers() {
    $( "#startDate" ).datepicker().val('01/01/2013');
    $( "#endDate" ).datepicker().val('01/30/2013');
}

function renderCompletionRate(eventVolumes) {
    var eventLength = eventVolumes.length;
    var completionRate = (eventVolumes[eventLength - 1] / eventVolumes[0] * 100).toFixed(2);
    $('.completion-rate').text(completionRate + '% Completion Rate');
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

function formatDate(date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
}