#!/bin/bash

URL="http://localhost:8080"
today=`date +'%Y%m%d'`
end_date=`(date -d '+14day' +'%Y%m%d' || date -v '+14d' +'%Y%m%d') 2> /dev/null`

echo -e "\033[1;31m=============== event related endpoints ================\033[0m\n"
echo -e "\033[1;32mtrack event:\033[0m anonymous user (w/ gnerated user id: generated_id_123) visited Codecademy's home page"
curl -X POST ${URL}/events/track --data "event_type=pageview&external_user_id=generated_id_123&page=home&experiment=homepage_v1&treatment=control"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to first exercise in the home page \033[0m"
curl -X POST ${URL}/events/track --data "event_type=submission&external_user_id=generated_id_123&exercise=homepage_1"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to the second exercise in the home page \033[0m"
curl -X POST ${URL}/events/track --data "event_type=submission&external_user_id=generated_id_123&exercise=homepage_2"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user sign up\033[0m"
curl -X POST ${URL}/events/track --data "event_type=signup&external_user_id=generated_id_123&experiment=signup_v1&treatment=control"
echo ""
echo -e "\033[1;32malias user :\033[0m alias from chengtao@codecademy.com to generated_id_123 \033[0m"
curl -X POST ${URL}/users/alias --data "from_external_user_id=chengtao@codecademy.com&to_external_user_id=generated_id_123"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user visited Codecademy's tracks page\033[0m"
curl -X POST ${URL}/events/track --data "event_type=pageview&external_user_id=chengtao@codecademy.com&page=tracks"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user started the javascript track\033[0m"
curl -X POST ${URL}/events/track --data "event_type=start_track&external_user_id=chengtao@codecademy.com&track=javascript"
echo ""
echo -e "\033[1;32mtrack event:\033[0m the user made a submission to the first exercise in the javascript track \033[0m"
curl -X POST ${URL}/events/track --data "event_type=submission&external_user_id=chengtao@codecademy.com&exercise=javascript_1"
echo ""

echo -e "\033[1;32mbatch track events for a few other users:\033[0m"
curl -X POST ${URL}/events/batch_track --data "events=[
{
  external_user_id: 'chengtao1@codecademy.com',
  event_type: 'pageview',
  experiment: 'homepage_v1',
  treatment: 'new console',
  page: 'home'
}, {
  external_user_id: 'chengtao1@codecademy.com',
  event_type: 'signup',
  experiment: 'signup_v1',
  treatment: 'fancy onboarding'
}, {
  external_user_id: 'chengtao1@codecademy.com',
  event_type: 'submission'
}, {
  external_user_id: 'chengtao2@codecademy.com',
  event_type: 'signup',
  experiment: 'signup_v1',
  treatment: 'fancy onboarding'
}, {
  external_user_id: 'chengtao2@codecademy.com',
  event_type: 'submission'
}, {
  external_user_id: 'chengtao3@codecademy.com',
  event_type: 'pageview',
  experiment: 'homepage_v1',
  treatment: 'new console',
  page: 'home'
}, {
  external_user_id: 'chengtao3@codecademy.com',
  event_type: 'signup',
  experiment: 'signup_v1',
  treatment: 'fancy onboarding'
}, {
  external_user_id: 'chengtao4@codecademy.com',
  event_type: 'pageview',
  experiment: 'homepage_v1',
  treatment: 'new console',
  page: 'home'
}]
"
echo ""

echo -e "\033[1;32mshow all event types:\033[0m"
curl ${URL}/events/types
echo ""

echo -e "\033[1;32mshow all event keys for \"signup\" event type:\033[0m"
curl '${URL}/events/keys?event_type=signup'
echo ""

echo -e "\033[1;32mshow all event values for 'treatment' event key and \"signup\" event type:\033[0m"
curl '${URL}/events/values?event_type=signup&event_key=treatment'
echo ""

echo -e "\033[1;32mshow all event values for 'treatment' event key, \"signup\" event type and prefix \"fa\":\033[0m"
curl '${URL}/events/values?event_type=signup&event_key=treatment&prefix=fa'
echo ""

echo -e "\033[1;32mshow all event types:\033[0m"
curl ${URL}/events/types
echo ""

echo -e "\033[1;32mshow server stats:\033[0m"
curl ${URL}/varz
echo ""

echo -e "\033[1;32mshow funnel:\033[0m pageview -> signup -> submission"
curl -X POST "${URL}/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&funnel_steps[]=submission&num_days_to_complete_funnel=7"
echo ""

echo -e "\033[1;32mshow cohort:\033[0m signup -> submission"
curl -X POST "${URL}/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=submission&num_days_per_row=7&num_columns=2"
echo ""

echo -e "\033[1;32mshow A/B testing signup funnel (control):\033[0m pageview -> signup"
curl -X POST "${URL}/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&num_days_to_complete_funnel=7&efk0[]=experiment&efv0[]=homepage_v1&efk0[]=treatment&efv0[]=control&efk0[]=page&efv0[]=home"
echo ""

echo -e "\033[1;32mshow A/B testing signup funnel (new console):\033[0m pageview -> signup"
curl -X POST "${URL}/events/funnel" --data "start_date=${today}&end_date=${end_date}&funnel_steps[]=pageview&funnel_steps[]=signup&num_days_to_complete_funnel=7&efk0[]=experiment&efv0[]=homepage_v1&efk0[]=treatment&efv0[]=new console&efk0[]=page&efv0[]=home"
echo ""

echo -e "\033[1;32mshow A/B testing cohort:\033[0m signup -> submission"
curl -X POST "${URL}/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=submission&num_days_per_row=7&num_columns=2&refk[]=experiment&refv[]=signup_v1&refk[]=treatment&refv[]=control"
echo ""

echo -e "\033[1;32mshow A/B testing cohort:\033[0m signup -> submission"
curl -X POST "${URL}/events/cohort" --data "start_date=${today}&end_date=${end_date}&row_event_type=signup&column_event_type=submission&num_days_per_row=7&num_columns=2&refk[]=experiment&refv[]=signup_v1&refk[]=treatment&refv[]=fancy onboarding"
echo ""


echo -e "\033[1;31m=============== user related endpoints ================\033[0m\n"
echo -e "\033[1;32mshow the first event for chengtao@codecademy.com\033[0m"
curl ${URL}/users/timeline\?external_user_id\=chengtao@codecademy.com\&offset\=0\&num_records\=1
echo ""

echo -e "\033[1;32mshow the following 10 events for chengtao@codecademy.com\033[0m"
curl ${URL}/users/timeline\?external_user_id\=chengtao@codecademy.com\&offset\=1\&num_records\=10
echo ""

echo -e "\033[1;32mshow ids of users whose email is chengtao1@codecademy.com\033[0m"
curl -X POST ${URL}/users/find --data "ufk[]=external_user_id&ufv[]=chengtao1@codecademy.com"
echo ""

echo -e "\033[1;32madd new user information\033[0m"
curl -X POST ${URL}/users/add_or_update --data "external_user_id=chengtao5@codecademy.com&foo=bar&hello=world1"
echo ""

echo -e "\033[1;32mupdate user information\033[0m"
curl -X POST ${URL}/users/add_or_update --data "external_user_id=chengtao3@codecademy.com&foo=bar&hello=world2"
echo ""

echo -e "\033[1;32mshow all user keys:\033[0m"
curl -X POST ${URL}/users/keys
echo ""

echo -e "\033[1;32mshow all user values for 'hello' user key:\033[0m"
curl -X POST ${URL}/users/values --data "user_key=hello"
echo ""

echo -e "\033[1;32mshow all user values for 'hello' user key and prefix 'w':\033[0m"
curl -X POST ${URL}/users/values --data "user_key=hello&prefix=w"
echo ""
