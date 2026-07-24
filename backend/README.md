> ## ⚠️ INACTIVE — MÃ NGUỒN CHẾT (DEAD CODE)
>
> Thư mục `backend/` **không còn được sử dụng** bởi ứng dụng Android Vocab Master và hiện ở trạng thái **INACTIVE**.
>
> - Không có mã nguồn Kotlin nào trong app import `com.google.firebase`, `retrofit2`, `okhttp3`, hay các thư viện identity/credentials (xác nhận qua `rg` over `*.kt`).
> - Ứng dụng đã chuyển sang kiến trúc **offline-first** (quyết định MEM001): bài kiểm tra Placement chạy hoàn toàn trên engine IRT cục bộ, không cần backend hay Firebase.
> - Thư mục này được **giữ lại** (không xoá) để lịch sử git bảo tồn bản tham chiếu IRT (FastAPI) và các mẫu xác thực (auth patterns) phục vụ nhu cầu di trữ dữ liệu trong tương lai.
> - **Đừng** re-introduce (tái đưa vào) backend/network dependencies vào build Android dựa trên thư mục này.
>
> Tài liệu thiết lập bên dưới chỉ được lưu giữ làm tham khảo lịch sử, không phản ánh trạng thái hiện tại của app.

---

# Hướng dẫn thiết lập Vocab Master Backend

> **Lưu ý:** Hướng dẫn bên dưới dành cho server Python cũ đã ngưng hoạt động (INACTIVE). Chỉ đọc khi cần tham khảo lịch sử.

Dự án này sử dụng Python (FastAPI) và cơ sở dữ liệu SQLite cực kỳ nhẹ.

## Yêu cầu
- Python 3.9 trở lên

## Cách cài đặt

### Trên Windows
1. Mở Terminal / PowerShell trong thư mục `backend`.
2. Tạo môi trường ảo: `python -m venv venv`
3. Kích hoạt môi trường: `venv\Scripts\activate`
4. Cài thư viện: `pip install -r requirements.txt`
5. Chạy server: `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
*Lưu ý: Nếu bị lỗi liên quan tới `orjson` hoặc `cryptography`, hãy cài phiên bản pre-compiled hoặc cập nhật pip.*

### Trên Linux / Ubuntu / VPS
1. Mở Terminal trong thư mục `backend`.
2. Tạo môi trường ảo: `python3 -m venv venv`
3. Kích hoạt: `source venv/bin/activate`
4. Cài thư viện: `pip install -r requirements.txt`
5. Chạy server: `./run.sh`

### Trên Termux (Android)
Vì Termux sử dụng CPU ARM, việc cài đặt một số thư viện mã hóa (cryptography) sẽ yêu cầu trình biên dịch Rust. 
1. Cài đặt các gói cần thiết: 
   ```bash
   pkg install python rust binutils build-essential
   ```
2. Thực hiện các bước cài đặt như Linux ở trên.

## Cấu trúc API (Swagger UI)
Sau khi chạy server, bạn có thể xem tài liệu API tự động tại:
- http://localhost:8000/docs
- http://localhost:8000/redoc