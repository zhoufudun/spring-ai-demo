package com.git.hui.springai.mvc;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Consumer;

/**
 * @author YiHui
 * @date 2025/7/14
 */
@RestController
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ZhiPuAiChatModel chatModel;

    private final ChatMemory chatMemory;

    private final ChatClient chatClient;

    private final ChatClient sessionClient;

    private final ChatClient promptClient;

    @Autowired
    public ChatController(ZhiPuAiChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是狂放不羁的诗仙李白，我们现在开始对话")
                .defaultAdvisors(
                        // 打印日志
                        new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter,ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 带参数的默认系统消息, 推荐方式
        this.sessionClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是{role}，我们现在开始对话")
                .defaultAdvisors(
                        // 打印日志
                        new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 每次交互时从记忆库检索历史消息，并将其作为消息集合注入提示词
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();


        //  区别于MessageChatMemoryAdvisor将多伦对话（包含内容、角色）返回给大模型
        //  PromptChatMemoryAdvisor主要是将消息内容以文本的方式追加到系统提示词中
        this.promptClient = ChatClient.builder(chatModel)
                .defaultSystem("你现在是狂放不羁的诗仙李白，我们现在开始对话")
                .defaultAdvisors(
                        // 打印日志
                        new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                        // 将之前的消息内容以文本的方式追加到系统提示词中
                        PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 基于ChatClient实现返回结果的结构化映射
     *
     * @param msg
     * @return
     */
    @GetMapping("/ai/generate")
    public Object generate(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @GetMapping("/ai/gen3")
    public Object gen3(@RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return promptClient.prompt(msg).call().content();
    }

    @GetMapping("/ai/{user}/gen")
    public Object gen2(
            @PathVariable("user") String user,
            @RequestParam(value = "role", defaultValue = "狂放不羁的诗仙李白") String role,
            @RequestParam(value = "msg", defaultValue = "你好") String msg) {
        return sessionClient.prompt()
                // 系统词模板
                .system(sp -> sp.param("role", role))
                .user(msg)
                // 设置会话ID，实现单独会话
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user))
                .call()
                .content();
    }
    @GetMapping("/ai/{user}/gen3")
    public Object gen3(
            @PathVariable("user") String user,
            @RequestParam(value = "role", defaultValue = "狂放不羁的诗仙李白") String role,
            @RequestParam(value = "msg", defaultValue = "你好") String msg) {

        String content = sessionClient.prompt()
                // 系统词模板
                .system(sp -> sp.param("role", role))
                // 用户消息
                .user(msg)
                //
                .advisors(new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, user);
                    }
                })
                .call()
                .content();
        return content;
    }

    @GetMapping("/ai/ChatModel")
    public Object ChatModel() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().build();
        String conversationId = "001";

        UserMessage myNameIsJamesBond = new UserMessage("My name is James Bond");
        memory.add(conversationId, myNameIsJamesBond);

        ChatResponse re = chatModel.call(new Prompt(memory.get(conversationId)));
        chatMemory.add(conversationId,re.getResult().getOutput());

        UserMessage userMessage = new UserMessage("What is my name?");
        memory.add(conversationId, userMessage);
        ChatResponse call = chatModel.call(new Prompt(memory.get(conversationId)));
        return call.getResult().getOutput();
    }
}
