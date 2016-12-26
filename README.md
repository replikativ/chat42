# chat42

This is a simple web-chat application which
leverages [replikativ](http://replikativ.io) for its state management. 

## Usage

Just run figwheel and edit `core.cljs` as needed. 

~~~clojure
lein figwheel
~~~

If you want to persist and distribute the state, run a server with:

~~~clojure
lein run
~~~

The web clients will automatically connect to this peer on localhost.

## License

Copyright © 2016 Konrad Kühne, Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
