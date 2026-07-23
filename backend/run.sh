#!/bin/bash
export PYTHONPATH=$PWD
echo "Starting Vocab Master API..."
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
