#!/usr/bin/env python3
import os
import json
import re
import socket
import urllib.request
import time
import random
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed

PROJECT_ROOT = "/data/data/com.termux/files/home/vocab-master"
JSON_INPUT_PATH = f"{PROJECT_ROOT}/data/src/main/assets/vocab_structured.json"
JSON_OUTPUT_PATH = f"{PROJECT_ROOT}/data/src/main/assets/lessons_v3.json"
LOCAL_PROXY_URL = "http://localhost:8317"
TEXT_MODEL = "qwen3-235b-a22b-instruct-2507"
MAX_WORKERS = 4

IS_LLM_AVAILABLE = False

def check_llm_availability():
    global IS_LLM_AVAILABLE
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1.0)
        result = sock.connect_ex(('localhost', 8317))
        sock.close()
        IS_LLM_AVAILABLE = (result == 0)
    except Exception:
        IS_LLM_AVAILABLE = False
    print(f"[*] Local LLM Proxy status: {'ONLINE' if IS_LLM_AVAILABLE else 'OFFLINE (using Fallback Narrative Templates)'}")

GRAMMAR_CURRICULUM = {
    "A1.1": [
        {"topic": "travel", "title": "Chào hỏi tại sân bay", "focus": "present_simple", "tips": ["Thì HTĐ mô tả hành động thường xuyên: I fly to Tokyo.", "S+V(s/es) ngôi 3 số ít: She works.", "Dấu hiệu: always, usually, every day"]},
        {"topic": "greeting", "title": "Gặp gỡ người mới", "focus": "wh_questions", "tips": ["Wh-question: What/Where/When/Who/Why/How", "Trợ động từ do/does: Where do you live?", "Question word + auxiliary + subject + verb"]},
        {"topic": "food", "title": "Cà phê sáng", "focus": "articles_a_an_the", "tips": ["A/an với danh từ đếm được số ít", "The cho danh từ xác định", "Không dùng mạo từ với số nhiều không xác định"]},
        {"topic": "daily", "title": "Thời gian biểu", "focus": "there_is_are", "tips": ["There is + singular: There is a book.", "There are + plural: There are books.", "Diễn tả sự tồn tại"]},
        {"topic": "city", "title": "Đi lại trong thành phố", "focus": "prepositions_place", "tips": ["At (điểm cụ thể): at the airport", "In (không gian kín): in the room", "On (bề mặt): on the table"]},
        {"topic": "work", "title": "Làm việc văn phòng", "focus": "can_cannot", "tips": ["Can diễn tả khả năng: I can swim.", "Cannot/can't phủ định", "Can + V nguyên mẫu (không to)"]},
        {"topic": "weather", "title": "Thời tiết hôm nay", "focus": "present_continuous_intro", "tips": ["Be + V-ing hành động đang xảy ra", "Dấu hiệu: now, right now, at the moment", "PC tạm thời, HTĐ thường xuyên"]}
    ],
    "A1.2": [
        {"topic": "shopping", "title": "Mua sắm cuối tuần", "focus": "past_simple_regular", "tips": ["V-ed worked/played/visited", "Quy tắc -ed/-d", "Dấu hiệu: yesterday, last week, ago"]},
        {"topic": "travel", "title": "Chuyến đi phượt", "focus": "past_simple_irregular", "tips": ["ĐT bất quy tắc go->went/have->had/see->saw", "Did/didn't câu hỏi + phủ định", "Dùng cho sự việc đã kết thúc ở quá khứ"]},
        {"topic": "description", "title": "Miêu tả người thân", "focus": "comparatives", "tips": ["Short adj + -er: taller", "Long adj more + adj: more beautiful", "Bất quy tắc: good->better, bad->worse"]},
        {"topic": "city", "title": "So sánh nơi chốn", "focus": "superlatives", "tips": ["The + adj + -est: the tallest", "The most + long adj", "Bất quy tắc: the best, the worst"]},
        {"topic": "plans", "title": "Kế hoạch tương lai", "focus": "future_will", "tips": ["Will + V dự đoán/quyết định tức thì", "Won't = will not", "Dấu hiệu: tomorrow, next week"]},
        {"topic": "food", "title": "Chuẩn bị bữa tối", "focus": "countable_uncountable", "tips": ["Countable: a/an + số nhiều", "Uncountable: không số nhiều", "Some/any với cả hai loại"]},
        {"topic": "review", "title": "Ôn tập A1", "focus": "present_perfect_intro", "tips": ["Have/has + V3 kinh nghiệm đến hiện tại", "I have visited Tokyo.", "Dấu hiệu: ever, never, already, yet"]}
    ]
}

