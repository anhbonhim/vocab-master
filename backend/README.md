# Hướng dẫn thiết lập Vocab Master Backend

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