describe("EventTracker without localStorage", function() {
  var name = "EventTracker";
  var sessionKey = name + "::activeSession";
  var generatedIdKey = name + "::generatedId";
  var identifiedUserKey = name + "::identifiedUser";
  var generatedUserKey = name + "::generatedUser";
  var eventTracker;

  beforeEach(function() {
    jasmine.clock().install();
    DevTips = jasmine.createSpyObj('DevTips', ['jsonp']);
    DevTips.jsonp.and.callFake(function(url, params, api, success, failure) {
      if (success) {
        success();
      }
    });

    eventTracker = new EventTracker(name,
      new StorageQueue(name, new FakeStorage()),
      new FakeStorage(),
      new FakeStorage(),
      { url: 'http://example.com' });
  });

  afterEach(function() {
    jasmine.clock().uninstall();
  });

  describe("basic tracking", function() {
    it("should send event with given url, event_type and properties", function() {
      eventTracker.track("submission", { a: 'b' });
      eventTracker.flush();

      expect(DevTips.jsonp.calls.count()).toBe(1);
      var args = DevTips.jsonp.calls.mostRecent().args;
      expect(args[0]).toBe('http://example.com/events/batch_track');
      var eventsSent = JSON.parse(args[1].events);
      expect(eventsSent[0]).toEqual(
        jasmine.objectContaining({ event_type: 'submission', a: 'b' }));
    });

    it("should batch send events after the flushInterval", function() {
      eventTracker.initialize();
      eventTracker.start();
      eventTracker.track("submission");
      eventTracker.track("submission");
      eventTracker.track("submission");

      jasmine.clock().tick(501);
      expect(DevTips.jsonp).not.toHaveBeenCalled();
      jasmine.clock().tick(500);
      expect(DevTips.jsonp.calls.count()).toBe(1);
      expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events).length).toBe(3);
      jasmine.clock().tick(5000);
      expect(DevTips.jsonp.calls.count()).toBe(1);
    });

    it("should send previously queued events after resumed", function() {
      eventTracker.track('prev-submission', { a: 'b' });
      eventTracker.initialize();
      eventTracker.track('submission', { c: 'd' });
      eventTracker.flush();

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
      expect(eventTracker._getUser().id).toBeUndefined();

      eventTracker.initialize();
      eventTracker.flush();
      expect(eventTracker._getUser().id).toBeDefined();
    });

    it("should send events with generated id", function() {
      eventTracker.initialize();
      eventTracker.track("submission");
      eventTracker.flush();

      expect(DevTips.jsonp.calls.count()).toBe(1);
      var eventsSent = JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events);
      var generatedId = eventTracker._getUser().id;
      expect(generatedId).toBeDefined();
      expect(eventsSent[0].external_user_id).toBe(generatedId);
    });
  });

  describe("tracking with identified user", function() {
    it("should send events with identified user id, and properties", function() {
      eventTracker.initialize();
      eventTracker.identify('foo@example.com', { foo: 'bar' });
      eventTracker.track("submission", { a: 'b' });
      eventTracker.flush();

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
  });
});