SECTIONS_CONFIG = [
    {"id": "sec_1", "index": 0, "name": "Khởi đầu", "cefrSublevel": "A1.1", "icon": "ic_section_seed", "description": "Làm quen với tiếng Anh cơ bản"},
    {"id": "sec_2", "index": 1, "name": "Bước đầu", "cefrSublevel": "A1.2", "icon": "ic_section_door", "description": "Tự tin giao tiếp cơ bản"}
]

def generate_unit_context(unit_config, sublevel):
    if IS_LLM_AVAILABLE:
        # call LLM here if online
        pass
        
    return {
        "storySummary": f"Câu chuyện thực tế về chủ đề {unit_config['title']}. Bạn sẽ học các từ vựng và cấu trúc {unit_config['focus']} cơ bản.",
        "guidebook": {
            "storyIntro": f"Chào mừng bạn đến với bài học {unit_config['title']}!",
            "keyPhrases": [
                {"phrase": f"Key phrase for {unit_config['topic']}", "translation": f"Cụm từ chủ đề {unit_config['title']}", "note": "Mẫu câu cơ bản"}
            ],
            "illustrationSvgDesc": "Flat design illustration"
        }
    }

def generate_node_context(unit_title, story_summary, total_nodes):
    nodes = []
    for i in range(total_nodes):
        nodes.append({
            "title": f"Chương {i+1}: Khám phá {unit_title}",
            "scenarioContext": f"Bối cảnh tình huống thực tế phần {i+1} của bài học {unit_title}."
        })
    return {"nodes": nodes}

def get_safe_filename(word: str) -> str:
    first_part = word.split('/')[0]
    clean = re.sub(r'[^a-zA-Z0-9]', '', first_part).lower()
    return clean if clean else "word_" + str(abs(hash(word)))

# --- DETERMINISTIC QUESTION GENERATOR ---

def make_introduction(word_data):
    word = word_data["word"]
    ex = word_data["examples"][0]
    prompt = ex["text"]
    translation = ex["translation"]
    
    options = [word] + [ct for ct in word_data["coordinate_terms"] if ct.lower() != word.lower()][:3]
    while len(options) < 4:
        options.append(f"option_{len(options)}")
    
    correct_val = options[0]
    random.shuffle(options)
    correct_idx = options.index(correct_val)
    
    return {
        "word": word,
        "type": "INTRODUCTION",
        "prompt": prompt,
        "options": options,
        "correctIndex": correct_idx,
        "translation": translation
    }

def make_fill_in_blank(word_data):
    word = word_data["word"]
    ex = word_data["examples"][0]
    text = ex["text"]
    translation = ex["translation"]
    
    pattern = re.compile(rf"\b{re.escape(word)}\b", re.IGNORECASE)
    prompt = pattern.sub("_____", text)
    if "_____" not in prompt:
        prompt = f"Please complete: _____ ({word_data['pos']})."
        
    options = [word] + [ct for ct in word_data["coordinate_terms"] if ct.lower() != word.lower()][:3]
    while len(options) < 4:
        options.append(f"option_{len(options)}")
        
    correct_val = options[0]
    random.shuffle(options)
    correct_idx = options.index(correct_val)
    
    return {
        "word": word,
        "type": "FILL_IN_BLANK",
        "prompt": prompt,
        "options": options,
        "correctIndex": correct_idx,
        "translation": translation
    }

def make_multiple_choice(word_data):
    word = word_data["word"]
    prompt = f"What does '{word}' mean?"
    
    options = [word_data["definition"]]
    for ct in word_data["coordinate_terms"][:3]:
        options.append(f"Meaning related to {ct}.")
        
    while len(options) < 4:
        options.append(f"Definition {len(options)}.")
        
    correct_val = options[0]
    random.shuffle(options)
    correct_idx = options.index(correct_val)
    
    return {
        "word": word,
        "type": "MULTIPLE_CHOICE",
        "prompt": prompt,
        "options": options,
        "correctIndex": correct_idx,
        "translation": word_data["translations_vi"][0]
    }

