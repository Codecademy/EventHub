//===============================================================================

var EVENT_TYPES;
var EVENT_TYPE_KEYS = {};

//===============================================================================

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
