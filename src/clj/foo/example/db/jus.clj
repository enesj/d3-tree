(ns foo.example.db.jus
  (:require [korma.core :refer [select select* where insert delete values update set-fields defentity limit order subselect
                                join fields modifier aggregate exec-raw]]
            [korma.db :refer [h2 transaction]]
            ;[spec-provider.provider :as sp]
            ;[clojure.test.check :as tc]
            ;[clojure.test.check.generators :as gen]
            ;[clojure.test.check.properties :as prop]
            [cheshire.core :refer [generate-string]]
            [clojure.java.io :as io])
  (:use
    [com.rpl.specter :rename {select* sell* subselect subsell select sell}]))


(require '[clojure.spec.alpha :as s])

;veza spec
(s/def ::Child string?)
(s/def ::Parent string?)

(s/def ::veza (s/keys :req-un [::Child ::Parent]))

;jus spec
(s/def ::Fali #{0 1})
(s/def ::Glasnik (s/nilable string?))
(s/def ::JUSgodina (s/nilable pos-int?))
(s/def ::Mandatory #{"" 0 1 2})
(s/def ::Strana (s/nilable pos-int?))
(s/def ::Naredba #{"" 0 1 2 3})
(s/def ::Direktiva string?)
(s/def ::JUSopis string?)
(s/def ::Locked #{0 1})
(s/def ::Link-d (s/nilable string?))
(s/def ::Napomena (s/nilable string?))
(s/def ::Link-n (s/nilable string?))
(s/def ::ICS (s/nilable string?))
(s/def ::JUSId string?)

(s/def
  ::jus
  (s/keys
    :req-un
    [::Direktiva
     ::Fali
     ::Glasnik
     ::ICS
     ::JUSId
     ::JUSgodina
     ::JUSopis
     ::Link-d
     ::Link-n
     ::Locked
     ::Mandatory
     ::Napomena
     ::Naredba
     ::Strana]))


;tree-data spec
(s/def ::title string?)
(s/def ::name string?)
(s/def ::shorttitle string?)
(s/def ::type #{"" 0 1 2})
(s/def ::mandatory #{"" 0 1 2})
(s/def
  ::children
  (s/coll-of
    (s/keys :req-un [::mandatory ::name ::shorttitle ::title ::type])))

(s/def
  ::tree-data
  (s/coll-of
    (s/keys
      :req-un
      [::Direktiva
       ::Fali
       ::Glasnik
       ::ICS
       ::JUSgodina
       ::Link-d
       ::Link-n
       ::Locked
       ::Mandatory
       ::Napomena
       ::Naredba
       ::Strana
       ::children
       ::name
       ::title])))


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


(defn count-veze-old [active-data]
  (let [[data veza] (active-data)]
    (into {} (map #(hash-map (first %) {:total 0 :locked 0 :childs (second %)})
                   (doall (for [id (map :JUSId data)] [id (map :Child (filterv #(= id (:Parent %)) veza))]))))))



(defn count-veze [active-data]
  (let [[data veza] active-data]
    (into {}
      (comp
        (map :JUSId)
        (map (fn [x] [x  (into [] (comp (filter (fn [y] (= x (:Parent y)))) (map :Child)) veza)]))
                        ;(map :Child (filterv (fn [y] (= x (:Parent y))) veza))])))
        (map #(hash-map (first %) {:total 0 :locked 0 :childs (second %)})))
      data)))



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
    (doall (for [id (pmap :JUSId (first active-data))]
             [id (count-veza id first-level-count)]))
    @count-veze-atom))

(defn all-childs [parent verbose jus-data veze]
  (let [groups (reduce merge (map (fn [x] {(first x) (set (map :Child (second x)))}) (group-by :Parent veze)))
        childs-memo (memoize (fn [jus-name] (get groups jus-name)))]
    (loop [all-childs #{} childs #{parent}]
      (let [new-childs (apply clojure.set/union (map (fn [x] (childs-memo x)) childs))]
        (if (empty? new-childs)
          [all-childs (if (= verbose "1") (filter #(all-childs (:JUSId %)) jus-data) [])]
          (recur (clojure.set/union new-childs all-childs) new-childs))))))

(defn all-parents [child verbose jus-data veze]
  (let [groups (reduce merge (map (fn [x] {(first x) (set (map :Parent (second x)))}) (group-by :Child veze)))]
    (loop [all-parents #{} parents #{child}]
      (let [new-parents (apply clojure.set/union (map (fn [x] (get groups x)) parents))]
        (if (empty? new-parents)
          [all-parents (if (= verbose "1") (filter #(all-parents (:JUSId %)) jus-data) [])]
          (recur (clojure.set/union new-parents all-parents) new-parents))))))


(defn search-data [doc verbose]
  (let [[jus-data veze]  (active-data)]
    [(all-parents doc verbose jus-data veze) (all-childs doc verbose jus-data veze)]))

(def all-children-path (comp-paths [ALL :children ALL]))


(defn tree-data []
  (let [[jus-data veze]  (active-data)
        get-jus (memoize (fn [jus-name] (first (drop-while  (fn [x] (not= (:JUSId x) jus-name) ) jus-data))))
        rename-jus  (fn [x] (clojure.set/rename-keys
                                   (get-jus  x)
                                   {:JUSId :name :JUSopis :title}))
        parents (pmap (fn [x] (merge (rename-jus (first x))
                                     {:children (mapv #(hash-map :name (:Child %)) (second x))}))
                      (group-by :Parent veze))
        parents (conj parents {:name "1000" :title "BiH naredbe harmonizirane sa evropskim direktivama" :shorttitle ""
                               :children [{:name "1" :children nil} {:name "2" :children nil} {:name "3" :children nil} {:name "4" :children nil}
                                          {:name "5" :children nil} {:name "6" :children nil} {:name "7" :children nil}] :x0 0 :y0 0})
        all-childs (into #{} (pmap #(hash-map :name (:Child %) :children nil) veze))
        all-parents (into #{} (pmap #(hash-map :name (:Parent %) :children nil) veze))
        no-childs (pmap (fn [y](rename-jus (:name y)))
                        (clojure.set/difference all-childs all-parents))]
    (compiled-transform  all-children-path #(let [JUS (get-jus (:name %))]
                                              (merge % {:type (:Naredba JUS) :mandatory (:Mandatory JUS) :title (:JUSopis JUS)
                                                        :shorttitle (if (= (:Naredba JUS) 0) (str (:JUSId JUS) ":" (:JUSgodina JUS)) "")}))
               (into [] (concat parents no-childs)))))
