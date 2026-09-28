package com.grabfood.flashsale.service;

import com.grabfood.flashsale.model.FlashSaleProduct;
import com.grabfood.flashsale.repository.FlashSaleProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service quản lý thông tin sản phẩm Flash Sale với cơ chế chống Cache Stampede.
 * 
 * =========================================================================================
 * BẢN CHẤT CỦA THUỘC TÍNH sync = true TRONG @Cacheable:
 * =========================================================================================
 * 1. KÍCH HOẠT CƠ CHẾ KHÓA ĐỒNG BỘ (SYNCHRONIZED / MUTEX LOCKING):
 *    - Khi nhiều luồng (ví dụ: 50 luồng) đồng thời gọi phương thức khi Cache trống (hoặc vừa hết hạn TTL),
 *      chỉ duy nhất luồng đầu tiên giành được Lock và thực thi hàm gốc (truy vấn DB trong 2000ms).
 *    - 49 luồng còn lại sẽ bị chặn (Blocked/Waiting) tại tầng Spring Cache Proxy và CHỜ luồng đầu tiên hoàn thành.
 * 
 * 2. CHỐNG SẬP DATABASE (PREVENT CACHE STAMPEDE / THUNDERING HERD):
 *    - Khi luồng đầu tiên tính toán xong và nạp kết quả vào Cache, Lock được mở.
 *    - Tất cả 49 luồng đang chờ sẽ lấy trực tiếp kết quả từ Cache vừa tạo mà KHÔNG truy xuất Database.
 *    - KẾT QUẢ: Database chỉ chịu ĐÚNG 1 LẦN TRUY VẤN DUY NHẤT!
 * =========================================================================================
 * 
 * Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductFlashSaleService {

    private final FlashSaleProductRepository productRepository;

    /**
     * Lấy thông tin sản phẩm Flash Sale có áp dụng cơ chế khóa chống Cache Stampede (sync = true).
     * 
     * - value = "flash-sale"
     * - key = "#id"
     * - sync = true
     */
    @Cacheable(value = "flash-sale", key = "#id", sync = true)
    public FlashSaleProduct getProductById(Long id) {
        log.info("Fetching from Database for product {}", id);
        
        try {
            // Giả lập truy vấn phức tạp tái tạo cache mất 2 giây (2000ms)
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Luồng bị gián đoạn: {}", e.getMessage());
        }

        return productRepository.findById(id).orElse(null);
    }

    /**
     * Phương thức đối chứng KHÔNG DÙNG sync = true (Mô phỏng Cache Stampede).
     * Khi có 50 luồng đồng thời gọi, cả 50 luồng đều chọc thẳng vào Database gây quá tải sập hệ thống.
     */
    @Cacheable(value = "flash-sale-nosync", key = "#id")
    public FlashSaleProduct getProductByIdWithoutSync(Long id) {
        log.warn("[NO-SYNC WARNING] Fetching from Database for product {} (GÂY NGHẼN DB!)", id);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return productRepository.findById(id).orElse(null);
    }
}
