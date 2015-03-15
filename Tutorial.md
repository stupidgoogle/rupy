<font color='red'><b><i>Prerequisites</i></b></font>_: You need a [JDK ](http://www.oracle.com/technetwork/java/javase/downloads/index.html) and [Ant](http://ant.apache.org/) installed on your computer and added to the path. Also you might need to add JAVA\_HOME and ANT\_HOME environment variables._

---


### local ###

> Download the [page](http://rupy.googlecode.com/files/page-0.6.zip) application, together with the latest [rupy](http://rupy.googlecode.com/files/rupy-1.1.zip) and extract them to the same folder. Go to the http/ folder first and start rupy with either the _run.bat_ or _run.sh_ script depending on your OS. Then go to the page/ folder and run the _ant local_ task, this should build and deploy the test application to your rupy server. Now try browsing to http://localhost:8000.

---


### remote ###

> If you wan't to deploy to [host.rupy.se](http://host.rupy.se) you can follow the **local** tutorial above with these changes:

  1. You need to edit the _page/build.xml_ script with your domain and the password given to you when you register at [host.rupy.se](http://host.rupy.se).
  1. You should run _ant remote_ instead of _ant local_.

---

We have started the development of [sprout](http://sprout.googlecode.com), a simple blogger; based on top of rupy and [memory](http://memory.googlecode.com). If you want a real-world example of rupy in action try this out!

---

Let's create a rootservice (when you browse to "/") that returns 'hello'.
```
import se.rupy.http.*;
public Hello extends Service {
	public String path() { return "/"; }
	public void filter(Event event) throws Event, Exception {
		event.output().print("hello");
	}
}
```
Compile the Hello class above and put it in a jar file (hello.jar in the example below), then upload it to a running rupy server (run.bat or run.sh) in your ant script like this:
```
<target name="deploy">
	<java fork="yes" classname="se.rupy.http.Deploy" classpath="http.jar">
		<arg line="localhost:8000"/>
		<arg line="hello.jar"/>
		<arg line="secret"/><!-- see run.bat and run.sh -->
	</java>
</target>
```