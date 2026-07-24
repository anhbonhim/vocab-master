#!/bin/bash
# Script upload thư mục audio lên CDN thông qua Github Vocab Assets repository

PROJECT_ROOT="/data/data/com.termux/files/home/vocab-master"
ASSETS_REPO_DIR="/data/data/com.termux/files/home/vocab-assets"
REPO_URL="git@github.com:anhbonhim/vocab-assets.git"

echo "=============================================="
echo "  UPLOADING AUDIO V2 TO CDN (JSDELIVR/GITHUB)"
echo "=============================================="

# Check if audio dir exists
if [ ! -d "$PROJECT_ROOT/output/audio/v2" ]; then
    echo "[!] Không tìm thấy thư mục audio v2: $PROJECT_ROOT/output/audio/v2"
    exit 1
fi

# Clone or pull repo
if [ ! -d "$ASSETS_REPO_DIR" ]; then
    echo "[+] Đang clone vocab-assets repo..."
    # Warning: Requires ssh key configured on device
    git clone $REPO_URL $ASSETS_REPO_DIR
    if [ $? -ne 0 ]; then
        echo "[!] Clone thất bại. Vui lòng kiểm tra SSH keys hoặc quyền truy cập."
        exit 1
    fi
else
    echo "[+] Đang pull latest từ vocab-assets repo..."
    cd $ASSETS_REPO_DIR
    git pull origin main
fi

# Copy files
echo "[+] Copying files..."
mkdir -p $ASSETS_REPO_DIR/audio/v2
cp -r $PROJECT_ROOT/output/audio/v2/* $ASSETS_REPO_DIR/audio/v2/

# Commit and Push
cd $ASSETS_REPO_DIR
echo "[+] Committing..."
git add audio/v2/
git commit -m "Update audio v2 assets for lessons_v2.json"

echo "[+] Pushing to remote..."
git push origin main

echo "[+] Thành công! Quá trình upload hoàn tất."
echo "[i] URL CDN base: https://cdn.jsdelivr.net/gh/anhbonhim/vocab-assets@main/audio/v2/"
