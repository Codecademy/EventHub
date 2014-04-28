var usersTableTemplate = '\
  <table class="table table-hover"> \
    <tbody> \
      {{#table}} \
      <tr data-user={{user}}> \
        <td>{{index}}</td> \
        <td>{{user}}</td> \
      </tr> \
      {{/table}} \
    </tbody> \
  </table> \
'