(ns clj-bigpanda.events-test
  (:use clojure.test
        clj-bigpanda.events))

(def token   (System/getenv "BIGPANDA_EVENTS_TOKEN"))
(def appkey (System/getenv "BIGPANDA_EVENTS_APPKEY"))

(when-not token
  (println "export BIGPANDA_EVENTS_TOKEN=\"...\" to run these tests."))
(when-not appkey
  (println "export BIGPANDA_EVENTS_APPKEY=\"...\" to run these tests."))

(def bigpanda-auth
  {:token token
   :appkey appkey})

(deftest parse-kw-test
         (is (= (parse-kw {"inside_job" {"vuash" 3}})
                {:inside-job {"vuash" 3}})))

(deftest unparse-kw-test
         (is (= (unparse-kw {:inside-job [{:vuash-fufu 7}]})
                {"inside_job" [{"vuash_fufu" 7}]})))

(def test-alert
  {:host "my-api-3"
   :status "ok"
   :check "CPU"})

(deftest create-alert-test
         (testing "reject nil status"
                  (is (thrown? java.lang.AssertionError
                               (create-alert bigpanda-auth {:status nil :host "h1" :check "cpu"}))))

         (testing "reject nil host"
                  (is (thrown? java.lang.AssertionError
                               (create-alert bigpanda-auth {:status "ok" :host nil :check "cpu"}))))

         (testing "reject nil check"
                  (is (thrown? java.lang.AssertionError
                               (create-alert bigpanda-auth {:status "ok" :host "h1" :check nil}))))

         (testing "alert"
                  (let [alert test-alert]
                    ; Submit alert
                    (create-alert bigpanda-auth alert))))

(defn test-create-alert
  [options]
  (testing "alert"
    (let [alert test-alert]
      ;; Submit alert
      (is (create-alert bigpanda-auth alert options) "is created"))))

(deftest create-alert-with-http-options-test
  (testing "reject nil status"
    (is (thrown? java.lang.AssertionError
                 (create-alert bigpanda-auth {:status nil
                                             :host "my-awesome-instance"
                                             :check "cpu"} {}))))

  (testing "reject nil host"
    (is (thrown? java.lang.AssertionError
                 (create-alert bigpanda-auth {:status "critical"
                                             :host nil
                                             :check "mate"} {}))))

  (testing "reject nil check"
    (is (thrown? java.lang.AssertionError
                 (create-alert bigpanda-auth {:status "critical"
                                             :host "my-awesome-instance"
                                             :check nil} {}))))

  (testing "with no http options"
    (test-create-alert nil)))

(def test-deployment
  {:hosts ["my-api-3", "my-api5"]
   :component "api"
   :version "1.6.1"})

(deftest start-deployment-test
         (testing "reject nil hosts"
                  (is (thrown? java.lang.AssertionError
                               (start-deployment bigpanda-auth {:hosts nil :component "db" :version "0.1.0"}))))

         (testing "reject nil component"
                  (is (thrown? java.lang.AssertionError
                               (start-deployment bigpanda-auth {:hosts ["my-db-1", "my-db-2"] :component nil :version "0.1.0"}))))

         (testing "reject nil version"
                  (is (thrown? java.lang.AssertionError
                               (start-deployment bigpanda-auth {:hosts ["my-db-1", "my-db-2"] :component "db" :version nil}))))

         (testing "deployment"
                  (let [deployment test-deployment]
                    ; Submit deployment
                    (start-deployment bigpanda-auth deployment))))

(defn test-start-deployment
  [options]
  (testing "deployment"
    (let [deployment test-deployment]
      ;; Submit deployment
      (is (start-deployment bigpanda-auth deployment options) "is created"))))

(deftest start-deployment-with-http-options-test
  (testing "reject nil hosts"
    (is (thrown? java.lang.AssertionError
                 (start-deployment bigpanda-auth {:hosts nil
                                                  :component "api"
                                                  :version "1.0.0"} {}))))

  (testing "reject nil component"
    (is (thrown? java.lang.AssertionError
                 (start-deployment bigpanda-auth {:hosts ["my-db-1"]
                                                  :component nil
                                                  :version "alpha"} {}))))

  (testing "reject nil check"
    (is (thrown? java.lang.AssertionError
                 (start-deployment bigpanda-auth {:hosts ["cache-1", "cache-2"]
                                                  :component "cache"
                                                  :version nil} {}))))

  (testing "with no http options"
    (test-start-deployment nil)))

(deftest end-deployment-test
         (testing "reject nil hosts"
                  (is (thrown? java.lang.AssertionError
                               (end-deployment bigpanda-auth {:hosts nil :component "db" :version "0.1.0"}))))

         (testing "reject nil component"
                  (is (thrown? java.lang.AssertionError
                               (end-deployment bigpanda-auth {:hosts ["my-db-1", "my-db-2"] :component nil :version "0.1.0"}))))

         (testing "reject nil version"
                  (is (thrown? java.lang.AssertionError
                               (end-deployment bigpanda-auth {:hosts ["my-db-1", "my-db-2"] :component "db" :version nil}))))

         (testing "deployment"
                  (let [deployment test-deployment]
                    ; Submit deployment
                    (end-deployment bigpanda-auth deployment))))

(defn test-end-deployment
  [options]
  (testing "deployment"
    (let [deployment test-deployment]
      ;; Submit deployment
      (is (end-deployment bigpanda-auth deployment options) "is created"))))

(deftest end-deployment-with-http-options-test
  (testing "reject nil hosts"
    (is (thrown? java.lang.AssertionError
                 (end-deployment bigpanda-auth {:hosts nil
                                                  :component "api"
                                                  :version "1.0.0"} {}))))

  (testing "reject nil component"
    (is (thrown? java.lang.AssertionError
                 (end-deployment bigpanda-auth {:hosts ["my-db-1"]
                                                  :component nil
                                                  :version "alpha"} {}))))

  (testing "reject nil check"
    (is (thrown? java.lang.AssertionError
                 (end-deployment bigpanda-auth {:hosts ["cache-1", "cache-2"]
                                                  :component "cache"
                                                  :version nil} {}))))

  (testing "with no http options"
    (test-end-deployment nil)))
