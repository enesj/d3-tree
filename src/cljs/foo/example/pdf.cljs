(ns foo.example.pdf
  (:require [cljsjs.pdfmake]
            [cljsjs.pdfmakefonts]
            [foo.example.translation :as translation :refer [tr]]))


(defn doc-text [result-type dokument naslov format-data group-data-by-type]
  (let [doc-types {:1 (tr [:pdf/bh])
                   :2 (tr [:pdf/yu])
                   :3 (tr [:pdf/obavezna])
                   :4 (tr [:pdf/djelimicno])
                   :5 (tr [:pdf/upotreba])}
        doc-list (map
                   (fn [[first-g second-g]]
                     ;(sort-by first
                       (into [(vector {:text (first-g doc-types), :bold true :alignment :center :fontSize 13 :margin [10 10 10 10]})]
                             (map #(vector (if (= (:Naredba %) 0) (str (:JUSId %) ":" (:JUSgodina %) " " (:JUSopis %)) (:JUSopis %))))
                             (if (or (= first-g :1) (= first-g :2))
                               (sort-by :JUSopis second-g)
                               (sort-by :JUSId second-g)))))
        table {:layout "lightHorizontalLines"
               :table  {:headerRows 1
                        :widths     ["*"]
                        :body       nil}}
        ;tables (for [rows doc-list]
        ;         (assoc-in table [:table :body] rows))
        title [
               (when result-type {:text result-type :style "vrsta"})
               (when dokument {:text dokument :style "naslov"})
               {:text naslov :style "veza"}]
        content

                (into []
                      (comp format-data doc-list (map #(assoc-in table [:table :body] %)))
                      (sort-by first group-data-by-type))
        impressum (tr [:pdf/impressum])]
    ;(println doc-list)
    (clj->js
      {
       :pageSize "A4"
       :header   (fn [currentPage pageCount] (when (> currentPage 1) (clj->js {:text (str dokument ", " naslov) :style "header"})))
       :footer   (fn [currentPage pageCount] (when (> currentPage 1) (clj->js {:text [{:text impressum}
                                                                                      {:text (str (tr [:pdf/strana]) currentPage (tr [:pdf/od]) pageCount)  :bold true}]
                                                                               :style "footer"})))
       :content  [title content]
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
                           :margin    [10 10 10 10]
                           :alignment :left}
                  :header {:fontSize  9
                           :italics   true
                           :margin    [20 10 20 2]
                           :alignment :center}}})))

(defn prepare-pdf [data data-type result]
  (let [naslov
        (case data-type
          :childs (str (tr [:pdf/vezani]) (count data)  ") : ")
          :parents (str (tr [:pdf/vezan-za]) (count data)  ") : ")
          :history (str (tr [:pdf/zapamceni]) (count data)  ") : "))
        dokument
        (case (:Naredba result)
          (1 2 3) (str (:title result))
          (str (:name result) ":" (:JUSgodina result) " " (:title result)))
        doc-type
        (case (:Naredba result)
          1 (tr [:pdf/tip-bh])
          (2 3) (tr [:pdf/tip-yu])
          (tr [:pdf/tip-jus]))
        group-data-by-type (group-by #(key (first %)) data)
        format-data (map #(vector (first %) (mapv (fn [x] (val (first x))) (second %))))]

    (if (= data-type :history)
      (doc-text nil nil naslov format-data data)
      (doc-text doc-type dokument naslov format-data group-data-by-type))))


(defn export-pdf [data result-type result]
  (.. js/pdfMake
      (createPdf (prepare-pdf data result-type result))
      (open)))



