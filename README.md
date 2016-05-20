# clj-bigpanda

A Clojure library for interacting with [BigPanda](https://bigpanda.io).

Feel free to contribute!

## Usage

````clojure
(require '[clj-bigpanda.metrics :as metrics])

; pass it an email, api key, list of gauges, and a list of counters
(metrics/collate "me@mydomain.com" "my-api-key" [{:name "gauge 1" :value 34 } {:name "gauge 2" :value 0}] 
                                                [{:name "a counter" :value 79213}])
````

## License

Copyright (C) 2016 Nubank <tech@nubank.com.br>

Distributed under the Eclipse Public License, the same as Clojure.
