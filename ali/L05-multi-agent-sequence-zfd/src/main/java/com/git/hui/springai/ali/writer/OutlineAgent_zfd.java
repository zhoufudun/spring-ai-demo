package com.git.hui.springai.ali.writer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 大纲创作agent
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Service
public class OutlineAgent_zfd {
    private static final String PROMPT = """
            你是一位经验丰富的文章大纲设计师。请根据用户提供的主题，生成一份清晰、有逻辑的文章大纲。
            大纲应包含主要章节和每个章节的核心要点，以帮助后续写作。
            用户主题：{input}
            请直接返回大纲内容，不要包含额外解释。
            """;
    @Autowired
    private ChatModel chatModel;

    public ReactAgent outlineAgent() {
        ReactAgent agent = ReactAgent.builder()
                .name("outline_agent")
                .model(chatModel)
                .description("大纲生成Agent")
                .instruction(PROMPT)
                .outputKey("outline") // 存放输出结果的key
                .includeContents(false) // includeContents 用来控制当前子 Agent 执行时，是只基于自己的 instruction 给到的内容工作，还是会带上所有父流程的上下文。
                .returnReasoningContents(false) // 不返回推理过程
                .enableLogging(true)
                .build();
        return agent;
    }
}
