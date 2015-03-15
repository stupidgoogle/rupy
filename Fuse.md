This requires latest [rupy](http://rupy.se/rupy.zip).

This is how the current client/server tools work in a co-operative solution:

```
time <----------------  goes forth & back  ------------------>
wait      ↓↓↓↓   ↓↓↓↓↓   ↓↓↓↓   ↓↓↓↓   ↓↓↓↓↓   ↓↓↓↓           
loss             ↓↓↓↓↓                 ↓↓↓↓↓                  (CPU)

+--------+    +---------+           +---------+    +---------+
|        | -> |         | ---+----> |         |    |         |
|  brow  |    |  sync1  |    |      |  sync2  | .. |  serv2  |
|        | <- |         | <----+--- |         |    |         |
+--------+    +---------+    | |    +---------+    +---------+
                             | |
                         +---------+
                         |         |
                         |  serv1  |
                         |         |
                         +---------+
```

  * `brow` = <font color='CD5C5C'><code>sync</code></font> `req`         <font color='grey'> <code>(browser)</code></font>
  * `serv` = <font color='CD5C5C'><code>sync</code></font> `res`         <font color='grey'> <code>(database)</code></font>
  * `sync` = <font color='CD5C5C'><code>sync</code></font> `req` & `res` <font color='grey'> <code>(old web-server)</code></font>

The idea is to only wait in the network (no related <font color='red'><code>IO-wait</code></font>) and to wait for multiple things at the same time:

```
time <---------------  goes forth & back  --------------->
wait      ↓↓↓↓           ↓↓   ↓↓           ↓↓↓↓

+--------+    +---------+       +---------+    +---------+
|        | -> |         | -+--> |         |    |         |
|  brow  |    |  asyn1  |  |    |  asyn2  | .. |  serv2  |
|        | <- |         | <--+- |         |    |         |
+--------+    +---------+  | |  +---------+    +---------+
                           | |
                           | +---+
                           +---+ |
                               | |
                           +---------+
                           |         |
                           |  serv1  |
                           |         |
                           +---------+
```

  * `brow` = <font color='CD5C5C'><code>sync</code></font> `req`         <font color='grey'> <code>(browser)</code></font>
  * `serv` = <font color='CD5C5C'><code>sync</code></font> `res`         <font color='grey'> <code>(database)</code></font>
  * `asyn` = <font color='5CCD5C'><code>asyn</code></font> `req` & `res` <font color='grey'><code>(</code></font><font color='green'><code>FUSE</code></font><font color='grey'><code>)</code></font>

For this we introduce a new concept; <font color='green'><code>FUSE</code></font>. With <font color='green'><code>FUSE</code></font> all threads <font color='5CCD5C'><code>asyn</code></font> end-to-end or socket-to-socket if you will, they never wait for anything.

Another way to describe this is time and <font color='red'><code>IO-wait</code></font> shown in the graph below:

  1. <font color='CD5C5C'><code>sync</code></font> `req` & `res` <font color='grey'> <code>(old web-server)</code></font>
  1. <font color='5CCD5C'><code>asyn</code></font> `req` <font color='grey'> <code>(netflix)</code></font>
  1. <font color='5CCD5C'><code>asyn</code></font> `req` & `res` <font color='grey'><code>(</code></font><font color='green'><code>FUSE</code></font><font color='grey'><code>)</code></font>

![http://rupy.se/fuse.gif](http://rupy.se/fuse.gif)

You can imagine that the compounded savings in terms of time and wait are huge, specially when the system grows in concurrency and depth. As a matter of fact, you can't scale at all without <font color='5CCD5C'><code>asyn</code></font> `req` and with <font color='5CCD5C'><code>asyn</code></font> `req` & `res` you're far better off.

If you want to use [µSOA](https://code.google.com/p/rupy/wiki/Process) this is the most performant way to build your services network.

You can even use this as a load balancer.

Another use case for this is our cluster database [ROOT](https://code.google.com/p/rupy/wiki/Persistence).

This [example](http://root.rupy.se/fuse) fuses data <font color='5CCD5C'><code>asyn</code></font> from multiple servers to your browser ASAP on a raspberry pi with zero thread <font color='red'><code>IO-wait</code></font>:
```
// TODO: Improve latch.
public static class Fuse extends Service {
	public String path() { return "/fuse"; }

	public void filter(final Event event) throws Event, Exception {
		if(event.push()) {
			Http[] http = (Http[]) event.query().get("http");
			Output out = event.output();
			int done = 0;

			for(int i = 0; i < http.length; i++) {
				if(http[i].print(out)) {
					done++;
				}
			}

			if(done == http.length) {
				out.println("</body></html>");
				out.finish();
			}

			out.flush();
		}
		else {
			event.query().put("time", System.currentTimeMillis());

			Http[] http = new Http[2];

			String path = "/v1/public/yql" +
			"?q=select%20*%20from%20yahoo.finance.xchange%20where%20pair%20in%20(%22USDSEK%22)" +
			"&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

			http[0] = new Http("one", "strip.rupy.se", "/?a=martin+kellerman&body", event);
			http[1] = new Http("two", "query.yahooapis.com", path, event);

			for(int i = 0; i < http.length; i++) {
				// The 10 at the end is very important.
				// It means the socket will be kept alive 
				// for 10 seconds since last active.
				// This is our Murphy's law solution.
				event.daemon().client().send(http[i].host, http[i], 10);
			}

			event.query().put("http", http);
		}
	}

	public static class Http extends Async.Work {
		private String name;
		private String host;
		private String path;
		private String body;
		private Event event;

		public Http(String name, String host, String path, Event event) {
			this.name = name;
			this.host = host;
			this.path = path;
			this.event = event;
		}

		public boolean print(Output out) throws Exception {
			if(event.query().get(name) instanceof Http) {
				long time = System.currentTimeMillis() - event.big("time");
				System.out.println("  " + name + " " + time + " ms.");
				out.println(body);
				event.query().put(name, "done");
				return true;
			}
			else if(event.query().get(name) instanceof String) {
				return ((String) event.query().get(name)).equals("done");
			}

			return false;
		}

		public void send(Async.Call call) throws Exception {
			call.get(path, "Host: " + host + "\r\n");
		}

		public void read(String body) throws Exception {
			this.body = body;
			event.query().put(name, this);
			System.out.println("  " + name + " success " + event.reply().wakeup());
			// Normally it's better to wakeup the response after all client requests are finished, 
			// but we wan't to show you the parts as they complete on-the-fly!
		}

		public void fail(String host, Exception e) throws Exception {
			if(e instanceof Async.Timeout) {
				System.out.println("  " + e);
				event.daemon().client().send(host, this, 10);
			}
			else {
				e.printStackTrace();
				event.query().put("response", e.toString());
				System.out.println("  " + name + " failure " + event.reply().wakeup());
			}
		}
	};
}
```