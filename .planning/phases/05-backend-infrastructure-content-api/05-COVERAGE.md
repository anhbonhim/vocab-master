# Phase 05 API Coverage Matrix

External-API integration (Opencode API) coverage plan.

| Capability | Integration Status | Justification / Notes |
|---|---|---|
| Text Generation | INTEGRATE | Dùng để sinh bài tập tự động từ danh sách từ vựng. Cốt lõi của CONT-02. |
| Streaming | OPT-OUT | Không cần thiết. Sinh bài tập cần trọn vẹn 1 cục JSON rồi mới validate qua Pydantic. |
| Function Calling | OPT-OUT | Sử dụng JSON schema / prompt engineering đủ để cấu trúc output, chưa cần tools/function calling. |
| Embedding | OPT-OUT | Chưa có nhu cầu search ngữ nghĩa hay tính độ tương đồng trong phase này. |

