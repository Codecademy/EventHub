# Event Tracker
Event Tracker enables companies to do cross devices event tracking. The events will be joined by their associated users at the server, and the server also comes with a built-in dashboard which can be used to answer the following common business questions
* what is my funnel conversion rate
* what is my cohorted KPI retention
* which variant in my A/B testing has higher conversion rate

Most important of all, it is free and open sourced.

**Table of Contents**
- [Quick Start](#quick-start)
- [Server](#server)
- [Dashboard](#dashboard)
- [Javascript Library](#javascript-library)

## Quick Start
### Required dependency
* [java sdk7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [maven](http://maven.apache.org)

### Compile and run
```bash
# set up proper JAVA_HOME for mac
export JAVA_HOME=$(/usr/libexec/java_home)

git clone https://github.com/mobicrave/EventTracker.git
cd EventTracker
export EVENT_TRACKER_DIR=`pwd`
mvn -am -pl web clean package
java -jar web/target/web-1.0-SNAPSHOT.jar
```

### How to run all the tests
#### Unit/Integration/Functional testing
```bash
mvn -am -pl web clean test
```

#### Manual testing with curl
* Add new event
    ```bash
    curl -X POST http://localhost:8080/events/track --data "event_type=signup&external_user_id=foobar&event_property_1=1"
    ```

* Batch add new event
    ```bash
    curl -X POST http://localhost:8080/events/batch_track --data "events=[{event_type: signup, external_user_id: foobar, date: 20130101, event_property_1: 1}]"
    ```

* Show all event types
    ```bash
    curl http://localhost:8080/events/types
    ```

* Show all property keys for the given event type
    ```bash
    curl 'http://localhost:8080/events/keys?event_type=signup'
    ```

* Show all property values for the given event type and property key
    ```bash
    curl 'http://localhost:8080/events/values?event_type=signup&event_key=treatment'
    ```

* Show server stats
    ```bash
    curl http://localhost:8080/varz
    ```

* Funnel query
    ```bash
    today=`date +'%Y%m%d'`
    end_date=`(date -d '+7day' +'%Y%m%d' || date -v '+7d' +'%Y%m%d') 2> /dev/null`

    curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1"
    ```

* Retention query
    ```bash
    today=`date +'%Y%m%d'`
    end_date=`(date -d '+7day' +'%Y%m%d' || date -v '+7d' +'%Y%m%d') 2> /dev/null`

    curl -X POST "http://localhost:8080/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=view_shopping_cart&num_days_per_row=1&num_columns=2"
    ```

#### Automated testing with curl
```bash
cd ${EVENT_TRACKER_DIR}; ./script.sh
```

#### Load testing with Jmeter
We use [Apache Jmeter](http://jmeter.apache.org) for load testing, and the load testing script can be found in `${EVENT_TRACKER_DIR}/jmeter.jmx`.
```bash
NUM_THREADS=1 java -jar ~/Downloads/apache-jmeter-2.11/bin/ApacheJMeter.jar -JnumThreads=${NUM_THREADS} -n -t jmeter.jmx -p jmeter.properties
NUM_THREADS=5 java -jar ~/Downloads/apache-jmeter-2.11/bin/ApacheJMeter.jar -JnumThreads=${NUM_THREADS} -n -t jmeter.jmx -p jmeter.properties
NUM_THREADS=10 java -jar ~/Downloads/apache-jmeter-2.11/bin/ApacheJMeter.jar -JnumThreads=${NUM_THREADS} -n -t jmeter.jmx -p jmeter.properties

# generate graph (require matplotlib)
./plot_jmeter_performance.py 1-jmeter-performance.csv 5-jmeter-performance.csv 10-jmeter-performance.csv

# open "Track Event.png"
```

## Server

### Key observations & design decisions
Our goal is to build something usable on a single machine with reasonably large SSD drive. Let's say, hypotheitcally, the server receives 100M events monthly (might cost you few thousand dollars per month to use SAAS provider), and each event is 500 bytes without compression. In the situation, storing all the events likely only takes you few hundreds GB to store all your data with compression, and chances are, only data in recent months are of interest.

Also, to efficiently run basic funnel and cohort queries without filtering, only two forward indices are needed, event index sharded by event types and event index sharded by users. Therefore, our strategy is to make those two indices as small as possible to fit in memory, and if the client want to do filtering for events, we build a bloomfilter rejects most of the non exact-match. Imagine we are running another hypothetical query while assuming both indices and the bloomfilters can be fitted in memory. Say there are 1M events that cannot rejected and need to hit the disk, assuming each SSD disk read is 16 microseconds, we are talking about sub-minute query time, while assuming none of the data are in memory. In practice, the situation is likely much better as we cache all the recently hit records, and most of the queries likely only concern about most recent data.

To simplifies the design of the server and store indices compactly so that they will fit in memory, we made the following two assumptions.

1. Times are associated to the events by the server when received
2. Date is the finest level of granularity

With the above two assumptions, we can rely on server generated monotonically increasing id to maintain the total order for the events. In addition, as long as we track the id of the first event in any given date, we do not need to store the time information in the indices (which greatly reduce the size of the indices). The direct implication for those assumptions are, first, if the client chose to cache some events locally and sent them later, the timing for those events will be recorded as the server receives them, not when the user made those actions; second, though the server maintains the total ordering of all events, it cannot answer questions like what is the conversion rate for the given funnel between 2pm and 3pm on a given date.

Lastly, for both indices, since they are sharded by event types or users, we expect the size of the indices can be significantly further reduced with proper compression.

### Architecture
At the highest level, `com.mobicrave.eventracker.web.EventTrackerHandler` is the main entry point. It runs a [Jetty](http://www.eclipse.org/jetty) server, reflectively collects supported commands under `com.mobicrave.eventracker.web.commands`, handles JSONP request transparently, handles requests to static resources like the dashboard, and most importantly, act as a proxy which translates http request and respones to and from method calls to `com.mobicrave.eventracker.EventTracker`.

`com.mobicrave.eventracker.EventTracker` can be thought of as a facade to the key components of `UserStorage`, `EventStorage`, `ShardedEventIndex`, `DatedEventIndex`, `UserEventIndex` and `PropertiesIndex`.

For `UserStorage` and `EventStorage`, at the lowest level, we implemented `Journal{User,Event}Storage` backed by [HawtJournal](https://github.com/fusesource/hawtjournal/) to store underlying records reliably. In addition, when clients is quering records which cannot be filtered by the supported indices, the server will loop through all tne potential hits, look up the properties from the `Journal` and then filter. For better performance, there are also decorators for each storage like `Cached{User,Event}Storage` to support caching and `BloomFiltered{User,Event}Storage` to support fast rejection for filters like `ExactMatch`. Please also beware that each `Storage` maintains a monotonically increasing counter as the internal id generator for each event and user received.

To make the funnel and corhot queries fast, `EventTracker` also maintains three indices, `ShardedEventIndex`, `UserEventIndex`, and `DatedEventIndex` behind the scene. `DatedEventIndex` simply tracks the mapping from a given date, the id of the first event received in that day. `ShardedEventIndex` can be thought of as sorted event ids sharded by event type. `UserEventIndex` can be thought of as sorted event ids sharded by users.

Lastly, `EventTracker` maintains a `PropertiesIndex` backed by [LevelDB Jni](https://github.com/fusesource/leveldbjni) to track what properties keys are available for a given event type and what properties values are available for a given event type and a property key.

### Horizontal scalabiltiy
While EventTracker does not need any information from different users, with a broker in front of EventTracker servers, EventTracker can be easily sharded by users and scale horizontally.

### Performance
In the experiment, the server was bootstrapped with subset of data from Codecademy, which has around 53M events and 2.4M users.

#### Memory footprint
Please beware that the current storage format on disk is fairly inefficient and has serious internal fragmentation. However, when the data are loaded to memory, it will be much more efficient as we would never load those "hole" pages into memory.

| Key Component             | Size in memory  | Note |
|---------------------------|-----------------|------|
| ShardedEventIndex         | 424Mb           | (data size) + (index size) <br>= (event id size * number of events) + negligible<br>= (8 * 53M) |
| UserEventIndex            | 722Mb           | (data size) + (index size) <br>= (event id size * number of events) + (index entry size * number of users)<br>= (8 * 53M) + ((numPointersPerIndexEntry * 2 + 1) * 8 + 4) * 2.4M)<br>= (8 * 53M) + (124 * 2.4M) |
| BloomFitleredEventStorage | 848Mb           | (bloomfilter size) * (number of events) <br>= 16 * 53M |

#### Write performance
#### Query performance
| Query                   | 1st time execution | 2nd time execution | # records |
|-------------------------|--------------------|--------------------|-----------|
| Funnel without filters  | 10K   | | |
| Funnel with filters     | 10K   | | |
| Cohort without filters  | 10K   | | |
| Cohort with filters     | 10K   | | |

## Dashboard
The server comes with a built-in dashboard which is simply some static resources stored in `/web/src/main/resources/frontend` and gets compiled into the server jar file. After running the server, the dashboard can be accessed at [http://localhost:8080](http://localhost:8080). Through the dashboard, you can access the server for your funnel and cohort analysis.

#### Screenshots
#### Password protection
The dashboard comes with insecure basic authentication which send unencrypted information without SSL. Please use it at your own discretion. The default username/password is codecademy/ryzacinc and you can change it by modifying your web.properties file or use the following command to start your server
```bash
USERNAME=foo
PASSWORD=bar
java -Deventtrackerhandler.username=${USERNAME} -Deventtrackerhandler.password=${PASSWORD} -jar web/target/web-1.0-SNAPSHOT.jar
```

## Javascript Library
The project comes with a javascript library which can be integrated with your website. Currently, the library depends on jQuery.

### How to run JS tests
#### install [karma](http://karma-runner.github.io/0.12/index.html)
```bash
cd ${EVENT_TRACKER_DIR}
karma start karma.conf.js
```

### API
The javascript library is extremely simple and heavily inspired by mixpanel. There are only five methods that developer needs to understand. Beware that behind the scene, the library maintains a queue backed by localStorage, buffers the events in the queue, and have a timer reguarly clear the queue. If the browser doesn't support localStorage, instead, a in-memory queue will be created as the EventTracker is created. Also, our implementation relies on the server to track the timestamp of each event. Therefore, in the case of a browser session disconnected before all the events are sent, the remaining events will be sent in the next browser session and thus have the timestamp recorded as the next session starts.

#### window.newEventTracker()
The method will create an EventTracker and start the timer which clears out the event queue in every second (default)
```javascript
var name = "EventTracker";
var options = {
  url: 'http://example.com/',
  flushInterval: 1000 /* in milliseconds */
};
var eventTracker = window.newEventTracker(name, options);
```

#### eventTracker.track()
The method enqueue the given event which will be cleared in batch at every flushInterval. Beware that if there is no identify method called before the track method is called, the library will automatically generate an user id which remain the same for the entire session (clear after the browser tab is closed), and send the generated user id along with the queued event. On the other hand, if there is an identify method called before the track method is called, the user information passed along with the identify method call will be merged to the queued event.
```javascript
eventTracker.track("signup", {
  property_1: 'value1',
  property_2: 'value2'
});
```

#### eventTracker.alias()
The method links the given user to the automatically generated user. Typically, you only want to call this method once, and right after the user successfully signs up.
```javascript
eventTracker.alias('chengtao@codecademy.com');
```

#### eventTracker.identify()
The method tells the library instead of using the automatically generated user information, use the given information instead.
```javascript
eventTracker.identify('chengtao@codecademy.com', {
  user_property_1: 'value1',
  user_property_2: 'value2'
});
```

#### eventTracker.register()
The method allows the developer to add additional information to the generated user.
```javascript
eventTracker.register({
  user_property_1: 'value1',
  user_property_2: 'value2'
});
```

### Scenario and Receipes
#### Link the events sent before and after an user sign up
The following code
```javascript
var eventTracker = window.newEventTracker('EventTracker', { url: 'http://example.com' });
eventTracker.track('pageview', { page: 'home' });
eventTracker.register({
  ip: '10.0.0.1'
});

// after user signup
eventTracker.alias('chengtao@codecademy.com');
eventTracker.identify('chengtao@codecademy.com', {
  gender: 'male'
});
eventTracker.track('pageview', { page: 'learn' });
```
 will result in a funnel like
```javascript
{
  user: 'something generated',
  event: 'pageview',
  page: 'home',
  ip: '10.0.0.1'
}
link 'chengtao@codecademy.com' to 'something generated'
{
  user: 'chengtao@codecademy.com',
  event: 'pageview',
  page: 'learn',
  gender: 'male'
}
```

#### A/B testing
The following code
```javascript
var eventTracker = window.newEventTracker('EventTracker', { url: 'http://example.com' });
eventTracker.identify('chengtao@codecademy.com', {});
eventTracker.track('pageview', {
  page: 'javascript exercise 1',
  experiment: 'fancy feature',
  treatment: 'new'
});
eventTracker.track('submit', {
  page: 'javascript exercise 1'
});
```
and
```javascript
var eventTracker = window.newEventTracker('EventTracker', { url: 'http://example.com' });
eventTracker.identify('bob@codecademy.com', {});
eventTracker.track('pageview', {
  page: 'javascript exercise 1',
  experiment: 'fancy feature',
  treatment: 'control'
});
eventTracker.track('skip', {
  page: 'javascript exercise 1'
});
```
will result in two funnels like
```javascript
{
  user: 'chengtao@codecademy.com',
  event: 'pageview',
  page: 'javascript exercise 1',
  experiment: 'fancy feature',
  treatment: 'new'
}
{
  user: 'chengtao@codecademy.com',
  event: 'submit',
  page: 'javascript exercise 1'
}
```
and
```javascript
{
  user: 'bob@codecademy.com',
  event: 'pageview',
  page: 'javascript exercise 1',
  experiment: 'fancy feature',
  treatment: 'control'
}
{
  user: 'bob@codecademy.com',
  event: 'skip',
  page: 'javascript exercise 1'
}
```

