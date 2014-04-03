#!/bin/bash

today=`date +'%Y%m%d'`
end_date=`(date -d '+7day' +'%Y%m%d' || date -v '+7d' +'%Y%m%d') 2> /dev/null`

echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=signup&external_user_id=foobar&event_property_1=1"
echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=foobar&event_property_1=1"
echo -e "\033[1;32malias user: \033[0m"
curl -X POST http://localhost:8080/users/alias --data "from_external_user_id=chengtao&to_external_user_id=foobar"
echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=chengtao&event_property_1=1"
echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=checkout&external_user_id=chengtao&event_property_1=1"
echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=view_shopping_cart&external_user_id=chengtao&event_property_1=1"
echo -e "\033[1;32mtrack event: \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=checkout&external_user_id=chengtao&event_property_1=1"

echo -e "\033[1;32mshow all event types: \033[0m"
curl http://localhost:8080/events/types

echo -e "\033[1;32mshow server stats: \033[0m"
curl http://localhost:8080/varz

echo -e "\033[1;32mshow funnel: \033[0m"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=1"
echo -e "\033[1;32mshow funnel: \033[0m"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=signup&funnel_steps[]=view_shopping_cart&funnel_steps[]=checkout&num_days_to_complete_funnel=7&eck=event_property_1&ecv=2"

echo -e "\033[1;32mshow cohort: \033[0m"
curl -X POST "http://localhost:8080/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=view_shopping_cart&num_days_per_row=1&num_columns=2"

echo -e "\033[1;32mbatch track events: \033[0m"
curl -X POST http://localhost:8080/events/batch_track --data "events=[{event_type: signup, external_user_id: foobar, date: 20130101, event_property_1: 1}]"
