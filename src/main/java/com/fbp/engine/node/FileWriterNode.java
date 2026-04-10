package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class FileWriterNode extends AbstractNode{
    private final String filePath;
    private BufferedWriter writer;

    public FileWriterNode(String id, String filePath) {
        super(id);
        this.filePath = filePath;
        addInputPort("in");
    }

    @Override
    public void initialize() {
        super.initialize();
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
            log.info("[{}] 파일 쓰기 준비 완료: {}", getId(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 열기 실패: " + filePath, e);
        }
    }

    @Override
    protected void onProcess(Message message) {
        try {
            if (writer != null) {
                writer.write(message.toString());
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            log.error("[{}] 파일 쓰기 에러", getId(), e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (writer != null) {
                writer.close();
                log.info("[{}] 파일 닫기 완료", getId());
            }
        } catch (IOException e) {
            log.error("[{}] 파일 닫기 에러", getId(), e);
        }
        super.shutdown();
    }
}
