describe("EventHub", function() {
  var name = "EventHub";
  var sessionKey = name + "::activeSession";
  var generatedIdKey = name + "::generatedId";
  var identifiedUserKey = name + "::identifiedUser";
  var generatedUserKey = name + "::generatedUser";
  var eventHub;

  var clearStorage = function() {
    delete sessionStorage[sessionKey];
    delete localStorage[generatedIdKey];
    delete localStorage[identifiedUserKey];
    delete localStorage[generatedUserKey];
    delete localStorage[name + '::Queue'];
  };

  beforeEach(function() {
    clearStorage();
    jasmine.clock().install();
    DevTips = jasmine.createSpyObj('DevTips', ['jsonp']);
    DevTips.jsonp.and.callFake(function(url, params, api, success, failure) {
      if (success) {
        success();
      }
    });

    eventHub = new EventHub(name,
      new StorageQueue(name, window.localStorage),
      window.localStorage,
      window.sessionStorage,
      { url: 'http://example.com' });
  });

  afterEach(function() {
    jasmine.clock().uninstall();
  });

  describe("basic tracking", function() {
    it("should send event with given url, event_type and properties", function() {
      eventHub.track("submission", { a: 'b' });
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(1);
      var args = DevTips.jsonp.calls.mostRecent().args;
      expect(args[0]).toBe('http://example.com/events/batch_track');
      var eventsSent = JSON.parse(args[1].events);
      expect(eventsSent[0]).toEqual(
        jasmine.objectContaining({ event_type: 'submission', a: 'b' }));
    });

    it("should batch send events after the flushInterval", function() {
      eventHub.initialize();
      eventHub.start();
      eventHub.track("submission");
      eventHub.track("submission");
      eventHub.track("submission");

      jasmine.clock().tick(501);
      expect(DevTips.jsonp).not.toHaveBeenCalled();
      jasmine.clock().tick(500);
      expect(DevTips.jsonp.calls.count()).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events).length).toBe(3);
      jasmine.clock().tick(5000);
      expect(DevTips.jsonp.calls.count()).toBe(1);
    });

    it("should send previously queued events after resumed", function() {
      eventHub.track('prev-submission', { a: 'b' });
      eventHub.initialize();
      eventHub.track('submission', { c: 'd' });
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(2);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events).length).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events)[0]).toEqual(
        jasmine.objectContaining({ event_type: 'prev-submission', a: 'b' }));
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events).length).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events)[0]).toEqual(
        jasmine.objectContaining({ event_type: 'submission', c: 'd' }));
    });
  });

  describe("tracking with generated user", function() {
    it("should generate a new user id during initialization", function() {
      expect(eventHub._getUser().id).toBeUndefined();

      eventHub.initialize();
      eventHub.flush();
      expect(eventHub._getUser().id).toBeDefined();
    });

    it("should send events with generated id", function() {
      eventHub.initialize();
      eventHub.track("submission");
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(1);
      var eventsSent = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      var generatedId = eventHub._getUser().id;
      expect(generatedId).toBeDefined();
      expect(eventsSent[0].external_user_id).toBe(generatedId);
    });

    it("should reuse previously generated user if it's not a new session", function() {
      eventHub.initialize();
      eventHub.register({ foo: 'bar' });
      eventHub.flush();
      var generatedUser = eventHub._getUser();

      eventHub.initialize();
      eventHub.track("submission");
      eventHub.flush();

      var eventsSent = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      expect(eventsSent[0]).toEqual({
        event_type: 'submission',
        external_user_id: generatedUser.id,
        foo: 'bar'
      });
    });

    it("should invalidate previously generated user if it's a new session", function() {
      eventHub.initialize();
      eventHub.flush();
      var generatedUser = eventHub._getUser();
      delete sessionStorage[sessionKey];

      eventHub.initialize();
      eventHub.track("submission");
      eventHub.flush();

      var eventsSent = DevTips.jsonp.calls.mostRecent().args[1].events;
      expect(eventsSent[0].external_user_id).not.toBe(generatedUser.id);
    });

    it("should send previously queued events after resumed", function() {
      eventHub.initialize();
      eventHub.register({ foo: 'bar' });
      eventHub.flush();
      var generatedUser = eventHub._getUser();

      eventHub.track('prev-submission', { a: 'b' });
      eventHub.initialize();
      eventHub.track('submission', { c: 'd' });
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(2);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events).length).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events)[0]).toEqual({
        external_user_id: generatedUser.id,
        event_type: 'prev-submission',
        a: 'b',
        foo: 'bar'
      });
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events).length).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events)[0]).toEqual({
        external_user_id: generatedUser.id,
        event_type: 'submission',
        c: 'd',
        foo: 'bar'
      });
    });
  });

  describe("tracking with identified user", function() {
    it("should send events with identified user id, and properties", function() {
      eventHub.initialize();
      eventHub.identify('foo@example.com', { foo: 'bar' });
      eventHub.track("submission", { a: 'b' });
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(1);
      var eventsSent = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      expect(eventsSent[0]).toEqual(
        jasmine.objectContaining({
          event_type: 'submission',
          external_user_id: 'foo@example.com',
          foo: 'bar',
          a: 'b'
        }));
    });

    it("should invalidate previously identified user", function() {
      eventHub.initialize();
      eventHub.identify('foo', { foo: 'bar' });
      eventHub.flush();

      eventHub.initialize();
      eventHub.track("submission");
      eventHub.flush();

      var eventsSent = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      expect(eventsSent[0].external_user_id).not.toBe('foo');
      expect(eventsSent[0].foo).toBeUndefined();
    });

    it("should send previously queued events after resumed", function() {
      eventHub.initialize();
      eventHub.identify('foo', { foo: 'bar' });
      eventHub.flush();

      eventHub.track('prev-submission', { a: 'b' });
      eventHub.initialize();
      eventHub.track('submission', { c: 'd' });
      eventHub.flush();

      expect(DevTips.jsonp.calls.count()).toBe(2);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events).length).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events)[0]).toEqual({
        external_user_id: 'foo',
        event_type: 'prev-submission',
        a: 'b',
        foo: 'bar'
      });
      var events = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      expect(events.length).toBe(1);
      expect(events[0].external_user_id).not.toBe('foo');
      expect(events[0].foo).toBeUndefined();
      expect(events[0]).toEqual(jasmine.objectContaining({
        event_type: 'submission',
        c: 'd'
      }));
    });
  });
});
