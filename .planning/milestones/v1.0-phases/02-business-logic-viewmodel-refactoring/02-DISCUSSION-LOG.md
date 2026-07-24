# Phase 02: Business Logic & ViewModel Refactoring - Discussion Log

## 1. Kiến trúc phân tách logic quiz
**Presented Options:**
- Tạo UseCase riêng cho từng luồng (Recommended)
- Giữ logic trong ViewModel (Cách hiện tại)
- Sử dụng extension functions trên ViewModel
- Sử dụng State Machine Pattern riêng biệt

**User Selected:** Tạo UseCase riêng cho từng luồng (Recommended)
**Notes:** Sử dụng UseCase để tách business logic (ví dụ: SubmitReviewUseCase) khỏi ViewModel, đảm bảo Clean Architecture.

## 2. Quản lý trạng thái UI (UiState)
**Presented Options:**
- Sử dụng Sealed Interface/Class cho root state (Loading, Success, Error) (Recommended)
- Một Single Data Class lớn chứa tất cả (Loading = bool, Error = string?)
- Sealed Class bọc Data Class (Loading, Error, Success(Data))
- Nhiều StateFlow song song trong ViewModel

**User Selected:** Sử dụng Sealed Interface/Class cho root state (Loading, Success, Error) (Recommended)
**Notes:** Sử dụng Sealed Interface/Class (Loading, Success, Error) để biểu diễn trạng thái UI, giúp compiler bắt buộc handle tất cả các trường hợp.

## 3. Điều hướng và Transition Quiz
**Presented Options:**
- ViewModel với SavedStateHandle (Recommended)
- Chỉ dựa vào Compose State / View Model in-memory
- Lưu tạm State xuống DB
- Dùng type-safe Navigation Arguments

**User Selected:** ViewModel với SavedStateHandle (Recommended)
**Notes:** Sử dụng SavedStateHandle trong ViewModel để lưu trữ các trạng thái cần thiết của quiz, giúp khôi phục state khi xoay màn hình (UX-02).

## 4. Chia nhỏ màn hình Compose lớn
**Presented Options:**
- Tách theo Feature / Section của màn hình (Recommended)
- Tách theo Design System Component siêu nhỏ
- Tách ngẫu nhiên theo dung lượng file (Mỗi file ~200 dòng)
- Dùng Slot API pattern cho toàn bộ màn hình lớn

**User Selected:** Tách theo Design System Component siêu nhỏ
**Notes:** Chia nhỏ các màn hình lớn (HomeScreen, QuizScreen) thành các component UI siêu nhỏ (Design System components) thay vì cắt theo feature block.
