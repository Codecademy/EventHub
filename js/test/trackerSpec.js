describe("EventTracker", function() {
  var name = "EventTracker";
  var sessionKey = name + "::activeSession";
  var generatedIdKey = name + "::generatedId";
  var identifiedUserKey = name + "::identifiedUser";
  var generatedUserKey = name + "::generatedUser";
  var eventTracker;

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
    $ = jasmine.createSpyObj('$', ['ajax']);

    eventTracker = new EventTracker(name,
      new StorageQueue(name),
      { url: 'http://example.com' });
  });

  afterEach(function() {
    jasmine.clock().uninstall();
  });

  describe("basic tracking", function() {
    it("should generate a new user id during initialization", function() {
      expect(eventTracker._getUser().id).toBeUndefined();

      eventTracker.initialize();
      eventTracker.flush();
      expect(eventTracker._getUser().id).toBeDefined();
    });

    it("should invalidate previously identified user", function() {
      eventTracker.initialize();
      eventTracker.identify('foo', { a: 'b' });
      eventTracker.flush();

      eventTracker.initialize();
      eventTracker.track("submission");
      eventTracker.flush();

      var eventsSent = $.ajax.calls.first().args[0].data.events;
      expect(eventsSent[0].external_user_id).not.toBe('foo');
    });

    it("should send events with generated id", function() {
      eventTracker.initialize();
      eventTracker.track("submission");
      eventTracker.flush();

      expect($.ajax.calls.count()).toBe(1);
      var args = $.ajax.calls.first().args;
      expect(args[0].url).toBe('http://example.com/events/batch_track');
      var eventsSent = args[0].data.events;
      expect(eventsSent[0].external_user_id).not.toBeNull();
    });

    it("should reuse previously generated user if it's not a new session", function() {
      eventTracker.initialize();
      eventTracker.register({ a: 'b' });
      eventTracker.flush();
      var generatedUser = eventTracker._getUser();

      eventTracker.initialize();
      eventTracker.track("submission");
      eventTracker.flush();

      var eventsSent = $.ajax.calls.first().args[0].data.events;
      expect(eventsSent[0]).toEqual({
        event_type: 'submission',
        external_user_id: generatedUser.id,
        a: 'b'
      });
    });

    it("should invalidate previously generated user if it's a new session", function() {
      eventTracker.initialize();
      eventTracker.flush();
      var generatedUser = eventTracker._getUser();
      delete sessionStorage[sessionKey];

      eventTracker.initialize();
      eventTracker.track("submission");
      eventTracker.flush();

      var eventsSent = $.ajax.calls.first().args[0].data.events;
      expect(eventsSent[0].external_user_id).not.toBe(generatedUser.id);
    });

    it("should only batch send events after the flushInterval", function() {
      eventTracker.initialize();
      eventTracker.start();
      eventTracker.track("submission");
      eventTracker.track("submission");
      eventTracker.track("submission");

      jasmine.clock().tick(501);
      expect($.ajax).not.toHaveBeenCalled();
      jasmine.clock().tick(500);
      expect($.ajax.calls.count()).toBe(1);
      expect($.ajax.calls.first().args[0].data.events.length).toBe(3);
      jasmine.clock().tick(5000);
      expect($.ajax.calls.count()).toBe(1);
    });
  });

  describe("in normal flow", function() {
    describe("anonymous user make some submissions", function() {

    });

    describe("signed-in user make some submissions", function() {
    });

    describe("anonymous user make some submissions, sign up, and then continue making submissions", function() {
    });

    describe("remembered user make some submissions, leave, come back and make more submissions ", function() {
    });
  });

  xdescribe("when session resumed", function() {
    describe("anonymous user left with events unsent, come back, and make more submissions", function() {
    });

    describe("signed-in user left with events unsent, come back, and make more submissions", function() {
    });
  });

  xdescribe("in A/B testing", function() {
    describe("A/B test signup flow", function() {
    });

    describe("A/B test which exercise has more submissions", function() {
    });
  });

  xdescribe("in cross devices tracking", function() {
    describe("user foo sign up using website, make some submissions, sign in using mobile device, then make more submissions", function() {
    });

    describe("user foo sign up using website, make some submissions, using mobile device, make some submissions, sign in, and then make more submissions", function() {
    });
  });
});