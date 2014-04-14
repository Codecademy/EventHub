var filterValueTemplate = ' \
  <div class="filter-value"> \
     <select class="selectpicker" name="filterValue"> \
        {{#filterValues}} \
          <option value="{{.}}">{{.}}</option> \
        {{/filterValues}} \
     </select> \
  </div> \
';