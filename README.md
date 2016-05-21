# clj-bigpanda

A Clojure library for interacting with [BigPanda](https://bigpanda.io).

Feel free to contribute!

## Usage

````clojure
(require '[clj-bigpanda.events :as events])

; pass it your token, app key and a list of alerts
(events/collate "16570a3212d52d66b839629c0b32f22c" "f8d7c868aa9ea512f362eb2a9c71d346" {:status "critical" :host "my-db-1" :check "CPU"})
(events/collate "981312token123" "appkey1239" {:status "critical" :host "my-db-1" :check "CPU"})
````

## License

Copyright (C) 2016 Nubank <tech@nubank.com.br>

Distributed under the Eclipse Public License, the same as Clojure.
