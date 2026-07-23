#!/usr/bin/env python3
import wn

def download_oewn():
    print("Downloading Open English WordNet 2025 (oewn:2025)...")
    try:
        wn.download("oewn:2025")
        print("Successfully downloaded Open English WordNet 2025!")
    except Exception as e:
        print(f"Error or already downloaded: {e}")
        # Retry with latest oewn version if specific key fails
        try:
            print("Retrying with 'oewn:2024' or standard oewn...")
            wn.download("oewn:2024")
            print("Downloaded oewn:2024 as fallback.")
        except Exception as ex:
            print(f"Failed fallback: {ex}")

if __name__ == "__main__":
    download_oewn()
