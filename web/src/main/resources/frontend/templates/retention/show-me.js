var showMeTemplate = ' \
  <div class="show-me"> \
    <p>Show me people who did</p> \
    &nbsp {{> event}} &nbsp \
    <p>then came back and did</p> \
    &nbsp {{> event}} &nbsp \
    <p>using</p> \
    &nbsp \
    <span> \
      <input class="two-digits" id="daysLater" type="text" name="daysLater" value="{{daysLater}}"> \
    </span> \
    &nbsp <p>day cohorts.</p>\
  </div> \
';