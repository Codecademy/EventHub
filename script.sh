#!/bin/bash

curl -X POST http://localhost:8080/events/track --data "event_type=signup&external_user_id=foobar&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=foobar&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/users/alias --data "from_external_user_id=chengtao&to_external_user_id=foobar"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/events/track --data "event_type=checkout&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=chengtao&date=20130102&event_property_1=1"
curl -X POST http://localhost:8080/events/track --data "event_type=checkout&external_user_id=chengtao&date=20130103&event_property_1=1"

curl http://localhost:8080/events/types
curl http://localhost:8080/varz

curl -X POST "http://localhost:8080/count_funnel_steps" --data "start_date=20130101&end_date=20130103&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1"
curl -X POST "http://localhost:8080/count_funnel_steps" --data "start_date=20130101&end_date=20130103&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=2"


curl -X POST "http://localhost:8080/users/add_or_update" --data "external_user_id=chengtao&user_property_1=1"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=20130101&end_date=20130103&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1&uck=user_property_1&ucv=1"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=20130101&end_date=20130103&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1&uck=user_property_1&ucv=2"

curl -X POST "http://localhost:8080/events/retention" --data "start_date=20130101&end_date=20130103&row_event_type=signup&column_event_type=view_shopping_cart&num_days_per_row=1&num_columns=2"


curl -X POST http://localhost:8080/events/batch_track --data "[ {event_type: 'checkout', external_user_id='ctc', date: '20130104', test: 'foo'}, {event_type: 'checkout', external_user_id='ctc', date: '20130104', test: 'bar' }]"
curl http://localhost:8080/events/view\?event_id=6
