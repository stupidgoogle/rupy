<font color='red'><i><b>We now have our own cloud database:</b></i></font> [ROOT](https://code.google.com/p/rupy/wiki/Persistence)

For true redundancy you need to use some custom cluster datastore since we now have hosting across multiple colocations!

Ordinary databases are not secure, scalable or redundant in a distributed cluster environment out of the box:

  * Database connections between colocations are not encrypted.
  * Database clients don't have async HTTP; you can't scale them easily.
  * If the colocation where you host your database is down, your site will be down.
  * You need to backup your data.

You register on the node you want your mysql database on: [one](http://one.rupy.se) or [two](http://two.rupy.se) hosted by FSData in Sweden and [tre](http://tre.rupy.se) hosted by PCExtreme in the Netherlands. For now if you need security, you have to change the DNS to the node with the MySQL (100% secure unless hardware is tampered with which would be criminal and impossible for us to not notice) or the two swedish nodes (only FSData can snoop your data). But then, of course, you lose the up-time capable distribution!

Using [memory](http://memory.googlecode.com) (you need to add memory.jar and util.jar to your classpath) ORM:

```
public class DataBase extends Service {
	public static Base BASE;
	public static Pool POOL;
	public static SQL SQL;

	public String path() { return "/somepath"; }

	public void create(Daemon daemon) {
                try {
                        SQL = new SQL(daemon);
                        POOL = new Pool(SQL, SQL);
                        BASE = new Base();
                        BASE.init(POOL);
                }
                catch(Exception e) {
                        e.printStackTrace();
                }
	}

	public void destroy() {
		POOL.close();
	}

	public void filter(Event event) throws Exception, Event {}

	public static class SQL implements Log, Settings {
		public String host, pass, ip;
		
		public SQL(Daemon daemon) throws Exception {
			JSONObject account = new JSONObject((String) daemon.send("{\"type\": \"account\"}"));
			host = account.getString("host");
			pass = account.getString("pass");
			ip = account.getString("ip");
		}

		public String driver() {
			return "com.mysql.jdbc.Driver";
		}

		public String url() {
			return "jdbc:mysql://" + ip + "/" + host.replaceAll("\\.", "_");
		}

		// MySQL username can only be 16 characters long
		public String user() {
			return pass;
		}

		public String pass() {
			return pass;
		}

		public void message(Object o) {}
	}
}
```