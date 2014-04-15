var filterKeyTemplate = ' \
  <div class="filters"> \
     <select class="selectpicker" name="filterKey"> \
        {{#filterKeys}} \
        <option value="{{.}}">{{.}}</option> \
        {{/filterKeys}} \
     </select> \
     <div class="remove remove-filter"><span class="glyphicon glyphicon-remove"></span></div> \
  </div> \
';