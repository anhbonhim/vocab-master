#!/usr/bin/env python3
import os
import json
import re
import asyncio
import subprocess
import edge_tts

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
JSON_PATH = f"{PROJECT_ROOT}/data/src/main/assets/lessons_v2.json"
AUDIO_OUT_DIR = f"{PROJECT_ROOT}/output/audio/v2/words"
SENTENCES_OUT_DIR = f"{PROJECT_ROOT}/output/audio/v2/sentences"
ERROR_LOG_PATH = f"{PROJECT_ROOT}/output/edge_tts_v2_error_log.txt"

VOICE_NORMAL = "en-US-AriaNeural"
VOICE_SLOW = "en-US-AriaNeural"
RATE_SLOW = "-20%"

CONCURRENCY_LIMIT = 15
MAX_RETRIES = 3

def get_safe_filename(text: str) -> str:
    first_part = text.split('/')[0]
    clean = re.sub(r'[^a-zA-Z0-9]', '', first_part).lower()
    return clean if clean else "audio_" + str(abs(hash(text)))

def extract_listening_sentence(prompt: str, word: str) -> str:
    """Extract the actual sentence to speak from a LISTENING prompt.

    Prompts look like:
      "Listen: 'She smiles at the officer.' Which word did you hear?"
      "Listen and choose: '____ need to check my bag.'"
    We only want the quoted part, with blanks filled in by the target word.
    Returns empty string if no quoted sentence is found.
    """
    quoted = re.findall(r"'([^']+)'", prompt)
    if quoted:
        sentence = re.sub(r"_{2,}", word if word else "", quoted[0])
        sentence = re.sub(r"\s+", " ", sentence).strip()
        return sentence
    # Fallback: if no quotes but prompt is long enough, use it as-is
    if len(prompt.split()) > 4:
        return re.sub(r"_{2,}", word if word else "", prompt).strip()
    return ""

def log_error(text: str, error_type: str, details: str):
    with open(ERROR_LOG_PATH, "a", encoding="utf-8") as f:
        f.write(f"TEXT: '{text}' | ERROR: {error_type} | DETAILS: {details}\n")

async def convert_mp3_to_ogg(mp3_path, ogg_path):
    proc = await asyncio.create_subprocess_exec(
        "ffmpeg", "-y", "-i", mp3_path, "-codec:a", "libvorbis", "-qscale:a", "3", ogg_path,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )
    await proc.wait()
    if os.path.exists(ogg_path) and os.path.exists(mp3_path):
        os.remove(mp3_path)
        return True
    return False

async def process_audio(semaphore, text, safe_name, out_dir, is_slow, idx, total):
    async with semaphore:
        suffix = "_slow" if is_slow else ""
        final_filename = f"{safe_name}{suffix}"
        temp_mp3 = os.path.join(out_dir, f"{final_filename}.mp3")
        final_ogg = os.path.join(out_dir, f"{final_filename}.ogg")
        
        if os.path.exists(final_ogg):
            if os.path.getsize(final_ogg) >= 5120:
                print(f"[{idx}/{total}] Skip (exists & valid): {text} ({'Slow' if is_slow else 'Normal'})")
                return
            else:
                try:
                    os.remove(final_ogg)
                except:
                    pass
            
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                rate_arg = RATE_SLOW if is_slow else "+0%"
                communicate = edge_tts.Communicate(text, VOICE_NORMAL, rate=rate_arg)
                await communicate.save(temp_mp3)
                
                if os.path.exists(temp_mp3) and os.path.getsize(temp_mp3) > 0:
                    success = await convert_mp3_to_ogg(temp_mp3, final_ogg)
                    if success and os.path.exists(final_ogg) and os.path.getsize(final_ogg) >= 5120:
                        print(f"[{idx}/{total}] Success: {text} -> {final_filename}.ogg")
                        return
                    else:
                        if os.path.exists(final_ogg):
                            try: os.remove(final_ogg)
                            except: pass
                        raise Exception("FFMPEG conversion failed or OGG too small")
                else:
                    raise Exception("Downloaded file empty")
            except Exception as e:
                if os.path.exists(temp_mp3):
                    try: os.remove(temp_mp3)
                    except: pass
                if attempt < MAX_RETRIES:
                    await asyncio.sleep(attempt * 2)
                else:
                    log_error(text, "EDGE_TTS_FAIL", str(e))

async def main():
    print("==============================================")
    print("   VOCAB MASTER - EDGE-TTS V2 PIPELINE")
    print("==============================================")
    
    os.makedirs(AUDIO_OUT_DIR, exist_ok=True)
    os.makedirs(SENTENCES_OUT_DIR, exist_ok=True)
    if os.path.exists(ERROR_LOG_PATH):
        os.remove(ERROR_LOG_PATH)
        
    if not os.path.exists(JSON_PATH):
        print(f"JSON file not found at: {JSON_PATH}")
        return
        
    with open(JSON_PATH, 'r', encoding='utf-8') as f:
        lessons_data = json.load(f)
        
    unique_words = {}
    unique_sentences = {}
    
    for section in lessons_data.get("sections", []):
        for unit in section.get("units", []):
            for node in unit.get("nodes", []):
                for session in node.get("sessions", []) or []:
                    for q in session.get("questions", []):
                        if q.get("word"):
                            w = q["word"]
                            unique_words[get_safe_filename(w)] = w
                        if q.get("type") == "LISTENING" and q.get("prompt"):
                            sentence = extract_listening_sentence(q.get("prompt", ""), q.get("word", ""))
                            if sentence:
                                unique_sentences[get_safe_filename(sentence)] = sentence

    total_tasks = len(unique_words) * 2 + len(unique_sentences) * 2
    print(f"Found {len(unique_words)} words and {len(unique_sentences)} sentences. Total tasks: {total_tasks}")
    
    semaphore = asyncio.Semaphore(CONCURRENCY_LIMIT)
    tasks = []
    
    idx = 1
    for safe_name, word in unique_words.items():
        tasks.append(process_audio(semaphore, word, safe_name, AUDIO_OUT_DIR, False, idx, total_tasks))
        idx += 1
        tasks.append(process_audio(semaphore, word, safe_name, AUDIO_OUT_DIR, True, idx, total_tasks))
        idx += 1
        
    for safe_name, sentence in unique_sentences.items():
        tasks.append(process_audio(semaphore, sentence, safe_name, SENTENCES_OUT_DIR, False, idx, total_tasks))
        idx += 1
        tasks.append(process_audio(semaphore, sentence, safe_name, SENTENCES_OUT_DIR, True, idx, total_tasks))
        idx += 1
        
    await asyncio.gather(*tasks)
    print("==============================================")
    print("   AUDIO V2 PIPELINE COMPLETE")
    print("==============================================")

if __name__ == "__main__":
    asyncio.run(main())
