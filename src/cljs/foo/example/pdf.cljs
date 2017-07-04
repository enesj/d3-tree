(ns foo.example.pdf
  (:require [cljsjs.pdfmake]
            [cljsjs.pdfmakefonts]))


(defn doc-text [result-type dokument naslov formated-data]
  (let [doc-types {:1 "BH naredbe"
                   :2 "YU naredbe pravilnici"
                   :3 "JUS sa obaveznom primjenom"
                   :4 "JUS sa djelimično obaveznom primejnom"
                   :5 "JUS za upotrebu"}
        doc-list (for [group (sort-by first formated-data)]
                   (into [(vector {:text ((first group) doc-types), :bold true :alignment :center :fontSize 13 :margin [10 10 10 10]})]
                         (mapv #(vector (if (= (:Naredba %) 0) (str (:JUSId %) ":" (:JUSgodina %) " " (:JUSopis %)) (:JUSopis %)))
                               (if (or (= (first group) :1) (= (first group) :2))
                                 (sort-by :JUSopis (second group))
                                 (sort-by :JUSId (second group))))))
        table {:layout "lightHorizontalLines"
               :table  {:headerRows 1
                        :widths     ["*"]
                        :body       nil}}
        tables (for [rows doc-list]
                 (assoc-in table [:table :body] rows))
        content (into [
                       (if result-type {:text result-type :style "vrsta"})
                       (if dokument {:text dokument :style "naslov"})
                       {:text naslov :style "veza"}]
                      tables)
        immpresum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"]
    (clj->js
      {
       :pageSize "A4"
       :header   (fn [currentPage pageCount] (when (> currentPage 1) (clj->js {:text (str dokument ", " naslov) :style "header"})))
       :footer   (fn [currentPage pageCount] (when (> currentPage 1) (clj->js {:text [{:text immpresum}
                                                                                      {:text (str "Strana: " currentPage " od " pageCount)  :bold true}]
                                                                               :style "footer"})))
       :content  content
       :styles

                 {:vrsta  {:fontSize  12
                           :alignment :left
                           :margin    [0 0 10 2]}
                  ;:italics   true}
                  :naslov {:fontSize  14
                           :alignment :justify
                           :margin    [0 0 10 10]
                           :bold      true}
                  :veza   {:fontSize  12
                           :alignment :right
                           :margin    [0 0 0 10]
                           :italics   true}
                  :text   {:fontSize  12
                           :alignment :left}
                  :footer {:fontSize  9
                           :italics   true
                           :margin    [40 10 20 10]
                           :alignment :left}
                  :header {:fontSize  9
                           :italics   true
                           :margin    [20 10 20 2]
                           :alignment :center}}})))

(defn prepare-pdf [data data-type result]
  (let [naslov
        (case data-type
          :childs (str "Prikaz vezanih dokumenata (" (count data)  ") : ")
          :parents (str "Prikaz dokumenata za koje je vezan (" (count data)  ") : ")
          :history (str "Prikaz zapamćenih dokumenata (" (count data)  ") : "))
        dokument
        (case (:Naredba result)
          (1 2 3) (str (:title result))
          (str (:name result) ":" (:JUSgodina result) " " (:title result)))
        doc-type
        (case (:Naredba result)
          1 "BH naredba"
          (2 3) "YU naredba/pravilnik"
          "JUS standard")
        group-data-by-type (group-by #(key (first %)) data)
        formated-data (mapv #(vector (first %) (mapv (fn [x] (val (first x))) (second %))) group-data-by-type)]

    (if (= data-type :history)
      (doc-text nil nil naslov formated-data)
      (doc-text doc-type dokument naslov formated-data))))


(defn export-pdf [data result-type result]
  (.. js/pdfMake
      (createPdf (prepare-pdf data result-type result))
      (open)))



