package se.rupy.http;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.CancelledKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Worker gets the job done. The worker holds the in/out/chunk buffers in order to
 * save resources, since the worker is assigned per event until a query is read and 
 * a reply is written.
 * 
 * @author marc
 */
public class Worker implements Runnable, Chain.Link {
	private Daemon daemon;
	private ByteBuffer in, out;
	private byte[] chunk;
	private Thread thread;
	private Event event;
	private int index, lock;
	private boolean awake, alive, exit = false;
	private long touch;
	private DateFormat date;

	protected Worker(Daemon daemon, int index) {
		this.daemon = daemon;
		this.index = index;

		in = ByteBuffer.allocateDirect(daemon.size);
		out = ByteBuffer.allocateDirect(daemon.size);

		chunk = new byte[daemon.size + Output.Chunked.OFFSET + 2];

		date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		date.setTimeZone(TimeZone.getTimeZone("GMT"));

		alive = true;

		thread = new Thread(this);
		thread.start();
	}

	protected DateFormat date() {
		return date;
	}

	protected ByteBuffer in() {
		touch();
		return in;
	}

	protected ByteBuffer out() {
		touch();
		return out;
	}

	protected byte[] chunk() {
		return chunk;
	}

	protected void wakeup() {
		if (Event.LOG) {
			if(event != null && event.daemon().debug)
				event.log("wakeup", Event.DEBUG);
		}

		touch();

		synchronized (thread) {
			thread.notify();
		}

		awake = true;
	}

	protected void touch() {
		/*
		if(event.daemon().debug) {
			event.log("touch " + (System.currentTimeMillis() - touch), Event.DEBUG);
		}
		 */
		touch = System.currentTimeMillis();
	}

	protected void snooze() {
		snooze(0);
	}

	protected void snooze(long delay) {
		if (Event.LOG) {
			if(event != null && event.daemon().debug)
				event.log("snooze " + delay, Event.DEBUG);
		}

		synchronized (thread) {
			try {
				if (delay > 0) {
					if(awake) {
						awake = false;
						return;
					}

					//if(event == null) {
					thread.wait(delay);
					//}
					//else {
					//	System.err.println("wtf1 " + event.index());
					//}	
				} else {
					if(event == null) {
						thread.wait();
					}
					//else {
					//	System.err.println("wtf2 " + event.index());
					//}
				}
			} catch (InterruptedException e) {
				event.disconnect(e);
			}

			awake = false;
		}
	}

	protected Event event() {
		return event;
	}

	protected void event(Event event) {
		this.event = event;
	}

	protected int lock() {
		return lock;
	}

	static protected String stack(Thread thread) {
		StackTraceElement[] stack = thread.getStackTrace();
		StringBuilder builder = new StringBuilder();

		for(int i = 0; i < stack.length; i++) {
			builder.append(stack[i]);

			if(i < stack.length - 1) {
				builder.append("\r\n");
			}
		}

		return builder.toString();
	}

	protected boolean busy() {
		if(event != null && touch > 0) {
			lock = (int) (System.currentTimeMillis() - touch);

			//if(lock > 20) {
			//	System.out.println(lock);
			//}

			if(lock > daemon.delay) {
				try {
					daemon.error.write(event.toString().getBytes());
					daemon.error.write(stack(thread).getBytes());
				}
				catch(IOException e) {}

				reset(new Exception("Threadlock " + lock + " (" + event.query().path() + ")"));

				if(exit) {
					System.err.println("Rupy stopped for debugging!");
					System.exit(-1);
				}

				event = null;
				return false;
			}

			return event != null;
		}

		return false;
	}

	public int index() {
		return index;
	}

	protected long id() {
		return thread.getId();
	}

	protected void stop() {
		synchronized (thread) {
			thread.notify();
		}

		alive = false;
	}

	public void run() {
		touch = System.currentTimeMillis();

		while (alive) {
			try {
				if (event != null) {
					if (event.push()) {
						event.write();
					} else {
						event.read();
					}
				}
			} catch (Exception e) {
				reset(e);

				if(exit) {
					System.err.println("Rupy stopped for debugging!");
					System.exit(-1);
				}
			} finally {
				if (event != null) {
					event.worker(null);
					event = daemon.next(this);

					if (event != null) {
						Daemon.match(event, this, false);
					} else {
						snooze();
					}
				} else {
					if(daemon.queue.size() > 0) {
						event = daemon.next(this);

						if (event != null) {
							Daemon.match(event, this, false);
						} else {
							snooze();
						}
					}
					else {
						snooze();
					}
				}
			}

			/*
			 * Sychronization when huge amounts of Reply.wakeup() are being hammered.
			 */
			if(event != null) {
				Worker worker = event.worker();

				if(worker == null)
					event = null;
				else if(worker.id() != Thread.currentThread().getId()) {
					event.worker(null);
					event = null;
				}
			}
		}
	}

	protected void reset(Exception ex) {
		if(event != null) {
			event.disconnect(ex);
		}

		out.clear();
		in.clear();
	}

	public String toString() {
		return "worker: " + index + Output.EOL + 
				"in: " + in + Output.EOL + 
				"out: " + out + Output.EOL + 
				"chunk: " + new String(chunk) + Output.EOL + 
				"lock: " + lock + Output.EOL + 
				"awake: " + awake + Output.EOL + 
				"alive: " + alive + Output.EOL + 
				"touch: " + touch + Output.EOL;
	}
}
