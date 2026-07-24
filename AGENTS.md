## Language Policy / Ngôn ngữ phản hồi

Tất cả phản hồi, câu hỏi, tương tác CLI và output từ agent/subagent (ngoại trừ tên biến, mã nguồn, đường dẫn file, lệnh terminal và thuật ngữ kỹ thuật) BẮT BUỘC phải sử dụng **Tiếng Việt**.

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

## Kiểm soát Subagent Explore (ANTI-LOOP SAFETY)

Khi sử dụng công cụ `Task` để gọi `subagent_type: "explore"`, bạn PHẢI tuân thủ nghiêm ngặt các quy tắc sau bằng cách nhúng chúng vào `prompt` giao việc:

1. **Khóa cứng thư mục (Sandboxing):** Bắt buộc subagent CHỈ ĐƯỢC PHÉP tìm kiếm bên trong thư mục dự án. Hãy truyền thẳng câu này vào prompt: 
   *"Use explicitly path='/data/data/com.termux/files/home/vocab-master' for all glob and grep calls. NEVER use path='/'."*
2. **Không giao Task mở (Open-ended):** KHÔNG dùng các lệnh chung chung như "explore toàn bộ app". Phải khoanh vùng rõ file hoặc thư mục con cần đọc.
3. **Điều kiện dừng bắt buộc (Mandatory Stop Condition):** BẮT BUỘC chèn câu này vào prompt:
   *"STRICT RULE: Do not retry `glob` or `grep` more than 7 times if it fails, times out, or returns too many results. Stop and report immediately."*

## Giới hạn path cho MỌI agent (không riêng @explore) — GLOBAL RULE

Bug đã ghi nhận: không chỉ subagent `@explore` khi được dispatch qua `Task`, mà cả agent chính (mode `plan`, `build`, v.v.) cũng có thể tự gọi trực tiếp `glob`/`grep` với `path="/"` mà không qua bất kỳ dispatch nào — rule "Kiểm soát Subagent Explore" ở trên KHÔNG che được trường hợp này vì nó chỉ có hiệu lực khi được nhúng thủ công vào `prompt` lúc gọi Task.

Do đó, quy tắc dưới đây áp dụng cho **mọi lệnh gọi `glob` hoặc `grep`, bất kể agent nào đang chạy, bất kể có qua Task/dispatch hay không**:

1. **TUYỆT ĐỐI KHÔNG** gọi `glob` hoặc `grep` với `path="/"` hoặc bỏ trống path (mặc định về root). Trên Termux/Android, việc này quét vào `/proc`, `/storage`, các mount point hệ thống → treo vô thời hạn, không có timeout tự ngắt.
2. **LUÔN LUÔN** giới hạn `path` vào một trong các giá trị sau:
   - `.` (thư mục hiện tại, nếu đang cwd đúng trong project)
   - `/data/data/com.termux/files/home/vocab-master` (tuyệt đối, an toàn nhất)
   - hoặc thư mục con cụ thể bên trong project (vd: `app/src/main/java`)
3. Nếu không chắc cwd hiện tại có đúng trong project hay không, chạy `pwd` trước để xác nhận trước khi gọi `glob`/`grep`.
4. Nếu một lệnh `glob`/`grep` không trả kết quả sau khi đã giới hạn path đúng, KHÔNG mở rộng path ra ngoài project để "thử tìm rộng hơn" — thay vào đó báo lại cho người dùng rằng không tìm thấy trong scope hiện tại.
5. Tìm file build output (APK, AAB, v.v.) LUÔN giới hạn trong `app/build/outputs/` — không glob toàn bộ project hay root để tìm `*.apk`.
