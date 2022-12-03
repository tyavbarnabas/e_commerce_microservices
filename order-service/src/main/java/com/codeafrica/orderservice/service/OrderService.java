package com.codeafrica.orderservice.service;

import com.codeafrica.orderservice.dto.InventoryResponse;
import com.codeafrica.orderservice.dto.OrderLineItemsDto;
import com.codeafrica.orderservice.dto.OrderRequest;
import com.codeafrica.orderservice.model.Order;
import com.codeafrica.orderservice.model.OrderLineItems;
import com.codeafrica.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    private final Tracer tracer;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();


        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

                Span inventoryServiceLookUp = tracer.nextSpan().name("inventoryServiceLookUp");
            try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookUp.start())){


                InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

                assert inventoryResponseArray != null;
                boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                        .allMatch(InventoryResponse::isInStock);

                if(allProductsInStock){
                    orderRepository.save(order);
                    return "Order Placed Successfully";
                } else {
                    throw new IllegalArgumentException("Product is not in stock, please try again later");
                }

            }finally {
                inventoryServiceLookUp.end();
            }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}