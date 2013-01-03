Cloud Event Bus
=====

A distributed event bus for Cloud Foundry.

Cloud Event Bus aims to provide a distributed publish/subscribe messaging system similar to
[NATS](https://github.com/derekcollison/nats) or the event bus in Vert.x. The ultimate goal is make Cloud Event Bus
easily deployable in private Cloud Foundry installations and made available to applications as a Cloud Foundry service.

## Why not just use NATS?

NATS is an awesome lightweight messaging system. In fact, a large part of Cloud Event Bus has been inspired by NATS.
The problem with NATS is that it's designed to work in a closed system. With a single username and password, you can
send a message on any subject and receive any message published within the system. This is fine for Cloud Foundry but
it doesn't work if you want to open NATS up for use by applications.

Cloud Event Bus will also support web sockets so that messages can be sent/received by web clients.

Cloud Event Bus employs a simple PKI for authentication and authorization instead of using user names and passwords.
By using a PKI, Cloud Event Bus can be scaled horizontally without having to rely on a centralized database for holding
authentication credentials. Securing web socket connections relies heavily on the PKI.
