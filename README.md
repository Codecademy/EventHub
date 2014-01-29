# Event Tracker

## How to start the server
### Required dependency
* [jdk7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
* [maven](http://maven.apache.org)

### Compile and run
```bash
cd ${EVENT_TRACKER_DIR}
mvn -am -pl web clean package
java -jar web/target/web-1.0-SNAPSHOT.jar
```

## Manually curl the server

### Add new event type
`curl -X POST http://localhost:8080/add_event_type --data "event_type=signup"`

### Add new user
`curl -X POST http://localhost:8080/register_user --data "external_user_id=chengtao"`

### Add new event
`curl -X POST http://localhost:8080/track_event --data "event_type=signup&external_user_id=chengtao&date=20130101&event_property_1=1"`

### Funnel query
`curl -X POST http://localhost:8080/count_funnel_steps --data "start_date=20130101&end_date=20130103&funnel_steps=signup&funnel_steps=view_shopping_cart&funnel_steps=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=2"`

### Run them all
Just execute `./script.sh` shell script file.

## How to run the load test
Download [Apache Jmeter](http://jmeter.apache.org) and open `./load_test.jmx`

## Performance
TODO
### Experiment setting
### Memory footprint
### Query performance

## Infrastructure
TODO
