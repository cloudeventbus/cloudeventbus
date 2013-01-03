Codec
=====

A [Netty](https://github.com/netty/nett) codec for for converting bytes sent over the network to easily usable Java
objects. This provides a clear separation between business logic and encoding logic. This makes the code easier to read
and much easier to test. The same codec is used on the Java client as well as the server.
