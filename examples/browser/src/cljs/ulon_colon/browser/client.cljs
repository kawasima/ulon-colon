(ns ulon-colon.browser.client
  (:use [ulon-colon.consumer :only [make-consumer consume consume-sync]]
        [goog.crypt.base64 :only [encodeByteArray]]
        [jayq.core :only [$ document-ready]]))

(document-ready
 (fn []
   (let [consumer (make-consumer "ws://localhost:5629")
         canvas   (first ($ "#image-1"))
         ctx      (. canvas getContext "2d")
         img      (js/Image.)]
     (set! (.-onload img)
           #((. ctx drawImage img 0 0)))
     (consume consumer
              (fn [msg]
                (set! (. img -src) (str "data:image/png;base64,"
                                        (encodeByteArray (msg :image)))))))))
