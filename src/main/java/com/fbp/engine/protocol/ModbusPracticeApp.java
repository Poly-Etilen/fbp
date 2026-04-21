package com.fbp.engine.protocol;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

// 과제 3-5
@Slf4j
public class ModbusPracticeApp {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);

        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);

        simulator.start();

        ModbusTcpClient client = null;
        try {
            Thread.sleep(500);

            client = new ModbusTcpClient("localhost", 5020);
            client.connect();
            log.info("클라이언트 접속됨");

            // 슬레이브 1, 주소 0번부터 레지스터 3개 읽기 (FC 03)
            int slaveId = 1;
            int[] initialValues = client.readHoldingRegisters(slaveId, 0, 3);
            log.info("[FC 03 읽기 요청] 주소 0~2");
            log.info("[응답 결과]: {}", Arrays.toString(initialValues));

            // 슬레이브 1, 주소 2번에 값 100 쓰기 (FC 06)
            log.info("[FC 06 쓰기 요청] 주소 2에 값 100 기록");
            client.writeSingleRegister(slaveId, 2, 100);
            log.info("[응답 결과]: 정상적으로 에코백 됨 (기록 성공)");

            // 다시 주소 0~2 레지스터를 읽어서 값이 변경되었는지 확인
            int[] changedValues = client.readHoldingRegisters(slaveId, 0, 3);
            log.info("[FC 03 변경 확인용 읽기 요청] 주소 0~2");
            log.info("[최종 응답 결과]: {}", Arrays.toString(changedValues));

            log.info("시뮬레이터 내부 메모리 2번 방의 값: {}", simulator.getRegister(2));

        } catch (ModbusException me) {
            log.error("MODBUS 로직 에러: {}", me.getMessage());
        } catch (Exception e) {
            log.error("네트워크 또는 기타 에러: {}", e.getMessage());
        } finally {
            if (client != null) {
                client.disconnect();
            }
            simulator.stop();
        }
    }
}