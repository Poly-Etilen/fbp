package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.ModbusException;
import com.fbp.engine.protocol.ModbusTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


// 과제 3-6
@Slf4j
public class ModbusReaderNode extends ProtocolNode{
    private ModbusTcpClient client;

    private final String host;
    private final int port;
    private final int slaveId;
    private final int startAddress;
    private final int count;
    private final Map<String, Object> registerMapping;
    public ModbusReaderNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("trigger");
        addOutputPort("out");
        addOutputPort("error");

        this.host = (String) config.get("host");
        this.port = config.containsKey("port") ? (int) config.get("port"): 502;
        this.slaveId = (int) config.get("slaveId");
        this.startAddress = (int) config.get("startAddress");
        this.count = (int)  config.get("count");
        this.registerMapping = (Map<String, Object>) config.get("registerMapping");
    }

    @Override
    protected void connect() throws IOException {
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
            int[] rawValues = client.readHoldingRegisters(slaveId, startAddress, count);

            Map<String, Object> payload = new HashMap<>();
            payload.put("slaveId", slaveId);
            payload.put("timestamp", System.currentTimeMillis());

            if (registerMapping != null) {
                Map<String, Object> mappedData = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    String regKey = String.valueOf(startAddress + i);
                    if (registerMapping.containsKey(regKey)) {
                        Map<String,Object> mappingInfo = (Map<String, Object>) registerMapping.get(regKey);
                        String name = (String) mappingInfo.get("name");
                        double scale = mappingInfo.containsKey("scale") ? (double) mappingInfo.get("scale") : 1.0;

                        mappedData.put(name, rawValues[i] * scale);
                    } else  {
                        mappedData.put(regKey, rawValues[i]);
                    }
                }
                payload.put("data", mappedData);
            } else {
                payload.put("registers", rawValues);
            }
        } catch (ModbusException| IOException e) {
            Map<String, Object> errPayload = new HashMap<>();
            errPayload.put("error", "MODBUS 통신 실패: " + e.getMessage());

            Message errMsg = new Message(errPayload);
            send("error", errMsg);
        }
    }
}
