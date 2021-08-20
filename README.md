## Web-Push learing (encrypt push message according subscription)


1. Forked from:
  https://github.com/web-push-libs/webpush-java        *MIT License*
   
   To learning purposes, most of the code has been simplified or removed.
   The original code is more mature/complex. 
   
2. Used Bouncy Castle Crypto library      *MIT License*
   https://bouncycastle.org/ 

3. Message encryption and vapid algorithm verified with:
   https://github.com/SherClockHolmes/webpush-go      *MIT Licence*


### Note: 
   For production use better HTTP client (not HttpsURLConnection).
   
   See for example original: https://github.com/web-push-libs/webpush-java
   
### How to use

Please read tutorial:
https://developers.google.com/web/fundamentals/push-notifications/subscribing-a-user


1. Generate VAPID server key
  (the keys can be generated via https://github.com/web-push-libs/web-push)
  
      Set your VAPID server keys into file: 
  c:\webpush-java\src\main\resources\server.properties
  
2. Create javascript service worker and create web-push subscription.

    Copy from subscription object endpoint, p256dh and auth values to file:
   c:\webpush-java\subscription.properties

3. Install 
- java  (>= JDK 1.8)
- gradle  

4. Build project:
   gradle build
   
5. Run example:
   gradle run --args="Hello Web Push !"
   The example send notification to Google (or Mozilla) server and then to your browser.
   
