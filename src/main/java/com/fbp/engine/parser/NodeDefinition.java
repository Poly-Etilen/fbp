package com.fbp.engine.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NodeDefinition {
    private String id;
    private String type;
    private Map<String, Object> config;
}
