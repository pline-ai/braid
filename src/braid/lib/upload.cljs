(ns braid.lib.upload
  (:require
   [clojure.string :as string]
   [goog.object :as o]))

(def regex (delay
             (re-pattern (str "https?://"
                              (-> (o/get js/window "asset_domain")
                                  (string/replace "." "\\."))
                              "/"))))

(defn ->path [url]
  (some-> url
          (string/replace
            @regex
            (str "//" (o/get js/window "api_domain") "/upload/"))))

(defn upload-path? [url]
  (string/includes? url (o/get js/window "asset_domain")))
