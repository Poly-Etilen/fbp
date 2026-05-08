package com.fbp.engine.parser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransportDefinition {
    private String type;
    private String broker;
    private int qos = 1;
}