(ns madek.downloader.main
  (:require
    [madek.downloader.core :as downloader]

    [clojure.tools.cli :refer [parse-opts]]
    [clojure.string :as string]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    )

  (:import
    [java.io File]
    [java.nio.file Files FileSystems Path Paths]
    )

  (:gen-class))

(logbug.thrown/reset-ns-filter-regex #".*madek.*")

(defn not-blank-or-nil-str [s]
  (when (not (clojure.string/blank? s))
    s))

;### path helper ##############################################################

(defn nio-path [s]
  (.getPath (FileSystems/getDefault)
            s (make-array String 0)))

(defn normalize-target-path [target]
  (let [target-path (-> target nio-path .normalize)
        absolute-target-path (if (.isAbsolute target-path)
                               target-path
                               (.resolve
                                 (-> (System/getProperty "user.dir") nio-path)
                                 target-path))]
    (.toString absolute-target-path)))


;### CLI parser ###############################################################

(def cli-options
  ;; An option with a required argument
  [["-l" "--login LOGIN" "Madek user / API-client login"
    :default (get (System/getenv) "MADEK_LOGIN")
    :parse-fn not-blank-or-nil-str]
   ["-p" "--password PASSWORD" "Password, the env. var. MADEK_PASSWORD is preferred"
    :default (get (System/getenv) "MADEK_PASSWORD")
    :parse-fn not-blank-or-nil-str]
   ["-u" "--url  URL" "Madek API URL, defaults to MADEK_API_URL"
    :default (or (get (System/getenv) "MADEK_API_URL")
                 "http://staging-v3-pdata.madek.zhdk.ch/api/")
    :parse-fn not-blank-or-nil-str]
   ["-s" "--session-token SESSION_TOKEN"
    (str "SESSION_TOKEN, preferred over  login/password for non api-clients, "
         "defaults to MADEK_SESSION_TOKEN")
    :default (get (System/getenv) "MADEK_SESSION_TOKEN")
    :parse-fn not-blank-or-nil-str]
   ["-t" "--target-dir DIRECTORY" "Directory where the files will be downloaded to"
    :default (System/getProperty "user.dir")
    :parse-fn normalize-target-path]
   ["-h" "--help"]])

(defn usage [options-summary & more]
  (->> ["Madek Downloader"
        ""
        "Usage: madek-downloader action entity [options]"
        ""
        "Options:"
        options-summary
        ""
        "Commands"
        "  download ID"
        "  check-credentials"
        ""
        more]
       flatten (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg & msgs]
  (println (string/join \newline (flatten [msg msgs])))
  (System/exit status))

(defmacro catch* [level return-expr & expressions]
  `(try
     ~@expressions
     (catch Throwable e#
       (logging/log ~level (logbug.thrown/stringify e#))
       (if (clojure.test/function? ~return-expr)
         (apply ~return-expr [e#])
         ~return-expr))))

(defn- options-to-http-options [options]
  (let [{login :login password :password
         session-token :session-token} options]
    (cond-> {}
      (and login password) (assoc :basic-auth
                                  [login password])
      session-token (assoc :cookies
                           {"madek-session"
                            {:value session-token}}))))

(defn download [id options]
  (logging/info "DOWNLOADING " id " with "
                (update-in options [:password]
                           (fn [pw] (when pw "*****"))))
  (let [{url :url target-dir :target-dir} options]
    (downloader/download-set id target-dir url
                             (options-to-http-options options))))

(defn check-credentials [options]
  (let [{url :url} options]
    (downloader/check-credentials
      url (options-to-http-options options))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        [cmd & cmd-args] arguments]
    (catch* :error (fn [e] (exit 1 (usage summary "ERROR" (str e))))
            (logging/info {:options options})
            ;; Handle help and error conditions
            (cond
              (:help options) (exit 0 (usage summary))
              errors (exit 1 (usage summary) (error-msg errors))
              (= cmd "download") (let [[id & more] cmd-args]
                                   (cond (clojure.string/blank? id)
                                         (throw (ex-info "The ID may not be empty" {}))
                                         :else (download id options)))
              (= cmd "check-credentials") (check-credentials options)
              :else (exit 1 (usage summary "ERROR" "No command given"))
              ))))


;(clojure.test/function? #(println "Hello"))
;(clojure.test/function? :fn)
;(ifn? :fn)


;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
(debug/debug-ns *ns*)
