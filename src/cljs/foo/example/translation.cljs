(ns foo.example.translation
  (:require [taoensso.tempura :as tempura]
            [reagent.core :as r :refer [atom]]))

(def lang (atom :bs))

(def dictionary
  {:bs
   {:missing   "bs missing text"
    :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
    :ac-label  "Naziv"
    :ac-hint   "Unesi dio teksta iz naslova"
    :badges    {:broj    "Broj dokumenata u grupi"
                :zapamti "Zapamti"
                :veze    "Veze"
                :nova    "Nova pretraga"
                :otvori  "Otvori"}
    :legend    {:bh          "BiH Naredbe"
                :yu          "YU Naredbe\\Pravilnici"
                :obavezna    "JUS sa obaveznom primjenom"
                :djelilmicno "JUS sa djelimično obaveznom primjenom"
                :upotreba    "JUS za upotrebu"
                :rezultat    "Naredbe\\standardi koji sadrže rezultat pretrage"
                :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                :primjer1    "Primjer 1: u grupi se nalazi 25 dokumenata"
                :primjer2    "Primjer 2: u grupi se nalazi 3 dokumenta"
                :broj        "Broj dokumenata u grupi"
                :prikazi     "Prikaži legendu"}
    :doc-view  {:naziv "Naziv"
                :vrsta "Vrsta: "}
    :yu-view   {:yu-name "YU naredba/pravilnik"
                :glasnik "Službeni glasnik: "
                :prikazi "Prikaži dokument"}
    :bh-view   {:bh-name   "BiH naredba"
                :glasnik   "Službeni glasnik: "
                :direktiva "Evropska direktiva: "
                :prikazi   "Prikaži dokument"}
    :jus-view  {:jus-name   "JUS standard"
                :godina     "Godina: "
                :primjena   "Primjena: "
                :obavezna   "Obavezna"
                :djelimicno "Djelimično obavezna"
                :upotreba   "Za upotrebu"
                :strana     "Broj strana: "
                :isc        "ICS: "}
    :doc-type  {:svi "Svi dokumenti"
                :bh  "BiH naredbe"
                :yu  "YU naredbe/pravilnici"
                :jus "JUS standardi"}
    :ac-tip    {:prikazi "Prikazi podatke o dokumentu"
                :zapamti "Zapamti ovaj dokument"
                :veza    "Prikazi dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
                :obrisi  "Obriši kriterij za pretragu"}
    :ac-button {:otvori  "Otvori"
                :zapamti "Zapamti"
                :veze    "Veze"
                :nova    "Nova pretraga"}
    :tooltip   {:brisi-vezu     "Briši vezu"
                :prikazi-vezu   "Prikaži vezu"
                :brisi-istoriju "Briši iz zapamćenih"
                :pdf-listing    "PDF listing"}
    :search    {:title     "Pretraga podataka o naredbama/pravilnicima/standardima"
                :vezuju    "Pretraga dokumenata koji vezuju"
                :vezani    "Pretraga vezanih dokumenata"
                :zapamceni "Pretraga zapamćenih dokumenta"
                :brisi     "Briši pretragu"}
    :table     {:veze      "Prikaži veze ("
                :zaboravi  "Zaboravi"
                :empty     "Nema dokumenata za prikaz"
                :zapamceni "Zapamćeni dokumenti"}
    :graph     {:otvori      "Otvori"
                :zapamti     "Zapamti"
                :zatvori     "Zatvori"
                :prikaz-long "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
                :prikaz      "Grafički prikaz"}
    :filter    {:bh         "BiH naredbe"
                :yu         "YU naredbe"
                :obavezna   "JUS standardi - obavezna primjena"
                :djelimicno "JUS standardi - djelimično obavezna primjena"
                :upotreba   "JUS standardi - za upotrebu"}
    :tabs      {:vezani    "Vezani dokumenti:"
                :vezan-za  "Vezan za dokumente:"
                :zapamceni "Zapamćeni dokumenti"}
    :veze      {:veza-sa  "Veza sa:"
                :pretraga "Pretraga zapamćenih dokumenta"}
    :pdf       {:bh          "BiH Naredbe"
                :yu          "YU Naredbe\\Pravilnici"
                :obavezna    "JUS sa obaveznom primjenom"
                :djelimicno "JUS sa djelimično obaveznom primjenom"
                :upotreba    "JUS za upotrebu"
                :strana "Strana "
                :od " od "
                :impressum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                :vezani "Prikaz vezanih dokumenata ("
                :vezan-za "Prikaz dokumenata za koje je vezan ("
                :zapamceni "Prikaz zapamćenih dokumenata ("
                :tip-bh "BH naredba"
                :tip-yu "YU naredba/pravilnik"
                :tip-jus  "JUS standard"}}

   :sr
   {:missing   "sr missing text"
    :title     "Везе ЈУС стандарда и хармонизираних БиХ наредби"
    :ac-label  "Назив"
    :ac-hint   "Унеси дио текста из наслова"
    :badges    {:broj    "Број докумената у групи"
                :zapamti "Запамти"
                :veze    "Везе"
                :nova    "Нова претрага"
                :otvori  "Отвори"}
    :legend    {:bh          "БиХ Наредбе"
                :yu          "YУ Наредбе\\Правилници"
                :obavezna    "ЈУС са обавезном примјеном"
                :djelilmicno "ЈУС са дјелимично обавезном примјеном"
                :upotreba    "ЈУС за употребу"
                :rezultat    "Наредбе\\стандарди који садрже резултат претраге"
                :mouse       "За објашњење пређи мишем преко одговарајуће боје"
                :primjer1    "Примјер 1: у групи се налази 25 докумената"
                :primjer2    "Примјер 2: у групи се налази 3 документа"
                :broj        "Број докумената у групи"
                :prikazi     "Прикажи легенду"}
    :doc-view  {:naziv "Назив"
                :vrsta "Врста: "}
    :yu-view   {:yu-name "YУ наредба/правилник"
                :glasnik "Службени гласник: "
                :prikazi "Прикажи докуменt"}
    :bh-view   {:bh-name   "БиХ наредба"
                :glasnik   "Службени гласник: "
                :direktiva "Европска директива: "
                :prikazi   "Прикажи докуменt"}
    :jus-view  {:jus-name   "ЈУС стандард"
                :godina     "Година: "
                :primjena   "Примјена: "
                :obavezna   "Обавезна"
                :djelimicno "Дјелимично обавезна"
                :upotreba   "За употребу"
                :strana     "Број страна: "
                :isc        "ICS: "}
    :doc-type  {:svi "Сви документи"
                :bh  "БиХ наредбе"
                :yu  "YУ наредбе/правилници"
                :jus "ЈУС стандарди"}
    :ac-tip    {:prikazi "Прикази податке о документу"
                :zapamti "Запамти овај документ"
                :veza    "Прикази документе који везују овај документ са изабраним документом"
                :obrisi  "Обриши критериј за претрагу"}
    :ac-button {:otvori  "Отвори"
                :zapamti "Запамти"
                :veze    "Везе"
                :nova    "Нова претрага"}
    :tooltip   {:brisi-vezu     "Бриши везу"
                :prikazi-vezu   "Прикажи везу"
                :brisi-istoriju "Бриши из запамћених"
                :pdf-listing    "PDF листинг"}
    :search    {:title     "Претрага података о наредбама/правилницима/стандардима"
                :vezuju    "Претрага докумената који везују"
                :vezani    "Претрага везаних докумената"
                :zapamceni "Претрага запамћених документа"
                :brisi     "Бриши претрагу"}
    :table     {:veze      "Прикажи везе ("
                :zaboravi  "Заборави"
                :empty     "Нема докумената за приказ"
                :zapamceni "Запамћени документи"}
    :graph     {:otvori      "Отвори"
                :zapamti     "Запамти"
                :zatvori     "Затвори"
                :prikaz-long "Графички приказ веза између хармонизираних наредби и ЈУС стандарда"
                :prikaz      "Графички приказ"}
    :filter    {:bh         "БиХ наредбе"
                :yu         "YУ наредбе"
                :obavezna   "ЈУС стандарди - обавезна примјена"
                :djelimicno "ЈУС стандарди - дјелимично обавезна примјена"
                :upotreba   "ЈУС стандарди - за употребу"}
    :tabs      {:vezani    "Везани документи:"
                :vezan-za  "Везан за документе:"
                :zapamceni "Запамћени документи"}
    :veze      {:veza-sa  "Веза са:"
                :pretraga "Претрага запамћених документа"}
    :pdf       {:bh          "БиХ Наредбе"
                :yu          "YУ Наредбе\\Правилници"
                :obavezna    "ЈУС са обавезном примјеном"
                :djelimicno "ЈУС са дјелимично обавезном примјеном"
                :upotreba    "ЈУС за употребу"
                :strana "Страна "
                :od " од "
                :impressum "eJUS - Институт за стандардизацију БиХ, Војводе Р. Путника 34, 71123 Источно Сарајево, Босна и Херцеговина, Тел. +387 (0)57 310 560\n"
                :vezani "Приказ везаних докумената ("
                :vezan-za "Приказ докумената за које је везан ("
                :zapamceni "Приказ запамћених докумената ("
                :tip-bh "БХ наредба"
                :tip-yu "YU naredba/pravilnik"
                :tip-jus  "ЈУС стандард"}}
   :hr {:missing   "hr missing text"
        :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
        :ac-label  "Naziv"
        :ac-hint   "Unesi dio teksta iz naslova"
        :badges    {:broj    "Broj dokumenata u grupi"
                    :zapamti "Zapamti"
                    :veze    "Veze"
                    :nova    "Nova pretraga"
                    :otvori  "Otvori"}
        :legend    {:bh          "BiH Naredbe"
                    :yu          "YU Naredbe\\Pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelilmicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :rezultat    "Naredbe\\standardi koji sadrže rezultat pretrage"
                    :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                    :primjer1    "Primjer 1: u grupi se nalazi 25 dokumenata"
                    :primjer2    "Primjer 2: u grupi se nalazi 3 dokumenta"
                    :broj        "Broj dokumenata u grupi"
                    :prikazi     "Prikaži legendu"}
        :doc-view  {:naziv "Naziv"
                    :vrsta "Vrsta: "}
        :yu-view   {:yu-name "YU naredba/pravilnik"
                    :glasnik "Službeni glasnik: "
                    :prikazi "Prikaži dokument"}
        :bh-view   {:bh-name   "BiH naredba"
                    :glasnik   "Službeni glasnik: "
                    :direktiva "Evropska direktiva: "
                    :prikazi   "Prikaži dokument"}
        :jus-view  {:jus-name   "JUS standard"
                    :godina     "Godina: "
                    :primjena   "Primjena: "
                    :obavezna   "Obavezna"
                    :djelimicno "Djelimično obavezna"
                    :upotreba   "Za upotrebu"
                    :strana     "Broj strana: "
                    :isc        "ICS: "}
        :doc-type  {:svi "Svi dokumenti"
                    :bh  "BiH naredbe"
                    :yu  "YU naredbe/pravilnici"
                    :jus "JUS standardi"}
        :ac-tip    {:prikazi "Prikazi podatke o dokumentu"
                    :zapamti "Zapamti ovaj dokument"
                    :veza    "Prikazi dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
                    :obrisi  "Obriši kriterij za pretragu"}
        :ac-button {:otvori  "Otvori"
                    :zapamti "Zapamti"
                    :veze    "Veze"
                    :nova    "Nova pretraga"}
        :tooltip   {:brisi-vezu     "Briši vezu"
                    :prikazi-vezu   "Prikaži vezu"
                    :brisi-istoriju "Briši iz zapamćenih"
                    :pdf-listing    "PDF listing"}
        :search    {:title     "Pretraga podataka o naredbama/pravilnicima/standardima"
                    :vezuju    "Pretraga dokumenata koji vezuju"
                    :vezani    "Pretraga vezanih dokumenata"
                    :zapamceni "Pretraga zapamćenih dokumenta"
                    :brisi     "Briši pretragu"}
        :table     {:veze      "Prikaži veze ("
                    :zaboravi  "Zaboravi"
                    :empty     "Nema dokumenata za prikaz"
                    :zapamceni "Zapamćeni dokumenti"}
        :graph     {:otvori      "Otvori"
                    :zapamti     "Zapamti"
                    :zatvori     "Zatvori"
                    :prikaz-long "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
                    :prikaz      "Grafički prikaz"}
        :filter    {:bh         "BiH naredbe"
                    :yu         "YU naredbe"
                    :obavezna   "JUS standardi - obavezna primjena"
                    :djelimicno "JUS standardi - djelimično obavezna primjena"
                    :upotreba   "JUS standardi - za upotrebu"}
        :tabs      {:vezani    "Vezani dokumenti:"
                    :vezan-za  "Vezan za dokumente:"
                    :zapamceni "Zapamćeni dokumenti"}
        :veze      {:veza-sa  "Veza sa:"
                    :pretraga "Pretraga zapamćenih dokumenta"}
        :pdf       {:bh          "BiH Naredbe"
                    :yu          "YU Naredbe\\Pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :strana "Strana "
                    :od " od "
                    :impressum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                    :vezani "Prikaz vezanih dokumenata ("
                    :vezan-za "Prikaz dokumenata za koje je vezan ("
                    :zapamceni "Prikaz zapamćenih dokumenata ("
                    :tip-bh "BH naredba"
                    :tip-yu "YU naredba/pravilnik"
                    :tip-jus  "JUS standard"}}
   :en {:missing   "en missing text"
        :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
        :ac-label  "Title"
        :ac-hint   "Unesi dio teksta iz naslova"
        :badges    {:broj    "Broj dokumenata u grupi"
                    :zapamti "Save"
                    :veze    "Links"
                    :nova    "New search"
                    :otvori  "Open"}
        :legend    {:bh          "BiH Naredbe"
                    :yu          "YU Naredbe\\Pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelilmicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :rezultat    "Naredbe\\standardi koji sadrže rezultat pretrage"
                    :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                    :primjer1    "Primjer 1: u grupi se nalazi 25 dokumenata"
                    :primjer2    "Primjer 2: u grupi se nalazi 3 dokumenta"
                    :broj        "Documents in group"
                    :prikazi     "Show legend"}
        :doc-view  {:naziv "Title"
                    :vrsta "Type: "}
        :yu-view   {:yu-name "YU naredba/pravilnik"
                    :glasnik "Službeni glasnik: "
                    :prikazi "Show document"}
        :bh-view   {:bh-name   "BiH naredba"
                    :glasnik   "Službeni glasnik: "
                    :direktiva "Evropska direktiva: "
                    :prikazi   "Show document"}
        :jus-view  {:jus-name   "JUS standard"
                    :godina     "Year: "
                    :primjena   "Primjena: "
                    :obavezna   "Obavezna"
                    :djelimicno "Djelimično obavezna"
                    :upotreba   "Za upotrebu"
                    :strana     "Pages: "
                    :isc        "ICS: "}
        :doc-type  {:svi "All documents"
                    :bh  "BiH naredbe"
                    :yu  "YU naredbe/pravilnici"
                    :jus "JUS standardi"}
        :ac-tip    {:prikazi "Show document data"
                    :zapamti "Save this document"
                    :veza    "Prikazi dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
                    :obrisi  "Clear search criteria"}
        :ac-button {:otvori  "Open"
                    :zapamti "Save"
                    :veze    "Links"
                    :nova    "New search"}
        :tooltip   {:brisi-vezu     "Delete link"
                    :prikazi-vezu   "Show link"
                    :brisi-istoriju "Delete form saved"
                    :pdf-listing    "PDF listing"}
        :search    {:title     "Pretraga podataka o naredbama/pravilnicima/standardima"
                    :vezuju    "Pretraga dokumenata koji vezuju"
                    :vezani    "Pretraga vezanih dokumenata"
                    :zapamceni "Pretraga zapamćenih dokumenta"
                    :brisi     "Delete seacrh"}
        :table     {:veze      "Show links ("
                    :zaboravi  "Forget"
                    :empty     "No documents for display"
                    :zapamceni "Saved documents"}
        :graph     {:otvori      "Open"
                    :zapamti     "Save"
                    :zatvori     "Close"
                    :prikaz-long "Grafički prikaz veza između harmoniziranih naredbi i JUS standarda"
                    :prikaz      "Graphic presentation"}
        :filter    {:bh         "BiH naredbe"
                    :yu         "YU naredbe"
                    :obavezna   "JUS standardi - obavezna primjena"
                    :djelimicno "JUS standardi - djelimično obavezna primjena"
                    :upotreba   "JUS standardi - za upotrebu"}
        :tabs      {:vezani    "Linked documents:"
                    :vezan-za  "Linked to documents:"
                    :zapamceni "Saved documents"}
        :veze      {:veza-sa  "Linked with:"
                    :pretraga "Search saved documents"}
        :pdf       {:bh          "BiH Naredbe"
                    :yu          "YU Naredbe\\Pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :strana "Page "
                    :od " of "
                    :impressum "eJUS - Institut for stadardization B&H, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                    :vezani "Prikaz vezanih dokumenata ("
                    :vezan-za "Prikaz dokumenata za koje je vezan ("
                    :zapamceni "Show saved documents ("
                    :tip-bh "BH naredba"
                    :tip-yu "YU naredba/pravilnik"
                    :tip-jus  "JUS standard"}}})


(def opts {:dict dictionary})
(defn tr [data] (tempura/tr opts [@lang] data))
