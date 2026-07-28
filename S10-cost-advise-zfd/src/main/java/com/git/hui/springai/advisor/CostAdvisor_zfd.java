package com.git.hui.springai.advisor;

import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * @author YiHui
 * @date 2025/8/4
 */
public class CostAdvisor_zfd implements CallAdvisor, StreamAdvisor {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CostAdvisor_zfd.class);


    @Override
    public String getName() {
        return "costAdvisor";
    }

    @Override
    public int getOrder() {
        // 指定最高优先级
        return Integer.MIN_VALUE;
    }

    //  同步调用
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        long start = System.currentTimeMillis();

        chatClientRequest.context().put("start-time", start);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        long end = System.currentTimeMillis();
        long cost = end - start;

        chatClientResponse.context().put("end-time", end);
        chatClientResponse.context().put("cost-time", cost);
        log.info("Prompt call cost: {} ms", cost);
        return chatClientResponse;
    }

    // 异步调用
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        long start = System.currentTimeMillis();
        chatClientRequest.context().put("start-time", start);

        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(chatClientRequest);
        return new ChatClientMessageAggregator().aggregateChatClientResponse(responseFlux, new Consumer<ChatClientResponse>() {
            @Override
            public void accept(ChatClientResponse chatClientResponse) {
                long end = System.currentTimeMillis();
                long cost = end - start;
                // 添加耗时
                chatClientResponse.context().put("end-time", end);
                chatClientResponse.context().put("cost-time", cost);
                log.info("Prompt stream cost: {} ms", cost);
            }
        });
    }
}
