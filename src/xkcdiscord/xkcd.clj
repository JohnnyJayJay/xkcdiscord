(ns xkcdiscord.xkcd
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))

(def base-url "https://xkcd.com")

(def random-url "https://c.xkcd.com/random/comic/")

(defn xkcd-url [num]
  (cond-> base-url num (str \/ num)))

(defn api-url [url]
  (str url "/info.0.json"))

(defn get
  ([] (get nil))
  ([num]
   (let [{:keys [status body]} @(http/get (api-url (xkcd-url num)))]
     (when (= status 200)
       (json/parse-string body keyword)))))

(def url-pattern #"https?:\/\/xkcd\.com/(\d+)\/")

(defn xkcd-url->num [url]
  (clojure.core/get (re-matches url-pattern url) 1 nil))

(defn random []
  (-> @(http/get random-url {:follow-redirects false})
      :headers
      :location
      xkcd-url->num
      get))
