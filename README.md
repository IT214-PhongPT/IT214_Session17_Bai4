# IT214 - Session 17 - Bài Tập 4: Chống Cache Stampede với thuộc tính sync = true

- **Học phần:** Kiến trúc Microservices (IT214)
- **Sinh viên:** Phạm Thanh Phong
- **MSSV:** N24DTCN120
- **Mã bài tập:** `IT214_Session17_BT4`

---

## 1. Giới thiệu dự án
Dự án áp dụng cơ chế khóa phân tán/đồng bộ **`sync = true`** trong Spring `@Cacheable` để giải quyết triệt để sự cố **Cache Stampede (Dog-piling / Thundering Herd Problem)** khi hàng nghìn request đồng thời ập vào một key Flash Sale vừa hết hạn TTL.

### Điểm nhấn kỹ thuật:
1. **Annotation:** `@Cacheable(value = "flash-sale", key = "#id", sync = true)`
2. **Cơ chế Mutex Locking:** Chỉ 1 luồng duy nhất được cấp quyền xuống CSDL tái tạo cache (`Thread.sleep(2000)` ~ 2 giây). 49 luồng khác bị chặn chờ và lấy kết quả trực tiếp từ Cache vừa nạp.
3. **Hiệu năng:** Database chỉ chịu 1 truy vấn duy nhất thay vì 50 truy vấn đồng thời.

---

## 2. Hướng dẫn chạy kiểm thử đa luồng (50 Requests đồng thời)

### Cách 1: Khởi động ứng dụng (Tự động chạy qua CommandLineRunner)
```bash
gradle bootRun
```

### Cách 2: Gọi REST API kiểm thử 50 requests
```bash
curl -X GET http://localhost:8080/flash-sale/concurrent-test/1001
```
