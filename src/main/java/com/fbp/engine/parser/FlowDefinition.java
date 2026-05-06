package com.fbp.engine.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FlowDefinition {
    private String id;
    private String name;
    private String description;
    private List<NodeDefinition> nodes;
    private List<ConnectionDefinition> connections;
}
