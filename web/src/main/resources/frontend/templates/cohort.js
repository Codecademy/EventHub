var cohortTemplate = ' \
<div class="cohort-show cf"> \
  <h2>Cohort Definition</h2><br> \
  <div class="cohort-definition"></div> \
  <div class="range-container"> \
    <h2>Parameters</h2><br> \
    <div class="date-container"> \
      <input type="text" id="cohortStartDate" placeholder="Start Date"> - <input type="text" id="cohortEndDate" placeholder="End Date"> \
    </div> \
    <input class="btn btn-success calculate-btn calculate-cohort" type="submit" value="Calculate"> \
    <img class="spinner" src="spinner.gif"> \
  </div> \
  <div class="cf table-container"> \
    <div class="dates-column-container"> \
      <div class="date-title"></div> \
      <div class="dates"> \
      </div> \
    </div> \
    <div class="events-column-container"> \
      <div class="event-title"></div> \
      <div class="events"> \
      </div> \
    </div> \
    <div class="cohort-container"> \
      <div class="cohort-title"> \
        <div class="axis"></div> \
      </div> \
      <div class="cohort"> \
      </div> \
    </div> \
  </div> \
</div> \
'