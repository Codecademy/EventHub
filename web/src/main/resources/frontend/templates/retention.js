var retentionTemplate = ' \
<div class="frame retention-show cf"> \
  <h2>Cohort Definition</h2><br> \
  <div class="eventType-container"></div> \
  <div class="retention-filters-toggle filter-toggle">Filters<span class="down-arrow"></span></div> \
  <div class="retention-filters"></div> \
  <div class="range-container"> \
    <h2>Parameters</h2><br> \
    <div class="date-container"> \
      <input type="text" id="retentionStartDate" placeholder="Start Date"> - <input type="text" id="retentionEndDate" placeholder="End Date"> \
    </div> \
    <input class="btn btn-success calculate-btn calculate-retention" type="submit" value="Calculate Retention"> \
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
    <div class="retention-container"> \
      <div class="retention-title"> \
        <div class="axis"></div> \
      </div> \
      <div class="retention"> \
      </div> \
    </div> \
  </div> \
</div> \
'