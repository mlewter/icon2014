# ICON 2014 Demo App
This application demonstrates an OAuth2.0 flow and simple calls against the Infusionsoft API

## Stack
- Play Framework (www.playframework.com) - a scalable Java framework<br>
- AngularJS (angularjs.org) - client side javascript framework<br>
- YAIL (https://bitbucket.org/dietsoda/yail) - a Java SDK for the Infusionsoft API<br>

## Install and Run
It is only necessary to install Play Framework in order to run this application<br>

  1. Install Play Framework<br>
  2. Fork the source<br>
  3. Update both the oauth.client.id & oauth.client.secret values in application.conf<br>
  4. Navigate to your icon2014 project directory<br>
  5. Compile the application: 'play compile'<br>
  6. Start the application using SSL: 'JAVA_OPTS=-Dhttps.port=9443 play run'<br>
  7. Navigate to https://your_local_host:9443/

<b>Please note:</b> you cannot use localhost as your domain. Not sure what to do?<br>
- For OSX, have a look at /private/etc/hosts<br>
- For Windows, it might be c:\WINDOWS\system32\drivers\etc\hosts (I recommend asking google)
<br>
<br>
<b>WARNING:</b> This examples in this app are not intended for production use - but to demonstrate some of the
concepts that were discussed during my ICON14 session.


