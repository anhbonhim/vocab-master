#!/usr/bin/env python3
import os
import urllib.request
import zipfile

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
URL = "https://www.cefr-j.org/data/CEFRJ_wordlist_ver1.6.zip"
ZIP_PATH = f"{PROJECT_ROOT}/data/CEFRJ_wordlist_ver1.6.zip"
EXTRACT_DIR = f"{PROJECT_ROOT}/data"

def download_and_extract():
    print(f"Downloading CEFR-J Wordlist from: {URL}")
    os.makedirs(EXTRACT_DIR, exist_ok=True)
    
    headers = {'User-Agent': 'Mozilla/5.0'}
    req = urllib.request.Request(URL, headers=headers)
    
    try:
        with urllib.request.urlopen(req) as response, open(ZIP_PATH, 'wb') as out_file:
            out_file.write(response.read())
        print(f"Saved zip to: {ZIP_PATH}")
        
        with zipfile.ZipFile(ZIP_PATH, 'r') as zip_ref:
            zip_ref.extractall(EXTRACT_DIR)
        print(f"Extracted zip to: {EXTRACT_DIR}")
        
        # Cleanup zip file
        if os.path.exists(ZIP_PATH):
            os.remove(ZIP_PATH)
            print("Removed zip archive.")
            
    except Exception as e:
        print(f"Error downloading CEFR-J: {e}")
        # Fallback to local placeholders or alert if it fails completely
        raise e

if __name__ == "__main__":
    download_and_extract()
