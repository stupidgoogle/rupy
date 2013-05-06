package se.rupy.http;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.text.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.nio.channels.*;

/**
 * A tiny HTTP daemon. The whole server is non-static so that you can launch
 * multiple contained HTTP servers in one application on different ports.<br>
 * <br>
 * See the configurations: {@link #Daemon()}
 * 
 * @author marc
 */

public class Daemon implements Runnable {
	private int selected, valid, accept, readwrite; // panel stats
	private HashMap archive, service;
	private Heart heart;
	private Selector selector;
	private String domain;
	private static DateFormat DATE;

	Chain workers, queue;
	Properties properties;
	PrintStream out, access, error;
	AccessControlContext control;
	ConcurrentHashMap events, session;
	int threads, timeout, cookie, delay, size, port, cache;
	boolean verbose, debug, host, alive, panel;

	/**
	 * Don't forget to call {@link #start()}.
	 */
	public Daemon() {
		this(new Properties());
	}

	/**
	 * Don't forget to call {@link #start()}. The parameters below
	 * should be in the properties argument. The parenthesis contains the default value.<br><br>
	 * <table cellpadding="10">
	 * <tr><td valign="top"><b>pass</b> ()
	 * </td><td>
	 *            the pass used to deploy services with {@link Deploy} via HTTP POST, 
	 *            not adding this disables remote hot-deploy.
	 * </td></tr>
	 * <tr><td valign="top"><b>port</b> (8000)
	 * </td><td>
	 *            which TCP port.
	 * </td></tr>
	 * <tr><td valign="top"><b>threads</b> (5)
	 * </td><td>
	 *            how many worker threads, the daemon also starts one selector 
	 *            and one heartbeat thread.
	 * </td></tr>
	 * <tr><td valign="top"><b>timeout</b> (300)
	 * </td><td>
	 *            session timeout in seconds or 0 to disable sessions 
	 * </td></tr>
	 * <tr><td valign="top"><b>cookie</b> (4)</td><td>
	 *            session key character length; default and minimum is 4, > 10 can 
	 *            be considered secure.
	 * </td></tr>
	 * <tr><td valign="top"><b>delay</b> (5000)
	 * </td><td>
	 *            milliseconds before started event gets dropped due to inactivity. 
	 *            This is also the dead socket worker cleanup variable, so if 
	 *            a worker has a socket that hasn't been active for longer than 
	 *            this the worker will be released and the socket deemed as dead.
	 * </td></tr>
	 * <tr><td valign="top"><b>size</b> (1024)</td><td>
	 *            IO buffer size in bytes, should be proportional to the data sizes 
	 *            received/sent by the server currently this is input/output- 
	 *            buffer, chunk-buffer, post-body-max and header-max lengths! :P
	 * </td></tr>
	 * <tr><td valign="top"><b>live</b> (false)
	 * </td><td>
	 *            is this rupy running live.
	 * </td></tr>
	 * <tr><td valign="top"><b>cache</b> (86400) <i>requires</i> <b>live</b>
	 * </td><td valign="top">
	 *            seconds to hard cache static files.
	 * </td></tr>
	 * <tr><td valign="top"><b>verbose</b> (false)
	 * </td><td>
	 *            to log information about these startup parameters, high-level 
	 *            info for each request and deployed services overview.
	 * </td></tr>
	 * <tr><td valign="top"><b>debug</b> (false)
	 * </td><td>
	 *            to log low-level NIO info for each request and class 
	 *            loading info.
	 * </td></tr>
	 * <tr><td valign="top"><b>log</b> (false)
	 * </td><td>
	 *            simple log of access and error in /log.
	 * </td></tr>
	 * <tr><td valign="top"><b>host</b> (false)
	 * </td><td>
	 *            to enable virtual hosting, you need to name the deployment 
	 *            jars [host].jar, for example: <i>host.rupy.se.jar</i>. 
	 *            Also if you want to deploy root domain, just deploy www.[host]; 
	 *            so for example <i>www.rupy.se.jar</i> will trigger <i>http://rupy.se</i>. 
	 *            To authenticate deployments you should use a properties file 
	 *            called <i>passport</i> in the rupy root where you store [host]=[pass].
	 * </td></tr>
	 * <tr><td valign="top"><b>domain</b> (host.rupy.se) <i>requires</i> <b>host</b>
	 * </td><td>
	 *            if your host is a <a href="http://en.wikipedia.org/wiki/Platform_as_a_service">PaaS</a> 
	 *            on <i>one machine</i>; add the passport file to your control domain 
	 *            app folder instead (for example app/host.rupy.se/passport; hide it 
	 *            from downloading with the code below) and create a symbolic link to 
	 *            that in the rupy root.
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;public static class Secure extends Service {<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;public String path() { return "/passport"; }<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;public void filter(Event event) throws Event, Exception {
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;event.output().print("Nice try!");
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>
&nbsp;&nbsp;&nbsp;&nbsp;}<br>
<br></tt>
	 *            if you are hosting a <a href="http://en.wikipedia.org/wiki/Platform_as_a_service">PaaS</a> 
	 *            <i>across a cluster</i>, you have to hook your control domain app up with 
	 *            {@link Daemon#set(Listener listener)}. And reply "OK" if the 
	 *            {"file": "[host].jar", "pass": "[pass]"} sent by {@link Deploy} 
	 *            authenticates.
	 * </td></tr>
	 * </table>
	 */
	public Daemon(Properties properties) {
		this.properties = properties;

		threads = Integer.parseInt(properties.getProperty("threads", "5"));
		cookie = Integer.parseInt(properties.getProperty("cookie", "4"));
		port = Integer.parseInt(properties.getProperty("port", "8000"));
		timeout = Integer.parseInt(properties.getProperty("timeout", "300")) * 1000;
		delay = Integer.parseInt(properties.getProperty("delay", "5000"));
		size = Integer.parseInt(properties.getProperty("size", "1024"));
		cache = Integer.parseInt(properties.getProperty("cache", "86400"));

		verbose = properties.getProperty("verbose", "false").toLowerCase()
				.equals("true");
		debug = properties.getProperty("debug", "false").toLowerCase().equals(
				"true");
		host = properties.getProperty("host", "false").toLowerCase().equals(
				"true");
		panel = properties.getProperty("panel", "false").toLowerCase().equals(
				"true");

		if(host) {
			domain = properties.getProperty("domain", "host.rupy.se");
			PermissionCollection permissions = new Permissions();
			control = new AccessControlContext(new ProtectionDomain[] {
					new ProtectionDomain(null, permissions)});
		}

		if (!verbose) {
			debug = false;
		}

		archive = new HashMap();
		service = new HashMap();
		session = new ConcurrentHashMap();
		events = new ConcurrentHashMap();

		workers = new Chain();
		queue = new Chain();

		try {
			out = new PrintStream(System.out, true, "UTF-8");

			if(properties.getProperty("log") != null || properties.getProperty("test", "false").toLowerCase().equals(
					"true")) {
				log();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Properties properties() {
		return properties;
	}

	protected void log() throws IOException {
		File file = new File("log");

		if(!file.exists()) {
			file.mkdir();
		}

		access = new PrintStream(new FileOutputStream(new File("log/access.txt")), true, "UTF-8");
		error = new PrintStream(new FileOutputStream(new File("log/error.txt")), true, "UTF-8");

		DATE = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
	}

	protected void error(Event event, Throwable t) throws IOException {
		if (error != null && t != null && !(t instanceof Failure.Close)) {
			Calendar date = Calendar.getInstance();
			StringBuilder b = new StringBuilder();

			b.append(DATE.format(date.getTime()));
			b.append(' ');
			b.append(event.remote());
			b.append(' ');
			b.append(event.query().path());

			String parameters = event.query().parameters();

			if(parameters != null) {
				b.append(' ');
				b.append(parameters);
			}

			b.append(Output.EOL);

			error.write(b.toString().getBytes("UTF-8"));

			t.printStackTrace(error);
		}
	}

	protected String access(Event event) throws IOException {
		if (access != null && !event.reply().push()) {
			Calendar date = Calendar.getInstance();
			StringBuilder b = new StringBuilder();

			b.append(DATE.format(date.getTime()));
			b.append(' ');
			b.append(event.remote());
			b.append(' ');
			b.append(event.query().path());
			b.append(' ');
			b.append(event.reply().code());

			int length = event.reply().length();

			if(length > 0) {
				b.append(' ');
				b.append(length);
			}

			return b.toString();
		}

		return null;
	}

	protected void access(String row, boolean push) throws IOException {
		if (access != null) {
			StringBuilder b = new StringBuilder();

			b.append(row);

			if(push) {
				b.append(' ');
				b.append('>');
			}

			b.append(Output.EOL);

			access.write(b.toString().getBytes("UTF-8"));
		}
	}

	/**
	 * Starts the selector, heartbeat and worker threads.
	 */
	public void start() {
		try {
			heart = new Heart();

			int threads = Integer.parseInt(properties.getProperty("threads",
					"5"));

			for (int i = 0; i < threads; i++) {
				workers.add(new Worker(this, i));
			}

			alive = true;

			new Thread(this).start();
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	/**
	 * Stops the selector, heartbeat and worker threads.
	 */
	public void stop() {
		Iterator it = workers.iterator();

		while(it.hasNext()) {
			Worker worker = (Worker) it.next();
			worker.stop();
		}

		workers.clear();
		alive = false;
		heart.stop();

		selector.wakeup();
	}

	protected ConcurrentHashMap session() {
		return session;
	}

	protected Selector selector() {
		return selector;
	}

	protected void chain(Deploy.Archive archive) throws Exception {
		Deploy.Archive old = (Deploy.Archive) this.archive.get(archive.name());

		if (old != null) {
			Iterator it = old.service().iterator();

			while (it.hasNext()) {
				final Service service = (Service) it.next();

				try {
					if(host) {
						//final Daemon daemon = this;

						AccessController.doPrivileged(new PrivilegedExceptionAction() {
							public Object run() throws Exception {
								service.destroy();
								return null;
							}
						}, archive.access());
					}
					else {
						service.destroy();
					}
				} catch (Exception e) {
					e.printStackTrace(out);
				}
			}
		}

		Iterator it = archive.service().iterator();

		while (it.hasNext()) {
			Service service = (Service) it.next();
			add(archive.chain(), service, archive);
		}

		this.archive.put(archive.name(), archive);
	}

	/**
	 * Get archive.
	 * @return If null the archive is not deployed.
	 */
	public Deploy.Archive archive(String name) {
		if(!name.endsWith(".jar")) {
			name += ".jar";
		}

		if(host) {
			if(name.equals(domain + ".jar")) {
				return Deploy.Archive.deployer;
			}

			Deploy.Archive archive = (Deploy.Archive) this.archive.get(name);

			if(archive == null) {
				return (Deploy.Archive) this.archive.get("www." + name);
			}
			else {
				return archive;
			}
		}
		else {
			return (Deploy.Archive) this.archive.get(name);
		}
	}

	private Listener listener;

	/**
	 * Send Object to listener. We recommend you only send bootclasspath loaded 
	 * classes here otherwise hotdeploy will fail.
	 * 
	 * @param message
	 * @return null if no listener was added or reply message.
	 * @throws Exception
	 */
	public Object send(Object message) throws Exception {
		if(listener == null) {
			return null;
		}

		return listener.receive(message);
	}

	/**
	 * Register your listener here.
	 * 
	 * @param listener
	 */
	public void set(Listener listener) {
		try {
			/*
			 * So only the controller can be added as listener since we use this feature to authenticate deployments.
			 */
			
			if(host) {
				File pass = new File("app/" + domain + "/passport");

				if(!pass.exists()) {
					pass.createNewFile();
				}

				pass.canRead();
			}

			this.listener = listener;
		}
		catch(IOException e) {
			// if passport could not be created
		}
	}

	/**
	 * Cross class-loader communication interface. So that a class deployed 
	 * in one archive can send messages to a class deployed in another.
	 * @author Marc
	 */
	public interface Listener {
		/**
		 * @param message
		 * @return the reply message to the sender.
		 * @throws Exception
		 */
		public Object receive(Object message) throws Exception;
	}

	public void add(Service service) throws Exception {
		add(this.service, service, null);
	}

	protected void add(HashMap map, final Service service, Deploy.Archive archive) throws Exception {
		String path = null;

		if(host) {
			path = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return service.path();
				}
			}, control);
		}
		else {
			path = service.path();
		}

		if(path == null) {
			path = "null";
		}

		StringTokenizer paths = new StringTokenizer(path, ":");

		while (paths.hasMoreTokens()) {
			path = paths.nextToken();
			Chain chain = (Chain) map.get(path);

			if (chain == null) {
				chain = new Chain();
				map.put(path, chain);
			}

			final Service old = (Service) chain.put(service);

			if(host) {
				final String p = path;

				AccessController.doPrivileged(new PrivilegedExceptionAction() {
					public Object run() throws Exception {
						if (old != null) {
							throw new Exception(service.getClass().getName()
									+ " with path '" + p + "' and index ["
									+ service.index() + "] is conflicting with "
									+ old.getClass().getName()
									+ " for the same path and index.");
						}

						return null;
					}
				}, control);
			}
			else {
				if (old != null) {
					throw new Exception(service.getClass().getName()
							+ " with path '" + path + "' and index ["
							+ service.index() + "] is conflicting with "
							+ old.getClass().getName()
							+ " for the same path and index.");
				}
			}

			if (verbose)
				out.println(path + padding(path) + chain);

			try {
				if(host) {
					final Daemon daemon = this;

					Event e = (Event) AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							service.create(daemon);
							return null;
						}
					}, archive == null ? control : archive.access());
				}
				else {
					service.create(this);
				}
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		}
	}

