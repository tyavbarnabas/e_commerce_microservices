package com.codeafrica.orderservice.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class InventoryResponse {

    private String skuCode;
    private boolean isInStock;


}
