(ns foo.example.pdf
  (:require [cljsjs.pdfmake]
            [cljsjs.pdfmakefonts]))

(def doc-text
  (clj->js
    {
     :pageSize "A4"

     :footer   (fn [currentPage pageCount] (clj->js {:text (str "Strana: " currentPage " od " pageCount) :style "footer"}))

     :content

               [
                {:text "Naslov" :style "header"}
                {:text "text1 text1 text1 text1 text1 text1 " :style "text"}
                {:text "text2 text2 text2 text2 text2 text2 " :style "text"}
                {:text "text3 text3 text3 text3 text3 text3 " :style "text" :pageBreak "after"}
                {:layout "lightHorizontalLines"
                 :table {
                         :header-rows 1
                         :widths [ "*", :auto, 100, "*"]
                         :body [
                                [ "First", "Second", "Third", "The last one"]
                                [ "Val 1", "Val 2", "Val 3", "Val 4"]
                                [ { :text "Bold value 1", :bold true }, "Val 2", "Val 3", "Val 4"]
                                [ { :text "Italics value 1", :italics true }, "Val 2", "Val 3", "Val 4"]]}}]

     :styles

               {:header    {:font-size 22
                            :alignment :center
                            :margin [10 10 10 10]
                            :bold      true}
                :text      {:font-size 12
                            :alignment :left}
                :footer    {:font-size 9
                            :italics true
                            :margin [10 10 10 10]
                            :alignment :right}}}))


(defn test-pdf []
  (.. js/pdfMake
      (createPdf doc-text)
      (open)))



