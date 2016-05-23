# clj-bigpanda

A Clojure library for interacting with [BigPanda](https://bigpanda.io).

Feel free to contribute!

## Usage

### Alerts

````clojure
(require '[clj-bigpanda.events :as events])
; set token and appkey
(def bigpanda-auth {:token "123123-my-token" :appkey "312312-my-appkey"})
; pass it your auth and an alert
(events/create-alert bigpanda-auth {:status "critical" :host "my-db-1" :check "CPU"})
````

### Deployments

````clojure
(require '[clj-bigpanda.events :as events])
; set token and appkey
(def bigpanda-auth {:token "123123-my-token" :appkey "312312-my-appkey"})
; pass it your auth and a deployment start
(events/start-deployment bigpanda-auth {:hosts ["prod-api-1", "prod-api-2"] :version "0.8.2" :component "billing"})
; pass it your auth and a deployment end
(events/end-deployment bigpanda-auth {:hosts ["prod-api-1", "prod-api-2"] :version "0.8.2" :component "billing" :status "success"})

## Tests

Run tests with `lein test`

## License

Copyright (C) 2016 Nubank <tech@nubank.com.br>

Distributed under the Eclipse Public License, the same as Clojure.

Based on clj-librato project: https://github.com/aphyr/clj-librato
