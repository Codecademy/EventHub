var userShowTemplate = '\
  <div class="user-show--row cf"> \
    <div class="user-properties"> \
      {{#properties}} \
        <div> \
          <div class="property-name">{{propertyName}}</div>: \
          <div class="property-value">{{propertyValue}}</div> \
        </div> \
        <br> \
      {{/properties}} \
    </div> \
    <div class="user-show--activity-feed"> \
      <div class="activity-feed--title">Activity Feed</div> \
      <table class="table"> \
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