package com.git.hui.springai.app.agents;

import com.git.hui.springai.app.executor.TravelState;
import com.git.hui.springai.app.service.AgentService;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class TravelRecommendAgent_zfd {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TravelRecommendAgent_zfd.class);
    public static final String NAME = "travelAgent";
    private final AgentService agentService;

    public TravelRecommendAgent_zfd(AgentService agentService) {
        this.agentService = agentService;
    }

    public Map<String, Object> callTravelAgent(TravelState state) {
        // 用户原始输入信息
        var area = state.getInput();
        // WeatherAgent执行结果
        var weather = state.getWeather();
//        log.info("TravelRecommendAgent_zfd: data="+state.getAll());
        log.info("[callTravelAgent]: {}, weather: {}", state, weather);

        String prompts = """
                请结合现在的天气，帮我推荐三个适合这个天气游玩的项目{area}{weather}""";
        PromptTemplate promptTemplate = new PromptTemplate(prompts);
        String render = promptTemplate.render(Map.of("area", area, "weather", weather));
        var recommendation = agentService.travel(render);

        Map<String, Object> travel = Map.of(TravelState.TRAVEL, recommendation);
        log.info("[callTravelAgent] Output: {}", travel);
        return travel;
    }
}
