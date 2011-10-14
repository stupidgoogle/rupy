package se.rupy.http;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * HTTP request and query in one.
 * 
 * @author marc
 */
public class Query extends Hash {
	static URLDecoder decoder = new URLDecoder();
	public final static int GET = 1 << 0, POST = 1 << 1, PUT = 1 << 2, DELETE = 1 << 3, HEAD = 1 << 4;
	private String path, version, parameters;
	private HashMap headers;
	private Input input;
	private int length, method;
	private long modified;
	private boolean done, parsed;

	protected Query(Event event) throws IOException {
		headers = new HashMap();
		input = new Input.Chunked(event);
	}

	protected boolean headers() throws IOException {
		headers.clear();

		String line = input.line();

		while(line.equals("")) {
			//System.out.println("loop");
			line = input.line();
		}

		StringTokenizer http = new StringTokenizer(line, " ");
		String method = http.nextToken();

		if (method.equalsIgnoreCase("get")) {
			this.method = GET;
		} else if (method.equalsIgnoreCase("post")) {
			this.method = POST;
			parsed = false;
		} else if (method.equalsIgnoreCase("put")) {
			this.method = PUT;
			parsed = false;
		} else if (method.equalsIgnoreCase("delete")) {
			this.method = DELETE;
			parsed = false;
		} else if (method.equalsIgnoreCase("head")) {
			this.method = HEAD;
		} else {
			return false;
		}

		String get = http.nextToken();
		int index = get.indexOf('?');

		if (index > 0) {
			path = decoder.decode(get.substring(0, index), "UTF-8");
			parameters = get.substring(index + 1);
			parsed = false;
		} else {
			path = decoder.decode(get, "UTF-8");
			parameters = null;
		}

		version = http.nextToken();
		line = input.line();
		int lines = 0;

		while (line != null && !line.equals("")) {
			int colon = line.indexOf(":");

			if (colon > -1) {
				String name = line.substring(0, colon).toLowerCase();
				String value = line.substring(colon + 1).trim();

				headers.put(name, value);
			}

			line = input.line();
			lines++;

			if (lines > 30) {
				throw new IOException("Too many headers.");
			}
		}

		String encoding = header("transfer-encoding");

		if (encoding != null && encoding.equalsIgnoreCase("chunked")) {
			length = -1;
		} else {
			String content = header("content-length");

			if(content != null) {
				length = Integer.parseInt(content);
			}
			else {
				length = 0;
			}
		}

		String since = header("if-modified-since");

		if (since != null && since.length() > 0) {
			try {
				modified = input.event().worker().date().parse(since).getTime();
			} catch (ParseException e) {
				modified = 0;
			}
		}

		String connection = header("connection");

		if (connection != null && connection.equalsIgnoreCase("close")) {
			input.event().close(true);
		}

		clear();

		if (Event.LOG) {
			input.event().log(
					method + " " + (length > -1 ? "" + length : "*") + " " + path
					+ (parameters != null ? "?" + parameters : ""),
					Event.VERBOSE);
		}

		input.init();
		return true;
	}

	/**
	 * Parse the parameters from GET or POST. This method will only parse once
	 * per query and cache the result so don't be afraid of calling this method.
	 * 
	 * @throws Exception
	 */
	public void parse() throws Exception {
		parse(input.event().daemon().size);
	}

	/**
	 * Parse the parameters from GET or POST. This method will only parse once
	 * per query and cache the result so don't be afraid of calling this method.
	 * 
	 * @param size Maximum amount of bytes.
	 * @throws Exception
	 */
	public void parse(int size) throws Exception {
		if (parsed) {
			return;
		} else {
			parsed = true;
		}

		if (method == POST) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			if (Deploy.pipe(input, out, size, size) > 0)
				parameters = new String(out.toByteArray());
		}

		if (Event.LOG) {
			input.event().log("query " + parameters, Event.VERBOSE);
		}
		
		if (parameters != null) {
			StringTokenizer amp = new StringTokenizer(parameters, "&");

			while (amp.hasMoreTokens()) {
				String equ = amp.nextToken();
				int pos = equ.indexOf('=');

				String key = null;
				String value = "false";

				if(pos == -1) {
					pos = equ.length();
					key = equ.substring(0, pos);
				}
				else {
					key = equ.substring(0, pos);
					value = equ.length() > pos + 1 ? decoder.decode(equ.substring(pos + 1), "UTF-8") : "";
				}

				put(key, value);
			}
		}
	}

	protected void done() throws IOException {
		input.end();
		modified = 0;
	}

	public int method() {
		return method;
	}

	public String path() {
		return path;
	}

	public String version() {
		return version;
	}

	public String type() {
		return header("content-type");
	}

	public long modified() {
		return modified;
	}

	public int length() {
		return length;
	}

	/**
	 * The headers are stored and fetched as lower case.
	 * 
	 * @param name
	 * @return the header value.
	 */
	public String header(String name) {
		return (String) headers.get(name.toLowerCase());
	}

	protected void header(String name, String value) {
		headers.put(name, value);
	}

	/**
	 * Returns the parameters of the request. For GET you can just call 
	 * this but for POST you need to call {@link #parse()} first.
	 * Important: this String is not decoded, because you want to be able 
	 * to parse parameters with equal signs in them.
	 * @return the non decoded parameter string without the '?' if present else null.
	 */
	public String parameters() {
		return parameters;
	}

	public HashMap header() {
		return headers;
	}

	public Input input() {
		return input;
	}
}
