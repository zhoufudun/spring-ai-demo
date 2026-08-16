package com.git.hui.springai.ali.mvc;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

/**
 * @author YiHui
 * @date 2026/3/9
 */
@RestController
public class DemoController {
    @Autowired
    private ChatModel chatModel;

    @GetMapping(path = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=utf-8")
    public Flux ask(String type, String msg) throws GraphRunnerException {
        return switch (type) {
            case "instruction":
                yield instructionUsage(msg);
            case "weather":
                yield toolUsage(msg);
            default:
                yield Flux.just("no function call!");
        };
    }

    /**
     * instructionUsage 方法展示了最简单的 Agent 使用方式——通过系统指令定义 AI 的角色和能力
     *
     * @param msg
     * @return
     * @throws GraphRunnerException
     */

    public Flux instructionUsage(String msg) throws GraphRunnerException {
        String instruction = """
                你是我的女朋友。
                
                在回答问题时，请：
                1、提供情绪。
                2、友好回话
                """;

        ReactAgent agent = ReactAgent.builder()
                .name("architect_agent")
                .model(chatModel)
                .instruction(instruction) // 设置系统提示词，定义 Agent 行为
                .build();

        System.out.println("BEFORE INVOKE");
        // 同步调用，并获取历史的返回结果
        Optional<OverAllState> result = agent.invoke(msg); // 阻塞等待完整结果

        System.out.println("END INVOKE");
        StringBuilder ans = new StringBuilder();
        if (result.isPresent()) {
            // 访问历史消息
            List<Message> messages = (List<Message>) result.get().value("messages").orElse(List.of()); // 从状态中提取对话历史
            for (Message message : messages) {
                if (message instanceof AssistantMessage) {
                    // 获取AI应答的消息
                    ans.append(message.getText());
                }
            }
        }
        System.out.println(ans);
        return Flux.just(Map.of("content", ans.toString()));
    }

    // 定义天气查询工具
    public static class WeatherTool implements BiFunction<Map, ToolContext, String> {

        @Override
        public String apply(@ToolParam(description = "城市名，如：武汉, key=city, value=具体城市") Map map, ToolContext toolContext) {
            String city = (String) map.get("city");
            System.out.println("[WeatherTool] Query: " + city);
            if (city == null || city.isEmpty()) {
                return "错误：未提供城市名称";
            }
            return "It's always sunny in " + city + "!";
        }
    }

    /**
     * 获取用户的位置的工具 - 使用上下文
     */
    public class LocationTool implements BiFunction<Map, ToolContext, String> {

        @Override
        public String apply(@ToolParam Map map, ToolContext toolContext) {
            // 从上下文中获取用户信息
            String user_id = null;
            if (toolContext != null && toolContext.getContext() != null) {
                RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
                Optional<Object> userId = runnableConfig.metadata("user_id");
                if (userId.isPresent()) {
                    user_id = (String) userId.get();
                }

            }
            if (user_id == null) {
                user_id = "1";
            }
            System.out.println("[LocationTool] Query: " + map + " -> " + user_id);
            return "1".equals(user_id) ? "武汉" : "上海";
        }
    }


    /**
     * toolUsage 方法展示了 Agent 的核心能力——自主调用外部工具完成复杂任务
     *
     * @param msg
     * @return
     * @throws GraphRunnerException
     */

    public Flux<Map<String, String>> toolUsage(String msg) throws GraphRunnerException {
        // 工具注册
        ToolCallback getWeatherTool = FunctionToolCallback.builder("getWeatherTool", new WeatherTool())
                .description("根据你传入的城市，返回对应的天气")
                .inputType(Map.class)
                .build();

        // 工具注册
        ToolCallback getUserLocationTool = FunctionToolCallback.builder("getLocationTool", new LocationTool())
                .description("根据用户id查询用户所处的城市名")
                .inputType(Map.class)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("weather-agent")
                .model(chatModel)
                .tools(getUserLocationTool, getWeatherTool)
                .systemPrompt("""
                        你现在是一个智能天气助手。当用户询问天气或提到城市时，请调用 getWeatherTool 工具查询天气。
                        如果你不知道具体的城市信息，请首先调用 getLocationTool 工具查询用户所在的城市，然后再查询对应的天气信息返回
                        注意：没有具体城市时，直接调用工具获取城市，不需要二次确认
                        """)
                .saver(new MemorySaver()) // 启用状态持久化
                .build();

        // threadId 是给定对话的唯一标识符
        // 配置上下文（用户ID、线程ID）
        String threadId = "1";
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).addMetadata("user_id", "1").build();


        // 运行 agent
        // 第一次调用
        System.out.println("开始进入调用：");

        Flux<NodeOutput> nodeOutputFlux = agent.stream(msg, config);
        // 处理 NodeOutput，提取有用的响应内容

        return nodeOutputFlux.map(nodeOutput -> {
            if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
                OutputType outputType = streamingOutput.getOutputType();
                System.out.println("OutputType: " + outputType + "\t => " + streamingOutput.message().getText());
                String tag;

                if (outputType == OutputType.AGENT_MODEL_STREAMING) {
                    // 流式消息，增量返回, 增量输出（打字机效果）
                    tag = nodeOutput.node() + "#delta:";
                } else if (outputType == OutputType.AGENT_MODEL_FINISHED) {
                    // 流式消息执行完成，返回完整的结果
                    tag = nodeOutput.node() + "#complete:";
                } else if (outputType == OutputType.AGENT_TOOL_FINISHED) {
                    // 工具执行结果
                    tag = nodeOutput.node() + "#tool_result:";
                    ToolResponseMessage toolResponseMessage = (ToolResponseMessage) ((StreamingOutput<?>) nodeOutput).message();
                    List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
                    StringBuilder toolRes = new StringBuilder();
                    for (ToolResponseMessage.ToolResponse rp : responses) {
                        toolRes.append("Tool:" + rp.name()).append(" ==rsp=> ").append(rp.responseData());
                    }
                    return Map.of("content", tag + toolRes);
                } else {
                    // 因为ReAct底层是基于Graph实现，因此除了 _AGENT_MODEL_ 节点之外，必然还存在 __START__ node 和 __END__ node
                    tag = nodeOutput.node() + "#" + outputType.name() + ":";
                }
                return Map.of("content", tag + streamingOutput.message().getText());

            } else if (nodeOutput.isSTART()) {
                System.out.println("START");
                return Map.of("content", "START");
            } else if (nodeOutput.isEND()) {
                System.out.println("END");
                return Map.of("content", "END");
            }
            return Map.of("content", "No response");
        });
    }

}
