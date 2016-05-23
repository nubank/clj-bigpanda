(ns clj-bigpanda.events
  (:use [slingshot.slingshot :only [try+]])
  (:require [cheshire.core   :as json]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string  :as string]
            [clojure.tools.logging :as logging]
            clj-http.util))

(def uri-base "https://api.bigpanda.io/data/")

(defn uri
  "The full URI of a particular resource, by path fragments."
  [& path-fragments]
  (apply str uri-base
         (interpose "/" (map (comp clj-http.util/url-encode str)
                             path-fragments))))

(defn unparse-kw
  "Convert a clojure-style dashed keyword map into string underscores.
  Recursive."
  [m]
  (cond
    (map? m) (into {} (map (fn [[k v]]
                             [(string/replace (name k) "-" "_")
                              (unparse-kw v)])
                           m))
    (coll? m) (map unparse-kw m)
    true m))

(defn parse-kw
  "Parse a response map into dashed, keyword keys. Not recursive: some bigpanda
  API functions return arbitrary string keys in maps."
  [m]
  (into {} (map (fn [[k v]] [(keyword (string/replace k "_" "-")) v]) m)))

(defn request
  "Constructs the HTTP client request map.
  options will be merged verbatim into the request map."
  ([auth params body]
    (let [token (:token auth)
          appkey (:appkey auth)]
      {:oauth-token token
       :content-type :json
       :accept :json
       :query-params (unparse-kw params)
       :body (json/generate-string (unparse-kw (assoc body :app_key appkey)))})))

(defn create-alert
  "Posts an alert. options is a map of clj-http options.

  https://api.bigpanda.io/data/v2/alerts"
  ([auth alert]
     (create-alert auth alert nil))
  ([auth alert options]
     (assert (:status alert) ":status undefined")
     (assert (:host alert) ":host undefined")
     (assert (:check alert) ":check undefined")
     (client/post (uri "v2/alerts")
                  (merge
                   options
                   (request auth {} alert)))))

(defn start-deployment
  "Creates a new deployment start. options is a map of clj-http options

  https://api.bigpanda.io/data/events/deployments/start"
  ([auth deployment]
     (start-deployment auth deployment nil))
  ([auth deployment options]
     (assert (:hosts deployment) ":status undefined")
     (assert (:component deployment) ":host undefined")
     (assert (:version deployment) ":check undefined")
     (client/post (uri "events/deployments/start")
                  (merge
                   options
                   (request auth {} deployment)))))

(defn end-deployment
  "Creates a new deployment end. options is a map of clj-http options

  https://api.bigpanda.io/data/events/deployments/end"
  ([auth deployment]
     (end-deployment auth deployment nil))
  ([auth deployment options]
     (assert (:hosts deployment) ":hosts undefined")
     (assert (:component deployment) ":component undefined")
     (assert (:version deployment) ":version undefined")
     (client/post (uri "events/deployments/end")
                  (merge
                   options
                   (request auth {} deployment)))))
