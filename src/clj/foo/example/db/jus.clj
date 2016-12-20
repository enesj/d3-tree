(ns foo.example.db.jus
  (:require [korma.core :refer [select select* where insert delete values update set-fields defentity limit order subselect
                                join fields modifier aggregate exec-raw]]
            [korma.db :refer [h2 transaction]]
            [cheshire.core :refer [generate-string]]
            [clojure.java.io :as io])
  (:use
    [com.rpl.specter :rename {select* sell subselect subsell}]
    [com.rpl.specter.macros :rename {select sel}]))

;(:import (java.util UUID))



(defentity JUS)
(defentity Veza)



(defn update-jus [filter field-data like]
  (let [criteria (if (= like "true") (transform [ALL LAST] #(into [] `(~'like ~(str "%" % "%"))) filter) filter)
        field-data (val (first field-data))]
    (transaction
      (update JUS (set-fields field-data) (where criteria)))))


(defn insert-jus [field-data]
  (let [field-data (val (first field-data))]
    (insert JUS (values field-data))))


(defn delete-jus [jusid]
  (delete JUS (where {:JUSId [= jusid]})))

(defn get-jus-filter [filter like comp]
  (if (> (count filter) 0)
    (let [criteria (if (= like "true") (transform [ALL LAST] #(into [] `(~'like ~(str "%" % "%"))) filter)
                                       (transform [ALL LAST] #(into [] `(~(symbol comp) ~%)) filter))]
      (select JUS (where criteria) (order :JUSId :asc)))
    (select JUS)))


(defn naredbe []
  (select JUS (where {:Naredba 1})))

(defn only-jus []
  (select JUS (where (= :Naredba 0))))

;(def only-jus
;	(only-jus-fn))

(defn get-veza []
  (select Veza))



(defn add-veza [parent child]
  ;(println parent child)
  (insert Veza (values {:Parent parent :Child child})))

(defn del-veza [parent child]
  (delete Veza (where {:Parent parent :Child child})))


(defn make-response [status value-map]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (generate-string value-map)})



(defn handle-upload [{:keys [filename size tempfile]}]
  (io/copy (io/file tempfile) (io/file (str "./public/pdf/" filename)))
  (if
    (or (not filename)
        (= "" filename))
    (make-response 400 {:status  "ERROR"
                        :message "No file parameter sent"})
    (make-response 200 {:status   "OK"
                        :filename filename
                        :size     (or size 0)
                        :tempfile (str tempfile)})))

(defn a-data []
  (select JUS (where {:JUSId [in (subselect Veza (fields :child))]})))

(defn active-data []
  (let [data (into (naredbe) (a-data))]
    [data (select Veza)]))


(defn count-veze [active-data]
  (let [[data veza] active-data]
    (into {} (pmap #(hash-map (first %) {:total 0 :locked 0 :childs (second %)})
                   (doall (for [id (pmap :JUSId data)] [id (mapv :Child (filterv #(= id (:Parent %)) veza))]))))))

(def count-veze-atom
  (atom nil))


(defn count-veza [id first-level-count]
  (let [total-count (:total-count (first-level-count id))]
    (if total-count
      [(first total-count) 0 0 0]
      (loop [childs [id]
             all-childs #{}]
        (if (not-empty childs)
          (let [childs-data (vals (select-keys first-level-count childs))
                result (into #{} (flatten (mapv :childs childs-data)))]
            (recur result (into all-childs result)))
          (swap! count-veze-atom assoc-in [id :total-count] [(count all-childs) 0])))))
  [0 0 0 0])


(defn count-veze-all []
  (let [active-data (active-data)
        first-level-count (reset! count-veze-atom (count-veze active-data))]
    (doall (for [id (pmap :JUSId (first active-data))] [id (count-veza id first-level-count)]))
    @count-veze-atom))

(defn get-path [id veze]
  (loop [id id path []]
    (let [parent (map :Parent (filter #(= id (:Child %)) veze))]
      (if (empty? parent)
        path
        (recur (first parent) (conj path (first parent)))))))


(defn all-acitve-paths []
  (let [[data veze] (active-data)]
    (group-by #(last (last %))
              (doall
                (for [child (map #(vector (:JUSId %) (:JUSopis %)) (filter #(and (= 0 (:Locked %)) (= 0 (:Fali %))) data))]
                  [(if (> (count (first child)) 3) (first child) (second child)) (count (get-path (first child) veze))
                   (mapv #(if (> (count %) 3) % (:JUSopis (first (filter (fn [x] (= % (:JUSId x))) data))))
                         (get-path (first child) veze))])))))

(defn all-paths []
  (let [[data veze] (active-data)]
    (group-by #(last (last %))
              (doall
                (for [child (map #(vector (:JUSId %) (:JUSopis %)) data)]
                  [(if (> (count (first child)) 3) (first child) (second child)) (count (get-path (first child) veze))
                   (mapv #(if (> (count %) 3) % (:JUSopis (first (filter (fn [x] (= % (:JUSId x))) data))))
                         (get-path (first child) veze))])))))

(defn all-childs [parent verbose]
  (let [groups (reduce merge (map (fn [x] {(first x) (set (map :Child (second x)))}) (group-by :Parent (get-veza))))]
    (loop [all-childs #{} childs #{parent}]
      (let [new-childs (apply clojure.set/union (map (fn [x] (get groups x)) childs))]
        (if (empty? new-childs)
          [all-childs (if (= verbose "1") (filter #(all-childs (:JUSId %)) (first (active-data))) [])]
          (recur (clojure.set/union new-childs all-childs) new-childs))))))

(defn all-parents [child verbose]
  (let [groups (reduce merge (map (fn [x] {(first x) (set (map :Parent (second x)))}) (group-by :Child (get-veza))))]
    (loop [all-parents #{} parents #{child}]
      (let [new-parents (apply clojure.set/union (map (fn [x] (get groups x)) parents))]
        (if (empty? new-parents)
          [all-parents (if (= verbose "1") (filter #(all-parents (:JUSId %)) (first (active-data))) [])]
          (recur (clojure.set/union new-parents all-parents) new-parents))))))

(defn search-data [doc verbose]
  [(all-parents doc verbose) (all-childs doc verbose)])


(defn tree-data []
  (let [veze (get-veza)
        jus-data (first (active-data))
        parents (mapv (fn [x] (merge (clojure.set/rename-keys (first (filter (fn [y] (= (first x) (:JUSId y))) jus-data)) {:JUSId :name :JUSopis :title})
                                     {:children (mapv #(hash-map :name (:Child %)) (second x))}))
                      (group-by :Parent veze))
        parents (conj parents {:name "1000" :title "BiH naredbe harmonizirane sa evropskim direktivama" :shorttitle ""
                               :children [{:name "1" :children nil} {:name "2" :children nil} {:name "3" :children nil} {:name "4" :children nil}
                                          {:name "5" :children nil} {:name "6" :children nil} {:name "7" :children nil}] :x0 0 :y0 0})
        all-childs (into #{} (map #(hash-map :name (:Child %) :children nil) veze))
        all-parents (into #{} (map #(hash-map :name (:Parent %) :children nil) veze))
        no-childs (mapv (fn [x] (clojure.set/rename-keys (first (filter (fn [y] (= (:name x) (:JUSId y))) jus-data)) {:JUSId :name :JUSopis :title}))
                        (clojure.set/difference all-childs all-parents))]
    (transform [ALL :children ALL] #(let [JUS (first (filter (fn [x] (= (:JUSId x) (:name %))) jus-data))]
                                     (merge % {:type (:Naredba JUS) :mandatory (:Mandatory JUS) :title (:JUSopis JUS)
                                               :shorttitle (if (= (:Naredba JUS) 0) (str (:JUSId JUS) ":" (:JUSgodina JUS)) "")}))
               (into [] (concat parents no-childs)))))



(defn ft-search-old [text phrase]
  (exec-raw
    (if phrase (str "SELECT * FROM vJUS WHERE vJUS MATCH '\"" text "\"'")
               (str "SELECT * FROM vJUS WHERE vJUS MATCH '" text "'")) :results))


(defn ft-search [text]
  (let [text (clojure.string/join " " (map (fn [x] (if (re-find #"\." x) (str "\"" x "\"") x))
                                           (clojure.string/split text #" ")))]
    (exec-raw (str "SELECT * FROM vJUS WHERE vJUS MATCH '" text "'") :results)))

;(map #(vector (first %) (count (second %))) (all-paths))

;(map #(vector (first %) (count (second %))) (all-acitve-paths))

;(defn get-user-by-email [email] (first (select user (where {:email email}) (limit 1))))
;(defn get-user-by-act-id [id] (first (select user (where {:activationid id}) (limit 1))))
;(defn get-user-by-uuid [uuid] (first (select user (where {:uuid uuid}) (limit 1))))
;
;(defn username-exists? [email] (some? (get-user-by-email email)))
;
;(defn create-user [email pw_crypted activationid & [is-active?]]
;  (insert user (values {:email email :pass pw_crypted :activationid activationid :is_active (or is-active? false)
;                         :uuid  (str (UUID/randomUUID))})))
;
;(defn set-user-active [activationid & [active]]
;  (update user (set-fields {:is_active (or active true)}) (where {:activationid activationid})))
;
;(defn update-user [uuid fields] (update user (set-fields fields) (where {:uuid uuid})))
;(defn delete-user [uuid] (delete user (where {:uuid uuid})))
;(defn change-password [email pw] (update user (set-fields {:pass pw}) (where {:email email})))
