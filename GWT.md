<font color='red'><b><i>NEW</i></b></font> [bitcoinbankbook.com](http://bitcoinbankbook.com) is an example of GWT on rupy.

**Decode, invoke and encode**

This is using GWT 2.1.1 or later.

This example uses 'book' as project/jar name, replace this to suit your project/jar.

Add this inner class below to GreetingServiceImpl.java in the generated GWT start app:

```
public static class Call extends Service {
	public String path() { return "/book/greet"; }
	public void filter(Event event) throws Event, Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Deploy.pipe(event.input(), out);
		String req = out.toString();

		/*
		 * on host.rupy.se this is done for you
		 * so you should remove this line since
		 * you don't have the privileges to alter
		 * context class loader there.
		 */
		Thread.currentThread().setContextClassLoader(event.daemon().archive("book"));

		RPCRequest rpc = RPC.decodeRequest(req, GreetingService.class);		  

		String res = null;

		try {
			Object response = rpc.getMethod().invoke(new GreetingServiceImpl(), rpc.getParameters());
			res = RPC.encodeResponseForSuccess(rpc.getMethod(), response);
		}
		catch(InvocationTargetException e) {
			res = RPC.encodeResponseForFailure(rpc.getMethod(), e.getCause());
		}

		event.output().print(res);
	}
}
```

Remove these servlet specific lines:

```
String serverInfo = getServletContext().getServerInfo();
String userAgent = getThreadLocalRequest().getHeader("User-Agent");
```

Then add http.jar and gwt-user.jar to the classpath and start rupy:

```
java -classpath "lib\http.jar;lib\gwt-user.jar" se.rupy.http.Daemon -verbose -pass secret -log
```

Finally deploy the war file as a jar to rupy with the following build.xml modifications and everything will just magically work:

```
<path id="project.class.path">
	...
	<pathelement location="lib/http.jar"/>
</path>

<target name="gwtc" depends="javac" ...>
	...
	<mkdir dir="bin"/>
	<jar jarfile="bin/book.jar">
		<manifest>
			<attribute name="Built-By" value="${user.name}"/>
		</manifest>
		<fileset dir="war"/>
	</jar>
	<java fork="yes" classname="se.rupy.http.Deploy" classpath="lib/http.jar">
		<arg line="localhost:8000"/>
		<arg line="bin/book.jar"/>
		<arg line="secret"/>
  	</java>
</target>
```

Before 2.1.1 you had to use SerializableException to encode a failure; even though the documentation told you not to.