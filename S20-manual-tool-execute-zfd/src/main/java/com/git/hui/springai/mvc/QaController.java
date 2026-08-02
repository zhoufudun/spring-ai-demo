package com.git.hui.springai.mvc;

import com.git.hui.springai.advisor.MyLoggingAdvisor;
import com.git.hui.springai.tools.QuizTools;
import com.git.hui.springai.tools.ToolResponseType;
import com.git.hui.springai.tools.WeatherTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2026/3/6
 */
@Slf4j
@RestController
public class QaController {
    private QuizTools quizTools;
    private WeatherTools weatherTools;

    private final List<ToolCallback> tools;
    private final ChatClient chatClient;

    public QaController(QuizTools quizTools, WeatherTools weatherTools, ChatClient.Builder chatClientBuilder) {
        this.quizTools = quizTools;
        this.weatherTools = weatherTools;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MyLoggingAdvisor.builder()
                                .showSystemMessage(true).showAvailableTools(true).build())
                .build();

//        ToolCallback[] t1 = MethodToolCallbackProvider.builder()
//                .toolObjects(quizTools)
//                .build()
//                .getToolCallbacks();
//        ToolCallback[] t2 = MethodToolCallbackProvider.builder()
//                .toolObjects(weatherTools)
//                .build()
//                .getToolCallbacks();
//        tools = new ArrayList<>();
//        tools.addAll(List.of(t1));
//        tools.addAll(List.of(t2));

        // 注册工具
        MethodToolCallbackProvider callbackProvider = MethodToolCallbackProvider.builder().toolObjects(quizTools).build();
        MethodToolCallbackProvider callbackProvider1 = MethodToolCallbackProvider.builder().toolObjects(weatherTools).build();

        tools = new ArrayList<>();
        tools.addAll(List.of(callbackProvider.getToolCallbacks()));
        tools.addAll(List.of(callbackProvider1.getToolCallbacks()));
    }

    /**
     * 缺点：
     *
     * ❌ 黑盒执行，难以干预
     * ❌ 无法在工具执行前后添加自定义逻辑
     * ❌ 多个工具并行执行时无法控制顺序
     * ❌ 难以记录详细的调用链路
     *
     * Spring AI 自动控制 适合：
     *
     * ✅ 快速原型开发
     * ✅ 简单的单次工具调用
     * ✅ 不需要特殊处理的场景
     * ✅ 对执行过程无特殊要求
     *
     * @param msg
     * @return
     */
    @RequestMapping(path = "executeTools")
    public String aiExecuteTools(String msg) {
        // 工具执行上下文
        Map<String, Object> toolContextData = new HashMap<>();
        toolContextData.put("sessionId", "demo-session-123");  // 示例 sessionId
        toolContextData.put("userId", "demo-user-456");        // 示例 userId
        toolContextData.put("timestamp", System.currentTimeMillis());

        // 默认的场景，由SpringAI来控制工具的执行；如果大模型一次性要求调用多个工具，Spring AI 会全部执行后再统一返回给大模型。最终由大模型组装完成的结果返回给用户
//        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
//                .internalToolExecutionEnabled(true)  // 启用自动执行
//                .toolContext(toolContextData)
//                .build();
        ToolCallingChatOptions options = ToolCallingChatOptions.builder().internalToolExecutionEnabled(true).toolContext(toolContextData).build();

        Prompt prompt = new Prompt(msg, options);
//        return chatClient.prompt(prompt).toolCallbacks(this.tools).call().content();
        return chatClient.prompt(prompt).toolCallbacks(this.tools).call().content();
    }

    /**
     * 特点：代码量增加，但完全掌控执行流程，适合复杂场景
     *
     * 手动控制 适合：
     *
     * ✅ 企业级生产环境
     * ✅ 需要权限校验的场景
     * ✅ 工具调用有先后依赖
     * ✅ 需要详细日志和监控
     * ✅ 需要限流、熔断等保护机制
     * ✅ 多轮对话中的工具调用
     *
     * @param msg
     * @return
     */
    @RequestMapping(path = "qa")
    public String qa(String msg) {
        // 工具执行上下文
        Map<String, Object> toolContextData = new HashMap<>();
        toolContextData.put("sessionId", "demo-session-123");  // 示例 sessionId
        toolContextData.put("userId", "demo-user-456");        // 示例 userId
        toolContextData.put("timestamp", System.currentTimeMillis());

        // 控制手动执行工具
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)  // 禁用自动执行，由我们手动控制
                .toolContext(toolContextData)
                .build();

        // ✅ 第 1 次调用: 获取大模型的调用意图（要调用哪个工具、参数是什么）
        Prompt prompt = new Prompt(msg, options);
        ChatResponse chatResponse = chatClient.prompt(prompt).toolCallbacks(this.tools).call().chatResponse();

        // AssistantMessage 中包含 List<ToolCall>
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
        if (CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
            // 非工具调用结果，将结果添加到上下文
            log.info("非工具调用结果：{}", assistantMessage.getText());
            return assistantMessage.getText();
        }

        // ai 返回执行工具调用，我们手动调用工具
        // 由我们主动来控制工具的执行(但是需要注意的是，不能有同名的工具，会报错)
        /**
         * 遍历所有工具调用请求
         * 根据名称匹配对应的 ToolCallback
         * 手动传入 ToolContext，携带会话、用户等信息
         * 获得工具执行结果
         */
        List<ToolResponseMessage.ToolResponse> list = new ArrayList<>();
        for (var call : assistantMessage.getToolCalls()) {
            for (ToolCallback callback : tools) {

                if (callback.getToolDefinition().name().equals(call.name())) {
                    var toolRsp = callback.call(call.arguments(), new ToolContext(toolContextData));
                    ToolResponseMessage.ToolResponse toolResponse =
                            new ToolResponseMessage.ToolResponse(call.id(), call.name(), toolRsp);
                    list.add(toolResponse);

                    if (callback instanceof MethodToolCallback) {
                        var target = ((MethodToolCallback) callback);
                        // 获取 toolMethod
                        Field field = ReflectionUtils.findField(target.getClass(), "toolMethod");
                        field.setAccessible(true);
                        var method = (Method) ReflectionUtils.getField(field, target);
                        if (method != null) {
                            var rspType = method.getDeclaredAnnotation(ToolResponseType.class);
                            log.info("工具方法定义信息：{}", rspType);
                        }
                    }

                    // 我们还可以通过反射的方式，获取工具上通过自定义注解维护的信息
                    log.info("工具定义信息：{}", callback.getToolDefinition().description());
                }
            }
        }


        // ✅ 第 2 次调用：将工具结果返回给大模型，让它总结
        /**
         * 构建完整的对话历史
         * 让大模型基于工具结果生成自然语言响应
         * 保证上下文的连贯性
         * #四、两种方案深
         */
        ToolResponseMessage toolMsg = ToolResponseMessage.builder().responses(list).build();

        ChatResponse finalResponse = chatClient.prompt(
                        new Prompt(List.of(
                                new UserMessage(msg),        // 用户原始问题
                                assistantMessage,            // AI 的工具调用请求
                                toolMsg                      // 工具执行结果
                        )))
                .call()
                .chatResponse();

        // 返回大模型的最终总结
        return finalResponse.getResult().getOutput().getText();
    }
}

