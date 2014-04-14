var showMeTemplate = ' \
  <div class="show-me"> \
    Show me people who did \
    &nbsp {{> eventType}} &nbsp \
    then came back and did \
    &nbsp {{> eventType}} &nbsp \
    using \
    &nbsp \
    <span> \
      <input class="two-digits" id="daysLater" type="text" name="daysLater" value="{{daysLater}}"> \
    </span> \
    &nbsp day cohorts.\
  </div> \
';