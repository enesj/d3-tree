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
                                 (sort-by :JUSopis  (second group))
                                 (sort-by :JUSId  (second group))))))]
    ;(println doc-list)
    (clj->js
      {
       :pageSize "A4"

       :footer   (fn [currentPage pageCount] (clj->js {:text (str "Strana: " currentPage " od " pageCount) :style "footer"}))

       :content

                 [
                  (if result-type {:text result-type :style "header"})
                  (if dokument {:text dokument :style "header-1"})
                  {:text naslov :style "header-2"}


                  {:layout "lightHorizontalLines"
                   :table  {
                            :header-rows 1
                            :widths      ["*"]
                            :body        (map vector (flatten doc-list))}}]

       :styles

                 {:header   {:fontSize  12
                             :alignment :left
                             :italics   true}
                  :header-1 {:fontSize  14
                             :alignment :left
                             :margin    [0 0 10 10]
                             :bold      true}
                  :header-2 {:fontSize  12
                             :alignment :right
                             :margin    [0 0 10 10]
                             :italics   true}
                  :text     {:fontSize  12
                             :alignment :left}
                  :footer   {:font-size 9
                             :italics   true
                             :margin    [10 10 10 10]
                             :alignment :right}}})))


(defn prepare-pdf [data data-type result]
  (let [naslov
        (case data-type
          :childs "Prikaz vezanih dokumenata: "
          :parents "Prikaz dokumenata za koje je vezan: "
          :history "Prikaz zapamćenih dokumenata: ")
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



