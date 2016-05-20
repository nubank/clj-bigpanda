(ns clj-bigpanda.metrics
  (:use [slingshot.slingshot :only [try+]])
  (:require [cheshire.core   :as json]
            [clj-http.client :as client]
            [clj-http.conn-mgr :as conn-mgr]
            [clojure.string  :as string]
            [clojure.tools.logging :as logging]
            clj-http.util))

(def uri-base "https://api.bigpanda.io/data/v2/")

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

(defn connection-manager
  "Return a connection manager that can be passed as :connection-manager in
  a request."
  [{:keys [timeout threads] :or {timeout 10 threads 2} :as options}]
  (conn-mgr/make-reusable-conn-manager
   (merge {:timeout timeout :threads threads :default-per-route threads}
          options)))

(defn request
  "Constructs the HTTP client request map.
  options will be merged verbatim into the request map."
  ([user api-key params]
   {:basic-auth [user api-key]
    :content-type :json
    :accept :json
    :throw-entire-message? true
    :query-params (unparse-kw params)})
  ([user api-key params body]
   (assoc (request user api-key params)
     :body (json/generate-string (unparse-kw body)))))

(defn collate
  "Posts a set of gauges and counters. options is a map of clj-http options."
  ([user api-key gauges counters]
     (collate user api-key gauges counters nil))
  ([user api-key gauges counters options]
     (assert (every? :name gauges))
     (assert (every? :name counters))
     (assert (every? :value gauges))
     (assert (every? :value counters))
     (client/post (uri "metrics")
                  (merge
                   options
                   (request user api-key {}
                            {:gauges gauges :counters counters})))))

(defn metric
  "Gets a metric by name.

  See http://dev.bigpanda.com/v1/get/metrics"
  ([user api-key name]
   (metric user api-key name {} nil))
  ([user api-key name params]
   (metric user api-key name params nil))

  ([user api-key name params options]
   (assert name)
   (try+
     (let [body (-> (client/get (uri "metrics" name)
                                (merge
                                 options
                                 (request user api-key params)))
                  :body json/parse-string parse-kw)]
       (assoc body :measurements
              (into {} (map (fn [[source measurements]]
                              [source (map parse-kw measurements)])
                            (:measurements body)))))
     (catch [:status 404] _
       (prn "caught 404")
       nil))))

(defn create-annotation
  "Creates a new annotation, and returns the created annotation as a map.

  http://dev.bigpanda.com/v1/post/annotations/:name"
  ([user api-key name annotation]
     (create-annotation user api-key name annotation nil))
  ([user api-key name annotation options]
     {:pre [(or (nil? options)(map? options))]}
     (assert name)
     (-> (client/post (uri "annotations" name)
                      (merge
                       options
                       (request user api-key {} annotation)))
         :body
         json/parse-string
         parse-kw)))

(defn update-annotation
  "Updates an annotation.

  http://dev.bigpanda.com/v1/put/annotations/:name/events/:id"
  ([user api-key name id annotation]
     (update-annotation user api-key name id annotation nil))
  ([user api-key name id annotation options]
     (assert name)
     (assert id)
     (client/put (uri "annotations" name id)
                 (merge
                  options
                  (request user api-key {} annotation)))))

(let [warn-on-deprecate (atom true)]
  ;; Deprecated due to argument ambiguity.
  ;; A future version could rename create-annotation as annotate.
  (defn annotate
    "Creates or updates an annotation. If id is given, updates. If id is
    missing, creates a new annotation."
    ([user api-key name annotation]
       (create-annotation user api-key name annotation nil))
    ([user api-key name annotation options]
       (if (map? annotation)
         (create-annotation user api-key name annotation options)
         (do
           ;; user api-key name id annotation
           (update-annotation user api-key name annotation options)
           (when @warn-on-deprecate
             (reset! warn-on-deprecate false)
             (logging/warn
              (str "`annotate` called for annotation update is deprecated. "
                   "Please use update-annotation."))))))))

(defn annotation
  "Find a particular annotation event.

  See http://dev.bigpanda.com/v1/get/annotations/:name/events/:id"
  ([user api-key name id]
     (annotation user api-key name id nil))
  ([user api-key name id options]
     (assert name)
     (assert id)
     (try+
      (-> (client/get (uri "annotations" name id)
                      (merge
                       options
                       (request user api-key {})))
          :body
          json/parse-string
          parse-kw)
      (catch [:status 404] _ nil))))
