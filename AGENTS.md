## Delegation

When a task needs specialized focus (e.g., reviewing Room migrations, checking FSRS calculation correctness, verifying Compose UI state handling) and doing it inline would pollute this session's context with a lot of tool output, dispatch it to @specialist instead of doing it yourself. In the dispatch message, always include:
- Role: what expertise to adopt for this task
- Scope: which files/directories are in bounds
- Constraints: e.g. read-only, no edits
- Deliverable: exact format expected back (e.g. "list of file:line issues, no fixes")

Do not create new agent files for this. Reuse @specialist every time with different role/scope in the message.

## Web search / tra cứu thông tin
Không có tool `google_search` hay bất kỳ tool tìm kiếm built-in nào của Gemini/Google — đừng cố gọi.


Thứ tự thử khi cần tìm kiếm (chưa biết URL cụ thể):
1. Dùng `bash` gọi curl với User-Agent giống trình duyệt thật (bắt buộc, thiếu sẽ bị chặn/CAPTCHA):
   curl -s -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36" "https://lite.duckduckgo.com/lite/?q=<từ khoá>" | grep -oP '(?<=href=")[^"]*uddg=\K[^"&]+' | head -5
   (dùng lite.duckduckgo.com/lite/, KHÔNG dùng html.duckduckgo.com — bản lite ít bị CAPTCHA hơn)
2. Nếu vẫn bị chặn/rỗng kết quả, thử Bing thay thế:
   curl -s -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36" "https://www.bing.com/search?q=<từ khoá>" | grep -oP '(?<=<a href=")[^"]+(?=" h=)' | head -5
3. Với `webfetch` cũng LUÔN thử lại 1 lần nếu "Transport error" (có thể do timeout tạm thời), nhưng KHÔNG lặp lại quá 2 lần trên cùng 1 domain.
4. Nếu sau khi thử cả 2 công cụ tìm kiếm ở bước 1-2 và webfetch đều thất bại từ 3 nguồn trở lên: DỪNG LẠI, báo cho tôi biết rõ đã thử gì và thất bại ở đâu, hỏi tôi có URL cụ thể nào khác không — không tự ý bịa thêm nguồn hay đoán URL.
