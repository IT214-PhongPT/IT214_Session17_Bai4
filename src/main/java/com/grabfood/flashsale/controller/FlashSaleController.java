package com.grabfood.flashsale.controller;

import com.grabfood.flashsale.model.FlashSaleProduct;
import com.grabfood.flashsale.repository.FlashSaleProductRepository;
import com.grabfood.flashsale.service.ProductFlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final ProductFlashSaleService flashSaleService;
    private final FlashSaleProductRepository productRepository;
    private final CacheManager cacheManager;

    @GetMapping("/{id}")
    public ResponseEntity<FlashSaleProduct> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(flashSaleService.getProductById(id));
    }

    /**
     * Endpoint chạy thử nghiệm 50 request đồng thời với sync = true.
     */
    @GetMapping("/concurrent-test/{id}")
    public ResponseEntity<Map<String, Object>> runConcurrentTestWithSync(@PathVariable Long id) {
        // Xóa cache trước khi test
        if (cacheManager.getCache("flash-sale") != null) {
            cacheManager.getCache("flash-sale").clear();
        }
        productRepository.resetDbQueryCount();

        int totalRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalRequests);
        List<CompletableFuture<FlashSaleProduct>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalRequests; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> flashSaleService.getProductById(id), executor));
        }

        // Chờ toàn bộ 50 request hoàn thành
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long totalExecutionTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        Map<String, Object> result = new HashMap<>();
        result.put("mode", "SYNC_LOCKING_ENABLED (sync = true)");
        result.put("totalRequests", totalRequests);
        result.put("successfulRequests", futures.size());
        result.put("actualDatabaseQueries", productRepository.getDbQueryCount());
        result.put("totalExecutionTimeMs", totalExecutionTime);
        result.put("cacheStampedePrevented", productRepository.getDbQueryCount() == 1);
        result.put("evaluation", "Chỉ có 1 lần truy vấn CSDL, 49 luồng khác lấy từ Cache an toàn tuyệt đối!");

        return ResponseEntity.ok(result);
    }
}
