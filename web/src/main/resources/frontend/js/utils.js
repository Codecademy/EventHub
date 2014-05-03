var Utils = (function () {

  var cls = function () {

  };

  cls.getEventTypes = function (cb) {
    $.ajax({
      type: "GET",
      url: "/events/types",
    }).done(cb);
  };

  cls.getEventKey = function(type) {
    var deferred = $.Deferred();
    $.ajax({
      type: "GET",
      url: '/events/keys?event_type=' + type
    }).done(function (keys) {
      keys = JSON.parse(keys);
      typeKeys = {};
      typeKeys[type] = keys; //I have no idea why {a: [1,2,3]} becomes [1,2,3]. Visit later...
      deferred.resolve(typeKeys);
    });
    return deferred.promise();
  };

  cls.getEventKeys = function(cb) {
    var self = this;
    var promises = [];

    EVENT_TYPES.forEach(function (type) {
      promises.push(self.getEventKey(type))
    });
    $.when.apply($,promises).done(function () {
      [].forEach.call(arguments, function (typeKeys) {
          $.extend(EVENT_TYPE_KEYS, typeKeys)
      });
      cb();
    });
  }

  cls.formatDate = function (date) {
    date = date.split('/');
    return date[2] + date[0] + date[1];
  };

  cls.unFormatDate = function (date) {
    return date.substring(4,6) + '/' + date.substring(6,8) + '/' + date.substring(0,4);
  };

  cls.getCurrentDate = function () {
    var date = new Date()
    var month = (date.getMonth() + 1) < 10 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1);
    var day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
    var year = date.getFullYear();
    return month + '/' + day + '/' + year;
  };

  cls.getOneWeekAgoDate = function () {
    var date = new Date();
    date.setDate(date.getDate() - 7);
    var month = (date.getMonth() + 1) < 10 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1);
    var day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
    var year = date.getFullYear();
    return month + '/' + day + '/' + year;
  };

  return cls;
});