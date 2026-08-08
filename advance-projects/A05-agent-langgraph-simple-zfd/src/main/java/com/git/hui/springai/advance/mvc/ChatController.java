package com.git.hui.springai.advance.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.advance.agents.WeatherRecommendAgent;
import com.git.hui.springai.advance.agents.WeatherRecommendAgent_zfd;
import com.git.hui.springai.advance.times.TimeWeatherTools;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Content;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Function;

/**
 * @author YiHui
 * @date 2025/8/5
 */
@RestController
public class ChatController {
    private final CompiledGraph<AgentExecutor.State> workflow;

    private final ChatClient chatClient;

    private final WeatherRecommendAgent_zfd weatherAgent;

    public ChatController(ChatModel chatModel) throws GraphStateException {
        workflow = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(new TimeWeatherTools())
                .build()
                .compile();

        chatClient = ChatClient.builder(chatModel)
                .defaultTools(new TimeWeatherTools())
                .build();


        weatherAgent = new WeatherRecommendAgent_zfd(chatClient);
    }

    public String toStr(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过agent方式访问大模型
     *
     * @param msg
     * @return
     */
    @GetMapping("/workflow")
    public Object workflow(String msg) {
        AgentExecutor.State last = null;
        int i = 0;
        for (NodeOutput<AgentExecutor.State> item : workflow.stream(Map.of("messages", new UserMessage(msg)))) {
            System.out.println(item);
            System.out.println("==================");
            last = item.state();
            System.out.printf("%02d : %s%n", i++, toStr(last.messages()));
        }

        // 返回最后一条消息
        return last.lastMessage().map(Content::getText).orElse("NoData");
    }

    @GetMapping("/chat3")
    public Object chat3(String msg) {
        AgentExecutor.State last = null;
        AsyncGenerator<NodeOutput<AgentExecutor.State>> nodeOutputs = workflow.stream(Map.of("messages", new UserMessage(msg)));
        int i = 0;
        for (NodeOutput<AgentExecutor.State> item : nodeOutputs) {
            System.out.println(item);
            last = item.state();
            System.out.printf("%02d : %s%n", i++, toStr(last.messages()));
        }
        // 返回最后一条消息
        if (last != null) {
            return last.lastMessage().map(new Function<>() {
                @Override
                public Object apply(Message message) {
                    return message.getText();
                }
            }).orElse("NoData");
        }
        return null;
    }

    /**
     * 直接调用大模型
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat2")
    public Object chat2(String msg) {
        return chatClient.prompt(msg).call().content();
    }


    @GetMapping("/recommend")
    public Object recommend(String area) {
        return weatherAgent.recommendByLocation(area);
    }
}
