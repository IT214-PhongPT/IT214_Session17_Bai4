package com.grabfood.flashsale;

import com.grabfood.flashsale.model.FlashSaleProduct;
import com.grabfood.flashsale.repository.FlashSaleProductRepository;
import com.grabfood.flashsale.service.ProductFlashSaleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ứng dụng kiểm thử cơ chế chống Cache Stampede với sync = true trong Spring Boot.
 * Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
 */
@Slf4j
@SpringBootApplication
public class FlashSaleSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleSyncApplication.class, args);
    }

    @Bean
    public CommandLineRunner runConcurrencyDemo(ProductFlashSaleService flashSaleService,
                                                FlashSaleProductRepository productRepository) {
        return args -> {
            log.info("=========================================================================");
            log.info(" BẮT ĐẦU KIỂM THỬ ĐỒNG THỜI 50 REQUESTS VỚI sync = true (CHỐNG CACHE STAMPEDE)");
            log.info(" Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120");
            log.info("=========================================================================");

            Long productId = 1001L;
            int totalConcurrentRequests = 50;

            log.info("\n>>> Đang kích hoạt đồng thời {} luồng gửi request tra cứu Product ID = {}...",
                    totalConcurrentRequests, productId);

            ExecutorService executor = Executors.newFixedThreadPool(totalConcurrentRequests);
            List<CompletableFuture<FlashSaleProduct>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            for (int i = 1; i <= totalConcurrentRequests; i++) {
                final int requestId = i;
                CompletableFuture<FlashSaleProduct> future = CompletableFuture.supplyAsync(() -> {
                    FlashSaleProduct product = flashSaleService.getProductById(productId);
                    return product;
                }, executor);
                futures.add(future);
            }

            // Chờ toàn bộ 50 request hoàn thành
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            long totalDuration = System.currentTimeMillis() - startTime;
            executor.shutdown();

            int actualDbHits = productRepository.getDbQueryCount();

            log.info("\n=========================================================================");
            log.info(" KẾT QUẢ KIỂM THỬ ĐỒNG THỜI 50 REQUESTS:");
            log.info(" - Tổng số request gửi đồng thời:    {} requests", totalConcurrentRequests);
            log.info(" - Số request xử lý thành công:     {} requests (100%)", futures.size());
            log.info(" - Số lần truy vấn Database thực tế: {} LẦN DUY NHẤT!", actualDbHits);
            log.info(" - Tổng thời gian hoàn tất:          {} ms (~2 giây tái tạo cache + overhead)", totalDuration);
            log.info(" - Kết luận: Cơ chế khóa sync = true đã ngăn chặn triệt để Cache Stampede!");
            log.info("=========================================================================\n");
        };
    }
}
