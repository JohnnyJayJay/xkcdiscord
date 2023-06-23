(ns xkcdiscord.command
  (:require [xkcdiscord.xkcd :as xkcd]
            [clojure.string :as string]
            [slash.command.structure :as cmd]
            [slash.component.structure :as cmp]
            [slash.command :refer [defhandler defpaths group]]
            [slash.response :as rsp])
  (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit)))

(def plain-option
  (cmd/option "plain" "Disable rich embedding" :boolean))

(def command
  (cmd/command
   "xkcd"
   "Get xkcd comics"
   :options
   [(cmd/sub-command
     "show"
     "Show the xkcd with the given number or the latest xkcd."
     :options
     [(cmd/option "number" "xkcd number (omit for latest)" :integer)
      plain-option])
    (cmd/sub-command "rand" "Show a random xkcd")
    (cmd/sub-command
     "search"
     "Search for xkcd comics"
     :options
     [(cmd/option "query" "The text to search for" :string :required true :autocomplete true)
      plain-option])
    (cmd/sub-command "info" "Display bot info")]))

(def archive
  (atom []))

(def ^ScheduledExecutorService scheduler (Executors/newSingleThreadScheduledExecutor))

(defn populate-archive! []
  (reset! archive (xkcd/read-archive)))

(defn update-archive! []
  (let [{[{:keys [title uri]}] :entries} (xkcd/read-rss)
        latest-num (xkcd/xkcd-url->num uri)]
    (swap! archive
           (fn [archive]
             (cond-> archive
               (-> archive last first (not= latest-num)) (conj [latest-num title]))))))

(defn start-rss-updates! [period]
  (.scheduleAtFixedRate scheduler ^Runnable update-archive! period period TimeUnit/MINUTES))

(defn comic->embed [{:keys [year month day num img safe_title alt]}]
  {:title safe_title
   :url (xkcd/xkcd-url num)
   :description alt
   :image {:url img}
   :color 0x96A8C8
   :timestamp (apply format "%04d-%02d-%02d" (map #(Integer/parseInt %) [year month day]))
   :footer {:text (str "xkcd no. " num)}})

(defn xkcd-response [{:keys [img] :as comic} plain?]
  (rsp/channel-message (if plain? {:content img} {:embeds [(comic->embed comic)]})))

(defn- show-xkcd [number plain]
  (let [comic (xkcd/get number)]
    (if comic
      (xkcd-response comic plain)
      (-> {:content (str "xkcd no. " number " doesn't exist :(")} rsp/channel-message rsp/ephemeral))))

(defhandler show-handler ["show"] _ [number plain]
  (show-xkcd number plain))

(defhandler rand-handler ["rand"] _ [plain]
  (let [comic (xkcd/random)]
    (xkcd-response comic plain)))

(defhandler search-handler ["search"] _ [query plain]
  (if-let [num (parse-long query)]
    (show-xkcd num plain)
    (let [results (->> (xkcd/search @archive query)
                       (map (fn [[num title]] (str "[`" num "` - " \" title "\"](https://xkcd.com/" num "/)")))
                       (take 15))]
      (-> {:embeds [{:title "Search results"
                     :description (if (seq results)
                                    (->> results (string/join "\n") (format "Results for \"%s\":\n\n%s" query))
                                    (str "No results found for \"" query "\" :("))
                     :color (if (seq results) 0x3BA55D 0xED4245)}]}
          rsp/channel-message))))

(defhandler search-autocompleter ["xkcd" "search"] _ [query]
  (->> (xkcd/search @archive query)
       (map (fn [[num title]] (cmd/choice (str num " - " \" title \") (str num))))
       (take 25)
       rsp/autocomplete-result))

(defhandler info-handler ["info"] _ []
  (rsp/channel-message
   {:content "Hi :wave:\nI'm a Discord app that displays xkcd comics for you :smile:"
    :components
    [(cmp/action-row
      (cmp/link-button "https://xkcd.com" :label "xkcd")
      (cmp/link-button "https://discord.com/oauth2/authorize?client_id=446771236970823687&scope=applications.commands" :label "Invite link")
      (cmp/link-button "https://github.com/JohnnyJayJay/xkcdiscord" :label "Source Code"))]}))

(defpaths command-paths
  (group ["xkcd"]
    show-handler
    rand-handler
    search-handler
    info-handler))
