// jsonp impl from http://stackapps.com/questions/891/how-to-call-the-api-via-jsonp-in-plain-old-javascript
window.DevTips=window.DevTips||{}; (function(dt){ var _2=0; var _3="Query string length exceeds maximum recommended value of "; var _4="Url path length exceeds maximum recommended value of "; var _5=dt.maxQueryStringLength=1000; var _6=dt.maxPathLength=240; var _7=dt.jsEncode=function(_8){ if(_8 instanceof Date){ return new Date(_8).getTime(); } if(_8 instanceof Array){ var _9=[]; for(var i=0;i<_8.length;i++){ _9.push(encodeURIComponent(_8[i])); } return _9.join(";"); } return encodeURIComponent(_8); }; var _b=dt.jsonp=function(_c,_d,_e,_f,_10,_11){ if(_c.length>_6){ throw new Error(_4+_6); } var _12="_callback"+_2++; var _13="?callback=DevTips.jsonp."+_12; if(_d){ for(var _14 in _d){ if(_d.hasOwnProperty(_14)){ _13=_13+"&"+_14+"="+_7(_d[_14]); } } } if(_13.length>_5){ throw new Error(_3+_5); } _b[_12]=function(_15){ delete _b[_12]; if(_15.error){ if(_10){ _15.error.callback=_12; _10(_15.error); } }else{ _f(_15); } }; var scr=document.createElement("script"); scr.type="text/javascript"; scr.src=_c+_13; var _17=document.getElementsByTagName("head")[0]; _17.insertBefore(scr,_17.firstChild); _11=_11||10000; window.setTimeout(function(){ if(typeof _b[_12]=="function"){ _b[_12]=function(_18){ delete _b[_12]; }; _10({code:408,message:"Request Timeout",callback:_12}); window.setTimeout(function(){ if(typeof _b[_12]=="function"){ delete _b[_12]; } },60000); } },_11); }; })(DevTips);

(function(window) {

  var generateId = function() {
    return Math.random().toString(36).substr(2, 9);
  };


  Storage.prototype.setObject = function(key, value) {
    this.setItem(key, JSON.stringify(value));
  };

  Storage.prototype.getObject = function(key) {
    var value = this.getItem(key);
    return value ? JSON.parse(value) : undefined;
  };


  var FakeStorage = function() {
    this.storage = {};
  };

  (function() {
    this.setObject = function(key, value) {
      this.storage[key] = value;
    };

    this.getObject = function(key) {
      return this.storage[key];
    };
  }).call(FakeStorage.prototype);


  var StorageQueue = function(name, localStorage) {
    this.key = name + "::Queue";
    this.localStorage = localStorage;
  };

  (function() { // merge to StorageQueue.prototype
    this._getQueue = function() {
      return this.localStorage.getObject(this.key) || [];
    };

    this._setQueue = function(queue) {
      this.localStorage.setObject(this.key, queue);
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


  var EventHub = function(name, queue, localStorage, sessionStorage, options) {
    this.queue = queue;
    this.localStorage = localStorage;
    this.sessionStorage = sessionStorage;

    options = options || {};
    this.url = options.url || "";
    this.flushInterval = (options.flushInterval || 1) * 1000;
    this.maxAttempts = options.maxAttempts || 10;
	  this.currentAttempts = 0;

    this.sessionKey = name + "::activeSession";
    this.generatedIdKey = name + "::generatedId";
    this.identifiedUserKey = name + "::identifiedUser";
    this.generatedUserKey = name + "::generatedUser";
    
    this.timeout = null;
  };

  (function() { // merge to EventHub.prototype
    var isTrackCommand = function(command) {
      return command.type === 'track';
    };

    this._getUser = function() {
      var identifiedUser = this.localStorage.getObject(this.identifiedUserKey);
      var generatedUser = this.localStorage.getObject(this.generatedUserKey);
      var generatedId = this.localStorage.getObject(this.generatedIdKey);
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

      window.DevTips.jsonp(this.url + '/events/batch_track', { events: JSON.stringify(events) }, "", this._resetFailures.bind(this), this._onFailure.bind(this));
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
      this.localStorage.setObject(this.identifiedUserKey, user);
    };

    this._aliasUser = function(params, success) {
      var generatedId = this.localStorage.getObject(this.generatedIdKey);

      window.DevTips.jsonp(this.url + '/users/alias', {
          from_external_user_id: params.id,
          to_external_user_id: generatedId
        }, "",
        function(res) {
	        success(res);
	        this._resetFailures();
        }.bind(this),
  	    this._onFailure.bind(this));
      };
  
    	this._resetFailures = function() {
    	  this.currentAttempts = 0;
    	};
  
      this._onFailure = function() {
  	  if (++this.currentAttempts === this.maxAttempts) {
  		  clearTimeout(this.timeout);
  		  throw '"Max Attempts" limit has been reached'
  	  }
  	};

    this._setGeneratedUser = function(properties) {
      var generatedId = this.localStorage.getObject(this.generatedIdKey);
      var user = { id: generatedId, properties: properties };
      this.localStorage.setObject(this.generatedUserKey, user);
    };

    this._invalidateGeneratedUser = function() {
      delete this.localStorage[this.generatedUserKey];
      this.localStorage.setObject(this.generatedIdKey, generateId());
    };

    this._invalidateIdentifiedUser = function() {
      delete this.localStorage[this.identifiedUserKey];
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
      properties = properties || {};

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
      if (!this.sessionStorage[this.sessionKey]) {
        this.sessionStorage[this.sessionKey] = true;
        this.invalidateGeneratedUser();
      }
      this.invalidateIdentifiedUser();
    };

    this.start = function() {
      var that = this;
      var flushWrapper = function() {
        that.flush();
        that.timeout = setTimeout(flushWrapper, that.flushInterval);
      };
      that.timeout = setTimeout(flushWrapper, this.flushInterval);
    };
  }).call(EventHub.prototype);


  window.FakeStorage = FakeStorage;
  window.StorageQueue = StorageQueue;
  window.EventHub = EventHub;
  window.newEventHub = function(name, options) {
    var storageQueue = new StorageQueue(name, window.localStorage || new FakeStorage());
    var eventHub = new EventHub(
        name,
        storageQueue,
        window.localStorage || new FakeStorage(),
        window.sessionStorage || new FakeStorage(),
        options);

    eventHub.initialize();
    eventHub.start();
    return eventHub;
  };
})(window);
