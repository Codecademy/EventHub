#!/bin/bash

curl -X POST http://localhost:8080/add_event_type --data "event_type=signup"
curl -X POST http://localhost:8080/add_event_type --data "event_type=view_shopping_cart"
curl -X POST http://localhost:8080/add_event_type --data "event_type=checkout"

curl -X POST http://localhost:8080/register_user --data "external_user_id=chengtao"

curl -X POST http://localhost:8080/track_event --data "event_type=signup&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/track_event --data "event_type=view_shopping_cart&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/track_event --data "event_type=view_shopping_cart&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/track_event --data "event_type=checkout&external_user_id=chengtao&date=20130101&event_property_1=1"
curl -X POST http://localhost:8080/track_event --data "event_type=view_shopping_cart&external_user_id=chengtao&date=20130102&event_property_1=1"
curl -X POST http://localhost:8080/track_event --data "event_type=checkout&external_user_id=chengtao&date=20130103&event_property_1=1"

curl -X POST http://localhost:8080/count_funnel_steps --data "start_date=20130101&end_date=20130103&funnel_steps=signup&funnel_steps=view_shopping_cart&funnel_steps=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1"
curl -X POST http://localhost:8080/count_funnel_steps --data "start_date=20130101&end_date=20130103&funnel_steps=signup&funnel_steps=view_shopping_cart&funnel_steps=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=2"

