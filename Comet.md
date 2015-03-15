<font color='red'><i><b>This covers Javascript longpoll</b></i></font> for custom client and Flash use [cometstream](https://code.google.com/p/rupy/wiki/CometStream).

| XHR | XMLHttpRequest |
|:----|:---------------|
| XDR**<sup>1</sup>** | Cross Domain Request |

_First, try either the [XHR](http://sprout.rupy.se) **or<sup>2</sup>** [XDR](http://hemlistan.se/om) chat demo, it's the little black box in the bottom right corner!_

If you want to chat between XDR _**and<sup>2</sup>**_ XHR chats make sure you use _**two different browser applications<sup>2</sup>**_.

To create and join a channel type _/join `<name>`_

To leave the channel type _/exit_

Type _/list_ for channel listing and _/color `<rrggbb>`_ to change color.


---


With XDR long-poll you can add the chat widget to any page, notwithstanding of platform and domain, without serverside modifications. In a few rows of html, some images, one css and js file; you're truly minutes away from having your own industrial strength, real-time, comet chat; accessible from behind the thickest of firewalls.

Try it now: [download](http://rupy.googlecode.com/files/talk-client-0.5.zip)!


---


Comet is all about corporate firewalls, because you want to reach everyone, everywhere, you need to use HTTP on port 80. Now you can push data over HTTP on port 80 in real-time.

Rupy enables you to connect 10000+ concurrent users with one server, this number can scale to 30000+ depending on your application.

We use long-polling for both transports because it's the most robust way to handle gateway timeouts and also the only implementation that works on all browsers without fragmenting the code (implementing different solutions for some browsers).

In web-browser context also known as 'Ajax Push', see [Comet](http://en.wikipedia.org/wiki/Comet_%28programming%29) for more information.


---


Download the [talk](http://rupy.googlecode.com/files/talk-1.1.zip) application and extract it. Enter the extracted talk/ folder and start rupy with either the _run.bat_ or _run.sh_ script depending on your OS. Then run _ant_ in that same talk/ folder, this should build and deploy the talk.jar application to your rupy server. Lastly, browse to http://localhost:8000/talk.html and chat in real-time over HTTP!

If you want to use rupy talk with apache enable mod\_proxy and add something along these lines:
```
<VirtualHost *:80>
        ProxyPass /poll http://localhost:8000/poll
        ProxyPass /broadcast http://localhost:8000/broadcast
</VirtualHost>
```


---


**<sup>1</sup>** Also known as XSS or cross site scripting.

**<sup>2</sup>** The session is reused for one browser type, so you need to use two different browser types (Internet Explorer, Firefox, Chrome or Safari). Also the HTTP spec. only allows clients to connect maximum two sockets per domain; so the long-poll requests will lock the browser if you try to chat between XHR tabs in the same browser type.