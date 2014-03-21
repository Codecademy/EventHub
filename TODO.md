# TODO

## JS client library
1. Need an command queue using local storage and a separate worker to make sure the client send requests in order. For instance, if an user call identify and then track, the library has to ensure identify happen before track. Therefore, for all the following apis, they should enqueue their operations instead of firing directly
2. Be able to generate unique id from the client side and persist in the localStorage
3. identify:

    ```javascript
    function identify(userId) {
      // update userId in local storage
      // send request to /users/identify?external_user_id=userId
      // server should return the properties associated with the user
      // merge and store those properties in local storage
    }
    ```
4. alias:

    ```javascript
    function alias(fromUserId) {
      // get userId from local storage
      // send request to /users/alias?from_external_user_id=fromUserId&to_external_user_id=userId
      // server should return the properties associated with the toUser
      // merge and store those properties in local storage
    }
    ```
5. track:

    ```javascript
    function track(eventType, properties) {
      // merge with stored user properties and send request to /events/track
    }
    ```

## Frontend
### Funnel
1. user segmentation widget
2. event filter widget

### User
1. input box to specify userId and list pagniated events

### Retention
1. user segmentation widget
2. event filter widget

## Backend
1. add/update identify and alias endpoint to return user properties
2. retention endpoint to support event & user filtering
3. failure recovery
4. add the benchmark document
5. README - include example
