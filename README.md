# chat42 <a href="https://gitter.im/replikativ/replikativ?utm_source=badge&amp;utm_medium=badge&amp;utm_campaign=pr-badge&amp;utm_content=badge"><img src="https://camo.githubusercontent.com/da2edb525cde1455a622c58c0effc3a90b9a181c/68747470733a2f2f6261646765732e6769747465722e696d2f4a6f696e253230436861742e737667" alt="Gitter" data-canonical-src="https://badges.gitter.im/Join%20Chat.svg" style="max-width:100%;"></a> 


This is a simple web-chat application which
leverages [replikativ](http://replikativ.io) for its state management. 


chat42 consists of two parts, a client written
with [ClojureScript](https://clojurescript.org/)
and [om-next](https://github.com/omcljs/om/wiki/Quick-Start-(om.next)) (react)
that compiles into efficient Javascript, and a server written in Clojure that
brokers communication between peers over a websocket. The server will be
available for node.js soon and is only necessary to ensure a communication
channel.

There is also a [react native client](https://github.com/replikativ/chat42app).

It was initially created as
a
[presentation for ClojureScript](https://github.com/replikativ/chat42/blob/master/presentation.org) in
our local JavaScript meetup.


## Usage


### Client development

Just run figwheel and edit `core.cljs` as needed. 

~~~clojure
lein figwheel
~~~

This allows you to develop the client in an offline mode.

### Server peer

If you want to persist and distribute the state, run the server with:

~~~clojure
lein run
~~~

The server uses an in-memory backend to allow quick resets, if you want to
persist the data on disk, use a filestore by [commenting out the mem-store instead](https://github.com/replikativ/chat42/blob/master/src/clj/chat42/core.clj#L14):

~~~clojure
(<?? S #_(new-mem-store) (new-fs-store "/tmp/chat42"))
~~~

The web clients will automatically connect to this peer on `localhost`. You can
now open two tabs and the two chat clients will communicate over the server and
update instantly. 

If you want to open the socket on a broader interface,
change
[the uri string](https://github.com/replikativ/chat42/blob/master/src/clj/chat42/core.clj#L11).
This is necessary if you want to connect clients from the local network or the
internet.

If you have any problems, questions or suggestions, please
join our gitter chat.

## License

Copyright © 2016-2017 Konrad Kühne, Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
