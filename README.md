# Bé Tập Đếm — app Android tập đếm cho bé (offline)

App thực hành đếm cho bé mới bắt đầu, làm từ khung của Bé Đánh Vần
nhưng thay toàn bộ nội dung bằng đếm số. Thực hành là chính, không dạy
lý thuyết, không đăng nhập, không Firebase, không online, không quảng cáo.

## Phạm vi — CHỐT 2026-10-29
- Chỉ số 1-20, KHÔNG phép trừ ở bất kỳ phần nào.
- Mỗi phần 10 điểm để qua và mở phần tiếp theo; đúng +1, sai 2 câu trừ 1
  (chỉ trừ ở câu sai thứ 2); điểm tính riêng từng phần.
- Câu hỏi sinh ngẫu nhiên, không lặp trong 1 lần vào phần.

## 7 phần
1. Nghe số, chọn đúng số (giọng đọc vi-VN, 4 nút số).
2. Nhìn hình, đếm rồi chọn đúng số (N biểu tượng emoji, 4 nút số).
3. Nhìn số, chọn đúng số lượng hình — DẠNG GHÉP: 1 hình lớn gồm 4 hình
   nhỏ 2x2 ghi Hình 1..4, mỗi ô 1 số lượng đồ vật cùng loại, đáp án là
   4 nút chữ "Hình 1".."Hình 4".
4. Dãy số tự nhiên liên tiếp, trống 1 ô ở giữa (không ở đầu/cuối).
5. Dãy số bất kỳ, đáp án luôn là 1 cặp "lớn nhất và nhỏ nhất".
6. Phép cộng: cộng 2 số dưới 10, hoặc cộng 3-4 số, kết quả dưới 20.
7. Điền vào ô ? kiểu tìm x, CHỈ phép cộng, ví dụ ? + 4 = 9, 6 + ? = 13.

## Trạng thái tên và package — TẠM
- Package tạm: `vn.danhvan.tieuhoc` (giữ tạm như mẫu, chưa chốt).
- Tên hiển thị: "Bé Tập Đếm" (`app_name` trong
  `app/src/main/res/values/strings.xml`).

## Chạy app
1. Mở thư mục này bằng Android Studio.
2. Chọn thiết bị / máy ảo Android (minSdk 24), bấm Run.
3. Mở app là vào thi luôn, không màn hình đăng nhập, không tài khoản.

## Phiên bản dùng
- Android Gradle Plugin 8.11.1, Kotlin 2.1.21, Kotlin Compose plugin 2.1.21.
- compileSdk/targetSdk 36, minSdk 24, Java 17.
- Nền các phần là ảnh AI dọc đã có trong `app/src/main/res/drawable/`
  (`nen_chinh.png`, `nen_chang1..7.webp`); hình đếm dùng emoji dạng Text,
  không cần thêm asset hình.
