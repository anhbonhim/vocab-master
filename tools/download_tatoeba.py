#!/usr/bin/env python3
import os
import urllib.request
import tarfile
import bz2

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
DEST_DIR = f"{PROJECT_ROOT}/data/tatoeba_source"

URLS = {
    "eng_sentences.tsv.bz2": "https://downloads.tatoeba.org/exports/per_language/eng/eng_sentences.tsv.bz2",
    "vie_sentences.tsv.bz2": "https://downloads.tatoeba.org/exports/per_language/vie/vie_sentences.tsv.bz2",
    "links.tar.bz2": "https://downloads.tatoeba.org/exports/links.tar.bz2",
    "tags.tar.bz2": "https://downloads.tatoeba.org/exports/tags.tar.bz2"
}

def download_file(url, dest_path):
    print(f"Downloading {url}...")
    headers = {'User-Agent': 'Mozilla/5.0'}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as response, open(dest_path, 'wb') as out_file:
        out_file.write(response.read())
    print(f"Saved to {dest_path}")

def extract_bz2(bz2_path, out_path):
    print(f"Decompressing {bz2_path}...")
    with bz2.open(bz2_path, 'rb') as f_in, open(out_path, 'wb') as f_out:
        f_out.write(f_in.read())
    print(f"Extracted to {out_path}")

def extract_tar(tar_path, dest_dir):
    print(f"Extracting {tar_path}...")
    with tarfile.open(tar_path, 'r:bz2') as tar:
        tar.extractall(path=dest_dir)
    print(f"Extracted to {dest_dir}")

def main():
    os.makedirs(DEST_DIR, exist_ok=True)
    
    for filename, url in URLS.items():
        local_path = os.path.join(DEST_DIR, filename)
        if not os.path.exists(local_path):
            try:
                download_file(url, local_path)
            except Exception as e:
                print(f"Failed to download {filename}: {e}")
                continue
        
        # Decompress/extract
        if filename.endswith(".tsv.bz2"):
            out_tsv = local_path.replace(".tsv.bz2", ".tsv")
            if not os.path.exists(out_tsv):
                extract_bz2(local_path, out_tsv)
        elif filename.endswith(".tar.bz2"):
            # links.tar.bz2 extracts to links.csv
            # tags.tar.bz2 extracts to tags.txt
            extract_tar(local_path, DEST_DIR)

    print("Tatoeba downloads and extraction complete!")

if __name__ == "__main__":
    main()
