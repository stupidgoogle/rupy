Because NIO uses a few threads to serve many clients, it should _not_ block to wait for slow connections.

So we have 3 options:

  1. Drop slow connections after 5 seconds (default)
  1. Wait for slow connections; then we need more threads: -delay 60000 -threads 50 (not ideal)
  1. Increase socket write buffer (memory usage) (preferred)

If you see these kind of errors in log/error.txt your clients are getting dropped:

```
13-05-28 22:46:29.884 127.0.0.1 /[file/service]

...

Caused by: java.lang.Exception: IO timeout. ([delay])
        at se.rupy.http.Event.block(Event.java:381)
        at se.rupy.http.Output.internal(Output.java:263)
        at se.rupy.http.Output.wrote(Output.java:225)
        ... 7 more
```

TCP auto-tuning has been in linux since kernel 2.4; so all we have to do is increase the max limit (for example, as root):

```
echo 'net.core.wmem_max=5242880' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_wmem=16384 65536 5242880' >> /etc/sysctl.conf

sysctl -p
```

Here you can play with the numbers, 5MB (5242880) is enough for us since our largest file is 5MB.

If you have very large client files (>5MB) you could make an application and share it with bittorrent!

A complementary solution is to [zip](https://code.google.com/p/rupy/wiki/GZIPService) your uncompressed static files.