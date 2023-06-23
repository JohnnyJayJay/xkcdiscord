(ns xkcdiscord.core
  (:require [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring-discord-auth.ring :refer [wrap-authenticate]]
            [ring.util.response :refer [response]]
            [org.httpkit.server :as server]
            [xkcdiscord.command :as cmd :refer [command-paths search-autocompleter]]
            [clojure.edn :as edn]
            [unilog.config :refer [start-logging!]]
            [clojure.tools.logging :as log]
            [slash.core :as slash]
            [slash.webhook :refer [webhook-defaults]])
  (:gen-class))

(def slash-handlers
  (assoc webhook-defaults :application-command command-paths :application-command-autocomplete search-autocompleter))

(defn handler [{:keys [body]}]
  (response (slash/route-interaction slash-handlers body)))

(defn -main
  [& _args]
  (println "Reading config...")
  (let [config (edn/read-string (slurp "config/config.edn"))
        logging-defaults {:level "info" :console true}]
    (start-logging! (merge logging-defaults (:logging config)))
    (log/info "Reading xkcd archive...")
    (cmd/populate-archive!)
    (log/info "Starting RSS polling...")
    (cmd/start-rss-updates! (:rss-period config))
    (log/info "Starting server...")
    (server/run-server
     (-> handler
         wrap-json-response
         (wrap-json-body {:keywords? true})
         (wrap-authenticate (:public-key config))
         #_wrap-with-logger
         #_wrap-reload)
     (:server config))))
