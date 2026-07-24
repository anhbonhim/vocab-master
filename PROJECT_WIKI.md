# Vocab Master - Project Wiki

Tài liệu này mô tả kiến trúc, luồng dữ liệu (Data Flow) và các cấu trúc dữ liệu cốt lõi của dự án `Vocab Master`, đặc biệt tập trung vào Data Layer. Tài liệu này đóng vai trò là nguồn thông tin (context) cho các agent khi làm việc với dự án.

## Core Data Structures (Entities & Models)

Ứng dụng sử dụng **Room Database** cho local storage, tập trung quản lý từ vựng và hệ thống Lặp lại ngắt quãng (Free Spaced Repetition System - FSRS).

### 1. `VocabularyCardEntity`
- **Mục đích**: Đại diện cho một thẻ từ vựng trong database, chứa cả thông tin từ vựng và thông tin lập lịch của thuật toán FSRS.
- **Table Name**: `vocabulary_cards`
- **Fields**:
  - Word Details: `word`, `definition`, `partOfSpeech`, `difficultyLevel`, `example`, `ipa`
  - FSRS Properties: `due` (LocalDateTime), `stability`, `difficulty`, `interval`, `reps`, `lapses`, `state` (New, Learning, Review, Relearning), `lastReview` (LocalDateTime)
- **Domain Mapping**: Cung cấp hàm `toDomain()` và `fromDomain()` để map giữa `VocabularyCardEntity` và `VocabularyItemWithCard` (là một composite của `VocabularyItem` và `Card` của FSRS).

### 2. `ReviewLogEntity`
- **Mục đích**: Ghi log lại mỗi phiên ôn tập của một card cụ thể, được sử dụng để thuật toán FSRS theo dõi và phân tích.
- **Table Name**: `review_logs`
- **Relationship**: Có Foreign Key constraint tham chiếu đến `VocabularyCardEntity` (cascade on delete). Được index trên `cardId`.
- **Fields**:
  - `cardId`
  - FSRS Properties: `rating` (Again, Hard, Good, Easy), `elapsed_days`, `scheduled_days`, `stability`, `difficulty`, `state`, `timestamp`
- **Domain Mapping**: Maps to/from the domain `ReviewLog` object.

### 3. Pre-populated Data Model (`VocabularyAssetItem`)
- Là một DTO nội bộ được dùng trong repository để parse dữ liệu file `lessons_v3.json` từ thư mục assets.
- Chứa các field như `word`, `level`, `type`, `translation`, và danh sách các ví dụ (Beginner, Intermediate, Advanced).

## Database Schema (Room DAOs)

### `VocabDatabase`
- Chứa `VocabularyCardEntity` và `ReviewLogEntity`.
- Sử dụng class `Converters` để xử lý các kiểu dữ liệu phức tạp: `LocalDateTime` to/from Epoch seconds, `State` (Enum) to/from Int, và `Rating` (Enum) to/from Int.
- Được cấu hình fallback to destructive migration (drop bảng cũ khi đổi version) trong Hilt module.

### `VocabDao`
Data Access Object chính cung cấp các operation cho Room:
- **Queries**:
  - `getDueAndNewCards`: Fetch các card có state là `New` HOẶC due date đã qua, sắp xếp theo state và due date, có giới hạn limit (Trả về Flow).
  - `getCardsByLevel`: Fetch card theo `difficultyLevel`.
  - `getCardById`: Fetch một card duy nhất.
  - Stats Queries: `getLearnedCount` (nơi state != New), `getStateCounts`, `getLevelCounts`.
- **Insert/Update**:
  - `insertCard`, `insertAllCards`, `updateCard`
  - `insertReviewLog`, `insertAllReviewLogs`
- **Utility**:
  - `getCardCount`, `getAllCards`, `getAllReviewLogsList`
  - Logic xóa toàn bộ (wipe) database: `deleteAllCards`, `deleteAllReviewLogs`.

## Data Sources

1. **Local Database (Room)**: Nguồn dữ liệu (source of truth) chính cho tiến độ học từ vựng và trạng thái FSRS.
2. **Local Assets (`lessons_v3.json`)**: Dữ liệu hạt giống (seed data) ban đầu. Chứa một mảng các từ vựng với nghĩa, phiên âm, và câu ví dụ.
3. **DataStore (Preferences)**: Dùng để lưu trữ dạng key-value cho cài đặt người dùng (Settings) và các chỉ số gamification (XP, Streak,...).

*Lưu ý: Không có remote data sources hay API calls nào được gọi trong module này. Mọi thứ hoạt động Offline-first.*

## API/Interface Implementations (Repositories)

Các Repository này triển khai các interface được định nghĩa ở tầng Domain và làm cầu nối giữa Room/DataStore và Domain layer.

### 1. `VocabularyRepositoryImpl`
- **Initialization/Pre-population**: Cung cấp hàm `checkAndPrepopulate()`. Nếu DB trống (`getCardCount() == 0`), nó sẽ đọc `lessons_v3.json` từ Android assets, parse thông qua `kotlinx.serialization`, format các ví dụ và thêm vào batch `VocabularyCardEntity` ban đầu với FSRS state được set là `New`.
- **Core Operations**:
  - `getDueCards`: Kiểm tra dữ liệu khởi tạo, trả về Flow chứa các card tới hạn/mới từ `VocabDao` và map sang Domain models.
  - `getCardsByLevel`, `updateCard`, `insertCard`, `insertAll`: Các thao tác CRUD tiêu chuẩn.

### 2. `ReviewRepositoryImpl`
- **Purpose**: Xử lý việc ghi log và thống kê các lần ôn tập từ vựng.
- **Operations**:
  - `insertReviewLog`: Lưu log ôn tập FSRS mới.
  - `getReviewLogs` / `getAllReviewLogs`: Lấy lịch sử review.
  - `getStats`: Kết hợp nhiều Room queries bằng coroutine `combine` để tạo ra một object `ReviewStats` chứa tổng số từ đã học và thống kê theo độ khó / trạng thái.

### 3. `SettingsRepositoryImpl`
- **DataStore Usage**: Triển khai `SettingsRepository` thông qua `androidx.datastore.preferences`.
- **Stored Preferences**:
  - Cài đặt học tập: `dailyGoalMinutes`, `desiredRetention`
  - Gamification/Stats: `currentStreak`, `longestStreak`, `availableFreezes`, `lastStudyDate`, `xpTotal`, `badgeStatus`
  - Cài đặt app: `theme`, `language`
- Cung cấp dữ liệu dạng `Flow<T>` và các `suspend` functions sử dụng `dataStore.edit` để update.

### 4. `BackupRepositoryImpl`
- **Purpose**: Xử lý việc Export/Import toàn bộ state của người dùng (Database + DataStore) thành định dạng chuỗi JSON.
- **Export**: Fetch toàn bộ data (cards, review logs, preferences) và serialize thành `BackupPayload` JSON thông qua `kotlinx.serialization`.
- **Import**: Deserialize chuỗi JSON. Sử dụng Room `withTransaction` block để xóa dữ liệu cũ và chèn dữ liệu mới đảm bảo toàn vẹn. Sau đó cập nhật lần lượt toàn bộ settings vào DataStore.
