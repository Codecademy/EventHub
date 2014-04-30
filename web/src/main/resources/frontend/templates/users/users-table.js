var usersTableTemplate = '\
  <table class="table table-hover text--light"> \
    <tbody> \
      {{#table}} \
      <tr data-user={{user}}> \
        <td class="index">{{index}}</td> \
        <td class="user">{{user}}</td> \
      </tr> \
      {{/table}} \
    </tbody> \
  </table> \
'