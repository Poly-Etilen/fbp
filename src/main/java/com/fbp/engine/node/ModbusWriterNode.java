package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusException;
import com.fbp.engine.protocol.ModbusTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// 과제 3-7
@Slf4j
public class ModbusWriterNode extends ProtocolNode{
    private ModbusTcpClient client;

    private final String host;
    private final int port;
    private final int slaveId;
    private final int registerAddress;
    private final String valueField;
    private final double scale;

    public ModbusWriterNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");
        addOutputPort("result");
        addOutputPort("error");

        this.host = (String) config.get("host");
        this.port = (Integer) config.get("port");
        this.slaveId = (int) config.get("slaveId");
        this.registerAddress = (int) config.get("registerAddress");
        this.valueField = (String) config.get("valueField");

        if (config.containsKey("scale")) {
            Object scaleObj = config.get("scale");
            this.scale = scaleObj instanceof  Number ? ((Number) scaleObj).doubleValue() : 1.0;
        } else {
            this.scale = 1.0;
        }
    }

    @Override
    protected void connect() throws Exception {
        client = new ModbusTcpClient(host, port);
        client.connect();
    }

    @Override
    protected void disconnect() {
        if (client != null) {
            client.disconnect();
        }
    }

    @Override
    protected void onProcess(Message message) {
        try {
            if (!message.hasKey(valueField)) {
                log.error("[{}] 지정된 필드({})가 메시지에 없습니다.", getId(), valueField);
                return;
            }
            Object rawValue = message.get(valueField);
            if (!(rawValue instanceof Number)) {
                log.error("[{}] 처리할 수 없는 데이터 타입입니다. : {}",  getId(), rawValue);
                return;
            }

            double doubleValue = ((Number) rawValue).doubleValue();
            int writeValue = (int) Math.round(doubleValue * scale);

            client.writeSingleRegister(slaveId, registerAddress, writeValue);

            Map<String, Object> resultPayload = new HashMap<>();
            resultPayload.put("status", "SUCCESS");
            resultPayload.put("targetAddress", registerAddress);
            resultPayload.put("writtenValue", writeValue);

            send("result", new Message(resultPayload));
        } catch (ModbusException | IOException e) {
            log.error("[{}] Modbus 쓰기 실패: {}",  getId(), e.getMessage());
            Map<String, Object> errPayload = new HashMap<>();
            errPayload.put("error", "쓰기 에러: " + e.getMessage());
            send("error", new Message(errPayload));
        }
    }
}
