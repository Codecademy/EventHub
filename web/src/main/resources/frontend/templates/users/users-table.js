var usersTableTemplate = '\
  <table class="table table-hover text--light"> \
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