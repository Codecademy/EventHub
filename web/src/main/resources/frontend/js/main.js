/*
  TODO
  - Add multiple event filters
  - Add remove filter option
  - Add event filters to URL param
  - Add zero state for when there is no data to display
  - Add default start date to today
  - Change from days to complete: [ ] to [ ] Days to complete Funnel.
  - Componetize UI elements
  - Center align text fields
  - Close date picker on tab
  - Autorefresh option?
*/

var EVENT_TYPES;
var EVENT_TYPE_KEYS = {};

var Retention = new Retention();
var Funnel = new Funnel();
var Utils = new Utils();

$(document).ready(function() {
  bindNavBar();
  handleParams();
});

function bindNavBar() {
  $('.nav li').click(function () {
     $('.nav li').removeClass('active');
     $(this).addClass('active');
  });
  $('.nav-funnel').click(function () {
     Funnel.render();
  });
  $('.nav-retention').click(function () {
     Retention.render();
  });
}

function handleParams() {
  var params = document.location.search;

  if (params.indexOf('type=retention') > -1) {
    $('.nav-retention').click();
  } else {
    $('.nav-funnel').click();
  }
}
