package com.fbp.engine.plugin;


import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.List;

public class MyCustomNodeProvider implements NodeProvider {

    // 플러그인을 통해 제공할 실제 커스텀 노드 클래스
    public static class CustomPluginNode extends AbstractNode {
        public CustomPluginNode(String id) { super(id); }
        @Override protected void onProcess(Message message) {}
    }

    @Override
    public List<NodeDescriptor> getNodeDescriptors() {
        return List.of(
            new NodeDescriptor(
                "TestPluginType", // NodeRegistry에 등록될 타입명
                "커스텀 노드",
                CustomPluginNode.class, 
                (id, config) -> new CustomPluginNode(id)
            )
        );
    }
}