
(ns xkcdiscord.core
  (:require [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring-discord-auth.core :refer [wrap-authenticate]]
            [ring-debug-logging.core :refer [wrap-with-logger]]
            [ring.util.response :refer [response bad-request]]
            [org.httpkit.server :as server]
            [xkcdiscord.command :as cmd :refer [handle-command]]
            [clojure.edn :as edn])
  (:gen-class))

(defn handler [{{:keys [type] :as body} :body}]
  (or (some->
       (case type
         1 {:type 1}
         2 (handle-command body)
         nil)
       response)
      (bad-request "Unsupported interaction type")))

(defn -main
  [& args]
  (println "Reading config...")
  (let [config (edn/read-string (slurp "config/config.edn"))]
    (println "Reading xkcd archive...")
    (cmd/populate-archive!)
    (println "Starting RSS polling...")
    (cmd/start-rss-updates! (:rss-period config))
    (println "Starting server...")
    (server/run-server
     (-> handler
         wrap-json-response
         (wrap-json-body {:keywords? true})
         (wrap-authenticate (:public-key config))
         #_wrap-with-logger
         #_wrap-reload)
     (:server config))))
