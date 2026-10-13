import { useState } from 'react'

const LANGUAGES = ['Java', 'JavaScript', 'TypeScript', 'Python', 'C++', 'Go', 'SQL', 'Other']

const SAMPLE = `public class UserService {
    private Map<String, String> cache = new HashMap<>();

    public User findUser(String id) {
        String query = "SELECT * FROM users WHERE id = '" + id + "'";
        User u = db.run(query);
        cache.put(id, u.getName());
        return u;
    }
}`

function ScoreBadge({ score }) {
  const level = score >= 8 ? 'good' : score >= 5 ? 'ok' : 'bad'
  return (
    <div className={`score score-${level}`}>
      <span className="score-num">{score}</span>
      <span className="score-max">/10</span>
    </div>
  )
}

function IssueCard({ issue }) {
  return (
    <div className={`issue issue-${issue.severity}`}>
      <div className="issue-head">
        <span className="badge">{issue.severity}</span>
        <strong>{issue.title}</strong>
        {issue.line != null && <span className="line">line {issue.line}</span>}
      </div>
      <p>{issue.explanation}</p>
      {issue.suggestion && <pre className="suggestion">{issue.suggestion}</pre>}
    </div>
  )
}

export default function App() {
  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('Java')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)

  async function handleReview() {
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const res = await fetch('/api/review', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, language }),
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.error || 'Something went wrong')
      setResult(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      <header>
        <h1>AI Code Reviewer</h1>
        <p>Paste your code and get bug hints, security checks and refactoring suggestions.</p>
      </header>

      <main>
        <section className="panel">
          <div className="toolbar">
            <select value={language} onChange={(e) => setLanguage(e.target.value)}>
              {LANGUAGES.map((l) => (
                <option key={l}>{l}</option>
              ))}
            </select>
            <button className="link" onClick={() => setCode(SAMPLE)}>
              Load sample
            </button>
          </div>
          <textarea
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Paste your code here..."
            spellCheck={false}
          />
          <button className="primary" onClick={handleReview} disabled={loading || !code.trim()}>
            {loading ? 'Reviewing...' : 'Review code'}
          </button>
          {error && <div className="error">{error}</div>}
        </section>

        <section className="panel">
          {!result && !loading && <p className="empty">Your review will appear here.</p>}
          {loading && <p className="empty">Claude is reading your code...</p>}
          {result && (
            <>
              <div className="summary">
                <ScoreBadge score={result.score} />
                <p>{result.summary}</p>
              </div>
              {result.issues.length === 0 && <p className="empty">No issues found. Nice work!</p>}
              {result.issues.map((issue, i) => (
                <IssueCard key={i} issue={issue} />
              ))}
            </>
          )}
        </section>
      </main>
    </div>
  )
}
