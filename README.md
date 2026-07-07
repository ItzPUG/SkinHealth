# SkinHealth - Ứng Dụng Hỗ Trợ Phân Loại Tình Trạng Da 🩸📱

SkinHealth là một dự án nghiên cứu khoa học sinh viên nhằm phát triển giải pháp hỗ trợ người dùng theo dõi và nhận diện sớm các vấn đề về da thông qua hình ảnh. Ứng dụng tích hợp mô hình Trí tuệ nhân tạo (Machine Learning) để phân tích hình ảnh vùng da đầu vào từ camera hoặc thư viện, đưa ra các kết quả đánh giá sơ bộ thời gian thực một cách nhanh chóng và mượt mà.

---

## 🚀 Tính Năng Chính (Key Features)

- **Chụp ảnh/Chọn ảnh thông minh:** Tích hợp Camera API (CameraX/Intent) hỗ trợ người dùng tự chụp vùng da cần kiểm tra hoặc tải ảnh lên từ thư viện thiết bị.
- **Xử lý ảnh tối ưu (Image Processing):** Tự động resize, nén bitmap để tối ưu dung lượng ảnh đầu vào, đảm bảo tốc độ truyền tải dữ liệu và hiệu năng phân tích của mô hình AI.
- **Tích hợp mô hình AI/ML:** Nhận diện và phân loại tình trạng da thời gian thực. Áp dụng luồng xử lý bất đồng bộ (Asynchronous) giúp giao diện người dùng (UI) luôn mượt mà, không xảy ra hiện tượng giật lag (ANR).
- **Lưu trữ dữ liệu kết quả:** Kết nối cơ sở dữ liệu để đồng bộ dữ liệu người dùng và lịch sử các lần kiểm tra.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

- **Ngôn ngữ lập trình:** Java / Kotlin
- **Môi trường phát triển:** Android Studio & Android SDK
- **Kiến trúc ứng dụng:** MVVM / MVC (Đảm bảo cấu trúc code mạch lạc, dễ bảo trì và mở rộng)
- **Cơ sở dữ liệu & Cloud:** Firebase (Authentication & Realtime Database)
- **Công cụ quản lý mã nguồn:** Git / GitHub

---

## 📸 Giao Diện Ứng Dụng (Screenshots)

*(Mẹo nhỏ cho Phúc: Bạn hãy chụp 2-3 tấm ảnh màn hình app từ điện thoại hoặc trình giả lập, upload lên một kho lưu trữ hoặc đẩy trực tiếp vào thư mục dự án rồi thay link ảnh vào dưới đây nhé)*

| Màn hình chính | Chụp ảnh & Phân tích | Kết quả đánh giá |
|:---:|:---:|:---:|
| <img src="https://via.placeholder.com/200x400?text=Home+Screen" width="200"> | <img src="https://via.placeholder.com/200x400?text=AI+Analysis" width="200"> | <img src="https://via.placeholder.com/200x400?text=Result+Screen" width="200"> |

---

## 👥 Thành Viên Thực Hiện (Team)

- **Dương Trọng Phúc** - *Vị trí:* Thành viên nghiên cứu & Phát triển ứng dụng di động Android.
- **Đồng đội** - *Vị trí:* Thành viên nghiên cứu & Huấn luyện mô hình Machine Learning.

---

## 📄 Hướng Dẫn Cài Đặt (Installation)

1. Clone dự án về máy:
   ```bash
   git clone [https://github.com/ItzPUG/SkinHealth.git](https://github.com/ItzPUG/SkinHealth.git)
<img width="449" height="811" alt="image" src="https://github.com/user-attachments/assets/182d6685-568d-40eb-ac1e-8870fc5bd691" />
<img width="461" height="1028" alt="image" src="https://github.com/user-attachments/assets/92bbcb43-2bbb-41c1-8ed1-1d84e6296d03" />
<img width="756" height="879" alt="image" src="https://github.com/user-attachments/assets/ae9dc721-9e99-44be-95da-f7e0d209bb28" />

Công nghệ sử dụng (Kotlin/Java, Firebase, Android Studio...)
