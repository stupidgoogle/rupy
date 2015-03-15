How can you use rupy, if you already have something running on port 80, without having to invest in another server or an expensive firewall/router.

It's simple, virtual-host-proxy the requests through your current HTTP server, ie. redirect the requests to a rupy instance running on another port.

The only drawback of this is that you won't profit from the concurrency scalability of rupy. But by the time you have concurrency problems, you will hopefully have the money to buy new hardware! ;)

Below is an example Apache 2.2 vhost.conf, you need to enable mod\_proxy.

This is particularly useful if you need HTTPS!

```
NameVirtualHost *:80

<VirtualHost *:80>
        ServerName yourdomain.com
        ServerAlias www.yourdomain.com
</VirtualHost>

<VirtualHost *:80>
        ServerName rupy.yourdomain.com
        ProxyPass / http://localhost:8000/
        ProxyPassReverse / http://localhost:8000/
</VirtualHost>

<VirtualHost *:443>
        ServerName rupy.yourdomain.com
        ProxyPass / http://localhost:8000/
        ProxyPassReverse / http://localhost:8000/
        SSLEngine on
        SSLCertificateFile /etc/apache2/ssl.crt/server.crt
        SSLCertificateKeyFile /etc/apache2/ssl.key/server.key
</VirtualHost>
```