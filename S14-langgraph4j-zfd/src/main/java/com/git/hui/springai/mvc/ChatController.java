package com.git.hui.springai.mvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Content;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    private final CompiledGraph<AgentExecutor.State> workflow;

    public ChatController(ChatModel chatModel) throws GraphStateException {

        workflow = AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(new TimeWeatherTools())
                .build()
                .compile();
    }

    /**
     * 通过agent方式访问大模型
     *
     * @param msg
     * @return
     */
    @GetMapping("/chat")
    public Object chat(String msg) {
        AgentExecutor.State last = null;
        int i = 0;
        for (NodeOutput<AgentExecutor.State> item : workflow.stream(Map.of("messages", new UserMessage(msg)))) {
            System.out.println(item.state().data());
            last = item.state();
            System.out.printf("%02d : %s%n", i++, toStr(last.messages()));
        }

        // 返回最后一条消息
        return last.lastMessage().map(Content::getText).orElse("NoData");
    }

    public String toStr(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
