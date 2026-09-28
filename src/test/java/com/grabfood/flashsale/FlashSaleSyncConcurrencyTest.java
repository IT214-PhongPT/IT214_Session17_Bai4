package com.grabfood.flashsale;

import com.grabfood.flashsale.config.CacheConfig;
import com.grabfood.flashsale.model.FlashSaleProduct;
import com.grabfood.flashsale.repository.FlashSaleProductRepository;
import com.grabfood.flashsale.service.ProductFlashSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đa luồng với 50 threads đồng thời xác thực cơ chế sync = true.
 */
@SpringBootTest(classes = {FlashSaleSyncApplication.class, CacheConfig.class})
class FlashSaleSyncConcurrencyTest {

    @Autowired
    private ProductFlashSaleService flashSaleService;

    @Autowired
    private FlashSaleProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("flash-sale") != null) {
            cacheManager.getCache("flash-sale").clear();
        }
        productRepository.resetDbQueryCount();
    }

    @Test
    @DisplayName("50 luồng đồng thời gọi getProductById với sync = true chỉ truy vấn Database đúng 1 lần")
    void testConcurrencyWithSyncTrue() {
        Long productId = 1001L;
        int totalRequests = 50;

        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        List<CompletableFuture<FlashSaleProduct>> futures = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> flashSaleService.getProductById(productId), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long duration = System.currentTimeMillis() - start;
        executor.shutdown();

        // 1. Kiểm tra toàn bộ 50 request đều thành công và không null
        for (CompletableFuture<FlashSaleProduct> future : futures) {
            FlashSaleProduct product = future.join();
            assertNotNull(product);
            assertEquals(productId, product.getId());
            assertEquals("Trà Sữa Trân Châu Hoàng Gia - Size L", product.getName());
        }

        // 2. KHẲNG ĐỊNH: Số lần truy vấn DB thực tế đúng bằng 1
        assertEquals(1, productRepository.getDbQueryCount(),
                "Cơ chế sync = true phải khóa và chỉ cho phép duy nhất 1 luồng truy vấn Database!");

        // 3. Thời gian hoàn thành xấp xỉ ~2000ms (chứ không phải 50 * 2000ms = 100s)
        assertTrue(duration >= 2000 && duration < 3500,
                "Tổng thời gian phải ~2s + overhead, thực tế: " + duration + "ms");
    }
}
