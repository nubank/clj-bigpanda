(ns clj-bigpanda.events-test
  (:use clojure.test
        clj-bigpanda.events))

(def token   (System/getenv "BIGPANDA_EVENTS_TOKEN"))
(def appkey (System/getenv "BIGPANDA_EVENTS_APPKEY"))

(when-not token
  (println "export BIGPANDA_EVENTS_TOKEN=\"...\" to run these tests."))
(when-not appkey
  (println "export BIGPANDA_EVENTS_APPKEY=\"...\" to run these tests."))

(defn now []
  (long (/ (System/currentTimeMillis) 1000)))

(deftest parse-kw-test
         (is (= (parse-kw {"hello_there" {"yarr" 2}})
                {:hello-there {"yarr" 2}})))

(deftest unparse-kw-test
         (is (= (unparse-kw {:hello-there [{:tiny-kitten 3}]})
                {"hello_there" [{"tiny_kitten" 3}]})))

(deftest create-alert-test
         (testing "reject nil names"
                  (is (thrown? java.lang.AssertionError
                               (create-alert token appkey {:status "ok" :host "h1" :check "cpu"}))))

         (testing "reject nil values"
                  (is (thrown? java.lang.AssertionError
                               (create-alert token appkey {:name "foo"
                                                      :value nil})))
                  (is (thrown? java.lang.AssertionError
                               (create-alert token appkey {:name "foo"
                                                         :value nil}))))

         (testing "alert"
                  (let [alert {:name "test.alert"
                               :source "clj-bigpanda"
                               :value (/ (rand-int 1000) (rand-int 1000))
                               :measure-time (now)}]
                    ; Submit alert
                    (create-alert token appkey alert)

                    ; Confirm receipt
                    (let [event (event token appkey (:name alert)
                                         {:end-time (:measure-time alert)
                                          :count 1
                                          :resolution 1})
                          m (-> event
                              :measurements
                              (get (:source alert))
                              (first))]
                      (is (= (:name event) (:name alert)))
                      (is (= (:type event) "alert"))
                      (is m)
                      (is (= (:measure-time m) (:measure-time alert)))
                      (is (= (:value m) (double (:value alert))))
                      (is (= (:count m) 1)))
                    )))

(defn test-value
  []
  {:name "test.alert"
   :source "clj-bigpanda"
   :value (/ (rand-int 1000) (rand-int 1000))
   :measure-time (now)})

(defn test-create-alert
  [options]
  (testing "alert"
    (let [alert (test-value)]
      ;; Submit alert
      (is (create-alert token appkey alert options) "is created")

      (testing "can be queried"
        (let [event (event token appkey (:name alert)
                             {:end-time (:measure-time alert)
                              :count 1
                              :resolution 1}
                             options)]
          (is (= (:type event) "alert"))
          (is (= (:name event) (:name alert)))
          (testing "has a last event value"
            (let [m (-> event
                        :measurements
                        (get (:source alert))
                        (first))]
              (is m)
              (is (= (:measure-time m) (:measure-time alert)))
              (is (= (:value m) (double (:value alert))))
              (is (= (:count m) 1)))))))))

(deftest create-alert-with-http-options-test
  (testing "reject nil status"
    (is (thrown? java.lang.AssertionError
                 (create-alert token appkey {:status nil
                                             :host "my-awesome-instance"
                                             :check "cpu"} {}))))

  (testing "reject nil host"
    (is (thrown? java.lang.AssertionError
                 (create-alert token appkey {:status "critical"
                                             :host nil
                                             :check "mate"} {}))))

  (testing "reject nil check"
    (is (thrown? java.lang.AssertionError
                 (create-alert token appkey {:status "critical"
                                             :host "my-awesome-instance"
                                             :check "nil"} {}))))

  (testing "with no http options"
    (test-create-alert nil))
  (testing "with persistent http options"
    (let [cm (connection-manager {})]
      (is cm "connection manager created")
      (test-create-alert {:connection-manager cm}))))

;(deftest deploy-test
;         (let [name  "test.deployments"
;               event {:title (str "A test event: " (rand 10000000))
;                      :source "clj-bigpanda"
;                      :description "Testing clj-bigpanda deployments"
;                      :start-time (now)
;                      :end-time (+ 10 (now))}
;               res (deploy token appkey name event)
;               e (deployment token appkey name (:id res))]
;
;           ; Verify deployment was created.
;           (is res)
;           (is e)
;           (is (= res e))
;           (is (= (:title e) (:title event)))
;           (is (= (:description e) (:description event)))
;           (is (= (:source e) (:source event)))
;           (is (= (:start-time e) (:start-time event)))
;           (is (= (:end-time e) (:end-time event)))
;
;           ; Update deployment
;           (deploy token appkey name (:id res)
;                     {:end-time (inc (:end-time event))})
;
;           ; Verify update was applied.
;           (is (= (inc (:end-time event))
;                  (:end-time (deployment token appkey name (:id res)))))))
;
;(defn test-deployment
;  []
;  {:title (str "A test event: " (rand 10000000))
;   :source "clj-bigpanda"
;   :description "Testing clj-bigpanda deployments"
;   :start-time (now)
;   :end-time (+ 10 (now))})
;
;(defn deployment=
;  [e1 e2]
;  (let [ks [:title :description :source :start-time :end-time]]
;    (= (select-keys e1 ks) (select-keys e2 ks))))
;
;(defn test-deploy
;  [options]
;  (testing "a test deployment"
;    (let [name "test.deployments"
;          deploy-map (test-deployment)]
;
;      (testing "can be created"
;        (let [res (deploy token appkey name deploy-map options)]
;          (is res "is created without error")
;
;          (testing "and is queryable"
;            (let [a (deployment token appkey name (:id res) options)]
;              (is a "can be queried by returned id")
;              (is (= res a) "is as returned by deploy")
;              (is (deployment= a deploy-map) "has matching attributes")))
;
;          (testing "and can be updated"
;            (update-deployment token appkey name (:id res)
;                               {:end-time (inc (:end-time deploy-map))}
;                               options)
;            (is (= (inc (:end-time deploy-map))
;                   (:end-time (deployment token appkey name (:id res) options)))
;                "updated attribute matches")))))))
;
;(deftest deploy-with-http-options-test
;  (testing "with no http options"
;    (test-deploy nil))
;  (testing "with persistent http options"
;    (let [cm (connection-manager {})]
;      (is cm "connection manager created")
;      (test-deploy {:connection-manager cm}))))
;
;(deftest deployment-test
;         ; 404s return nil
;         (is (nil? (deployment token appkey "asdilhugflsdbfg" 9999)))
;         (is (nil? (deployment token appkey name 8869889912345))))
;
;(deftest deployment-with-http-options-test
;         ; 404s return nil
;         (is (nil? (deployment token appkey "vuash" 9999 {})))
;         (is (nil? (deployment token appkey name 8869889912345 {}))))
