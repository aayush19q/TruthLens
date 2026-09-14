import React, { useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import {
  Check,
  X,
  Search,
  ShieldCheck,
  Clock3,
  ExternalLink,
  AlertCircle,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Sparkles
} from 'lucide-react'
import './styles.css'

const verdictMeta = {
  TRUE: {
    label: 'Likely True',
    icon: CheckCircle2,
    className: 'true'
  },
  FALSE: {
    label: 'Likely False',
    icon: XCircle,
    className: 'false'
  },
  MIXED: {
    label: 'Mixed',
    icon: HelpCircle,
    className: 'mixed'
},
  UNCERTAIN: {
    label: 'Uncertain',
    icon: HelpCircle,
    className: 'uncertain'
  },
  PENDING: {
    label: 'Pending',
    icon: Clock3,
    className: 'pending'
  }
}

function getAllEvidence(claims = []) {
  return claims.flatMap(claim => claim.evidence || [])
}

function evidenceScore(evidence) {
  return (evidence?.similarityScore || 0) * (evidence?.reliabilityScore || 0)
}

function getStrongestEvidence(claims = []) {
  return getAllEvidence(claims).reduce((strongest, evidence) => {
    if (!strongest || evidenceScore(evidence) > evidenceScore(strongest)) {
      return evidence
    }

    return strongest
  }, null)
}

function VerdictBadge({ value }) {
  const meta = verdictMeta[value] || verdictMeta.UNCERTAIN
  const Icon = meta.icon

  return (
    <span className={`badge ${meta.className}`}>
      <Icon size={15} />
      {meta.label}
    </span>
  )
}

function App() {
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [history, setHistory] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [active, setActive] = useState('verify')

  const loadHistory = async () => {
    try {
      const res = await fetch('/api/verifications')

      if (res.ok) {
        setHistory(await res.json())
      }
    } catch {
      // backend may be offline
    }
  }

  useEffect(() => {
    loadHistory()
  }, [])

  const verify = async () => {
    if (!text.trim()) return

    setLoading(true)
    setError('')
    setResult(null)

    try {
      const res = await fetch('/api/verifications', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          originalText: text.trim()
        })
      })

      const data = await res.json()

      if (!res.ok) {
        throw new Error(data.message || 'Verification failed')
      }

      setResult(data)
      setActive('verify')
      loadHistory()

    } catch (e) {
      setError(
        e.message || 'Could not connect to the backend.'
      )
    } finally {
      setLoading(false)
    }
  }

  const showHistoryItem = (item) => {
    setResult(item)
    setActive('verify')
  }

  /*
   * Calculate overall confidence from all analyzed claims.
   *
   * Example:
   * Claim 1 = 100%
   * Claim 2 = 0%
   *
   * Overall = (100 + 0) / 2 = 50%
  */
  const calculateOverallConfidence = () => {
    if (typeof result?.confidence === 'number') {
      return result.confidence
    }

    if (!result?.claims || result.claims.length === 0) {
      return 0
    }

    const total = result.claims.reduce(
      (sum, claim) => sum + (claim.confidence || 0),
      0
    )

    return total / result.claims.length
  }

  const overallConfidence = calculateOverallConfidence()
  const strongestEvidence = result
    ? getStrongestEvidence(result.claims || [])
    : null

  return (
    <div className="app">

      {/* ================= HEADER ================= */}

      <header className="topbar">

        <div className="brand">

          <div className="logo">
            <ShieldCheck size={22} />
          </div>

          <div>
            <strong>TruthLens</strong>
            <small>Evidence before belief.</small>
          </div>

        </div>

        <nav>

          <button
            className={
              active === 'verify'
                ? 'nav active'
                : 'nav'
            }
            onClick={() => setActive('verify')}
          >
            Verify
          </button>

          <button
            className={
              active === 'history'
                ? 'nav active'
                : 'nav'
            }
            onClick={() => setActive('history')}
          >
            History
          </button>

        </nav>

      </header>


      {/* ================= MAIN ================= */}

      <main className="container">

        {/* ================= HISTORY PAGE ================= */}

        {active === 'history' ? (

          <section className="history-page">

            <div className="page-title">

              <div>

                <span className="eyebrow">
                  AUDIT TRAIL
                </span>

                <h1>
                  Verification history
                </h1>

                <p>
                  Previously checked claims saved in MongoDB.
                </p>

              </div>

            </div>


            {history.length === 0 ? (

              <div className="empty">

                <Clock3 size={30} />

                <h3>
                  No verifications yet
                </h3>

                <p>
                  Run your first verification to see it here.
                </p>

              </div>

            ) : (

              <div className="history-list">

                {history
                  .slice()
                  .reverse()
                  .map(item => (

                    <button
                      className="history-item"
                      key={item.id}
                      onClick={() => showHistoryItem(item)}
                    >

                      <div>

                        <strong>
                          {
                            item.originalText?.slice(0, 110)
                            || 'Untitled verification'
                          }
                        </strong>

                        <small>
                          {
                            item.createdAt
                              ? new Date(
                                  item.createdAt
                                ).toLocaleString()
                              : ''
                          }
                        </small>

                      </div>

                      <VerdictBadge
                        value={item.status}
                      />

                    </button>

                  ))}

              </div>

            )}

          </section>

        ) : (

          <>
            {/* ================= VERIFY PAGE ================= */}

            <section className="hero">

              <div className="hero-copy">

                <span className="eyebrow">
                  <Sparkles size={14} />
                  AI-ASSISTED FACT CHECKING
                </span>

                <h1>
                  Check a claim.
                  <br />
                  <em>See the evidence.</em>
                </h1>

                <p>
                  TruthLens extracts claims, searches the web
                  for evidence, evaluates source reliability,
                  and produces an explainable verdict.
                </p>

              </div>


              <div className="input-card">

                <label>
                  Paste a news story or claim
                </label>

                <textarea
                  value={text}
                  onChange={e => setText(e.target.value)}
                  placeholder="Example: India's GDP growth was 20% in 2025. The government reduced fuel prices by 20%."
                />

                <div className="input-footer">

                  <span>
                    {text.length} characters
                  </span>

                  <button
                    className="primary"
                    onClick={verify}
                    disabled={
                      loading ||
                      !text.trim()
                    }
                  >

                    <Search size={17} />

                    {
                      loading
                        ? 'Verifying…'
                        : 'Verify now'
                    }

                  </button>

                </div>

              </div>

            </section>


            {/* ================= ERROR ================= */}

            {error && (

              <div className="error">

                <AlertCircle size={18} />

                <span>
                  {error}
                </span>

              </div>

            )}


            {/* ================= RESULTS ================= */}

            {result && (

              <section className="results">

                <div className="result-header">

                  <div>

                    <span className="eyebrow">
                      VERIFICATION RESULT
                    </span>

                    <h2>
                      Assessment
                    </h2>

                  </div>

                  <VerdictBadge
                    value={result.status}
                  />

                </div>


                {/* ================= SCORE GRID ================= */}

                <div className="score-grid">

                  {/* OVERALL CONFIDENCE */}

                  <div className="score-card">

                    <small>
                      OVERALL CONFIDENCE
                    </small>

                    <strong>
                      {Math.round(
                        overallConfidence * 100
                      )}
                      %
                    </strong>

                    <div className="meter">

                      <i
                        style={{
                          width: `${Math.min(
                            100,
                            overallConfidence * 100
                          )}%`
                        }}
                      />

                    </div>

                  </div>


                  {/* CLAIMS ANALYZED */}

                  <div className="score-card">

                    <small>
                      CLAIMS ANALYZED
                    </small>

                    <strong>
                      {result.claims?.length || 0}
                    </strong>

                    <span>
                      Each claim was checked separately.
                    </span>

                  </div>


                  {/* EVIDENCE SOURCES */}

                  <div className="score-card">

                    <small>
                      EVIDENCE SOURCES
                    </small>

                    <strong>
                      {
                        result.claims?.reduce(
                          (n, c) =>
                            n +
                            (c.evidence?.length || 0),
                          0
                        ) || 0
                      }
                    </strong>

                    <span>
                      Search results evaluated.
                    </span>

                  </div>

                </div>

                {result.originalText && (
                  <div className="original-text-card">

                    <span className="eyebrow">
                      ORIGINAL TEXT
                    </span>

                    <p>
                      {result.originalText}
                    </p>

                  </div>
                )}

{result.explanation && (
  <div className="explanation-card">

    <div className="explanation-icon">
      <ShieldCheck size={20} />
    </div>

    <div className="explanation-content">

      <span className="eyebrow">
        WHY THIS VERDICT?
      </span>

      <h3>Evidence-based explanation</h3>

      <p className="explanation-text">
        {result.explanation}
      </p>

      {strongestEvidence && (
        <div className="evidence-breakdown">

          <div className="breakdown-title">
            Evidence breakdown
          </div>

          <div className="evidence-stats">

            <div className="evidence-stat">
              <strong>
                {Math.round(
                  (strongestEvidence.similarityScore || 0) * 100
                )}%
              </strong>

              <span>Semantic match</span>
            </div>

            <div className="evidence-stat">
              <strong>
                {Math.round(
                  (strongestEvidence.reliabilityScore || 0) * 100
                )}%
              </strong>

              <span>Source reliability</span>
            </div>

          </div>

          <div className="strongest-evidence">

            <div className="strongest-evidence-header">
              <span>Strongest evidence</span>

              <span
                className={`relationship-badge ${
                  strongestEvidence.relationship?.toLowerCase()
                }`}
              >
                {strongestEvidence.relationship}
              </span>
            </div>

            <h4>
              {strongestEvidence.sourceName}
            </h4>

            <p>
              {strongestEvidence.content}
            </p>

          </div>

        </div>
      )}

    </div>
  </div>
)}

                {/* ================= CLAIM ANALYSIS ================= */}

                <div className="claims">

                  <h3>
                    Claim-by-claim analysis
                  </h3>


                  {(result.claims || []).map(
                    (claim, i) => (

                      <article
                        className="claim-card"
                        key={claim.id || i}
                      >

                        {/* CLAIM HEADER */}

                        <div className="claim-top">

                          <span className="claim-number">
                            {String(i + 1).padStart(2, '0')}
                          </span>

                          <div className="claim-content">

                            <p className="claim-text">
                              {claim.text}
                            </p>

                            <div className="claim-meta">

                              <VerdictBadge
                                value={claim.verdict}
                              />

                              <span>
                                Confidence{' '}
                                {Math.round(
                                  (claim.confidence || 0) *
                                  100
                                )}
                                %
                              </span>

                            </div>

                          </div>

                        </div>


                        {/* ================= EVIDENCE ================= */}

                        <div className="evidence-list">

{/* ================= SUPPORTING EVIDENCE ================= */}

{(claim.evidence || []).some(
  (e) => e.relationship === "SUPPORTS"
) && (
  <div className="evidence-group">

    <div className="evidence-group-header supports-header">
      <span className="evidence-group-dot"></span>
      <span>Supporting Evidence</span>

      <span className="evidence-group-count">
        {
          (claim.evidence || []).filter(
            (e) => e.relationship === "SUPPORTS"
          ).length
        }
      </span>
    </div>

    {(claim.evidence || [])
      .filter((e) => e.relationship === "SUPPORTS")
      .map((e, j) => (

        <div
          className="evidence"
          key={`supports-${e.url || j}`}
        >

<div
  className={`evidence-icon ${e.relationship?.toLowerCase()}`}
>
  {e.relationship === "SUPPORTS" && (
    <Check size={17} />
  )}

  {e.relationship === "CONTRADICTS" && (
    <X size={17} />
  )}

  {e.relationship === "UNCLEAR" && (
    <HelpCircle size={17} />
  )}
</div>

          <div className="evidence-main">

            <a
              href={e.url}
              target="_blank"
              rel="noreferrer"
            >
              {e.title ||
                e.sourceName ||
                "Evidence source"}

              <ExternalLink size={13} />
            </a>

            <div className="source">
              {e.sourceName}
              {" · "}
              {e.relationship}
            </div>

            <p>
              {e.content}
            </p>

            <div className="evidence-scores">

              <span>
                Similarity{" "}
                <b>
                  {Math.round(
                    (e.similarityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

              <span>
                Reliability{" "}
                <b>
                  {Math.round(
                    (e.reliabilityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

            </div>

          </div>

        </div>

      ))}
  </div>
)}


{/* ================= CONTRADICTING EVIDENCE ================= */}

{(claim.evidence || []).some(
  (e) => e.relationship === "CONTRADICTS"
) && (
  <div className="evidence-group">

    <div className="evidence-group-header contradicts-header">
      <span className="evidence-group-dot"></span>
      <span>Contradicting Evidence</span>

      <span className="evidence-group-count">
        {
          (claim.evidence || []).filter(
            (e) => e.relationship === "CONTRADICTS"
          ).length
        }
      </span>
    </div>

    {(claim.evidence || [])
      .filter((e) => e.relationship === "CONTRADICTS")
      .map((e, j) => (

        <div
          className="evidence"
          key={`contradicts-${e.url || j}`}
        >

<div
  className={`evidence-icon ${e.relationship?.toLowerCase()}`}
>
  {e.relationship === "SUPPORTS" && (
    <Check size={17} />
  )}

  {e.relationship === "CONTRADICTS" && (
    <X size={17} />
  )}

  {e.relationship === "UNCLEAR" && (
    <HelpCircle size={17} />
  )}
</div>

          <div className="evidence-main">

            <a
              href={e.url}
              target="_blank"
              rel="noreferrer"
            >
              {e.title ||
                e.sourceName ||
                "Evidence source"}

              <ExternalLink size={13} />
            </a>

            <div className="source">
              {e.sourceName}
              {" · "}
              {e.relationship}
            </div>

            <p>
              {e.content}
            </p>

            <div className="evidence-scores">

              <span>
                Similarity{" "}
                <b>
                  {Math.round(
                    (e.similarityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

              <span>
                Reliability{" "}
                <b>
                  {Math.round(
                    (e.reliabilityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

            </div>

          </div>

        </div>

      ))}
  </div>
)}


{/* ================= UNCLEAR EVIDENCE ================= */}

{(claim.evidence || []).some(
  (e) => e.relationship === "UNCLEAR"
) && (
  <div className="evidence-group">

    <div className="evidence-group-header unclear-header">
      <span className="evidence-group-dot"></span>
      <span>Unclear Evidence</span>

      <span className="evidence-group-count">
        {
          (claim.evidence || []).filter(
            (e) => e.relationship === "UNCLEAR"
          ).length
        }
      </span>
    </div>

    {(claim.evidence || [])
      .filter((e) => e.relationship === "UNCLEAR")
      .map((e, j) => (

        <div
          className="evidence"
          key={`unclear-${e.url || j}`}
        >

<div
  className={`evidence-icon ${e.relationship?.toLowerCase()}`}
>
  {e.relationship === "SUPPORTS" && (
    <Check size={17} />
  )}

  {e.relationship === "CONTRADICTS" && (
    <X size={17} />
  )}

  {e.relationship === "UNCLEAR" && (
    <HelpCircle size={17} />
  )}
</div>

          <div className="evidence-main">

            <a
              href={e.url}
              target="_blank"
              rel="noreferrer"
            >
              {e.title ||
                e.sourceName ||
                "Evidence source"}

              <ExternalLink size={13} />
            </a>

            <div className="source">
              {e.sourceName}
              {" · "}
              {e.relationship}
            </div>

            <p>
              {e.content}
            </p>

            <div className="evidence-scores">

              <span>
                Similarity{" "}
                <b>
                  {Math.round(
                    (e.similarityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

              <span>
                Reliability{" "}
                <b>
                  {Math.round(
                    (e.reliabilityScore || 0) * 100
                  )}
                  %
                </b>
              </span>

            </div>

          </div>

        </div>

      ))}
  </div>
)}


                          {/* NO EVIDENCE */}

                          {(
                            !claim.evidence ||
                            claim.evidence.length === 0
                          ) && (

                            <div className="no-evidence">

                              No usable search evidence
                              was returned for this claim.

                            </div>

                          )}

                        </div>

                      </article>

                    )
                  )}

                </div>

              </section>

            )}

          </>

        )}

      </main>


      {/* ================= FOOTER ================= */}

      <footer>
        TruthLens · Evidence-based verification prototype
      </footer>

    </div>
  )
}


createRoot(
  document.getElementById('root')
).render(
  <App />
)
