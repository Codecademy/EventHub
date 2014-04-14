//===============================================================================

var EVENT_TYPES;
var EVENT_TYPE_KEYS = {};

//===============================================================================

$(document).ready(function() {
    bindNavBar();

    var params = document.location.search;

    if (params.indexOf('type=retention') > -1) {
      $('.nav-retention').click();
    } else {
      $('.nav-funnel').click();
    }
});

function bindNavBar() {
   $('.nav li').click(function () {
      $('.nav li').removeClass('active');
      $(this).addClass('active');
   });
   $('.nav-funnel').click(function () {
      initFunnelShow();
   });
   $('.nav-retention').click(function () {
      initRetentionShow();
   });
}


function getEventTypes(cb) {
    $.ajax({
      type: "GET",
      url: "/events/types",
    }).done(cb);
}

function getEventKey(type) {
  var deferred = $.Deferred();
  $.ajax({
    type: "GET",
    url: 'http://localhost:8080/events/keys?event_type=' + type
  }).done(function (keys) {
    keys = JSON.parse(keys);
    typeKeys = {};
    typeKeys[type] = keys; //I have no idea why {a: [1,2,3]} becomes [1,2,3]. Visit later...
    deferred.resolve(typeKeys);
  })
  return deferred.promise();
}

function getEventKeys(cb) {
  var promises = [];
  EVENT_TYPES.forEach(function (type) {
    promises.push(getEventKey(type))
  });
  $.when.apply($,promises).done(function () {
    [].forEach.call(arguments, function (typeKeys) {
        $.extend(EVENT_TYPE_KEYS, typeKeys)
    });
    cb();
  });
}

function formatDate(date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
}

function unFormatDate(date) {
  return date.substring(4,6) + '/' + date.substring(6,8) + '/' + date.substring(0,4);
}
