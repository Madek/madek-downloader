(ns madek.downloader.core
  (:require

    [json-roa.client.core :as roa]

    [cheshire.core :as cheshire]
    [clojure.java.io :as io]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.debug :as debug :refer [identity-with-logging ÷> ÷>>]]
    [logbug.thrown :as thrown]
    [logbug.catcher :as catcher]
    [logbug.ring :as logbug-ring :refer [wrap-handler-with-logging]]
    )

  (:import
    [java.io File]
    )
  )


;### title and prefix #########################################################

(defn get-title [media-resource]
  (-> media-resource
      (roa/relation :meta-data)
      (roa/get {:meta_keys (cheshire/generate-string ["madek_core:title"])})
      roa/coll-seq
      first
      (roa/get {})
      roa/data
      :value))

(defn useableFileName [s]
  (.replaceAll s "[^a-zA-Z0-9]" ""))

(defn path-prefix [media-resource]
  (let [prefix
        (if-let [title (get-title media-resource)]
          (str (useableFileName title) "_")
          "")]
    (str prefix (-> media-resource roa/data :id))))


;### meta data ################################################################

(defn get-meta-data-for-md-relation-type [md]
  (-> md (roa/get {})
      roa/data))

(defn get-collection-meta-datum-values [meta-datum]
  (merge (-> meta-datum roa/data
             (dissoc :value))
         {:values (->> meta-datum
                       roa/coll-seq
                       (map get-meta-data-for-md-relation-type))}))

(defn get-scalar-meta-datum-value [meta-datum]
  (-> meta-datum
      roa/data))

(defn get-metadata [media-resource]
  (->> (-> media-resource
           (roa/relation :meta-data)
           (roa/get {})
           roa/coll-seq)
       (map #(roa/get % {}))
       (map (fn [meta-datum]
              (case (-> meta-datum roa/data :type)
                ("MetaDatum::Text" "TextDate") (get-scalar-meta-datum-value meta-datum)
                (get-collection-meta-datum-values meta-datum)
                )))))

(defn write-meta-data [target-dir meta-data]
  (let [path (str target-dir File/separator "meta-data.json")]
    (io/make-parents path)
    (spit path (cheshire/generate-string meta-data {:pretty true}))))


;### DL Previews ##############################################################

(defn download-previews [target-dir media-file]
  (let [previews-dir (str target-dir File/separator "previews")]
    (doseq [preview-rel (-> media-file roa/coll-seq)]
      (let [preview (roa/get preview-rel {})
            preview-path (str previews-dir File/separator
                              (-> preview roa/data :filename))
            preview-response (-> preview
                                 (roa/relation :data-stream)
                                 (roa/get {} :mod-conn-opts #(assoc % :as :stream)))]
        (logging/info "Downloading preview " (-> preview roa/data :filename))
        (io/make-parents preview-path)
        (clojure.java.io/copy (-> preview-response :body)
                              (clojure.java.io/file preview-path))
        ))))

;### DL Media-Files ###########################################################

(defn download-media-file [target-dir media-file]
  (let [response (-> media-file
                     (roa/relation :data-stream)
                     (roa/get {} :mod-conn-opts #(assoc % :as :stream)))
        file-name (let [filename (-> media-file roa/data :filename)]
                    (if (clojure.string/blank? filename)
                      (-> media-file roa/data :id)
                      filename))
        file-path (str target-dir File/separator file-name) ]
    (logging/info "Downloading media-file to " file-path)
    (io/make-parents file-path)
    (clojure.java.io/copy (-> response :body) (clojure.java.io/file file-path))
    (download-previews target-dir media-file)
    ))

(defn download-media-files [target-dir media-entry]
  (catcher/wrap-with-log-error
    (let [media-files-dir (str target-dir File/separator "media-files")]
      (doseq [media-file [(-> media-entry (roa/relation :media-file) (roa/get {}))]]
        (let [media-file-data (roa/data media-file)
              media-file-dir (str media-files-dir File/separator (:id media-file-data))
              media-file-data-path (str media-file-dir File/separator "data.json")]
          (io/make-parents media-file-data-path)
          (spit media-file-data-path (cheshire/generate-string media-file-data {:pretty true}))
          (download-media-file media-file-dir media-file)
          )))))


;### DL Media-Entry ###########################################################

(defn download-media-entry [dir-path media-entry-rel]
  (let [media-entry (roa/get media-entry-rel {})
        id (-> media-entry roa/data :id)
        entry-prefix-path (path-prefix media-entry)
        entry-dir-path (str dir-path File/separator entry-prefix-path)
        meta-data (get-metadata media-entry)]
    (logging/info "Downloading media-entry " id " to " entry-dir-path)
    (io/make-parents entry-dir-path)
    (write-meta-data entry-dir-path meta-data)
    (download-media-files entry-dir-path media-entry)))


;### check credentials ########################################################

(defn check-credentials [api-entry-point api-http-opts]
  (let [response (-> (roa/get-root api-entry-point :default-conn-opts api-http-opts)
                     (roa/relation :auth-info)
                     (roa/get {}))]
    (logging/info (-> response roa/data))))

;### DL Set ###################################################################

(defn download-set [id dl-path api-entry-point api-http-opts]
  (catcher/wrap-with-log-error
    (let [me-get-opts (merge {:collection_id id}
                             (if (or (:basic-auth api-http-opts)
                                     (-> api-http-opts :cookies (get "madek-session")))
                               {:me_get_full_size "true"}
                               {:public_get_full_size "true"}))
          collection (-> (roa/get-root api-entry-point
                                       :default-conn-opts api-http-opts)
                         (roa/relation :collection )
                         (roa/get {:id id}))
          path-prefix (path-prefix collection)
          target-dir-path (str dl-path File/separator path-prefix)]
      (logging/info "Downloading " id " to " target-dir-path)
      (doseq [me (÷> identity-with-logging
                     (roa/get-root api-entry-point
                                   :default-conn-opts api-http-opts)
                     (roa/relation :media-entries)
                     (roa/get me-get-opts)
                     roa/coll-seq)]
        (download-media-entry target-dir-path me)))))


;##############################################################################

;(def ^:dynamic *dl-dir* (str (System/getProperty "user.dir") File/separator "tmp"))

;(def dev-api-entry-url "http://localhost:3100/api")

;(def staging-api-entry-url "http://staging-v3-pdata.madek.zhdk.ch/api/")

;(download-set "f41f40e4-c6b6-487e-8ac1-ca6f866a7e7e" *dl-dir* dev-api-entry-url {})

;##############################################################################

;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)
