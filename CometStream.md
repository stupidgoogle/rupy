<font color='red'><b><i>NEW</i></b></font> [Aeon Alpha](http://aeonalpha.com) is an example of Comet Stream on rupy.

You can now [stream](http://rupy.googlecode.com/files/talk-1.1.zip) messages over HTTP with rupy.

As [RFC](https://rupy.googlecode.com/files/draft-talk-comet-stream-protocol-00.txt) I supply this reference implementation.<font color='red'> - Termination is now \n for this text based implementation.</font>

The message energy performance per second of the test included above:<br>

<table><thead><th>CPU</th><th>usage</th><th>in</th><th>out</th><th>total</th><th>watt</th><th> </th><th>mess./joule</th><th> </th><th>arch.</th></thead><tbody>
<tr><td>3770S</td><td>0%</td><td>4.000</td><td>9.000</td><td>13.000</td><td>/ </td><td>20</td><td>= </td><td>650</td><td>x86</td></tr>
<tr><td>4010U</td><td>7.5%</td><td>3.000</td><td>6.000</td><td>9.000</td><td>/ </td><td>10</td><td>= </td><td>900</td><td>x86</td></tr>
<tr><td>D510MO</td><td>50%</td><td>3.000</td><td>6.000</td><td>9.000</td><td>/ </td><td>15</td><td>= </td><td>600</td><td>x86</td></tr>
<tr><td>RPi 2</td><td>12.5%</td><td>2.000</td><td>4.500</td><td>6.500</td><td>/ </td><td>1 </td><td>= </td><td><font color='red'><b><i>6.500</i></b></font></td><td>ARM</td></tr>
<tr><td>RPi</td><td>99%</td><td>300</td><td>700</td><td>1.000</td><td>/ </td><td>2 </td><td>= </td><td>500</td><td>ARM</td></tr></tbody></table>

You can try it quickly by going to this flash demo:<br>
<br>
<blockquote>Flash: <a href='http://talk.rupy.se/stream.html'>stream</a> (see code below) (server is in Helsingborg, Sweden)</blockquote>

Or right-clicking these two links and opening them <br>
in different tabs/windows/devices (so you can see the stream in real-time):<br>
<br>
<blockquote>HTTP | <a href='http://talk.rupy.se/pull?name=one'>one</a> | < "hello" | <a href='http://talk.rupy.se/push?name=two&message=hello%3Cbr%3E'>two</a> | <font color='red'> - <del>Chrome doesn't render without a html tag now.</del> Now it does again.</font></blockquote>

<blockquote>event-stream | <a href='http://talk.rupy.se/event.html'>one</a> | < "hello" | <a href='http://talk.rupy.se/push?name=two&message=hello%3Cbr%3E'>two</a> |</blockquote>

<blockquote>You will then send "hello" from <i>two</i> to <i>one</i>. <br>
Reloading <i>two</i> will resend the message.<br>
You can play around with more windows <br>
and <i>name=<a href='http://talk.rupy.se/pull?name=three'>three</a>&message=<a href='http://talk.rupy.se/push?name=four&message=hi%3Cbr%3E'>hi</a></i> values.</blockquote>

<blockquote>If it doesn't work, try later, somebody else might <br>
be trying it at the same time.</blockquote>

<blockquote>IE doesen't render until a certain amount is <br>
received, so hold down F5 in <i>two</i> for a while.</blockquote>

This code allows you to receive dynamic chunks with flash:<br>
<br>
<pre><code>package {<br>
	import flash.display.*;<br>
	import flash.system.*;<br>
	import flash.errors.*;<br>
	import flash.events.*;<br>
	import flash.utils.*;<br>
	import flash.text.*;<br>
	import flash.net.*;<br>
<br>
	public class stream extends flash.display.MovieClip {<br>
		var host = "talk.rupy.se";<br>
		var port = 80;<br>
		var pull, push; // sockets<br>
		var id = 1;<br>
		var output;<br>
		var first = true;<br>
		var length = 120;<br>
		var sleep = 20;<br>
		var start;<br>
		<br>
		public function stream() {<br>
			output = new TextField();<br>
			output.width = 400;<br>
			output.height = 400;<br>
			output.multiline = true;<br>
			<br>
			addChild(output);<br>
			<br>
			output.appendText("security...");<br>
<br>
			pull = socket(pull_data, pull_connect);<br>
		}<br>
<br>
		function socket(data, connect):Socket {<br>
			try {<br>
				var socket = new Socket();<br>
				socket.addEventListener(IOErrorEvent.IO_ERROR, error);<br>
				socket.addEventListener(SecurityErrorEvent.SECURITY_ERROR, error);<br>
				socket.addEventListener(ProgressEvent.SOCKET_DATA, data); <br>
				socket.addEventListener(Event.CONNECT, connect);<br>
				socket.connect(host, port);<br>
				return socket;<br>
			} catch(error) {<br>
				output.appendText(error + "\n");<br>
			}<br>
			<br>
			return null;<br>
		}<br>
<br>
		function pull_connect(event) {<br>
			output.appendText(" done \n");<br>
			<br>
			start = new Date().getTime();<br>
			<br>
			pull.writeUTFBytes("GET /pull?name=one HTTP/1.1\r\n");<br>
			pull.writeUTFBytes("Host: " + host + "\r\n");<br>
			pull.writeUTFBytes("Head: less\r\n"); // enables TCP no delay<br>
			pull.writeUTFBytes("\r\n");<br>
			pull.flush();<br>
			<br>
			push = socket(push_data, push_connect);<br>
		}<br>
<br>
		function push_connect(event) {<br>
			//var timer = new Timer(sleep, length);<br>
			//timer.addEventListener(TimerEvent.TIMER, tick);<br>
			//timer.start();<br>
			<br>
			tick(null);<br>
		}<br>
		<br>
		function tick(event) {<br>
			push.writeUTFBytes("POST /push HTTP/1.1\r\n");<br>
			push.writeUTFBytes("Host: " + host + "\r\n");<br>
				<br>
			if(first) {<br>
				push.writeUTFBytes("Head: less\r\n"); // tells rupy to not send any headers<br>
				first = false;<br>
			}<br>
				<br>
			push.writeUTFBytes("\r\n");<br>
			push.writeUTFBytes("name=two&amp;message=" + id++);<br>
			push.flush();<br>
		}<br>
<br>
		function push_data(event) {<br>
			var message = push.readUTFBytes(event.bytesLoaded);<br>
			//output.appendText(message + "\n"); // HTTP/1.1 200 OK<br>
			if(id &lt;= length) {<br>
				tick(null);<br>
			}<br>
			else {<br>
				var latency = int((new Date().getTime() - start) / length);<br>
				output.appendText("avg. packet latency " + latency + " ms.\n");<br>
				output.scrollV+=10000;<br>
			}<br>
		}<br>
		<br>
		/* here we need to split on \n <br>
		 * or binary fixed length<br>
		 */<br>
		function pull_data(event) {<br>
			var message = pull.readUTFBytes(event.bytesLoaded);<br>
			var array = message.split("\r\n");<br>
			for(var i = 1; i &lt; array.length; i += 2) {<br>
				output.appendText(array[i]);<br>
				output.scrollV+=10000;<br>
			}<br>
		}<br>
<br>
		function error(event) {<br>
			output.appendText(event + "\n");<br>
		}<br>
	}<br>
}<br>
</code></pre>

Tutorial:<br>
<br>
<ul><li>Create 2 new files:<br>
<ol><li><i>ActionScript</i> - Paste the code above into it and save it as stream.as.<br>
</li><li><i>Flash (AS 3.0)</i> - Edit the Class to 'stream' in the PROPERTIES PUBLISH pane on the right, save in the same folder.<br>
</li></ol></li><li>Then run Control -> Test Movie (Ctrl+Enter).