package com.git.hui.springai.ali.writer;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author YiHui
 * @date 2026/3/17
 */
@Component
public class WriterAgent_zfd {

    private static final String instruction = """
            你是一个知名的作家，擅长用生动、易懂的语言创作文章。
            
            ---
            请根据大纲进行完整的内容创作。大纲内容如下：
            {online}
            
            ---
            直接返回文章内容，不要包含任何额外说明。
            用户的提问是: 
            {input}
            """;
    @Autowired
    private ChatModel chatModel;

    /**
     * OverAllState 自动管理，无需手动创建
     * Agent 通过 outputKey 将输出存入状态
     * 后续 Agent 通过占位符 {key} 引用状态中的值
     *
     * Instruction 占位符: Agent 之间的数据传递通过 Instruction 占位符{xxx} 实现：
     * @return
     */
    public ReactAgent writerAgent() {
//        ReactAgent agent = ReactAgent.builder()
//                .name("write_agent")
//                .model(chatModel)
//                .description("专业写作Agent")
//                .instruction(instruction)
//                .outputKey("article_draft") // 这个agent的输出结果放在这个key下，这个key放在全局OverAllState下
//                .includeContents(false)
//                .returnReasoningContents(false)
//                .enableLogging(true)
//                .build();
//        return agent;


        return ReactAgent.builder()
                .model(chatModel)
                .name("write_agent")
                .enableLogging(true)
                .instruction(instruction)
                .description("专业写作Agent")
                .outputKey("article_draft") // 这个agent的输出结果放在这个key下，这个key放在全局OverAllState下
                .includeContents(false) //?? 什么作用
                .returnReasoningContents(false)
                .build();
    }
}
