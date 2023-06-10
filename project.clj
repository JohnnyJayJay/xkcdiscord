(defproject xkcdiscord "0.1.0"
  :description "Discord xkcd bot"
  :url "https://github.com/JohnnyJayJay/xkcdiscord"
  :license {:name "MIT"
            :url "https://mit-license.org"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-json "0.5.1"]
                 [http-kit "2.6.0"]
                 [com.github.johnnyjayjay/ring-discord-auth "1.0.1"]
                 [com.github.johnnyjayjay/slash "0.6.0-SNAPSHOT"]
                 [remus "0.2.4"]
                 [org.clj-commons/hickory "0.7.3"]]
  :plugins [[com.github.johnnyjayjay/lein-licenses "0.2.0"]
            [lein-ancient "1.0.0-RC3"]]
  :main ^:skip-aot xkcdiscord.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[bananaoomarang/ring-debug-logging "1.1.0"]
                                  [ring/ring-devel "1.10.0"]]}})
