package com.grabfood.flashsale.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Đối tượng sản phẩm trong chương trình Flash Sale của GrabFood.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private Integer stockQuantity;
    private Integer discountPercentage;
}
