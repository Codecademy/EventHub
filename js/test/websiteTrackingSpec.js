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
    $.ajax.and.callFake(function(params) {
      if (params.success) {
        params.success();
      }
    });

    eventTracker = new EventTracker(name,
      new StorageQueue(name),
      { url: 'http://example.com' });
  });

  afterEach(function() {
    jasmine.clock().uninstall();
  });

  describe("normal website tracking", function() {
    describe("anonymous user make some submissions", function() {
      it("should batch send tracking event to the correct url with generated id", function() {
        eventTracker.initialize();
        eventTracker.register({ age: 30 });
        eventTracker.start();

        eventTracker.track("submission", { hello: 'world' });
        eventTracker.track("pageview", { foo: 'bar' });
        eventTracker.track("submission");
        jasmine.clock().tick(1001);

        expect($.ajax.calls.count()).toBe(1);
        expect($.ajax.calls.mostRecent().args[0].url).toBe('http://example.com/events/batch_track');
        var generatedId = eventTracker._getUser().id;
        expect(generatedId).not.toBeNull();
        expect($.ajax.calls.mostRecent().args[0].data.events).toEqual([
          {
            event_type: 'submission',
            external_user_id: generatedId,
            hello: 'world',
            age: 30
          }, {
            event_type: 'pageview',
            external_user_id: generatedId,
            foo: 'bar',
            age: 30
          }, {
            event_type: 'submission',
            external_user_id: generatedId,
            age: 30
          }]);
      });
    });

    describe("signed-in user make some submissions", function() {
      it("should batch send tracking event with identified user's properties", function() {
        eventTracker.initialize();
        eventTracker.identify('foo@example.com', { age: 30 });
        eventTracker.start();

        eventTracker.track("submission", { hello: 'world' });
        eventTracker.track("pageview", { foo: 'bar' });
        eventTracker.track("submission");
        jasmine.clock().tick(1001);

        expect($.ajax.calls.count()).toBe(1);
        expect($.ajax.calls.mostRecent().args[0].data.events).toEqual([
          {
            event_type: 'submission',
            external_user_id: 'foo@example.com',
            hello: 'world',
            age: 30
          }, {
            event_type: 'pageview',
            external_user_id: 'foo@example.com',
            foo: 'bar',
            age: 30
          }, {
            event_type: 'submission',
            external_user_id: 'foo@example.com',
            age: 30
          }]);
      });
    });

    describe("anonymous user make some submissions, sign up, and then continue making submissions", function() {
      it("should send tracking event as well as the alias event", function() {
        eventTracker.initialize();
        eventTracker.flush();
        var generatedId = eventTracker._getUser().id;
        eventTracker.track("pageview", { foo: 'bar' });
        eventTracker.initialize();
        eventTracker.alias('foo@example.com');
        eventTracker.identify('foo@example.com', { age: 30 });
        eventTracker.track("submission", { hello: 'world' });
        eventTracker.start();

        jasmine.clock().tick(3001);

        expect($.ajax.calls.count()).toBe(3);
        expect($.ajax.calls.all()[0].args[0].url).toBe('http://example.com/events/batch_track');
        expect($.ajax.calls.all()[0].args[0].data.events[0]).toEqual(jasmine.objectContaining({
          event_type: 'pageview',
          foo: 'bar'
        }));
        expect($.ajax.calls.all()[1].args[0].url).toEqual('http://example.com/users/alias');
        expect($.ajax.calls.all()[1].args[0].data).toEqual(jasmine.objectContaining({
          from_external_user_id: 'foo@example.com',
          to_external_user_id: generatedId
        }));
        expect($.ajax.calls.all()[2].args[0].data.events[0]).toEqual(jasmine.objectContaining({
          external_user_id: 'foo@example.com',
          event_type: 'submission',
          hello: 'world',
          age: 30
        }));
      });
    });

    describe("remembered user make some submissions, leave, come back and make more submissions ", function() {
      it("should batch send tracking event with identified user's properties", function() {
        eventTracker.initialize();
        eventTracker.identify('foo@example.com', { age: 30 });
        eventTracker.track("submission", { hello: 'world' });
        eventTracker.flush();

        eventTracker.initialize();
        eventTracker.start();
        eventTracker.identify('foo@example.com', { ip: '10.0.0.1' });
        eventTracker.track("pageview", { foo: 'bar' });
        jasmine.clock().tick(1001);

        expect($.ajax.calls.count()).toBe(2);
        expect($.ajax.calls.first().args[0].data.events).toEqual([
          {
            event_type: 'submission',
            external_user_id: 'foo@example.com',
            hello: 'world',
            age: 30
          }]);
        expect($.ajax.calls.mostRecent().args[0].data.events).toEqual([
          {
            event_type: 'pageview',
            external_user_id: 'foo@example.com',
            foo: 'bar',
            ip: '10.0.0.1'
          }]);
      });
    });
  });
});