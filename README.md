# Event Tracker
**Table of Contents**
- [Server](#server)
- [Dashboard](#dashboard)
- [Javascript Library](#javascript-library)

## Server
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

### Manually testing server endpoints
#### Test with curl
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

#### Run them all
```bash
cd ${EVENT_TRACKER_DIR}; ./script.sh
```

### How to run the load test
We use [Apache Jmeter](http://jmeter.apache.org) for load testing, and the load testing script can be found in `${EVENT_TRACKER_DIR}/./load_test.jmx`

### Architecture

### Performance
#### Experiment setting
#### Memory footprint
#### Query performance

## Dashboard
The server comes with a built-in dashboard which can be found at [http://localhost:8080](http://localhost:8080). Through the dashboard, you can access the server for your funnel and cohort analysis.

## Javascript Library
The project comes with a javascript library which can be integrated with your website. Currently, the library depends on jQuery.

### How to run JS tests
#### install [karma](http://karma-runner.github.io/0.12/index.html)
```bash
cd ${EVENT_TRACKER_DIR}
karma start karma.conf.js
```

### API
#### window.newEventTracker()
#### EventTracker.track()
#### EventTracker.alias()
#### EventTracker.identify()
#### EventTracker.register()

### Receipes
#### Link the events sent before and after an user sign up
#### A/B testing
