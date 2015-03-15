
```
public static class GZIPService extends Service {
	public String path() { return "/some.file"; }
	public void filter(Event event) throws Event, Exception {
		event.reply().header("Content-Encoding", "gzip");

		File to = compress();
		FileInputStream in = new FileInputStream(to);
		Deploy.pipe(in, event.reply().output(to.length()));
		in.close();
	}

	public synchronized File compress() {
		File from = new File("app/content/some.file");
		File to = new File("app/content/some.file.gzip");
		
		boolean zip = false;
		
		if(to.exists()) {
			if(from.lastModified() > to.lastModified()) {
				zip = true;
			}
		}
		else {
			to.createNewFile();
			zip = true;
		}
		
		if(zip) {
			FileInputStream in = new FileInputStream(from);
			GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to));

			Deploy.pipe(in, out);

			out.finish();
			out.close();
			in.close();
		}

		return to;
	}
}
```

You could also zip the files before deployment.