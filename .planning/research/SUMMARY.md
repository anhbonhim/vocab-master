# Project Research Summary

## Key Findings

### Stack Additions
- **Audio Playback:** `androidx.media3:media3-exoplayer` (cùng với `media3-ui-compose`) là lựa chọn tối ưu cho bài tập nghe, dễ dàng quản lý vòng đời trong Compose.
- **Gamified Animations:** `com.airbnb.android:lottie-compose` (`DotLottie`) được khuyến nghị cho các hoạt ảnh phản hồi tức thì (instant feedback) nhờ hiệu năng cao và khả năng kiểm soát trạng thái tốt hơn ảnh GIF.
- **Drag & Drop (Sắp xếp câu):** `sh.calvin.reorderable:reorderable` cung cấp giải pháp kéo thả hiện đại cho Compose, hỗ trợ `Modifier.animateItem`.
- **Database & State:** Room Database và Kotlin Coroutines (Flow) tiếp tục đóng vai trò nòng cốt, cần sử dụng `@Relation` hoặc Junction tables cho cấu trúc phân cấp thay vì lưu JSON.

### Feature Table Stakes
- **Hierarchical Curriculum:** Người dùng cần lộ trình học rõ ràng (Topic -> Lesson -> Exercise).
- **Instant Visual Feedback:** Cần phản hồi đúng/sai ngay lập tức (sử dụng `AnimatedVisibility`, đổi màu, Lottie animations).
- **Multiple Choice Exercises:** Định dạng bài tập tiêu chuẩn.
- **FSRS Integration:** Cần tách biệt trạng thái đánh giá FSRS khỏi cấu trúc bài học.

### Differentiators
- **Interactive Lottie Feedback:** Tạo cảm giác "game-like" khen thưởng người dùng.
- **Fill-in-the-blanks & Sentence Arrangement:** Tăng cường kiểm tra recall và ngữ pháp chủ động.

### Watch Out For
- **Mixing Manual Lesson Scheduling with FSRS:** Không cố định thời gian hoặc dùng bước học dài cho cấu trúc bài học tĩnh. Cần sử dụng FSRS Presets riêng biệt cho từng loại bài tập (Audio vs Text).
- **Unstable Lambdas in Compose:** Tránh tạo lambda mới mỗi frame trong quá trình animation phản hồi để ngăn chặn recomposition toàn màn hình. Sử dụng hàm tham chiếu (`viewModel::submitAnswer`).
- **Fat Composables:** Phá vỡ nguyên tắc OCP khi xử lý nhiều loại bài tập trong một Compose (như `QuizScreen`). Cần thiết kế `QuizType` dưới dạng `sealed interface` và dùng Factory/Strategy pattern cho UI.
- **Direct Media Dependencies in ViewModels:** Cần đóng gói ExoPlayer qua một interface (e.g., `AudioPlayerUseCase`) để duy trì Clean Architecture.
- **Data Modeling:** Tránh lưu cấu trúc phân cấp phức tạp thành dạng JSON trong Room `@TypeConverter`. Luôn dùng cấu trúc quan hệ chuẩn.

## Implications for Roadmap
1. **Data Layer Updates:** Bắt đầu với việc mô hình hóa dữ liệu (Topic -> Lesson -> Exercise) bằng Room với quan hệ (Relations), và đảm bảo tách biệt logic FSRS.
2. **ViewModel State Machine:** Implement state machine (`QuizInteractionState`) trong `QuizViewModel` để xử lý luồng Instant Feedback.
3. **UI Modularization & Gamification:** Refactor `QuizScreen` sử dụng các component phân tách theo `QuestionType` (Polymorphic UI) và thêm Lottie animations.
4. **Specific Exercise Implementations:** Tích hợp Audio (ExoPlayer), Fill-in-the-blanks, và Drag-and-drop (Reorderable) UI components.

## Sources
- Android Developers, Airbnb Lottie Docs, Anki FSRS Forums, Room Architecture Guidelines.
