var stepTemplate ='<div><select name="events">\n<option value="event1">event1</option>\n<option value="event2">event2</option>\n<option value="event3">event3</option>\n</select></div>';

$(document).ready(function () {
	$('.add-step').click(function (e) {
		e.preventDefault();
		$('.steps').append(stepTemplate);
	})
})