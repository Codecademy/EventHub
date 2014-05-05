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

  describe("normal website tracking", function() {
    describe("anonymous user make some submissions", function() {
      it("should batch send tracking event to the correct url with generated id", function() {
        eventHub.initialize();
        eventHub.register({ age: 30 });
        eventHub.start();

        eventHub.track("submission", { hello: 'world' });
        eventHub.track("pageview", { foo: 'bar' });
        eventHub.track("submission");
        jasmine.clock().tick(1001);

        expect(DevTips.jsonp.calls.count()).toBe(1);
        expect(DevTips.jsonp.calls.mostRecent().args[0]).toBe('http://example.com/events/batch_track');
        var generatedId = eventHub._getUser().id;
        expect(generatedId).not.toBeNull();
        expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events)).toEqual([
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
        eventHub.initialize();
        eventHub.identify('foo@example.com', { age: 30 });
        eventHub.start();

        eventHub.track("submission", { hello: 'world' });
        eventHub.track("pageview", { foo: 'bar' });
        eventHub.track("submission");
        jasmine.clock().tick(1001);

        expect(DevTips.jsonp.calls.count()).toBe(1);
        expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events)).toEqual([
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
        eventHub.initialize();
        eventHub.flush();
        var generatedId = eventHub._getUser().id;
        eventHub.track("pageview", { foo: 'bar' });
        eventHub.initialize();
        eventHub.alias('foo@example.com');
        eventHub.identify('foo@example.com', { age: 30 });
        eventHub.track("submission", { hello: 'world' });
        eventHub.start();

        jasmine.clock().tick(3001);

        expect(DevTips.jsonp.calls.count()).toBe(3);
        expect(DevTips.jsonp.calls.all()[0].args[0]).toBe('http://example.com/events/batch_track');
        expect(JSON.parse(DevTips.jsonp.calls.all()[0].args[1].events)[0]).toEqual(jasmine.objectContaining({
          event_type: 'pageview',
          foo: 'bar'
        }));
        expect(DevTips.jsonp.calls.all()[1].args[0]).toEqual('http://example.com/users/alias');
        expect(DevTips.jsonp.calls.all()[1].args[1]).toEqual(jasmine.objectContaining({
          from_external_user_id: 'foo@example.com',
          to_external_user_id: generatedId
        }));
        expect(JSON.parse(DevTips.jsonp.calls.all()[2].args[1].events)[0]).toEqual(jasmine.objectContaining({
          external_user_id: 'foo@example.com',
          event_type: 'submission',
          hello: 'world',
          age: 30
        }));
      });
    });

    describe("remembered user make some submissions, leave, come back and make more submissions ", function() {
      it("should batch send tracking event with identified user's properties", function() {
        eventHub.initialize();
        eventHub.identify('foo@example.com', { age: 30 });
        eventHub.track("submission", { hello: 'world' });
        eventHub.flush();

        eventHub.initialize();
        eventHub.start();
        eventHub.identify('foo@example.com', { ip: '10.0.0.1' });
        eventHub.track("pageview", { foo: 'bar' });
        jasmine.clock().tick(1001);

        expect(DevTips.jsonp.calls.count()).toBe(2);
        expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events)).toEqual([
          {
            event_type: 'submission',
            external_user_id: 'foo@example.com',
            hello: 'world',
            age: 30
          }]);
        expect(JSON.parse(DevTips.jsonp.calls.mostRecent().args[1].events)).toEqual([
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
