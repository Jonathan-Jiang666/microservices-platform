package com.tech.microservices.api_gateway.routes;

import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class Routes {

    @Bean 
    public RouterFunction<ServerResponse> productServiceRoute() {
        return GatewayRouterFunctions.route( "product_service")
        .route(
            RequestPredicates.path("/api/product"),
            HandlerFunctions.http())
            .before(BeforeFilterFunctions.uri("http://localhost:8080"))
        .build();
    }

    @Bean 
    public RouterFunction<ServerResponse> orderServiceRoutes() {
        return GatewayRouterFunctions.route( "order_service")
        .route(
            RequestPredicates.path("/api/orders"),
            HandlerFunctions.http())
            .before(BeforeFilterFunctions.uri("http://localhost:8081"))
        .build();
    }


    @Bean 
    public RouterFunction<ServerResponse> inventoryServiceRoutes() {
        return GatewayRouterFunctions.route( "inventory_service")
        .route(
            RequestPredicates.path("/api/inventory/**"),
            HandlerFunctions.http())
            .before(BeforeFilterFunctions.uri("http://localhost:8082"))
        .build();
    }
}
