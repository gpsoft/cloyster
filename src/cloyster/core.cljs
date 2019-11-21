(ns cloyster.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [re-com.core :as rc]))

;; re-frame Events
(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:form {:name "いかだ8号"
            :lat "34.3122"
            :lng "132.3120"}}))

(rf/reg-event-db
  :form-change
  (fn [db [_ nm new-val]]
    (assoc-in db [:form nm] new-val)))

;; re-frame subscriptions
(rf/reg-sub
  :form
  (fn [db _] (:form db)))

;; re-frame views
(defn add-oyster
  [{:keys [name lat lng]}]
  (let [lat (double lat)
        lng (double lng)]
    (-> js/L
        (.marker #js [lat lng])
        (.addTo js/map)
        (.bindPopup (str name "[" lat ", " lng "]")))
    nil))

(defn parse-csv [csv-text] (->> (clojure.string/split csv-text #"[\r\n]+")
       (map #(clojure.string/split % #","))
       (map (fn [[a b c]] {:name a :lat (double b) :lng (double c)}))))

(defn add-oysters
  []
  (when-let [fil (aget (.getElementById js/document "csv") "files" 0)]
    (let [fr (js/FileReader.)]
      (aset fr "onload" #(dorun (map add-oyster (parse-csv (aget % "target" "result")))))
      (.readAsText fr fil))))

(defn base64encode
  [s]
  ;; Not sure it's correct.
  (-> s
      (js/encodeURIComponent)
      (js/unescape)
      (js/btoa)))

(defn backup!
  []
  (let [data (clojure.string/join "\r\n"
                                  [ "いかだ1号,34.308,132.311"
                                   "いかだ2号,34.3085,132.312"
                                   "いかだ3号,34.3082,132.313"])
        data (base64encode data)
        href (str "data:text/csv;charset=utf-8;base64," data)
        a (.createElement js/document "a")]
    (.setAttribute a "download" "data.csv")
    (.setAttribute a "href" href)
    (.appendChild (.getElementById js/document "app") a)
    (.click a)
    (.remove a)))

(defn form
  []
  (let [{:keys [name lat lng] :as inputs} @(rf/subscribe [:form])]
    [:div.form
     [rc/h-box
      :align :center
      :children [[rc/label :label "いかだ名" :class "input-label"]
                 [rc/input-text
                  :model name
                  :on-change #(rf/dispatch [:form-change :name %])]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "緯度" :class "input-label"]
                 [rc/input-text
                  :model (str lat)
                  :on-change #(rf/dispatch [:form-change :lat %])]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "経度" :class "input-label"]
                 [rc/input-text
                  :model (str lng)
                  :on-change #(rf/dispatch [:form-change :lng %])]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "" :class "input-label"]
                 [rc/button
                  :label "追加"
                  :class "btn-danger"
                  :on-click #(add-oyster inputs)]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "CSV" :class "input-label"]
                 [:input#csv {:type "file"}]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "" :class "input-label"]
                 [rc/button
                  :label "アップロード"
                  :class "btn-danger"
                  :on-click add-oysters]]]
     [rc/h-box
      :align :center
      :children [[rc/label :label "" :class "input-label"]
                 [rc/button
                  :label "ダウンロード"
                  :class "btn-danger"
                  :on-click backup!]]]
     ]))

(defn ui
  []
  [:div.content
   [:div.title "今日のカキいかだ"]
   [#'form]
   ])


;; app
(defn render
  []
  (reagent/render [ui]
                  (.getElementById js/document "app")))

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (render))

(run)

(comment
  (js/alert "Hey")
  (run)
  (rf/dispatch [:time-change (js/Date. "1995-12-17T12:24:00")])
  )
