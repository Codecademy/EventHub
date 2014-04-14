var eventTypeTemplate = ' \
  <select class="selectpicker" name="events"> \
     {{#eventTypes}} \
     <option value="{{.}}">{{.}}</option> \
     {{/eventTypes}} \
  </select> \
';