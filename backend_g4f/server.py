#!/usr/bin/env python3
"""
g4f (GPT4Free) Backup Backend Server for AirSpeak
Exposes an OpenAI-compatible REST API at http://0.0.0.0:1337/v1/chat/completions
Can be deployed to Render, Railway, HuggingFace, or run locally.
"""
import os
import sys

try:
    from g4f.api import run_api
except ImportError:
    print("g4f package not found. Install via: pip install -U g4f[all] fastapi uvicorn")
    sys.exit(1)

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 1337))
    host = os.environ.get("HOST", "0.0.0.0")
    print(f"Starting g4f API server on http://{host}:{port} ...")
    run_api(host=host, port=port)
