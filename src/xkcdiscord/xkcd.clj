(ns xkcdiscord.xkcd
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [hickory.core :as html]
            [hickory.select :as s]
            [remus :as rss]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(def base-url "https://xkcd.com")

(def random-url "https://c.xkcd.com/random/comic/")

(defn xkcd-url [page]
  (cond-> base-url page (str \/ page)))

(def archive-url (xkcd-url "archive"))

(def rss-url (xkcd-url "atom.xml"))

(defn read-rss []
  (-> @(http/get rss-url)
      :body
      (.getBytes "UTF-8")
      (io/input-stream)
      rss/parse-stream))

(defn api-url [url]
  (str url "/info.0.json"))

(defn get
  ([] (get nil))
  ([num]
   (let [{:keys [status body] :as response} @(http/get (api-url (xkcd-url num)))]
     (if (= status 200)
       (json/parse-string body keyword)
       (throw (ex-info "Request failed" response))))))

(def url-pattern #"https?:\/\/xkcd\.com/(\d+)\/?")

(defn xkcd-url->num [url]
  (clojure.core/get (re-matches url-pattern url) 1 nil))

(defn random []
  (-> @(http/get random-url {:follow-redirects false})
      :headers
      :location
      xkcd-url->num
      get))

(defn read-archive []
  (->> @(http/get archive-url)
       :body
       html/parse
       html/as-hickory
       (s/select (s/child (s/tag :body) (s/id "middleContainer") (s/tag :a)))
       (map (juxt (comp xkcd-url->num (partial str base-url) :href :attrs) (comp first :content)))
       (reverse)
       (vec)))

;; Search in transcripts?
;; How to get that text at scale?
(defn search [archive query]
  (let [lower-query (string/lower-case query)]
    (filter (comp #(string/includes? % lower-query) string/lower-case second) archive)))
