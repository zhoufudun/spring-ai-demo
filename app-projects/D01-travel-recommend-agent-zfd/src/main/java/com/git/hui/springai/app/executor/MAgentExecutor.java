package com.git.hui.springai.app.executor;

import com.git.hui.springai.app.agents.*;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * @author YiHui
 * @date 2025/8/12
 */
@Service
public class MAgentExecutor {
    private static final Logger log = LoggerFactory.getLogger(MAgentExecutor.class);

    private final WeatherAgent_zfd weatherAgent;
    private final TravelRecommendAgent_zfd travelAgent;
    private final FoodRecommendAgent_zfd foodAgent;
    private final XhsBlogGenerateAgent_zfd xhsBlogGenerateAgent;

    private final CompiledGraph<TravelState> compiledGraph;

    public MAgentExecutor(WeatherAgent_zfd weatherAgent, TravelRecommendAgent_zfd travelAgent, FoodRecommendAgent_zfd foodAgent, XhsBlogGenerateAgent_zfd xhsBlogGenerateAgent) throws GraphStateException {
        this.weatherAgent = weatherAgent;
        this.travelAgent = travelAgent;
        this.foodAgent = foodAgent;
        this.xhsBlogGenerateAgent = xhsBlogGenerateAgent;
        this.compiledGraph = new GraphBuilder().build().compile();
        this.printPlantUml();
    }

    public CompiledGraph<TravelState> getCompiledGraph() {
        return compiledGraph;
    }

    public TravelState invoke(String input) {
        return getCompiledGraph().invoke(Map.of(TravelState.INPUT, input)).orElseGet(() -> new TravelState(Map.of("Error", "NoDataResponse")));
    }

    public class GraphBuilder {

//        public StateGraph<TravelState> build() throws GraphStateException {
//            var shouldContinue = (EdgeAction<TravelState>) state -> {
//                log.info("shouldContinue RecommendAgent state: {}", state);
//                // 如果输入中，要求我们进行推荐，则继续进行旅游项目和食品的推荐
//                return state.getInput().contains("recommendations") || state.getInput().contains("推荐") ? "recommend" : "xhs";
//            };
//

        /// /            return new StateGraph<>(TravelState.SCHEMA, TravelState::new)
//            return new StateGraph<>(TravelState.serializer())
//                    .addNode(WeatherAgent.NAME, node_async(weatherAgent::callWeatherAgent))
//                    .addNode(TravelRecommendAgent.NAME, node_async(travelAgent::callTravelAgent))
//                    .addNode(FoodRecommendAgent.NAME, node_async(foodAgent::callFoodAgent))
//                    .addNode(XhsBlogGenerateAgent.NAME, node_async(xhsBlogGenerateAgent::callResponseAgent))
//                    .addEdge(START, WeatherAgent.NAME)
//                    .addConditionalEdges(WeatherAgent.NAME,
//                            edge_async(shouldContinue),
//                            Map.of(
//                                    "recommend", TravelRecommendAgent.NAME,
//                                    "xhs", XhsBlogGenerateAgent.NAME
//                            )
//                    )
//                    .addEdge(TravelRecommendAgent.NAME, FoodRecommendAgent.NAME)
//                    .addEdge(FoodRecommendAgent.NAME, XhsBlogGenerateAgent.NAME)
//                    .addEdge(XhsBlogGenerateAgent.NAME, END);
//        }
        public StateGraph<TravelState> build() throws GraphStateException {

            return new StateGraph<TravelState>(TravelState.serializer())
                    .addNode(WeatherAgent_zfd.NAME, AsyncNodeAction.node_async(new NodeAction<TravelState>() {
                        @Override
                        public Map<String, Object> apply(TravelState travelState) throws Exception {
                            return weatherAgent.callWeatherAgent(travelState);
                        }
                    }))
                    .addNode(TravelRecommendAgent_zfd.NAME, AsyncNodeAction.node_async(travelAgent::callTravelAgent))
                    .addNode(FoodRecommendAgent_zfd.NAME, node_async(foodAgent::callFoodAgent))
                    .addNode(XhsBlogGenerateAgent_zfd.NAME, node_async(xhsBlogGenerateAgent::callResponseAgent))

                    .addEdge(START, WeatherAgent_zfd.NAME)

                    .addConditionalEdges(WeatherAgent_zfd.NAME, AsyncEdgeAction.edge_async(travelState -> {
                        log.info("shouldContinue RecommendAgent state: {}", travelState);
                        // 如果输入中，要求我们进行推荐，则继续进行旅游项目和食品的推荐
                        return travelState.getInput().contains("recommendations") || travelState.getInput().contains("推荐") ? "recommend" : "xhs";
                    }), Map.of("recommend", TravelRecommendAgent_zfd.NAME, "xhs", XhsBlogGenerateAgent.NAME))

                    .addEdge(TravelRecommendAgent_zfd.NAME, FoodRecommendAgent_zfd.NAME)
                    .addEdge(FoodRecommendAgent_zfd.NAME, XhsBlogGenerateAgent_zfd.NAME)
                    .addEdge(XhsBlogGenerateAgent_zfd.NAME, END);
        }
    }


    /**
     * 打印 plantUml 格式流程图
     *
     * @return
     */
    private String printPlantUml() {
        // 在线 mermaid绘制地址：https://mermaid.live/
//        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID, "TravelRecommendAgent", true);

        // 在线uml绘制地址： https://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa700002
        GraphRepresentation representation = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML, "TravelRecommendAgent", true);

        GraphRepresentation graph = compiledGraph.getGraph(GraphRepresentation.Type.PLANTUML, "TravelRecommendAgent", true);

        // 获取 PlantUML 文本
        System.out.println(">>>>>>>>>>>> online uml render site:  https://www.plantuml.com/plantuml/uml/SyfFKj2rKt3CoKnELR1Io4ZDoSa700002");
        System.out.println("=== PlantUML Start ===");
        System.out.println(representation.content());
        System.out.println("------- PlantUML End ---------");
        return representation.content();
    }
}
