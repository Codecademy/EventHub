(function (window) {

  var StorageQueue = function (name) {
    this.key = name + "::Queue";
  };

  (function () { // merge to StorageQueue.prototype
    this._getQueue = function () {
      return JSON.parse(localStorage[this.key] || "[]");
    };

    this._setQueue = function (queue) {
      localStorage.setItem(this.key, JSON.stringify(queue));
    };

    this.enqueue = function (element) {
      this._setQueue(this._getQueue().concat(element));
    };

    this.dequeue = function () {
      var queue   = this._getQueue();
      var element = queue.splice(0,1)[0];

      this._setQueue(queue);
      return element;
    };

    this.peek = function () {
      return this._getQueue()[0];
    };
  }).call(StorageQueue.prototype);

  var EventTracker = function (name, queue, options) {
    this.name = name;
    this.queue = queue;

    options = options || {};
    this.url = options.url || "";
    this.flushInterval = (options.flushInterval || 10) * 1000;

    this.sessionKey = name + "::activeSession";
    this.generatedIdKey = name + "::generatedId";
    this.identifiedUserkey = name + "::identifiedUser";
    this.registeredUserkey = name + "::registeredUser";

    if (!sessionStorage[this.sessionKey]) {
      sessionStorage[this.sessionKey] = true;
      localStorage[this.genereratedIdKey] = this._generateId();
    }
  };

  (function () { // merge to EventTracker.prototype
    this._generateId = function () {
      return Math.random().toString(36).substr(2, 9);
    };

    this._flush = function () {
      var nextEvent = this.queue.peek();
      if (nextEvent && nextEvent.type !== 'event') {
        this._synchronousSend(nextEvent);
      } else {
        var events = this._dequeueUntil(function(event) {
          return event.type === 'event';
        });
        this._sendEvents(events);
      }
    };

    this._sendEvents = function(events) {
      if (!events.length) return; // No events to send

      var identifiedUser = localStorage[this.identifiedUserkey];
      var registeredUser = localStorage[this.registeredUserkey];
      var generatedId = localStorage[this.genereratedIdKey];
      var user = identifiedUser || registeredUser || { id: generatedId, properties: {} };

      var userEvents = [];
      events.forEach(function(e) {
        var userEvent = {
          event_type: e.params.eventType,
          external_user_id: user.id,
        };
        Object.keys(user.properties).forEach(function(p) {
          userEvent[p] = user.properties[p];
        });
        userEvents.push(userEvent);
      });

      $.ajax({
        url: this.url + '/events/batch_track',
        jsonp: "callback",
        dataType: "jsonp",
        data: {
          events: userEvents
        }
      });
    };

    this._synchronousSend = function(blockingCommand) {
      if (blockingCommand.type === 'identify') {
        this._setIdentifiedUser(blockingCommand.params);
      } else if (blockingCommand.type === 'alias') {
        this._aliasUser(blockingCommand.params);
      } else if (blockingCommand.type === 'register') {
        this._setRegisteredUser(blockingCommand.params);
      } else if (blockingCommand.type === 'invalidate') {
        this._invalidateUser();
      } else {
        throw 'Unknown Blocking Command';
      }
    };

    this._setIdentifiedUser = function (user) {
      localStorage.setItem(this.identifiedUserkey, JSON.stringify(user));
    };

    this._aliasUser = function (params) {
      var that = this;
      var identifiedUser = localStorage[this.identifiedUserkey];

      $.ajax({
        url: this.url + '/events/alias',
        jsonp: "callback",
        dataType: "jsonp",
        data: {
          from_external_user_id: identifiedUser.id,
          to_external_user_id: params.id
        },
        success: function () {
          that.queue.dequeue();
        }
      });
    };

    this._setRegisteredUser = function(properties) {
      var generatedId = localStorage[this.genereratedIdKey];
      var user = { id: generatedId, properties: properties };
      localStorage.setItem(this.registeredUserkey, JSON.stringify(user));
    };

    this._invalidateUser = function () {
      delete localStorage[this.identifiedUserkey];
      delete localStorage[this.registeredUserkey];
      localStorage[this.genereratedIdKey] = this._generateId();
    };

    this._dequeueUntil = function (predicate) {
      var eventBuffer = [];
      var nextEvent = this.queue.peek();

      while (nextEvent && predicate(nextEvent)) {
        var event = this.queue.dequeue();
        eventBuffer.push(event);
        nextEvent = this.queue.peek();
      }

      return eventBuffer;
    };

    this.track = function (eventType) {
      this.queue.enqueue({ type: 'event', params: { eventType: eventType } });
    };

    this.identify = function(id, properties) {
      this.queue.enqueue({
        type: "identify",
        params: {
          id: id,
          properties: properties
        }
      });
    };

    this.alias = function(id) {
      this.queue.enqueue({ type: "alias", params: { id: id } });
    };

    this.register = function (user) {
      this.queue.enqueue({ type: "register", params: user });
    };

    this.invalidate = function () {
      this.queue.enqueue({ type: 'invalidate' });
    };

    this.startInterval = function () {
      var that = this;
      var flushWrapper = function() {
        that._flush();
        setTimeout(flushWrapper, that.flushInterval);
      };
      setTimeout(this._flush.bind(this), this.flushInterval);
    };
  }).call(EventTracker.prototype);

  window.EventTracker = EventTracker;

})(window);