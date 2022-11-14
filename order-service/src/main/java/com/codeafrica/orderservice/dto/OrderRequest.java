package com.codeafrica.orderservice.dto;

import com.codeafrica.orderservice.model.OrderLineItems;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderRequest {

    private List<OrderLineItemsDto> orderLineItemsDtoList = new ArrayList<>();

}
