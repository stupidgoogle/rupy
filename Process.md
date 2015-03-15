### 1. Guide ###

  1. **µSOA<sup>1</sup>**: _One developer per microservice.**<sup>2</sup>**_ HTTP is the only interface between _everything_.
  1. **Agile**: You need to be able to hot-deploy _everywhere_ because you'll want to work with others remotely at high iteration speeds.
  1. **Architecture**: The developer is completely free to choose any software (languages, tools, persistence etc.) of the service.
  1. **Turnaround**: Maximum build & deploy time on _live_ is how long it takes for you to switch to the test client. (1-2 seconds)
  1. **Ownership**: The developer owns and is responsible for the service _everywhere_ at all times.
  1. **Errors**: Should be pushed from each live client to the service developers mobile phone in realtime.
  1. **TDD**: Test only what makes development slip due to complexity.

### 2. Manage ###

  1. The data -producer, -consumer and developer trio have 1 minute per person remote**<sup>3</sup>** daily yester/morrow meetings.
  1. No other mandatory scheduled meetings.
  1. You should be able to work from home.
  1. If you finish quicker than expected, you own that remaining time.
  1. No documentation. _Automatic_ [API](http://sprout.rupy.se/api).

### 3. Store ###

  1. If [persistence](https://code.google.com/p/rupy/wiki/Persistence) can be a file, make it a file.
  1. If you need:
    * Index; use the file system.
    * Relations; use a graph.
    * Full text search; combine the [two](http://root.rupy.se/node/data/text/full%20text%20search)!
  1. Your persistence should use HTTP.

### 4. Develop ###

  1. Each module describes it's own use: f.ex. if you browse to [/login](http://sprout.rupy.se/login) you should be able to login! ([model](http://rupy.se/sprout/se/rupy/sprout/Node.html), [view](https://code.google.com/p/sprout/source/browse/trunk/res/jar/login.html) and [controller](https://code.google.com/p/sprout/source/browse/trunk/src/se/rupy/sprout/User.java#207))
  1. Use async for:
    * Server [response](https://code.google.com/p/rupy/wiki/AsyncResponse); if you have high iowait or realtime services.
    * Client [requests](https://code.google.com/p/rupy/source/browse/trunk/src/se/rupy/http/Async.java); if you can concurrently build the response to reduce latency.
    * For top performance and speed [FUSE](https://code.google.com/p/rupy/wiki/Fuse) both.
  1. Use the same development keyboard and OS everywhere.
  1. Use JavaScript injection for modularity, over; chunked response, CO-XHR or even XSS (careful with the cookies though).


### 5. Host ###

  1. Minimum two subdomains per service (f.ex. dev.`<`name`>`.host.com and `<`name`>`.host.com):
    * On same server provider.
    * Port 80 only.
  1. Increase socket memory if you [serve large files](https://code.google.com/p/rupy/wiki/ServeLargeFiles).
  1. If it makes economical sense to use Akamai your software is probably bad.
  1. DNS Roundrobin across backbones allows for 100% uptime. But this requires a new kind of real-time distributed [persistence](https://code.google.com/p/rupy/wiki/Persistence).
  1. Install, configure, [monitor](http://monitor.rupy.se) and notify yourself; even the hardcore [stuff](http://one.rupy.se/panel).

**<sup>1</sup>** Micro Service Oriented Architecture an evolution of [Microservices](http://martinfowler.com/articles/microservices.html) and [SOA](http://queue.acm.org/detail.cfm?id=1142065).<br>
<b><sup>2</sup></b> If two people choose the exact same tools they can work on the same service; the whole point is that nobody should have to compromise with their tools. The work should not be to configure others tools, but to design a specific product with clear separation of responsibilities between developers.<br>
<b><sup>3</sup></b> <a href='https://www.youtube.com/watch?v=5XD2kNopsUs'>Jason Fried</a>, <a href='https://weworkremotely.com'>We Work Remotely</a>, <a href='http://stet.editorially.com/articles/making-remote-teams-work/'>Mandy Brown</a>.<br>