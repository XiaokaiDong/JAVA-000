package io.github.kimmking.gateway.router;

import lombok.Data;

import java.util.List;

@Data
public class RoutingConfig {
    List<RoutingContext> contextList;
}
