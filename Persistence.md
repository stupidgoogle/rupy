This requires latest [rupy](http://rupy.se/rupy.zip) in hosted mode.

For true redundancy you need to use some custom cluster database since we [host](http://host.rupy.se) across multiple colocations!
Ordinary databases are not secure or redundant in a distributed cluster environment out of the box, but most of all:

> _Database clients don't use async HTTP; you can't scale them easily._

On the other hand modern file systems are now cheap and fast (ext4) and async is becoming common place; See the entry on [FUSE](https://code.google.com/p/rupy/wiki/Fuse) to learn more on how you can scale any [µSOA](https://code.google.com/p/rupy/wiki/Process) cluster with async HTTP.

ROOT takes a little bit from all NoSQL databases: document, key-value and graph; It's basically a key-value store (Base58-JSON) with indexing (that we call sort) on custom fields (including full text search) in the JSON and graph relationships between the key-values.

The JSON objects and indexes are stored as a plain file (symbolic hard link). The relations and full text search are just binary files with lists of longs (the hashed Base58 key). Simple, powerful and in line with both _Moore's_ and _Murphy's_ laws.

<font color='red'>_Try the current version:</font> [root.rupy.se](http://root.rupy.se)_

TODO:

  1. ~~new [async](https://code.google.com/p/rupy/source/browse/trunk/src/se/rupy/http/Async.java) client~~
  1. ~~_edit node_~~
  1. ~~_sort node_ text index~~
  1. ~~_link node_ relation graph~~
  1. ~~_find node_ full text search~~
  1. binary file table data
  1. pagination in lists
  1. _trim node_ & _link_

WISH:

  1. link _node_ many-to-many-to-many... relations, you can do this by folder path.
  1. replicate any file, images would be nice!

Source: [User](http://root.rupy.se/code?path=/User.java), [Root](http://root.rupy.se/code?path=/Root.java)