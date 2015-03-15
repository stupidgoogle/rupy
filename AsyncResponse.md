This [example](http://test.rupy.se/async) pushes chunks asynchronously.

```
public class Async extends Service {
	int limit = 30;
	
	public String path() { return "/async"; }
	public void filter(Event event) throws Event, Exception {
		Output out = event.output();

		if(event.push()) {
			String data = event.query().string("data");
			out.println(data);
			if(data.equals("" + limit))
				event.output().finish();
			out.flush();
		}
		else {
			event.hold();
			start(event);

			out.println("Async response started!");
			out.flush();
		}
	}

	private void start(final Event event) {
		new Thread(new Runnable() {
			public void run() {
				try {
					int state = Reply.OK;
                    			int count = 0;

					while(state == Reply.OK && count < limit + 1) {
						Thread.currentThread().sleep(300);
						event.query().put("data", "" + count++);
						state = event.reply().wakeup();
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
```