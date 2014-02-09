var barTemplate = '<div class="bar" style="height: {{height}}%; width: 50px;"><div class="numEvents">{{numEvents}}</div><div class="eventName" style="width: 50px;">{{eventName}}</div></div>'
var spaceTemplate = '<div class="space"><div class="conversion">{{conversion}}%</div></div>'

var mockObj = {
	events: [{
			name: 'Event1',
			volume: 8001
		}, {
			name: 'Event2',
			volume: 5000
		}, {
			name: 'Event3',
			volume: 2000
		}, {
			name: 'Event4',
			volume: 1000
		}
	]
};

var maxEventVolume = Math.max.apply(Math, mockObj.events.map(function(e){return e.volume;}));
var diviser = Math.pow(10, (maxEventVolume.toString().length - 2));
var Y_AXIS_MAX = Math.ceil(maxEventVolume / diviser) * diviser;

$(document).ready(function() {
    $( "#startDate" ).datepicker();
    $( "#endDate" ).datepicker();

    $('.y-value').each(function (i, el) {
        $(el).text(parseInt(Y_AXIS_MAX / 6 * (i + 1)));
    });

    var eventLength = mockObj.events.length;
    var completionRate = (mockObj.events[eventLength - 1].volume / mockObj.events[0].volume * 100).toFixed(2);
    $('.completion-rate').text(completionRate + '% Completion Rate');

    var previousVolume;
    mockObj.events.forEach(function (e, i) {
        if (i > 0) {
            view = {
                conversion: parseInt(e.volume / previousVolume * 100)
            };
            $('.graph').append(Mustache.render(spaceTemplate, view));
        }
        var view = {
            height: (e.volume / Y_AXIS_MAX * 100),
            numEvents: e.volume,
            eventName: e.name
        };
        previousVolume = e.volume;
        $('.graph').append(Mustache.render(barTemplate, view));
    });
});