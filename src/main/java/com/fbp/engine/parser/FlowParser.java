package com.fbp.engine.parser;

import java.io.IOException;
import java.io.InputStream;

public interface FlowParser {
    FlowDefinition parse(InputStream stream) throws FlowParserException;
}
