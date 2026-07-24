#!/data/data/com.termux/files/usr/bin/bash
cd /data/data/com.termux/files/home/vocab-master/backend
source venv/bin/activate
exec python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
