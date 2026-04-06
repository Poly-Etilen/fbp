package com.fbp.engine.core;

import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Queue;

@RequiredArgsConstructor
public class Connection {
    // Connection은 메세지를 잠깐 저장하는 버퍼 역할을 함.
    private final Queue<Message> buffer;
    @Setter
    private InputPort target;

    public void transmit(Message message) {
        // 큐에서 메세지를 넣고
        buffer.offer(message);

        // 큐에서 마지막에 처음에 넣은 메세지를 msg에 저장하고 삭제
        Message msg = buffer.poll();
        // 꺼낸 메시지가 있다면 InputPort에게 전달을 함
        // 메시지를 보내려면 InputPort가 필요함. 따라서 없으면 메시지를 보내지 않음. 있어야 메시지를 보냄
        if (msg != null && target != null) {
            target.receive(msg);
        }
    }
}
