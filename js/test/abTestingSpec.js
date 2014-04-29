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
    DevTips = jasmine.createSpyObj('DevTips', ['jsonp']);
    DevTips.jsonp.and.callFake(function(url, params, api, success, failure) {
      if (success) {
        success();
      }
    });

    eventTracker = new EventTracker(name,
     new StorageQueue(name, window.localStorage),
      window.localStorage,
      window.sessionStorage,
      { url: 'http://example.com' });
  });

  afterEach(function() {
    jasmine.clock().uninstall();
  });

  describe("in A/B testing", function() {
    describe("signup conversion", function() {
      it("should send tracking events with generated id and registered properties, " +
          "then alias, and finally send events with identified user properties", function() {
        eventTracker.initialize();
        eventTracker.register({ experiment: 'signup_v20', treatment: 'A' });
        eventTracker.start();
        jasmine.clock().tick(1001);
        var generatedId = eventTracker._getUser().id;

        eventTracker.track('pageview', { page: 'home page' });
        eventTracker.alias('foo@example.com');
        eventTracker.identify('foo@example.com', { age: 30 });
        eventTracker.track('signup', { hello: 'world' });

        jasmine.clock().tick(5001);
        expect(DevTips.jsonp.calls.count()).toBe(3);
        expect(DevTips.jsonp.calls.all()[0].args[0]).toBe('http://example.com/events/batch_track');
        expect(JSON.parse(DevTips.jsonp.calls.all()[0].args[1].events)[0]).toEqual(jasmine.objectContaining({
          event_type: 'pageview',
          page: 'home page',
          experiment: 'signup_v20',
          treatment: 'A'
        }));
        expect(DevTips.jsonp.calls.all()[1].args[0]).toEqual('http://example.com/users/alias');
        expect(DevTips.jsonp.calls.all()[1].args[1]).toEqual(jasmine.objectContaining({
          from_external_user_id: 'foo@example.com',
          to_external_user_id: generatedId
        }));
        expect(JSON.parse(DevTips.jsonp.calls.all()[2].args[1].events)[0]).toEqual(jasmine.objectContaining({
          external_user_id: 'foo@example.com',
          event_type: 'signup',
          hello: 'world',
          age: 30
        }));
      });
    });

    describe("feature conversion", function() {
      it("should send tracking events with generated id and registered properties, " +
          "then alias, and finally send events with identified user properties", function() {
        eventTracker.initialize();
        eventTracker.start();
        eventTracker.identify('foo@example.com', {
          age: 30,
          experiment: 'track_page_v20',
          treatment: 'A'
        });
        eventTracker.track('pageview', { page: 'javascript track page' });
        eventTracker.track('start track', { track: 'javascript' });

        jasmine.clock().tick(5001);
        expect(DevTips.jsonp.calls.count()).toBe(1);
        expect(DevTips.jsonp.calls.first().args[0]).toBe('http://example.com/events/batch_track');
        expect(JSON.parse(DevTips.jsonp.calls.first().args[1].events)).toEqual([{
          external_user_id: 'foo@example.com',
          event_type: 'pageview',
          page: 'javascript track page',
          experiment: 'track_page_v20',
          treatment: 'A',
          age: 30
        }, {
          external_user_id: 'foo@example.com',
          event_type: 'start track',
          track: 'javascript',
          experiment: 'track_page_v20',
          treatment: 'A',
          age: 30
        }]);
      });
    });
  });
});