var userShowTemplate = '\
  <div class="user-show--row cf"> \
    <div class="user-properties"> \
      <div class="user-properties--title">User Properties</div> \
      <br> \
      {{#properties}} \
        <dl> \
          <dt class="property-name text--light">{{propertyName}}</dt>: \
          <dd class="property-value text--light">{{propertyValue}}</dd> \
        </dl> \
        <br> \
      {{/properties}} \
    </div> \
    <div class="user-show--activity-feed"> \
      <div class="activity-feed--title">Activity Feed</div> \
      <table class="table text--light"> \
        <tbody> \
          {{#timeline}} \
          <tr> \
            <td>{{date}}</td> \
            <td>{{event_type}}</td> \
          </tr> \
          {{/timeline}} \
          {{^timeline}} \
            No user timeline available \
          {{/timeline}} \
        </tbody> \
      </table> \
    </div> \
  </div> \
'