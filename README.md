# AI Code Reviewer

Paste code, get an AI review: a score, a summary, and a list of issues (bugs, security, performance, readability) with suggested fixes. Spring Boot backend calls OpenRouter; React frontend shows the results.

## Structure

```
ai-code-reviewer/
  backend/    Spring Boot 3 (Java 17), REST API at /api/review
  frontend/   React + Vite UI
```

## Run it

You need Java 17+, Maven, Node 18+ and an OpenRouter API key (https://openrouter.ai/keys).

**1. Backend**

```bash
cd backend
mvn spring-boot:run
```

Before starting the backend, copy `backend/.env.example` to `backend/.env` and put your OpenRouter key there:

```dotenv
OPENROUTER_API_KEY=your-openrouter-key
```

The real `.env` file is ignored by Git. Do not commit or share it.

Check it: http://localhost:8080/api/health

**2. Frontend** (new terminal)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, click "Load sample", then "Review code".

## API

`POST /api/review`

```json
{ "code": "public class A { ... }", "language": "Java" }
```

Response:

```json
{
  "summary": "...",
  "score": 4,
  "issues": [
    { "severity": "high", "line": 5, "title": "SQL injection", "explanation": "...", "suggestion": "..." }
  ]
}
```

## How it works

1. The frontend sends the code to `ReviewController`.
2. `ReviewService` prefixes each line with its number, then calls OpenRouter's chat completions API with a system prompt that asks for a strict JSON review.
3. The JSON is parsed into `ReviewResponse` and returned to the UI.

The OpenRouter model is set in `backend/src/main/resources/application.properties` (`openrouter.model`).

## Ideas to extend it

- Review a GitHub repo or pull request (fetch the diff with the GitHub API)
- Save past reviews in PostgreSQL and show a history page
- Stream the response so results appear as they are generated
- Add login (Spring Security + JWT) and per-user rate limits
- Dockerize both apps with a docker-compose file
