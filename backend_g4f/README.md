# g4f (GPT4Free) Backup Backend for AirSpeak

This directory provides the optional, zero-key g4f backend proxy for AirSpeak.

## How It Works
1. **Primary Engine**: The Android app calls **Pollinations.ai** directly (zero keys, zero cost, no server needed).
2. **Backup Engine**: If Pollinations is slow or rate-limited, the app automatically fails over to this **g4f (GPT4Free)** OpenAI-compatible endpoint.
3. **Local Safety Fallback**: If network is disconnected or the backend is offline, the app executes the local linguistic heuristic engine so the user never faces crashes or blanks.

## Quick Run (Local)

```bash
# 1. Install dependencies
pip install -r requirements.txt

# 2. Run the server (default port 1337)
python server.py
```

The server starts on `http://0.0.0.0:1337/v1/chat/completions`.
- In Android Emulator: Connected automatically via `http://10.0.2.2:1337/v1/chat/completions`.
- On Physical Android Device: Connect via your machine's Wi-Fi IP (e.g. `http://192.168.1.X:1337/v1/chat/completions`).

## Deploy for Free (Cloud)
You can deploy this Docker container with 1 click to:
- **Render.com** (Web Service -> Docker)
- **Railway.app** (Deploy from GitHub repo)
- **Hugging Face Spaces** (Docker space)
