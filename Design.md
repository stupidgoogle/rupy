**Thread Model**

Rupy has one selector thread that feeds many worker threads. This model is simple to understand, but hard to synchronize; fortunately that's been done for you!

**Delay Variable**

The events have a network timeout variable called _delay_ which is 5 seconds by default.

The way this works is; that if the event socket IO stalls during an ongoing request / response the server frees the worker from this event.

Since 0.4.1 delay is also a cleanup duration for dead TCP layer sockets. Which means that if a worker thread is idle with an event for more than delay then the event is disregarded and the socket canceled.

**API**

The API is designed to be as small as possible, and therefore you have to handle the asynchronous events being recycled. While other Comet API's provide you with special methods for asynchronous callbacks, we rely on the Event.push() method to tell us wether an event is "real" or "recycled".

**Code**

The hierarchy of the code is as follows:

```

Event -+-- Query <-- Input <--- +---------+
       |                        | Browser |
       +-> Reply --> Output --> +---------+

```

**Hot-Deployment**

When you upload your application jar over HTTP or put it manually in the /app folder it will be extracted so that all .java files are loaded into a separate classloader (Service implementations are instantiated) and all other files are copied into a subfolder (/content on standalone and /your.host on hosted).

When the whole jar has been loaded/copied the classloader is swapped so all requests will be handled by the new classloader. This has one major benefit: if you write your code in the right way your users (or developers for that matter) _will continue to be able to use the site even though you release a patch while they are on the site_ without having to re-login. This is a HUGE advancement in server side technology, and once developers understand this there is no going back!