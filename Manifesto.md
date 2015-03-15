**Creativity**

> With rupy I wanted to remove all obstacles and annoyances programmers experience when developing web applications with other Java HTTP servers. With rupy the compile-bundle-hotdeploy roundtrip (turnaround) is a couple of seconds which increases productivity and allows for a more creative "experimental" style of development.

**Simplicity**

> _You want to serve active HTML? We figured that precompiling the Services before deployment would be simpler than traditional server-side JSP compiling; so we built a Java/HTML [processor](http://rupy.googlecode.com/files/page-0.6.zip), which transforms a HTML page with plain Java in it, to a Java class that prints HTML. Simple!_

**Agility**

> _You won't need to restart Rupy every time you edit a Service (the equivalent of Servlets), there's no servlet context to be reloaded and your session state will be kept out of the box; without "out of memory" issues. This is specially welcomed when you design dynamic GUIs with Ajax and JSON since you don't have to login **again** every time you redeploy._

> With rupy you can remote [hot-replace](Deployment.md) a major refactoring, without _any_ kind of interruption for your end users and _directly_ from your development station to the dev, staging or live environment cluster.

> Configuration by hot-deployment enables you to write all configurations directly in the java code instead of having the traditional XML configuration which often requires a reboot, is typo prone and always induces an extra learning step.

> _This is particularily useful if you have a dev environment against which you test some external integration, use a property that you initialize rupy with to separate the different environments from each other._

> Once you try this you will never go back to fragmented environment development.

**Design**

> The Rupy [API](http://rupy.se/doc/) and core [design](http://code.google.com/p/rupy/wiki/Design) was built from the ground up to be simpler than the JSP and Servlet API's and their existing implementations without losing any needed functionality.

> At the end of 2003, Greg Wilkins, the author of the Jetty Web container and contributor to the Servlet specifications, blogged about some issues with Servlets:

  * No clear separation between the protocol concerns and the application concerns.
  * Unable to take full advantage of the non-blocking NIO due to blocking IO assumptions.
  * Full Servlet Web containers are overkill for many applications.

**Modularity**

> Ideally, you should be able to choose your tools and fix or replace a broken tool quickly. That's what modular software is all about, like binary lego. Dividing your application over many servers is a proven approach to scalability and provides a clear separation of the architectural interfaces. See [this](http://queue.acm.org/detail.cfm?id=1142065) interview with the Amazon CTO on SOA.