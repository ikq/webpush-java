Web Push specification learing.
Building web push message according subscription.


1. Forked from:
   https://github.com/web-push-libs/webpush-java       MIT License
   
Main part of code removed.   
Used mostly as example of Bouncy Castle API usage.

2. Used https://bouncycastle.org/ Crypto library      MIT License.


3. Message encryption and vapid algorithm taken from (or verified with):
   https://github.com/SherClockHolmes/webpush-go      MIT Licence.


Note: 
   It's learning. 
   
   For production use better HTTP client (not HttpsURLConnection).
   
   See for example original: https://github.com/web-push-libs/webpush-java
   
---------------------------------------------------------------------------------
           Usage

Please use tutorial:
https://developers.google.com/web/fundamentals/push-notifications/subscribing-a-user


1. Let set your VAPID server keys into file: 
  c:\webpush-java\src\main\resources\server.properties
  
  (the keys can be generated via https://github.com/web-push-libs/web-push)

2. Create javascript service worker and create web-push subscription.
   Copy from subscription object endpoint, p256dh and auth values to file:
   c:\webpush-java\subscription.properties

3. Install java, gradle. 

4. Build project:
   gradle build
   
5. Run project gradle run --args="Hello Web Push !"
   It send notification to Google (or Mozilla) server, and then send to your Chrome (or Firefox) browser
