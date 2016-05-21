(ns clj-bigpanda.events
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
  ([token appkey params]
   {:oauth-token token
    :content-type :json
    :accept :json
    :throw-entire-message? true
    :query-params (unparse-kw params)})
  ([token appkey params body]
   (print body)
   (assoc (request token appkey params)
     :body (json/generate-string (unparse-kw (assoc body :app_key appkey))))))

(defn create-alert
  "Posts a set of alerts. options is a map of clj-http options."
  ([token appkey alerts]
     (create-alert token appkey alerts nil))
  ([token appkey alerts options]
     (assert (:status alerts) ":status undefined")
     (assert (:host alerts) ":host undefined")
     (assert (:check alerts) ":check undefined")
     (client/post (uri "alerts")
                  (merge
                   options
                   {:debug true :debug-body true}
                   (request token appkey {} alerts)))))

(defn event
  "Gets an event by name.

  See http://dev.bigpanda.com/v1/get/events"
  ([token appkey name]
   (event token appkey name {} nil))
  ([token appkey name params]
   (event token appkey name params nil))

  ([token appkey name params options]
   (assert name)
   (try+
     (let [body (-> (client/get (uri "" name)
                                (merge
                                 options
                                 (request token appkey params)))
                  :body json/parse-string parse-kw)]
       (assoc body :measurements
              (into {} (map (fn [[source measurements]]
                              [source (map parse-kw measurements)])
                            (:measurements body)))))
     (catch [:status 404] _
       (prn "caught 404")
       nil))))

(defn create-deployment
  "Creates a new deployment, and returns the created deployment as a map.

  http://dev.bigpanda.com/v1/post/deployments/:name"
  ([token appkey name deployment]
     (create-deployment token appkey name deployment nil))
  ([token appkey name deployment options]
     {:pre [(or (nil? options)(map? options))]}
     (assert name)
     (-> (client/post (uri "deployments" name)
                      (merge
                       options
                       (request token appkey {} deployment)))
         :body
         json/parse-string
         parse-kw)))

(defn update-deployment
  "Updates an deployment.

  http://dev.bigpanda.com/v1/put/deployments/:name/events/:id"
  ([token appkey name id deployment]
     (update-deployment token appkey name id deployment nil))
  ([token appkey name id deployment options]
     (assert name)
     (assert id)
     (client/put (uri "deployments" name id)
                 (merge
                  options
                  (request token appkey {} deployment)))))

(let [warn-on-deprecate (atom true)]
  ;; Deprecated due to argument ambiguity.
  ;; A future version could rename create-deployment as deploy.
  (defn deploy
    "Creates or updates an deployment. If id is given, updates. If id is
    missing, creates a new deployment."
    ([token appkey name deployment]
       (create-deployment token appkey name deployment nil))
    ([token appkey name deployment options]
       (if (map? deployment)
         (create-deployment token appkey name deployment options)
         (do
           ;; token appkey name id deployment
           (update-deployment token appkey name deployment options)
           (when @warn-on-deprecate
             (reset! warn-on-deprecate false)
             (logging/warn
              (str "`deploy` called for deployment update is deprecated. "
                   "Please use update-deployment."))))))))

(defn deployment
  "Find a particular deployment event.

  See http://dev.bigpanda.com/v1/get/deployments/:name/events/:id"
  ([token appkey name id]
     (deployment token appkey name id nil))
  ([token appkey name id options]
     (assert name)
     (assert id)
     (try+
      (-> (client/get (uri "deployments" name id)
                      (merge
                       options
                       (request token appkey {})))
          :body
          json/parse-string
          parse-kw)
      (catch [:status 404] _ nil))))
