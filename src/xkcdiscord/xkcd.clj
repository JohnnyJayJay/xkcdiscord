(ns xkcdiscord.xkcd
  (:refer-clojure :exclude [get])
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [hickory.core :as html]
            [hickory.select :as s]
            [remus :as rss]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import (java.net URI)
           (javax.net.ssl SNIHostName SSLEngine)))

(defn ssl-configurer [^SSLEngine eng, ^URI uri]
  (let [host-name (SNIHostName. (.getHost uri))
        params (doto (.getSSLParameters eng)
                 (.setServerNames [host-name]))]
    (doto eng
      (.setUseClientMode true) ;; required for JDK12/13 but not for JDK1.8
      (.setSSLParameters params))))

(def http-client (http/make-client {:ssl-configurer ssl-configurer}))

(def base-url "https://xkcd.com")

(def random-url "https://c.xkcd.com/random/comic/")

(defn xkcd-url [page]
  (cond-> base-url page (str \/ page)))

(def archive-url (xkcd-url "archive"))

(def rss-url (xkcd-url "atom.xml"))

(defn read-rss []
  (-> @(http/get rss-url {:client http-client})
      :body
      (.getBytes "UTF-8")
      (io/input-stream)
      rss/parse-stream))


(defn api-url [url]
  (str url "/info.0.json"))

(def headers
  {"User-Agent" "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/113.0"
   "Host" "xkcd.com"
   "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"})

(defn get
  ([] (get nil))
  ([num]
   (let [{:keys [status body] :as response} @(http/get (api-url (xkcd-url num)) {:client http-client})]
     (if (= status 200)
       (json/parse-string body keyword)
       (ex-info "Request failed" response)))))

(def url-pattern #"https?:\/\/xkcd\.com/(\d+)\/?")

(defn xkcd-url->num [url]
  (clojure.core/get (re-matches url-pattern url) 1 nil))

(defn random []
  (-> @(http/get random-url {:follow-redirects false :client http-client})
      :headers
      :location
      xkcd-url->num
      get))

(defn read-archive []
  (->> @(http/get archive-url {:client http-client})
       :body
       html/parse
       html/as-hickory
       (s/select (s/child (s/tag :body) (s/id "middleContainer") (s/tag :a)))
       (map (juxt (comp xkcd-url->num (partial str base-url) :href :attrs) (comp first :content)))
       (reverse)
       (vec)))

(defn search [archive query]
  (let [lower-query (string/lower-case query)]
    (filter (comp #(string/includes? % lower-query) string/lower-case second) archive)))
