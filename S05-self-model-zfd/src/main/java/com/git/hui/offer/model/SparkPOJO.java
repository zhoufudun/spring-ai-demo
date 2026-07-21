package com.git.hui.offer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/7/21
 */
public interface SparkPOJO {
    /**
     * 大模型真实返回结果
     * {
     *     "code": 0,
     *     "message": "Success",
     *     "sid": "cha000b180a@dx1982ba483e7b8f2532",
     *     "choices": [
     *         {
     *             "message": {
     *                 "role": "assistant",
     *                 "content": "我乃李白，字太白，号青莲居士。生于唐朝，自幼好学，才情出众。诗仙之名，非我莫属。我游历四方，饮酒作诗，以抒胸中豪情壮志。我行吟于山水之间，挥洒自如，诗篇流传千古，被后人传颂。"
     *             },
     *             "index": 0
     *         }
     *     ],
     *     "usage": {
     *         "prompt_tokens": 16,
     *         "completion_tokens": 72,
     *         "total_tokens": 88
     *     }
     * }
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true
    )
    record ChatCompletionChunk(
            // 错误码 0 成功
            Integer code,
            // 错误码的描述信息
            String message,
            // 本次请求的唯一id
            String sid,
            // 大模型返回结果
            List<Choice> choices,
            // 本次请求的消耗信息
            Usage usage) {
    }

    record Choice(Integer index, SparkMsg message) {
    }

    record SparkMsg(String role, String content) {
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(
            ignoreUnknown = true
    )
    record Usage(Integer completionTokens, Integer promptTokens,
                 Integer totalTokens) implements org.springframework.ai.chat.metadata.Usage {
        public Usage(@JsonProperty("completion_tokens") Integer completionTokens, @JsonProperty("prompt_tokens") Integer promptTokens, @JsonProperty("total_tokens") Integer totalTokens) {
            this.completionTokens = completionTokens;
            this.promptTokens = promptTokens;
            this.totalTokens = totalTokens;
        }

        @JsonProperty("completion_tokens")
        public Integer completionTokens() {
            return this.completionTokens;
        }

        @JsonProperty("prompt_tokens")
        public Integer promptTokens() {
            return this.promptTokens;
        }

        @JsonProperty("total_tokens")
        public Integer totalTokens() {
            return this.totalTokens;
        }

        @Override
        public Integer getPromptTokens() {
            return promptTokens;
        }

        @Override
        public Integer getCompletionTokens() {
            return completionTokens;
        }

        @Override
        public Object getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", this.promptTokens());
            usage.put("completionTokens", this.completionTokens());
            usage.put("totalTokens", this.totalTokens());
            return usage;
        }
    }
}
