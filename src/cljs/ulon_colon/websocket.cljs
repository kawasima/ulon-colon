(ns ulon-colon.websocket
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [fressian-cljs.core :as fress]
            [cljs.core.async :refer [chan <! put!]]))

(defn ReconnectingWebSocket [url protocols]
  (this-as this
    (set! (. this -url) url)
    (set! (. this -protocols) (or protocols []))))
