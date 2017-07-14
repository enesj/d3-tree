(ns foo.example.translation
  (:require [taoensso.tempura :as tempura]
            [reagent.core :as r :refer [atom]]))

(def lang (atom :bs))

(def dictionary
  {:bs
       {:missing   "bs missing"
        :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
        :ac-label  "Naziv"
        :ac-hint   "Unesi dio teksta iz naslova"
        :badges    {:broj    "Broj dokumenata u grupi"
                    :zapamti "Zapamti"
                    :veze    "Veze"
                    :nova    "Nova pretraga"
                    :otvori  "Otvori"}
        :legend    {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :rezultat    "Naredbe\\standardi koji sadržavaju rezultat pretrage"
                    :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                    :primjer1    "Primjer 1: u grupi se nalazi 25 dokumenata"
                    :primjer2    "Primjer 2: u grupi se nalaze 3 dokumenta"
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
                    :ics        "ICS: "}
        :doc-type  {:svi "Svi dokumenti"
                    :bh  "BiH naredbe"
                    :yu  "YU naredbe/pravilnici"
                    :jus "JUS standardi"}
        :ac-tip    {:prikazi "Prikaži podatke o dokumentu"
                    :zapamti "Zapamti ovaj dokument"
                    :veza    "Prikaži dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
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
                    :zapamceni "Pretraga zapamćenih dokumenata"
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
                    :pretraga "Pretraga zapamćenih dokumenata"}
        :pdf       {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :strana "Strana "
                    :od " od "
                    :impressum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                    :vezani "Prikaz vezanih dokumenata ("
                    :vezan-za "Prikaz dokumenata za koje je vezan ("
                    :zapamceni "Prikaz zapamćenih dokumenata ("
                    :tip-bh "BiH naredba"
                    :tip-yu "YU naredba/pravilnik"
                    :tip-jus  "JUS standard"}}


   :sr
       {:missing   "sr missing"
        :title     "Везе JUS стандарда и хармонизованих БиХ наредби"
        :ac-label  "Назив"
        :ac-hint   "Унеси дио текста из наслова"
        :badges    {:broj    "Број докумената у групи"
                    :zapamti "Запамти"
                    :veze    "Везе"
                    :nova    "Нова претрага"
                    :otvori  "Отвори"}
        :legend    {:bh          "БиХ наредбе"
                    :yu          "YU наредбе\\правилници"
                    :obavezna    "JUS са обавезном примјеном"
                    :djelimicno "JUS са дјелимично обавезном примјеном"
                    :upotreba    "JUS за употребу"
                    :rezultat    "Наредбе\\стандарди који садрже резултат претраге"
                    :mouse       "За објашњење пређи мишем преко одговарајуће боје"
                    :primjer1    "Примјер 1: у групи се налази 25 докумената"
                    :primjer2    "Примјер 2: у групи се налазе 3 документа"
                    :broj        "Број докумената у групи"
                    :prikazi     "Прикажи легенду"}
        :doc-view  {:naziv "Назив"
                    :vrsta "Врста: "}
        :yu-view   {:yu-name "YU наредба/правилник"
                    :glasnik "Службени гласник: "
                    :prikazi "Прикажи документ"}
        :bh-view   {:bh-name   "БиХ наредба"
                    :glasnik   "Службени гласник: "
                    :direktiva "Европска директива: "
                    :prikazi   "Прикажи документ"}
        :jus-view  {:jus-name   "JUS стандард"
                    :godina     "Година: "
                    :primjena   "Примјена: "
                    :obavezna   "Обавезна"
                    :djelimicno "Дјелимично обавезна"
                    :upotreba   "За употребу"
                    :strana     "Број страна: "
                    :ics        "ICS: "}
        :doc-type  {:svi "Сви документи"
                    :bh  "БиХ наредбе"
                    :yu  "YU наредбе/правилници"
                    :jus "JUS стандарди"}
        :ac-tip    {:prikazi "Прикажи податке о документу"
                    :zapamti "Запамти овај документ"
                    :veza    "Прикажи документе који везују овај документ са изабраним документом"
                    :obrisi  "Обриши критеријум за претрагу"}
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
                    :zapamceni "Претрага запамћених докумената"
                    :brisi     "Бриши претрагу"}
        :table     {:veze      "Прикажи везе ("
                    :zaboravi  "Заборави"
                    :empty     "Нема докумената за приказ"
                    :zapamceni "Запамћени документи"}
        :graph     {:otvori      "Отвори"
                    :zapamti     "Запамти"
                    :zatvori     "Затвори"
                    :prikaz-long "Графички приказ веза између хармонизованих наредби и JUS стандарда"
                    :prikaz      "Графички приказ"}
        :filter    {:bh         "БиХ наредбе"
                    :yu         "YU наредбе"
                    :obavezna   "JUS стандарди - обавезна примјена"
                    :djelimicno "JUS стандарди - дјелимично обавезна примјена"
                    :upotreba   "JUS стандарди - за употребу"}
        :tabs      {:vezani    "Везани документи:"
                    :vezan-za  "Везан за документе:"
                    :zapamceni "Запамћени документи"}
        :veze      {:veza-sa  "Веза са:"
                    :pretraga "Претрага запамћених докуменaта"}
        :pdf       {:bh          "БиХ наредбе"
                    :yu          "YU наредбе\\правилници"
                    :obavezna    "JUS са обавезном примјеном"
                    :djelimicno "JUS са дјелимично обавезном примјеном"
                    :upotreba    "JUS за употребу"
                    :strana "Страна "
                    :od " од "
                    :impressum "eJUS - Институт за стандардизацију БиХ, Војводе Р. Путника 34, 71123 Источно Сарајево, Босна и Херцеговина, Тел. +387 (0)57 310 560\n"
                    :vezani "Приказ везаних докумената ("
                    :vezan-za "Приказ докумената за које је везан ("
                    :zapamceni "Приказ запамћених докумената ("
                    :tip-bh "БиХ наредба"
                    :tip-yu "YU наредба/правилник"
                    :tip-jus  "JUS стандард"}}

   :hr {:missing   "hr missing"
        :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
        :ac-label  "Naziv"
        :ac-hint   "Unesi dio teksta iz naslova"
        :badges    {:broj    "Broj dokumenata u skupini"
                    :zapamti "Zapamti"
                    :veze    "Veze"
                    :nova    "Nova pretraga"
                    :otvori  "Otvori"}
        :legend    {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obveznom primjenom"
                    :djelilmicno "JUS sa djelomično obveznom primjenom"
                    :upotreba    "JUS za uporabu"
                    :rezultat    "Naredbe\\standardi koji sadržavaju rezultat pretrage"
                    :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                    :primjer1    "Primjer 1: u skupini se nalazi 25 dokumenata"
                    :primjer2    "Primjer 2: u skupini se nalaze 3 dokumenta"
                    :broj        "Broj dokumenata u skupini"
                    :prikazi     "Prikaži legendu"}
        :doc-view  {:naziv "Naziv"
                    :vrsta "Vrsta: "}
        :yu-view   {:yu-name "YU naredba/pravilnik"
                    :glasnik "Službeni glasnik: "
                    :prikazi "Prikaži dokument"}
        :bh-view   {:bh-name   "BiH naredba"
                    :glasnik   "Službeni glasnik: "
                    :direktiva "Europska direktiva: "
                    :prikazi   "Prikaži dokument"}
        :jus-view  {:jus-name   "JUS standard"
                    :godina     "Godina: "
                    :primjena   "Primjena: "
                    :obavezna   "Obvezna"
                    :djelimicno "Djelomično obvezna"
                    :upotreba   "Za uporabu"
                    :strana     "Broj stranica: "
                    :ics        "ICS: "}
        :doc-type  {:svi "Svi dokumenti"
                    :bh  "BiH naredbe"
                    :yu  "YU naredbe/pravilnici"
                    :jus "JUS standardi"}
        :ac-tip    {:prikazi "Prikaži podatke o dokumentu"
                    :zapamti "Zapamti ovaj dokument"
                    :veza    "Prikaži dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
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
                    :zapamceni "Pretraga zapamćenih dokumenata"
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
                    :obavezna   "JUS standardi - obvezna primjena"
                    :djelimicno "JUS standardi - djelomično obvezna primjena"
                    :upotreba   "JUS standardi - za uporabu"}
        :tabs      {:vezani    "Vezani dokumenti:"
                    :vezan-za  "Vezan za dokumente:"
                    :zapamceni "Zapamćeni dokumenti"}
        :veze      {:veza-sa  "Veza sa:"
                    :pretraga "Pretraga zapamćenih dokumenata"}
        :pdf       {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obveznom primjenom"
                    :djelimicno "JUS sa djelomično obveznom primjenom"
                    :upotreba    "JUS za uporabu"
                    :strana "Stranica "
                    :od " od "
                    :impressum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                    :vezani "Prikaz vezanih dokumenata ("
                    :vezan-za "Prikaz dokumenata za koje je vezan ("
                    :zapamceni "Prikaz zapamćenih dokumenata ("
                    :tip-bh "BH naredba"
                    :tip-yu "YU naredba/pravilnik"
                    :tip-jus  "JUS standard"}}

   :en {:missing   "en missing"
        :title     "Veze JUS standarda i harmoniziranih BiH naredbi"
        :ac-label  "Naziv"
        :ac-hint   "Unesi dio teksta iz naslova"
        :badges    {:broj    "Broj dokumenata u grupi"
                    :zapamti "Zapamti"
                    :veze    "Veze"
                    :nova    "Nova pretraga"
                    :otvori  "Otvori"}
        :legend    {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :rezultat    "Naredbe\\standardi koji sadržavaju rezultat pretrage"
                    :mouse       "Za objašnjenje pređi mišem preko odgovarajuće boje"
                    :primjer1    "Primjer 1: u grupi se nalazi 25 dokumenata"
                    :primjer2    "Primjer 2: u grupi se nalaze 3 dokumenta"
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
                    :ics        "ICS: "}
        :doc-type  {:svi "Svi dokumenti"
                    :bh  "BiH naredbe"
                    :yu  "YU naredbe/pravilnici"
                    :jus "JUS standardi"}
        :ac-tip    {:prikazi "Prikaži podatke o dokumentu"
                    :zapamti "Zapamti ovaj dokument"
                    :veza    "Prikaži dokumente koji vezuju ovaj dokument sa izabranim dokumentom"
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
                    :zapamceni "Pretraga zapamćenih dokumenata"
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
                    :pretraga "Pretraga zapamćenih dokumenata"}
        :pdf       {:bh          "BiH naredbe"
                    :yu          "YU naredbe\\pravilnici"
                    :obavezna    "JUS sa obaveznom primjenom"
                    :djelimicno "JUS sa djelimično obaveznom primjenom"
                    :upotreba    "JUS za upotrebu"
                    :strana "Strana "
                    :od " od "
                    :impressum "eJUS - Institut za standardizaciju BiH, Vojvode R. Putnika 34, 71123 Istočno Sarajevo, Bosna i Hercegovina, Tel. +387 (0)57 310 560\n"
                    :vezani "Prikaz vezanih dokumenata ("
                    :vezan-za "Prikaz dokumenata za koje je vezan ("
                    :zapamceni "Prikaz zapamćenih dokumenata ("
                    :tip-bh "BiH naredba"
                    :tip-yu "YU naredba/pravilnik"
                    :tip-jus  "JUS standard"}}})


(def opts {:dict dictionary})
(defn tr [data] (tempura/tr opts [@lang] data))