	protected String padding(String path) {
		StringBuffer buffer = new StringBuffer();

		for(int i = 0; i < 10 - path.length(); i++) {
			buffer.append(' ');
		}

		return buffer.toString();
	}

	protected void verify(final Deploy.Archive archive) throws Exception {
		Iterator it = archive.chain().keySet().iterator();

		while (it.hasNext()) {
			final String path = (String) it.next();
			Chain chain = (Chain) archive.chain().get(path);

			for (int i = 0; i < chain.size(); i++) {
				final Service service = (Service) chain.get(i);

				if(host) {
					final HashMap a = this.archive;
					final int j = i;

					AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							if (j != service.index()) {
								a.remove(archive.name());
								throw new Exception(service.getClass().getName()
										+ " with path '" + path + "' has index ["
										+ service.index() + "] which is too high.");
							}

							return null;
						}
					}, control);
				}
				else {
					if (i != service.index()) {
						this.archive.remove(archive.name());
						throw new Exception(service.getClass().getName()
								+ " with path '" + path + "' has index ["
								+ service.index() + "] which is too high.");
					}
				}
			}
		}
	}

	protected Deploy.Stream content(Query query) {
		if(host) {
			return content(query.header("host"), query.path());
		}
		else {
			return content(query.path());
		}
	}

	protected Deploy.Stream content(String path) {
		return content("content", path);
	}

	protected Deploy.Stream content(String host, String path) {
		if(!this.host) {
			host = "content";
		}

		File file = new File("app" + File.separator + host + File.separator + path);

		if(file.exists() && !file.isDirectory()) {
			return new Deploy.Big(file);
		}

		if(this.host) {
			file = new File("app" + File.separator + "www." + host + File.separator + path);

			if(file.exists() && !file.isDirectory()) {
				return new Deploy.Big(file);
			}
		}

		return null;
	}

	protected Chain chain(Query query) {
		if(host) {
			return chain(query.header("host"), query.path());
		}
		else {
			return chain(query.path());
		}
	}

	public Chain chain(String path) {
		return chain("content", path);
	}

	public Chain chain(String host, String path) {
		synchronized (this.service) {
			Chain chain = (Chain) this.service.get(path);

			if (chain != null) {
				return chain;
			}
		}

		if(!this.host) {
			host = "content";
		}

		synchronized (this.archive) {
			if(this.host) {
				Deploy.Archive archive = (Deploy.Archive) this.archive.get(host + ".jar");

				if(archive == null) {
					archive = (Deploy.Archive) this.archive.get("www." + host + ".jar");
				}

				if(archive != null) {
					Chain chain = (Chain) archive.chain().get(path);

					if (chain != null) {
						return chain;
					}
				}
			}
			else {
				Iterator it = this.archive.values().iterator();

				while (it.hasNext()) {
					Deploy.Archive archive = (Deploy.Archive) it.next();

					if (archive.host().equals(host)) {
						Chain chain = (Chain) archive.chain().get(path);

						if (chain != null) {
							return chain;
						}
					}
				}
			}
		}

		return null;
	}

	protected synchronized Event next(Worker worker) {
		synchronized (this.queue) {
			if (queue.size() > 0) {
				if (Event.LOG) {
					if (debug)
						out.println("worker " + worker.index()
								+ " found work " + queue);
				}

				return (Event) queue.remove(0);
			}
		}
		return null;
	}

	public void run() {
		String pass = properties.getProperty("pass", "");
		ServerSocketChannel server = null;

		try {
			selector = Selector.open();
			server = ServerSocketChannel.open();
			server.socket().bind(new InetSocketAddress(port));
			server.configureBlocking(false);
			server.register(selector, SelectionKey.OP_ACCEPT);

			DecimalFormat decimal = (DecimalFormat) DecimalFormat.getInstance();
			decimal.applyPattern("#.##");

			if (verbose)
				out.println("daemon started\n" + "- pass       \t"
						+ pass + "\n" + "- port       \t" + port + "\n"
						+ "- worker(s)  \t" + threads + " thread"
						+ (threads > 1 ? "s" : "") + "\n" + 
						"- session    \t" + cookie + " characters\n" + 
						"- timeout    \t"
						+ decimal.format((double) timeout / 60000) + " minute"
						+ (timeout / 60000 > 1 ? "s" : "") + "\n"
						+ "- IO timeout \t" + delay + " ms." + "\n"
						+ "- IO buffer  \t" + size + " bytes\n"
						+ "- debug      \t" + debug + "\n"
						+ "- live       \t" + properties.getProperty("live", "false").toLowerCase()
						.equals("true"));

			if (pass != null && pass.length() > 0 || host) {
				if(host) {
					add(new Deploy("app" + File.separator));
				}
				else {
					add(new Deploy("app" + File.separator, pass));
				}

				File[] app = new File(Deploy.path).listFiles(new Filter());

				if (app != null) {
					for (int i = 0; i < app.length; i++) {
						try {
							Deploy.deploy(this, app[i], null);
						}
						catch(Error e) {
							e.printStackTrace();
						}
					}
				}
			}

			/*
			 * Used to debug thread locks and file descriptor leaks.
			 */

			if(panel) {
				add(new Service() {
					public String path() { return "/panel"; }
					public void filter(Event event) throws Event, Exception {
						Iterator it = workers.iterator();
						event.output().println("<pre>workers: {size: " + workers.size() + ", ");
						while(it.hasNext()) {
							Worker worker = (Worker) it.next();
							event.output().print(" worker: {index: " + worker.index() + ", busy: " + worker.busy() + ", lock: " + worker.lock());

							if(worker.event() != null) {
								event.output().println(", ");
								event.output().println("  event: {index: " + worker.event() + ", init: " + worker.event().reply().output.init + ", done: " + worker.event().reply().output.done + "}");
								event.output().println(" }");
							}
							else {
								event.output().println("}");
							}
						}
						event.output().println("}");
						event.output().println("events: {size: " + events.size() + ", selected: " + selected + ", valid: " + valid + ", accept: " + accept + ", readwrite: " + readwrite + ", ");
						it = events.values().iterator();
						while(it.hasNext()) {
							Event e = (Event) it.next();
							event.output().println(" event: {index: " + e + ", last: " + (System.currentTimeMillis() - e.last()) + "}");
						}
						event.output().println("}</pre>");
					}
				});
			}

			if (properties.getProperty("test", "false").toLowerCase().equals(
					"true")) {
				new Test(this, 1);
			}
		} catch (Exception e) {
			e.printStackTrace(out);
			System.exit(1);
		}

		int index = 0;
		Event event = null;
		SelectionKey key = null;

		while (alive) {
			try {
				selector.select();

				Set set = selector.selectedKeys();
				int valid = 0, accept = 0, readwrite = 0, selected = set.size();
				Iterator it = set.iterator();

				while (it.hasNext()) {
					key = (SelectionKey) it.next();
					it.remove();

					if (key.isValid()) {
						valid++;
						if (key.isAcceptable()) {
							accept++;
							event = new Event(this, key, index++);
							events.put(new Integer(event.index()), event);

							if (Event.LOG) {
								event.log("accept ---");
							}
						} else if (key.isReadable() || key.isWritable()) {
							readwrite++;
							key.interestOps(0);

							event = (Event) key.attachment();
							Worker worker = event.worker();

							if (Event.LOG) {
								if (debug) {
									if (key.isReadable())
										event.log("read ---");
									if (key.isWritable())
										event.log("write ---");
								}
							}

							if (key.isReadable() && event.push()) {
								event.disconnect(null);
							} else if (worker == null) {
								employ(event);
							} else {
								worker.wakeup();
							}
						}
					}
				}

				this.valid = valid;
				this.accept = accept;
				this.readwrite = readwrite;
				this.selected = selected;
			} catch (Exception e) {
				/*
				 * Here we get mostly ClosedChannelExceptions and
				 * java.io.IOException: 'Too many open files' when the server is
				 * taking a beating. Better to drop connections than to drop the
				 * server.
				 */
				if(event == null) {
					System.out.println(events + " " + key);
				}
				else {
					event.disconnect(e);
				}
			}
		}

		try {
			if(selector != null) {
				selector.close();
			}
			if(server != null) {
				server.close();
			}
		} catch (IOException e) {
			e.printStackTrace(out);
		}
	}

	protected void queue(Event event) {
		synchronized (this.queue) {
			queue.add(event);
		}

		if (Event.LOG) {
			if (debug)
				out.println("queue " + queue.size());
		}
	}

	protected synchronized void employ(Event event) {
		/*
		if(queue.size() > 0) {
			queue(event);
			return;
		}
		 */
		workers.reset();
		Worker worker = (Worker) workers.next();

		if (worker == null) {
			queue(event);
			return;
		}

		while (worker.busy()) {
			worker = (Worker) workers.next();

			if (worker == null) {
				queue(event);
				return;
			}
		}

		if (Event.LOG) {
			if (debug)
				out.println("worker " + worker.index() + " hired. (" + queue.size() + ")");
		}

		event.worker(worker);
		worker.event(event);
		worker.wakeup();
	}

	class Filter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.endsWith(".jar")) {
				return true;
			}

			return false;
		}
	}

	protected void log(PrintStream out) {
		if(out != null) {
			this.out = out;
		}
	}

	protected void log(Object o) {
		if(out != null) {
			out.println(o);
		}
	}

	class Heart implements Runnable {
		boolean alive;

		Heart() {
			alive = true;
			new Thread(this).start();
		}

		protected void stop() {
			alive = false;
		}

		public void run() {
			while (alive) {
				try {
					Thread.sleep(1000);

					Iterator it = session.values().iterator();

					while (it.hasNext()) {
						Session se = (Session) it.next();

						if (System.currentTimeMillis() - se.date() > timeout) {
							it.remove();
							se.remove();

							if (Event.LOG) {
								if (debug)
									out.println("session timeout "
											+ se.key());
							}
						}
					}

					it = workers.iterator();

					while(it.hasNext()) {
						Worker worker = (Worker) it.next();
						worker.busy();
					}

					it = events.values().iterator();

					while(it.hasNext()) {
						Event event = (Event) it.next();

						if(System.currentTimeMillis() - event.last() > timeout) {
							event.disconnect(null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(out);
				}
			}
		}
	}

	public static void main(String[] args) {
		Properties properties = new Properties();

		for (int i = 0; i < args.length; i++) {
			String flag = args[i];
			String value = null;

			if (flag.startsWith("-") && ++i < args.length) {
				value = args[i];

				if (value.startsWith("-")) {
					i--;
					value = null;
				}
			}
			//System.out.println(flag + " " + value);
			if (value == null) {
				properties.put(flag.substring(1).toLowerCase(), "true");
			} else {
				properties.put(flag.substring(1).toLowerCase(), value);
			}
		}

		if (properties.getProperty("help", "false").toLowerCase()
				.equals("true")) {
			System.out.println("Usage: java -jar http.jar -verbose");
			return;
		}

		new Daemon(properties).start();

		/*
		 * If this is run as an application we log PID to pid.txt file in root.
		 */

		try {
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			PrintWriter out = new PrintWriter("pid.txt");
			out.println(pid.substring(0, pid.indexOf('@')));
			out.flush();
			out.close();
		}
		catch(Exception e) {}
	}
}
