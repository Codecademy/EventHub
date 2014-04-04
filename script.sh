#!/bin/bash

today=`date +'%Y%m%d'`
end_date=`(date -d '+14day' +'%Y%m%d' || date -v '+14d' +'%Y%m%d') 2> /dev/null`

echo -e "\033[1;32mtrack event:\033[0m anonymous user (w/ gnerated user id: generated_id_123) visited Codecademy's home page"
curl -X POST http://localhost:8080/events/track --data "event_type=pageview&external_user_id=generated_id_123&page=home&experiment_signup_v1=control"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to first exercise in the home page \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=submission&external_user_id=generated_id_123&exercise=homepage_1"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to the second exercise in the home page \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=submission&external_user_id=generated_id_123&exercise=homepage_2"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user sign up\033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=signup&external_user_id=generated_id_123"
echo ""
echo -e "\033[1;32malias user :\033[0m alias from chengtao@codecademy.com to generated_id_123 \033[0m"
curl -X POST http://localhost:8080/users/alias --data "from_external_user_id=chengtao@codecademy.com&to_external_user_id=generated_id_123"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user visited Codecademy's tracks page\033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=pageview&external_user_id=chengtao@codecademy.com&page=tracks"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user started the javascript track\033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=start_track&external_user_id=chengtao@codecademy.com&track=javascript"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to the first exercise in the javascript track \033[0m"
curl -X POST http://localhost:8080/events/track --data "event_type=submission&external_user_id=chengtao@codecademy.com&exercise=javascript_1"
echo ""

echo -e "\033[1;32mbatch track events for a few other users:\033[0m"
curl -X POST http://localhost:8080/events/batch_track --data "events=[
{
  external_user_id: 'chengtao+1@codecademy.com',
  event_type: 'pageview',
  experiment_signup_v1: 'new console',
  page: 'home'
}, {
  external_user_id: 'chengtao+1@codecademy.com',
  event_type: 'signup'
}, {
  external_user_id: 'chengtao+1@codecademy.com',
  event_type: 'submission'
}, {
  external_user_id: 'chengtao+2@codecademy.com',
  event_type: 'signup'
}, {
  external_user_id: 'chengtao+2@codecademy.com',
  event_type: 'submission'
}, {
  external_user_id: 'chengtao+3@codecademy.com',
  event_type: 'pageview',
  experiment_signup_v1: 'new console',
  page: 'home'
}, {
  external_user_id: 'chengtao+3@codecademy.com',
  experiment_signup_v1: 'new console',
  event_type: 'signup'
}]
"
echo ""

echo -e "\033[1;32mshow all event types:\033[0m"
curl http://localhost:8080/events/types
echo ""

echo -e "\033[1;32mshow server stats:\033[0m"
curl http://localhost:8080/varz
echo ""

echo -e "\033[1;32mshow funnel:\033[0m pageview -> signup -> submission"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&funnel_steps[]=submission&num_days_to_complete_funnel=7"
echo ""

echo -e "\033[1;32mshow cohort:\033[0m signup -> submission"
curl -X POST "http://localhost:8080/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=submission&num_days_per_row=7&num_columns=2"
echo ""

echo -e "\033[1;32mshow A/B testing signup funnel (control):\033[0m pageview -> signup"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&num_days_to_complete_funnel=7&efk=experiment_signup_v1&efv=control"
echo ""

echo -e "\033[1;32mshow A/B testing signup funnel (new console):\033[0m pageview -> signup"
curl -X POST "http://localhost:8080/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&num_days_to_complete_funnel=7&efk=experiment_signup_v1&efv=new console"
echo ""
