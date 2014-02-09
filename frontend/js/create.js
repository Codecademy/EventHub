var mockObj = {
    events: ['Event1', 'Event2', 'Event3']
};

var stepTemplate ='<div><select name="events">\n{{#events}}<option value="{{.}}">{{.}}</option>{{/events}}\n</select></div>';

$(document).ready(function () {
    var view = {
        events: mockObj.events
    };
    $('.steps').append(Mustache.render(stepTemplate, view));
	$('.add-step').click(function (e) {
		e.preventDefault();
		$('.steps').append(Mustache.render(stepTemplate, view));
	})
});