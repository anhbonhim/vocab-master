#!/bin/bash
export PYTHONPATH=$PWD
echo "Starting Vocab Master API..."
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
