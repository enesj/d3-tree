(ns foo.example.routes.jus
  (:require [compojure.core :refer [defroutes GET POST]]
            [foo.example.db.jus :as jus-db]
            [ring.util.response :refer [response]]))
            ;[compojure.route :as route]))

(defn jus-data [filter like comp]
  (response (jus-db/get-jus-filter filter like comp)))

(defn jus-update [filter field-data like]
  (response (jus-db/update-jus filter field-data like)))

(defn jus-insert [field-data]
  (response (jus-db/insert-jus field-data)))

(defn jus-delete [jusid]
  (response (jus-db/delete-jus jusid)))

(defn jus-veza []
  (response (jus-db/get-veza)))

(defn jus-tree []
  (response (jus-db/tree-data)))

;(defn jus-childs [parent verbose]
;  (response (jus-db/all-childs parent verbose)))
;
;(defn jus-parents [child]
;  (response (jus-db/all-parents child verbose)))

(defn search-data [doc verbose]
  (response (jus-db/search-data  doc verbose)))

(defn add-jus-veza [parent child]
  (response (jus-db/add-veza parent child)))

(defn del-jus-veza [parent child]
  (response (jus-db/del-veza parent child)))

(defn jus-active-data []
  (response (jus-db/active-data)))

(defn jus-count-veze []
  (response (jus-db/count-veze-all)))

(defn jus-only-jus []
  (response (jus-db/only-jus)))

(defn jus-naredbe []
  (response (jus-db/naredbe)))

(defn handle-upload [upload-file]
  (jus-db/handle-upload upload-file))


(defroutes jus-routes
           (GET "/jus/data" [filter like comp] (jus-data filter like comp))
           (GET "/jus/active-data" [] (jus-active-data))
           (GET "/jus/count-veze" [] (jus-count-veze))
           (GET "/jus/naredbe" [] (jus-naredbe))
           (GET "/jus/only-jus" [] (jus-only-jus))
           (GET "/jus/veza" [] (jus-veza))
           (GET "/jus/tree" [] (jus-tree))
           ;(GET "/jus/childs" [parent] (jus-childs parent))
           ;(GET "/jus/parents" [child] (jus-parents child))
           (GET "/jus/search-data" [doc verbose] (search-data doc verbose))
           (GET "/jus/add-veza" [parent child] (add-jus-veza parent child))
           (GET "/jus/del-veza" [parent child] (del-jus-veza parent child))
           (GET "/jus/update" [filter field-data like] (jus-update filter field-data like))
           (POST "/jus/upload" [upload-file] (handle-upload upload-file))
           (GET "/jus/insert" [field-data] (jus-insert  field-data))
           (GET "/jus/delete" [jusid] (jus-delete  jusid)))
