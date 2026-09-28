package com.grabfood.flashsale.repository;

import com.grabfood.flashsale.model.FlashSaleProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository mô phỏng tầng lưu trữ CSDL vật lý cho Flash Sale.
 * Có đếm số lần gọi thực tế xuống Database để chứng minh cơ chế sync = true.
 */
@Slf4j
@Repository
public class FlashSaleProductRepository {

    private final Map<Long, FlashSaleProduct> database = new ConcurrentHashMap<>();
    private final AtomicInteger dbQueryCounter = new AtomicInteger(0);

    public FlashSaleProductRepository() {
        initSampleProducts();
    }

    private void initSampleProducts() {
        database.put(1001L, FlashSaleProduct.builder()
                .id(1001L)
                .name("Trà Sữa Trân Châu Hoàng Gia - Size L")
                .originalPrice(new BigDecimal("60000"))
                .flashSalePrice(new BigDecimal("19000"))
                .stockQuantity(500)
                .discountPercentage(68)
                .build());

        database.put(1002L, FlashSaleProduct.builder()
                .id(1002L)
                .name("Gà Rán Giòn Cay Combo 3 Miếng")
                .originalPrice(new BigDecimal("110000"))
                .flashSalePrice(new BigDecimal("49000"))
                .stockQuantity(300)
                .discountPercentage(55)
                .build());
    }

    public Optional<FlashSaleProduct> findById(Long id) {
        int currentCount = dbQueryCounter.incrementAndGet();
        log.warn("===> [DATABASE HIT] Truy vấn CSDL vật lý lần thứ #{} cho Product ID: {}", currentCount, id);
        return Optional.ofNullable(database.get(id));
    }

    public int getDbQueryCount() {
        return dbQueryCounter.get();
    }

    public void resetDbQueryCount() {
        dbQueryCounter.set(0);
    }
}
