(defproject xkcdiscord "0.1.0"
  :description "Discord xkcd bot"
  :url "https://github.com/JohnnyJayJay/xkcdiscord"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [ring/ring-core "1.9.3"]
                 [ring/ring-devel "1.9.3"]
                 [ring/ring-json "0.5.1"]
                 [http-kit "2.5.3"]
                 [com.github.johnnyjayjay/ring-discord-auth "1.0.0"]
                 [bananaoomarang/ring-debug-logging "1.1.0"]
                 [remus "0.2.1"]
                 [hickory "0.7.1"]]
  :main ^:skip-aot xkcdiscord.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
