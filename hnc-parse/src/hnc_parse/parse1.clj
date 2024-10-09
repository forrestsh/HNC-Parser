(ns hnc-parse.parse1
  (:require [clojure.set :as set])
;;  (:require [graph :as dg])
)

;; Import the directed graph functions
;; Assuming the directed graph functions are in the 'mygraph' namespace
;; (require '[hnc-parse.graph :as dg])
(require '[hnc-parse.graph :as dg])

;; Define a function to create an initial graph from input vector 'v'
(defn create-graph [v]
  (let [N (count v)
        ;; Initialize the graph with nodes from 0 to N
        graph (reduce (fn [g i]
                        (dg/dg-add-edge g i (inc i) (v i)))
                      (dg/dg-init)
                      (range N))]
    graph))

;; Create an empty graph (without any edges)
(defn create-empty-graph [v]
  (let [N (count v)
        ;; Initialize the graph with nodes from 0 to N
        nodes (into {} (map (fn [i] [i {:next #{}, :prev #{}}]) (range (inc N))))]
    {:nodes nodes, :edges {}}))

;; Create a queue from the input vector 'v'
(defn create-queue
  ([]
   clojure.lang.PersistentQueue/EMPTY)
  ([v]
   (reduce conj clojure.lang.PersistentQueue/EMPTY
           (map-indexed (fn [i e] {:begin i :end (inc i) :value e}) v))))

;; Word table for matching words
(def word-table ["Hello" "World"])

;; Function to match words from a sequence of characters
(defn match-words [chars word-table]
  (let [char-str (apply str chars)
        matches  (filter #(= char-str %) word-table)]
    (if (empty? matches)
      chars
      matches)))

(def words-set 
  #{
    "hello" "world" "in" "the" "beginning" "god" "created" "heavens" "and" "earth"
    }
)

(defn find-word-keyword [input-string]
  (let [lower-input (clojure.string/lower-case input-string)]
    (if (some #(= (clojure.string/lower-case %) lower-input) words-set)
      (keyword lower-input)
      nil)))

;; Example usage:
(find-word-keyword "Hello")
;; => :hello

(find-word-keyword "HELLO")
;; => :hello

(find-word-keyword  "hi")
;; => nil

(defn invoke1 [item]
  (case item
    "Hello" [:Hello]
    "World" [:World]
    \space [:Spaces]
    \! [:Symbol-E]
    []
    )
)

(defn invoke [item]
  (case item
    \space [:Spaces]
    \. [:End-Punc]
    [:in :the :beginning] ['(BEGIN)]
    [:the :heavens] ['(HEAVEN {:number "Plural" :spec "Definite"})]
    [:the :earth] ['(EARTH)]
    [(HEAVEN {:number "Plural" :spec "Definite"}) :and (EARTH)] ['(AND '(HEAVEN {:number "Plural" :spec "Definite"}) '(EARTH))]
    [:god :created (AND '(HEAVEN {:number "Plural" :spec "Definite"}) '(EARTH))] ['(CREATE '(GOD) '(AND '(HEAVEN {:number "Plural" :spec "Definite"}) '(EARTH)) {:tense "Past"})]
    [(BEGIN) (CREATE '(GOD) '(AND '(HEAVEN {:number "Plural" :spec "Definite"}) '(EARTH)) {:tense "Past"}) :End-Punc] ['(CREATE '(GOD) '(AND '(HEAVEN {:number "Plural" :spec "Definite"}) '(EARTH)) {:tense "Past" :time '(BEGIN)})]
  (let [matched-word (find-word-keyword item)]
    (if (nil? matched-word) 
      []
      [matched-word])
    )))

 ;; Combine function to concatenate two values
(defn combine1 [s item]
  (if (and (string? s) (string? item)) (str s item) nil)
)


(defn combine [s item]
  (cond
      (and (= s \space) (string? item)) nil
      (and (string? s) (= item \space)) nil
      (and (= s \space) (char? item)) nil
      (and (char? s) (= item \space)) nil
      (and (string? s) (string? item)) (str s item)
      (and (char? s) (char? item)) (str s item)
      (and (string? s) (char? item)) (str s item)
      (and (char? s) (not (string? item))) nil
      (and (char? s) (string? item)) (str s item) 
      (and (char? s) (not (string? item))) nil 
      (and (= s :Hello) (= item :Spaces)) (vec [s item])
      (and (keyword? s) (= item :Spaces)) s
      (and (keyword? s) (keyword? item)) (vec [s item])
      :else nil
  )
)

(defn combine [s item]
  (cond
    (and (keyword? s) (= item :Spaces)) s
    (and (keyword? s) (keyword? item)) (vec [s item])
    (and (list? s) (list? item)) (vec [s item])
    (and (list? s) (keyword? item)) (vec [s item])
    (and (keyword? s) (list? item)) (vec [s item])
    (and (vector? s) (= (count s) 2) (or (list? item) (keyword? item))) (vec [(s 0) (s 1) item])
    ;;(and (vector? s) (keyword? item)) (vec (flatten (vec [s item])))
    (and (= s \space) (string? item)) nil
    (and (string? s) (= item \space)) nil
    (and (= s \space) (char? item)) nil
    (and (char? s) (= item \space)) nil
    (and (string? s) (string? item)) (str s item)
    (and (char? s) (char? item)) (str s item)
    (and (string? s) (char? item)) (str s item)
    (and (char? s) (string? item)) (str s item)
    (and (= s :Hello) (= item :Spaces)) (vec [s item])
    (and (= s [:Hello :Spaces]) (= item :World)) (flatten (vec [s item]))
    (and (= s [:Hello :Spaces :World]) (= item :Symbol-E)) '(Greeting)
    (and (char? s) (not (string? item))) nil
    :else nil))

(defn combine2 [s item]
  (str s item)
)

;; Parse function using the directed graph
(defn parse [graph queue]
  (if (empty? queue)
    {:graph graph :queue queue}
    (let [item       (peek queue)
          queue-rest (pop queue)
          b          (:begin item)
          e          (:end item)
          v          (:value item)
          ;; Update the graph by adding an edge from 'b' to 'e' with value 'v'
          updated-graph (dg/dg-add-edge graph b e v)
          N          (apply max (keys (:nodes updated-graph)))]

      ;; Define helper functions to process combinations
      (letfn
          [(process-invoke-current [queue]
             (let [new-values (invoke v)]
               ;; (println new-values)
               (reduce
               (fn [q v] (conj q {:begin b :end e :value v}))
               queue
               new-values)               
               )
             
             )
           
           (process-combinations-before [queue]
                (reduce
                  (fn [q1 pos]
                    (reduce
                      (fn [q2 s]
                        (let [sv (combine s v)
                              matches (match-words [sv] word-table)
                              r {:begin pos :end e :value sv}]
                          (if (or  (nil? sv) (dg/dg-add-edge? updated-graph pos e sv))
                            q2
                            (conj q2 r))))
                      q1
                      (dg/get-edge-elements updated-graph pos b)))
                  queue
                  (:prev ((:nodes updated-graph) b))))

              
              (process-combinations-after [queue]
                (reduce
                  (fn [q1 pos]
                    (reduce
                      (fn [q2 s]
                        (let [vs (combine v s)
                              matches (match-words [vs] word-table)
                              r {:begin b :end pos :value vs}]
                          (if (or (nil? vs) (dg/dg-add-edge? updated-graph b pos vs))
                            q2
                            (conj q2 r))))
                      q1
                      (dg/get-edge-elements updated-graph e pos)))
                  queue
                  (:next ((:nodes updated-graph) e))))
              ]

      
        ;; Process combinations before and after
        (let [queue-after-invoke (process-invoke-current queue-rest)
              queue-after-ab (process-combinations-before queue-after-invoke)
              queue-after-ef (process-combinations-after queue-after-ab)]

       ;;     (println updated-graph)
      
          [updated-graph queue-after-ef]
          ;; Recursively call parse with the updated graph and queue
          ;; (parse updated-graph queue-after-ef)
)))))

;; Helper functions to get edge elements (values) between nodes
(defn get-edge-elements [graph i j]
  (get-in graph [:edges [i j]]))

;; Function to check if an edge already exists with a given value
(defn dg-add-edge? [graph i j value]
  (contains? (get-edge-elements graph i j) value))

;; Example usage
(def sentence "In the beginning God created the heavens and the earth.")
;; (def sentence "Hello World!")
(def test-graph (create-empty-graph (vec sentence)))
(def test-queue (create-queue (vec sentence)))

;; Run the parser
(def result (parse test-graph test-queue))

;; Extract the updated graph
(def updated-graph (:graph result))

(loop []
  (do
    ;; Your code block here
    (def result (parse (result 0) (result 1)))
    )
  (when (not (empty? (result 1)))
    (recur)))

;; Display the graph
;; (println updated-graph)

