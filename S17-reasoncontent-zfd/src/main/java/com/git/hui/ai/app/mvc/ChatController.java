package com.git.hui.ai.app.mvc;

import io.micrometer.common.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author YiHui
 * @date 2025/8/26
 */
@RestController
public class ChatController {
    /**
     * 阿里的百炼模型
     */
    private final ChatModel dashModel;


    /**
     * 智谱模型
     */
    private final ChatModel zhipuModel;

    public ChatController(Environment environment) {
        // 通过手动的方式，注册 阿里百炼模型
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(getApiKey(environment, "dash-api-key"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .completionsPath("/v1/chat/completions")
                .build();
//        OpenAiApi.builder().apiKey(getApiKey(environment, "dash-api-key"))
//                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
//                .completionsPath("/v1/chat/completions")
//                .build();

        dashModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-plus-latest")
                        .extraBody(Map.of("enable_thinking", true))
                        .build())
                .build();

//        OpenAiChatModel.builder()
//                .openAiApi(openAiApi)
//                .defaultOptions(OpenAiChatOptions.builder().model("qwen-plus-latest").extraBody(Map.of("enable_thinking",true)).build())
//                .build();

        OpenAiApi zhipuApi = OpenAiApi.builder().apiKey(getApiKey(environment, "zhipuai-api-key"))
                .baseUrl("https://open.bigmodel.cn")
                .completionsPath("/api/paas/v4/chat/completions")
                .build();
        zhipuModel = OpenAiChatModel.builder()
                .openAiApi(zhipuApi)
                .defaultOptions(OpenAiChatOptions.builder().model("glm-4.5-flash")
//                        支持思考推理（前提是大模型本身要支持这个能力）
//                        默认是开启推理，可以使用下面的方式关闭
//                        .extraBody(Map.of("thinking", Map.of("type", "disabled")))
                        .build())
                .build();

    }

    private String getApiKey(Environment environment, String key) {
        // 1. 通过 --dash-api-key 启动命令传参
        String val = environment.getProperty(key);
        if (StringUtils.isBlank(val)) {
            // 2. 通过jvm传参 -Ddash-api-key=
            val = System.getProperty(key);
            if (val == null) {
                // 3. 通过环境变量传参
                val = System.getenv(key);
            }
        }
        return val;
    }

    /**
     * 阿里百炼模型
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "aliChatWithThinking", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatV5(String msg) {
//        SseEmitter sseEmitter = new SseEmitter();
//        Flux<ChatResponse> res = dashModel.stream(new Prompt(new UserMessage(msg)));
//        StringBuilder content = new StringBuilder();
//        StringBuilder reason = new StringBuilder();
//        res.doOnComplete(() -> {
//                    sseEmitter.complete();
//                    System.out.println("思考过程:" + reason);
//                    System.out.println("结果:" + content);
//                })
//                .subscribe(txt -> {
//                    Generation generation = txt.getResult();
//
//                    var r = generation.getOutput().getMetadata().get("reasoningContent");
//                    if (r != null) {
//                        reason.append(r);
//                    }
//
//                    var t = generation.getOutput().getText();
//                    if (t != null) {
//                        content.append(t);
//                    }
//                    try {
//                        sseEmitter.send("思考:" + reason + "===>\n<br/>\n==>" + content);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//        return sseEmitter;
        SseEmitter sseEmitter = new SseEmitter();
        Flux<ChatResponse> responseFlux = dashModel.stream(new Prompt(msg));
        StringBuilder content = new StringBuilder();
        StringBuilder reason = new StringBuilder();
        responseFlux.doOnComplete(sseEmitter::complete).subscribe(new Consumer<ChatResponse>() {
            @Override
            public void accept(ChatResponse chatResponse) {
                Generation result = chatResponse.getResult();
                var r = result.getOutput().getMetadata().get("reasoningContent");
                if (r != null) {
                    reason.append(r);
                }
                var t = result.getOutput().getText();
                if (t != null) {
                    content.append(t);
                }
                try {
                    sseEmitter.send("thinking: " + reason + "\n\r" + content);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return sseEmitter;
    }

    /**
     * 智谱模型
     *
     * @param msg
     * @return
     */
    @GetMapping(path = "zhipuChat")
    public Map zhipuChat(String msg) {
        ChatClient client = ChatClient.builder(zhipuModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        Flux<ChatResponse> res = client.prompt(new Prompt(msg)).stream().chatResponse();
        StringBuilder content = new StringBuilder();
        StringBuilder reason = new StringBuilder();
//        ChatResponse response = res.doOnComplete(() -> {
//            System.out.println("思考过程:" + reason);
//            System.out.println("结果:" + content);
//        }).doOnNext(txt -> {
//            Generation generation = txt.getResult();
//            var r = generation.getOutput().getMetadata().get("reasoningContent");
//            if (r != null) {
//                reason.append(r);
//                System.out.println("思考:" + r);
//            }
//            var t = generation.getOutput().getText();
//            if (t != null) {
//                content.append(t);
//                System.out.println("结果:" + t);
//            }
//        }).blockLast();
//        // token使用
//        var usage = response.getMetadata().getUsage();
//        // 构建完成的返回结果
//        return Map.of("思考过程", reason, "结果", content, "token消耗", usage);

        ChatResponse chatResponse = res.doOnComplete(new Runnable() {
            @Override
            public void run() {
                System.out.println("思考过程:" + reason);
                System.out.println("结果:" + content);
            }
        }).doOnNext(new Consumer<ChatResponse>() {
            @Override
            public void accept(ChatResponse chatResponse) {
                Generation result = chatResponse.getResult();
                Object reasoningContent = result.getOutput().getMetadata().get("reasoningContent");
                if (reasoningContent != null) {
                    reason.append(reasoningContent);
                    System.out.println("思考:" + reasoningContent);
                }
                String output = result.getOutput().getText();
                if (output != null) {
                    content.append(output);
                    System.out.println("结果:" + output);
                }
            }
        }).blockLast();// 阻塞
        // token使用
        var usage = chatResponse.getMetadata().getUsage();
        // 构建完成的返回结果
        return Map.of("思考过程", reason, "结果", content, "token消耗", usage);
    }

    @GetMapping(path = "zhipuChatV2")
    public Map zhipuChatV2(String msg) {
//        ChatClient client = ChatClient.builder(zhipuModel)
//                .defaultAdvisors(new SimpleLoggerAdvisor())
//                .build();
//        ChatResponse response = client.prompt(new Prompt(msg)).call().chatResponse();
//        var reason = response.getResult().getOutput().getMetadata().get("reasoningContent");
//        var content = response.getResult().getOutput().getText();
//
//        // token使用
//        var usage = response.getMetadata().getUsage();
//        // 构建完成的返回结果
//        return Map.of("思考过程", reason == null ? "" : reason, "结果", content, "token消耗", usage);

        /**
         * 无法返回思考过程：主要原因是推理过程需要是stream()方式调用
         */
        ChatClient chatClient = ChatClient.builder(zhipuModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();
        ChatResponse chatResponse = chatClient.prompt(new Prompt(msg)).call().chatResponse();
        Object reasoningContent = chatResponse.getResult().getOutput().getMetadata().get("reasoningContent");
        String content = chatResponse.getResult().getOutput().getText();

        Usage usage = chatResponse.getMetadata().getUsage();
        return Map.of("思考过程", reasoningContent == null ? "" : reasoningContent, "结果", content, "token消耗", usage);
    }
}
