(ns xkcdiscord.command
  (:require [xkcdiscord.xkcd :as xkcd]))

(def plain-option
  {:name "plain"
   :description "Disable rich embedding"
   :type 5})

(def command
  {:name "xkcd"
   :description "Get xkcd comics"
   :options
   [{:name "show"
     :description "Show the xkcd with the given number or the latest xkcd"
     :type 1
     :options
     [{:name "number"
       :description "xkcd number (omit for latest)"
       :type 4}
      plain-option]}
    {:name "rand"
     :description "Show a random xkcd"
     :type 1
     :options [plain-option]}]})

(defn command-path [{{:keys [name options]} :data}]
  (into
   [name]
   (->> (get options 0 nil)
        (iterate (comp #(get % 0 nil) :options))
        (take-while (comp #{1 2} :type))
        (map :name))))

(defn command-options [interaction depth]
  (as-> interaction $
    (get-in $ (into [:data :options] (flatten (repeat depth [0 :options]))))
    (zipmap (map (comp keyword :name) $) (map :value $))))

(defn comic->embed [{:keys [year month day num img safe_title]}]
  {:title safe_title
   :url (xkcd/xkcd-url num)
   :image {:url img}
   :color 0x96A8C8
   :timestamp (apply format "%04d-%02d-%02d" (map #(Integer/parseInt %) [year month day]))
   :footer {:text (str "xkcd no. " num)}})

(defmulti handle-command command-path)

(defn xkcd-response [{:keys [img] :as comic} plain?]
  {:type 4
   :data (if plain? {:content img} {:embeds [(comic->embed comic)]})})

(defmethod handle-command ["xkcd" "show"]
  [command]
  (let [{:keys [number plain]} (command-options command 1)
        comic (xkcd/get number)]
    (if comic
      (xkcd-response comic plain)
      {:type 4 :data {:content (str "xkcd no. " number " doesn't exist :(") :flags 64}})))

(defmethod handle-command ["xkcd" "rand"]
  [command]
  (let [{:keys [plain]} (command-options command 1)
        comic (xkcd/random)]
    (xkcd-response comic plain)))
