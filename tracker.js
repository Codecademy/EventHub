(function (window) {

  var StorageQueue = function (name) {
    this.key = ("EventTracker" || options.name) + "::Queue";
  };

  (function () { // merge to StorageQueue.prototype
    this._getQueue = function () {
      return JSON.parse(localStorage[this.key] || "[]");
    }

    this._setQueue = function (queue) {
      localStorage.setItem(this.key, JSON.stringify(queue))
    }

    this.enqueue = function (element) {
      this._setQueue(this._getQueue().concat(element))
    }

    this.dequeue = function () {
      var queue = this._getQueue()
        , element = queue.splice(0,1)[0];

      this._setQueue(queue);
      return element;
    }

    this.peek = function () {
      return this._getQueue().slice(-1)[0];
    }
  }).call(StorageQueue.prototype)

  var EventTracker = function (options) {
    options = options || {};

    this.FLUSH_INTERVAL = (options.flushInterval || 10) * 1000;
    this.name = "EventTracker" || options.name
    this.queue = new StorageQueue(options.name);
    this.url = options.url;

    this._initializeKeys();
    this._generateId();
    this._startInterval();
  };

  (function () { // merge to EventTracker.prototype
    this._initializeKeys = function () {
      this.sessionKey = this.name + "::activeSession";
      this.generatedIdKey = this.name + "::generatedId";
      this.identifiedUserkey = this.name + "::identifiedUser";
      this.registeredUserkey = this.name + "::registeredUser";
    }

    this._checkForNewSession = function () {
      if (!sessionStorage[this.sessionKey]) {
        sessionStorage[this.sessionKey] = true;
        this._generateId();
      }
    }

    this._generateId = function () {
      var generatedId = Math.random().toString(36).substr(2, 9);
      localStorage[this.genereratedIdKey] = generatedId;
    }

    this._startInterval = function () {
      setInterval(this._flush.bind(this), this.FLUSH_INTERVAL);
    }

    this._flush = function () {
      var nextEvent = this.queue.peek();
      if (nextEvent && nextEvent.type !== 'event') {
        this._synchronousSend(nextEvent);
      } else {
        var events = this._dequeueUntilBlockingAction();
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
    }

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
    }

    this._setIdentifiedUser = function (user) {
      localStorage.setItem(this.identifiedUserkey, JSON.stringify(user));
    }

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
    }

    this._setRegisteredUser = function(properties) {
      var generatedId = localStorage[this.genereratedIdKey]
      var user = { id: generatedId, properties: properties }
      localStorage.setItem(this.registeredUserkey, JSON.stringify(user));
    }

    this._invalidateUser = function () {
      delete localStorage[this.identifiedUserkey];
      delete localStorage[this.registeredUserkey];
      this._generateId();
    }

    this._dequeueUntilBlockingAction = function () {
      var eventBuffer = [];
      var nextEvent = this.queue.peek();

      while (nextEvent && nextEvent.type === 'event') {
        var event = this.queue.dequeue();
        eventBuffer.push(event);
        nextEvent = this.queue.peek();
      }

      return eventBuffer;
    }

    this.track = function (eventType) {
      this.queue.enqueue({ type: 'event', params: { eventType: eventType } });
    }

    this.identify = function(id, properties) {
      this.queue.enqueue({
        type: "identify",
        params: {
          id: id,
          properties: properties
        }
      });
    }

    this.alias = function(id) {
      this.queue.enqueue({ type: "alias", params: { id: id } });
    }

    this.register = function (user) {
      this.queue.enqueue({ type: "register", params: user });
    }

    this.invalidate = function () {
      this.queue.enqueue({ type: 'invalidate' })
    }
  }).call(EventTracker.prototype);

  window.EventTracker = EventTracker;

})(window);