def make_listening(word_data):
    word = word_data["word"]
    prompt = "Listen and choose the word you hear"
    
    options = [word] + [ct for ct in word_data["coordinate_terms"] if ct.lower() != word.lower()][:3]
    while len(options) < 4:
        options.append(f"option_{len(options)}")
        
    correct_val = options[0]
    random.shuffle(options)
    correct_idx = options.index(correct_val)
    
    return {
        "word": word,
        "type": "LISTENING",
        "prompt": prompt,
        "options": options,
        "correctIndex": correct_idx,
        "translation": word_data["translations_vi"][0]
    }

def make_matching(words_in_session):
    selected_words = words_in_session[:4]
    pairs = []
    for w in selected_words:
        pairs.append({
            "left": w["word"],
            "right": w["translations_vi"][0]
        })
        
    return {
        "word": None,
        "type": "MATCHING",
        "prompt": "Match the English words with their Vietnamese meanings",
        "matchingPairs": pairs
    }

def make_scrambled(word_data):
    word = word_data["word"]
    ex = word_data["examples"][0]
    correct_sentence = ex["text"]
    translation = ex["translation"]
    
    words = re.findall(r"[\w']+|[.,!?;]", correct_sentence)
    if len(words) < 3:
        correct_sentence = f"This is a {word}."
        translation = f"Đây là một {word}."
        words = re.findall(r"[\w']+|[.,!?;]", correct_sentence)
        
    scrambled = list(words)
    random.shuffle(scrambled)
    
    return {
        "word": word,
        "type": "SCRAMBLED",
        "prompt": "Arrange the words to make a correct sentence",
        "correctSentence": correct_sentence,
        "scrambledWords": scrambled,
        "translation": translation
    }

def make_typing(word_data):
    word = word_data["word"]
    prompt = "Type the word you hear"
    
    return {
        "word": word,
        "type": "TYPING",
        "prompt": prompt,
        "correctSentence": word,
        "translation": word_data["translations_vi"][0]
    }

def generate_session_content_deterministic(node, words, session_index):
    questions = []
    
    for idx, w in enumerate(words):
        q_type_1 = [make_introduction, make_fill_in_blank, make_multiple_choice][(idx + session_index) % 3]
        q_type_2 = [make_listening, make_scrambled, make_typing][(idx + session_index) % 3]
        
        questions.append(q_type_1(w))
        questions.append(q_type_2(w))
        
    questions.append(make_matching(words))
    
    return {
        "title": f"Bài học {session_index + 1}",
        "durationMinutes": 3,
        "questions": questions
    }

def process_unit(section_config, unit_config, unit_index, allocated_words):
    unit_id = f"{section_config['id']}_unit_{unit_index}"
    print(f"  -> Generating context for {unit_id} ({unit_config['title']})")
    
    ctx_res = generate_unit_context(unit_config, section_config['cefrSublevel'])
        
    unit = {
        "id": unit_id,
        "sectionId": section_config["id"],
        "index": unit_index,
        "topic": unit_config["topic"],
        "title": unit_config["title"],
        "storySummary": ctx_res.get("storySummary", ""),
        "icon": "ic_unit_default",
        "guidebook": {
            "id": f"{unit_id}_gb",
            "grammarTips": unit_config["tips"],
            "keyPhrases": ctx_res.get("guidebook", {}).get("keyPhrases", []),
            "storyIntro": ctx_res.get("guidebook", {}).get("storyIntro", ""),
            "illustrationSvg": None
        },
        "nodes": []
    }
    
    nodes_res = generate_node_context(unit["title"], unit["storySummary"], 9)
    lesson_scenarios = nodes_res.get("nodes", [])
        
    node_configs = [
        ("LESSON", 0), ("LESSON", 1), ("LESSON", 2),
        ("REVIEW", -1),
        ("LESSON", 3), ("LESSON", 4), ("LESSON", 5),
        ("REVIEW", -1),
        ("LESSON", 6), ("LESSON", 7), ("LESSON", 8),
        ("UNIT_CHECKPOINT", -1)
    ]
    
    final_nodes = []
    for idx, (ntype, sc_idx) in enumerate(node_configs):
        node_id = f"{unit_id}_node_{idx}"
        if ntype == "LESSON":
            scen = lesson_scenarios[sc_idx]
            n_words = allocated_words.get(node_id, [])
            final_nodes.append({
                "id": node_id, "unitId": unit_id, "index": idx, "type": ntype,
                "title": scen["title"], "scenarioContext": scen["scenarioContext"],
                "icon": "ic_node_lesson", "sessions": [],
                "_words": n_words, "_cefr": section_config["cefrSublevel"], "_story": unit["storySummary"]
            })
        elif ntype == "REVIEW":
            final_nodes.append({
                "id": node_id, "unitId": unit_id, "index": idx, "type": ntype,
                "title": f"Ôn tập {idx+1}", "scenarioContext": "Ôn lại các từ đã học.",
                "icon": "ic_node_review", "sessions": []
            })
        else:
            final_nodes.append({
                "id": node_id, "unitId": unit_id, "index": idx, "type": ntype,
                "title": "Kiểm tra cuối Unit", "scenarioContext": "Bài thi đánh giá năng lực.",
                "icon": "ic_node_unit_checkpoint", "sessions": []
            })
            
    unit["nodes"] = final_nodes
    return unit

