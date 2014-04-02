(function(window) {
  Storage.prototype.setObject = function(key, value) {
    this.setItem(key, JSON.stringify(value));
  };

  Storage.prototype.getObject = function(key) {
    var value = this.getItem(key);
    return value ? JSON.parse(value) : undefined;
  };

  var generateId = function() {
    return Math.random().toString(36).substr(2, 9);
  };

  var StorageQueue = function(name) {
    this.key = name + "::Queue";
  };

  (function() { // merge to StorageQueue.prototype
    this._getQueue = function() {
      return localStorage.getObject(this.key) || [];
    };

    this._setQueue = function(queue) {
      localStorage.setObject(this.key, queue);
    };

    this.enqueue = function(element) {
      this._setQueue(this._getQueue().concat(element));
    };

    this.dequeue = function() {
      var queue   = this._getQueue();
      var element = queue.splice(0,1)[0];

      this._setQueue(queue);
      return element;
    };

    this.peek = function() {
      return this._getQueue()[0];
    };
  }).call(StorageQueue.prototype);

  var EventTracker = function(name, queue, options) {
    this.queue = queue;

    options = options || {};
    this.url = options.url || "";
    this.flushInterval = (options.flushInterval || 1) * 1000;

    this.sessionKey = name + "::activeSession";
    this.generatedIdKey = name + "::generatedId";
    this.identifiedUserKey = name + "::identifiedUser";
    this.generatedUserKey = name + "::generatedUser";
  };

  (function() { // merge to EventTracker.prototype
    var isTrackCommand = function(command) {
      return command.type === 'track';
    };

    this._getUser = function() {
      var identifiedUser = localStorage.getObject(this.identifiedUserKey);
      var generatedUser = localStorage.getObject(this.generatedUserKey);
      var generatedId = localStorage.getObject(this.generatedIdKey);
      return identifiedUser || generatedUser || { id: generatedId, properties: {} };
    };

    this._sendEvents = function(trackCommands) {
      if (!trackCommands.length) return; // No events to send

      var events = [];
      var user = this._getUser();
      trackCommands.forEach(function(trackCommand) {
        var event = {};

        Object.keys(user.properties).forEach(function(p) {
          event[p] = user.properties[p];
        });
        Object.keys(trackCommand.params.properties).forEach(function(p) {
          event[p] = trackCommand.params.properties[p];
        });

        event.event_type = trackCommand.params.eventType;
        event.external_user_id = user.id;
        events.push(event);
      });

      $.ajax({
        url: this.url + '/events/batch_track',
        jsonp: "callback",
        dataType: "jsonp",
        data: {
          events: events
        }
      });
    };

    this._synchronousSend = function(blockingCommand) {
      if (blockingCommand.type === 'identify') {
        this._setIdentifiedUser(blockingCommand.params);
        this.queue.dequeue();
      } else if (blockingCommand.type === 'alias') {
        var that = this;
        this._aliasUser(blockingCommand.params, function() {
          that.queue.dequeue();
        });
      } else if (blockingCommand.type === 'register') {
        this._setGeneratedUser(blockingCommand.params);
        this.queue.dequeue();
      } else if (blockingCommand.type === 'invalidateGeneratedUser') {
        this._invalidateGeneratedUser();
        this.queue.dequeue();
      } else if (blockingCommand.type === 'invalidateIdentifiedUser') {
        this._invalidateIdentifiedUser();
        this.queue.dequeue();
      } else {
        throw 'Unknown Blocking Command';
      }
    };

    this._setIdentifiedUser = function(user) {
      localStorage.setObject(this.identifiedUserKey, user);
    };

    this._aliasUser = function(params, success) {
      var generatedId = localStorage.getObject(this.generatedIdKey);

      $.ajax({
        url: this.url + '/users/alias',
        jsonp: "callback",
        dataType: "jsonp",
        data: {
          from_external_user_id: params.id,
          to_external_user_id: generatedId
        },
        success: success
      });
    };

    this._setGeneratedUser = function(properties) {
      var generatedId = localStorage.getObject(this.generatedIdKey);
      var user = { id: generatedId, properties: properties };
      localStorage.setObject(this.generatedUserKey, user);
    };

    this._invalidateGeneratedUser = function() {
      delete localStorage[this.generatedUserKey];
      localStorage.setObject(this.generatedIdKey, generateId());
    };

    this._invalidateIdentifiedUser = function() {
      delete localStorage[this.identifiedUserKey];
    };

    this._dequeueUntil = function(predicate) {
      var eventBuffer = [];
      var nextEvent = this.queue.peek();

      while (nextEvent && predicate(nextEvent)) {
        var event = this.queue.dequeue();
        eventBuffer.push(event);
        nextEvent = this.queue.peek();
      }

      return eventBuffer;
    };

    this.track = function(eventType, properties) {
      properties = properties || {};

      this.queue.enqueue({
        type: 'track',
        params: { eventType: eventType, properties: properties }
      });
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

    this.register = function(user) {
      this.queue.enqueue({ type: "register", params: user });
    };

    this.invalidateGeneratedUser = function() {
      this.queue.enqueue({ type: 'invalidateGeneratedUser' });
    };

    this.invalidateIdentifiedUser = function() {
      this.queue.enqueue({ type: 'invalidateIdentifiedUser' });
    };

    this.flush = function() {
      var nextCommand = this.queue.peek();
      while (nextCommand) {
        if (nextCommand.type === 'track') {
          var commands = this._dequeueUntil(isTrackCommand);
          this._sendEvents(commands);
        } else if (nextCommand.type === 'alias') {
          this._synchronousSend(nextCommand);
          break;
        } else {
          this._synchronousSend(nextCommand);
        }
        nextCommand = this.queue.peek();
      }
    };

    this.initialize = function() {
      if (!sessionStorage[this.sessionKey]) {
        sessionStorage[this.sessionKey] = true;
        this.invalidateGeneratedUser();
      }
      this.invalidateIdentifiedUser();
    };

    this.start = function() {
      var that = this;
      var flushWrapper = function() {
        that.flush();
        setTimeout(flushWrapper, that.flushInterval);
      };
      setTimeout(flushWrapper, this.flushInterval);
    };
  }).call(EventTracker.prototype);

  window.StorageQueue = StorageQueue;
  window.EventTracker = EventTracker;
  window.newEventTracker = function(name, options) {
    var storageQueue = new StorageQueue(name);
    var eventTracker = new EventTracker(name, storageQueue, options);

    eventTracker.initialize();
    eventTracker.start();
    return eventTracker;
  };
})(window);