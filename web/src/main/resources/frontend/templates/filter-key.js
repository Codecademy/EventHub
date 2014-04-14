var filterKeyTemplate = ' \
  <div class="filters"> \
     <select class="selectpicker" name="filterKey"> \
        {{#filterKeys}} \
        <option value="{{.}}">{{.}}</option> \
        {{/filterKeys}} \
     </select> \
  </div> \
';