def process_lesson_node_deterministic(node):
    words = node.pop("_words", [])
    if not words: return node
    
    sessions = []
    for s_idx in range(3):
        sess_id = f"{node['id']}_s_{s_idx}"
        s_data = generate_session_content_deterministic(node, words, s_idx)
        
        q_list = []
        for q_idx, q in enumerate(s_data.get("questions", [])):
            q["id"] = f"{sess_id}_q_{q_idx}"
            word = q.get("word")
            if word:
                safe = get_safe_filename(word)
                q["audioUrl"] = f"https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/words/{safe}.ogg"
                q["audioUrlSlow"] = f"https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/words/{safe}_slow.ogg"
            q_list.append(q)
            
        sessions.append({
            "id": sess_id,
            "index": s_idx,
            "title": s_data.get("title", f"Session {s_idx+1}"),
            "durationMinutes": s_data.get("durationMinutes", 3),
            "questions": q_list
        })
    node["sessions"] = sessions
    node.pop("_cefr", None)
    node.pop("_story", None)
    return node

def main():
    print("==============================================")
    print("  VOCAB MASTER - LESSONS V3 PIPELINE")
    print("==============================================")
    
    check_llm_availability()

    with open(JSON_INPUT_PATH, 'r', encoding='utf-8') as f:
        vocab_list = json.load(f)

    a1_vocab = [v for v in vocab_list if v.get("level") in ["A1.1", "A1.2"]]
    
    by_sublevel = {
        "A1.1": [v for v in a1_vocab if v.get("level") == "A1.1"],
        "A1.2": [v for v in a1_vocab if v.get("level") == "A1.2"]
    }
    
    allocation = {}
    for s_idx, sec in enumerate(SECTIONS_CONFIG):
        pool = by_sublevel[sec["cefrSublevel"]]
        for u_idx in range(7):
            unit_id = f"{sec['id']}_unit_{u_idx}"
            for n_idx in range(12):
                if n_idx in [3, 7, 11]: continue
                node_id = f"{unit_id}_node_{n_idx}"
                if len(pool) < 7:
                    pool = by_sublevel[sec["cefrSublevel"]]
                allocation[node_id] = pool[:7]
                pool = pool[7:]
                
    output_data = {"schemaVersion": 3, "generatedAt": datetime.now().isoformat(), "sections": []}
        
    for sec_conf in SECTIONS_CONFIG:
        print(f"\n[+] Processing Section {sec_conf['id']} ({sec_conf['name']})")
        section = {k:v for k,v in sec_conf.items()}
        section["units"] = []
        
        grammar_units = GRAMMAR_CURRICULUM[sec_conf["cefrSublevel"]]
        
        for u_idx, u_conf in enumerate(grammar_units):
            unit = process_unit(sec_conf, u_conf, u_idx, allocation)
            if not unit: continue
            
            lesson_nodes = [n for n in unit["nodes"] if n["type"] == "LESSON"]
            with ThreadPoolExecutor(max_workers=MAX_WORKERS) as exe:
                futures = {exe.submit(process_lesson_node_deterministic, dict(n)): n for n in lesson_nodes}
                for f in as_completed(futures):
                    res_node = f.result()
                    for i, n in enumerate(unit["nodes"]):
                        if n["id"] == res_node["id"]:
                            unit["nodes"][i] = res_node
                            break
                            
            section["units"].append(unit)
            
        output_data["sections"].append(section)
                
    with open(JSON_OUTPUT_PATH, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, ensure_ascii=False, indent=2)
    print(f"\n[+] Done generating {JSON_OUTPUT_PATH}!")

if __name__ == "__main__":
    main()