/**
 *
 * ToolContext
 *
 * 场景 1：多用户会话隔离Map<String, Object> toolContextData = new HashMap<>();
 * toolContextData.put("sessionId", "session-123");
 * toolContextData.put("userId", "user-456");
 * toolContextData.put("tenantId", "tenant-789");  // 多租户 ID
 *
 * // 工具内部可以根据这些信息做数据隔离
 * @Tool
 * public WeatherCard queryWeather(String city, ToolContext toolContext) {
 *     String tenantId = toolContext.get("tenantId");
 *     String userId = toolContext.get("userId");
 *
 *     // 查询该租户下该用户的天气偏好
 *     UserPreference preference = preferenceRepository
 *             .findByTenantAndUser(tenantId, userId);
 *
 *     // ... 使用偏好定制天气报告
 * }
 * # 场景 2：审计日志toolContextData.put("requestId", UUID.randomUUID().toString());
 * toolContextData.put("timestamp", System.currentTimeMillis());
 * toolContextData.put("sourceIp", request.getRemoteAddr());
 *
 * // 工具执行时记录审计日志
 * @Tool
 * public QuizCard createQuiz(String topic, ToolContext toolContext) {
 *     String requestId = toolContext.get("requestId");
 *
 *     log.info("【审计日志】RequestId: {}, 用户：{}, 创建题目：{}",
 *              requestId, toolContext.get("userId"), topic);
 *
 *     // ... 业务逻辑
 * }
 * # 场景 3：链路追踪// 在微服务架构中传递追踪信息
 * toolContextData.put("traceId", MDC.get("traceId"));
 * toolContextData.put("spanId", UUID.randomUUID().toString());
 *
 * // 工具调用其他服务时携带这些信息
 * @Tool
 * public WeatherCard queryWeather(String city, ToolContext toolContext) {
 *     String traceId = toolContext.get("traceId");
 *
 *     // 调用天气 API 时传递 traceId
 *     return weatherApiClient.getWeather(city, traceId);
 * }
 * ------
 * 原文链接：https://ppai.top/ai-guides/ai-dev/%E5%9F%BA%E7%A1%80%E7%AF%87/19.Function%20Calling%E8%BF%9B%E9%98%B6%E4%BD%BF%E7%94%A8.html#_5-2-%E5%85%B8%E5%9E%8B%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF
 *
 */
