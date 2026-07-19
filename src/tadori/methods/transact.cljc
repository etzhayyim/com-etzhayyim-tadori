(ns tadori.methods.transact
  "tadori 辿 threat-intel → live kotoba datomic.transact (operator-gated host edge).
  ADR-2605301400 + ADR-2605231525 (no-server-key) + ADR-2605262130.
  Clojure port of the HTTP/CLI leg of `kotoba/ingest_threat_intel.py`.

  This is the ONLY tadori namespace that does network I/O — deliberately split out of the
  pure `tadori.methods.ingest` core so the autonomous self-audit loop stays no-external-I/O
  (G-no-io test) and pywasm-runnable. It is an OPERATOR tool: it transacts an operator-staged
  passive-archive corpus into a running kotoba node and verifies read-back. It holds NO key —
  the credential is supplied at runtime via KOTOBA_SESSION_POP / KOTOBA_TOKEN (the operator's,
  not the platform's). Without a credential it is a DRY RUN (prints the tx_edn summary).

  Live data writes require a `case` anchor (TADORI_CASE_ID / per-record case_id) and reject any
  collection_mode other than operator-staged-passive-archive (G3); vendor-compatible feeds may
  not be system-of-record (G4) — both enforced by tadori.methods.ingest/validate-records.

  Network and codec authority are explicit host dependencies."
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [tadori.methods.ingest :as ingest]))

(def nsid-session-verify "com.etzhayyim.pds.session.verify")
(def nsid-datomic-transact "com.etzhayyim.apps.kotoba.datomic.transact")
(def nsid-datomic-datoms "com.etzhayyim.apps.kotoba.datomic.datoms")

(defn http-post
  "POST JSON through an explicitly granted request capability."
  [request-fn url body token]
  (when-not (fn? request-fn)
    (throw (ex-info "tadori live transact requires an explicit HTTP request capability"
                    {:capability :http-request})))
  (let [headers (cond-> {"Content-Type" "application/json"}
                  token (assoc "Authorization" (str "Bearer " token)))
        resp (request-fn {:method :post :uri url :headers headers
                          :body (json/generate-string body) :throw false :timeout 30000})
        raw (or (not-empty (:body resp)) "{}")]
    [(:status resp) (try (json/parse-string raw true) (catch Exception _ {:error raw}))]))

(defn verify-session [request-fn base-url pop-token]
  (let [[status body] (http-post request-fn (str base-url "/xrpc/" nsid-session-verify) {:token pop-token} nil)]
    [(and (= status 200) (boolean (:valid body))) body]))

(defn transact [request-fn base-url graph tx-edn token]
  (http-post request-fn (str base-url "/xrpc/" nsid-datomic-transact) {:graph graph :tx_edn tx-edn} token))

(defn read-datoms [request-fn base-url graph entity attr token]
  (http-post request-fn (str base-url "/xrpc/" nsid-datomic-datoms)
             {:graph graph :index ":eavt"
              :components_edn [(ingest/edn-string entity) (str ":" attr)] :limit 1}
             token))

(def ^:private default-seed (io/file "wire" "seed.threat-intel.jsonl"))

(defn run-cli
  "Operator CLI. Args: [seed-path] [graph] [url]. Reads KOTOBA_SESSION_POP / KOTOBA_TOKEN +
  TADORI_CASE_ID from the env. No credential ⇒ DRY RUN (validate + print tx_edn summary)."
  [request-fn args]
  (let [env (System/getenv)
        seed (if (first args) (io/file (first args)) default-seed)
        graph (or (second args) (.get env "TADORI_GRAPH") "etzhayyim/tadori/threat-intel")
        url (or (nth args 2 nil) (.get env "KOTOBA_URL") "http://127.0.0.1:8077")
        case-id (.get env "TADORI_CASE_ID")
        token (or (.get env "KOTOBA_SESSION_POP") (.get env "KOTOBA_TOKEN"))
        live (boolean token)
        records (ingest/load-jsonl (slurp seed))]
    (ingest/validate-records records {:allow-tier-d false :live live :case-id case-id})
    (let [datoms (mapcat #(ingest/record->datoms % {:case-id case-id}) records)
          tx-edn (ingest/datoms->tx-edn datoms)]
      (println (format "   parsed %d records → %d datoms from %s" (count records) (count datoms) (str seed)))
      (if-not live
        (do (println "   DRY RUN — no writes. Set KOTOBA_SESSION_POP or KOTOBA_TOKEN to transact.")
            (println (format "   tx[data] count~%d bytes=%d" (count datoms) (count (.getBytes ^String tx-edn "UTF-8")))))
        (do
          (when (.get env "KOTOBA_SESSION_POP")
            (let [[ok info] (verify-session request-fn url (.get env "KOTOBA_SESSION_POP"))]
              (when-not ok
                (binding [*out* *err*] (println (str "!! session PoP rejected: " info)))
                (System/exit 1))
              (println (str "   session valid for " (get info :did "?")))))
          (println (format "--> datomic.transact data count~%d graph=%s" (count datoms) graph))
          (let [[status body] (transact request-fn url graph tx-edn token)]
            (when (not= status 200)
              (binding [*out* *err*] (println (str "!! transact failed: " status " " body)))
              (System/exit 1))
            (println (str "    ok tx_cid=" (get body :tx_cid "?") " datom_count=" (get body :datom_count "?"))))
          (doseq [[entity attr] (ingest/readback-checks records)]
            (let [[status body] (read-datoms request-fn url graph entity attr token)]
              (when (or (not= status 200) (< (long (get body :datom_count 0)) 1))
                (binding [*out* *err*] (println (str "!! readback failed for " entity " " attr ": " status " " body)))
                (System/exit 1))))
          (println (format "    readback ok checks=%d graph=%s" (count (ingest/readback-checks records)) graph)))))))

(defn -main [& args]
  (run-cli nil args))
