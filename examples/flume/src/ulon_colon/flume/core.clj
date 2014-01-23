(ns ulon-colon.flume.core
  (:use [flume-node.core :as flume :only [defsink defsource defagent]]
        [ulon-colon.producer :only [start-producer produce]])
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as j])
  (:import [org.apache.flume.event EventBuilder]
           [java.sql Types]))

(def oracle-db {:classname "oracle.jdbc.driver.OracleDriver"
                :subprotocol "oracle"
                :subname "thin:@localhost:1521/XE"
                :user "scott"
                :password "tiger"})
(def oracle-conn (atom nil))
(def watch-stmt  (atom nil))

(defsource oracle-insert-source
  :start (fn []
           (reset! oracle-conn (j/get-connection oracle-db))
           (let [stmt (.prepareCall @oracle-conn "{call DBMS_ALERT.REGISTER(?)}")]
             (doto stmt
               (.setString 1 "NEW_EMP")
               (.executeUpdate)
               (.close)))
           (let [stmt (.prepareCall @oracle-conn "{call DBMS_ALERT.WAITANY(?,?,?,?)}")]
             (doto stmt
               (.registerOutParameter 1 Types/VARCHAR)
               (.registerOutParameter 2 Types/VARCHAR)
               (.registerOutParameter 3 Types/INTEGER)
               (.setInt 4 300))
             (reset! watch-stmt stmt)))

  :process (fn []
             (.commit @oracle-conn)
             (.executeUpdate @watch-stmt)
             (when (= 0 (.getInt @watch-stmt 3))
               (let [msg (.getString @watch-stmt 2)]
                 (EventBuilder/withBody (.getBytes msg)))))
  :stop (fn []
           (let [stmt (.prepareCall @oracle-conn "{call DBMS_ALERT.REMOVE(?)}")]
             (doto stmt
               (.setString 1 "NEW_EMP")
               (.executeUpdate)
               (.close)))
           (.close @watch-stmt)
           (.close @oracle-conn)))

(defsink ulon-colon-sink
  :start  (fn [] (start-producer))
  :process (fn [event]
             (produce (String. (.getBody event)))))

(defagent :a1
  (flume/source :r1
          :type "ulon-colon.flume.core/oracle-insert-source"
          :channels :c1)
  (flume/sink :k1
        :type "ulon-colon.flume.core/ulon-colon-sink"
        :channel :c1)
  (flume/channel :c1
           :type "memory"
           :capacity 1000
           :transactionCapacity 100))

(defn -main []
  (let [application (flume/make-app)]))
