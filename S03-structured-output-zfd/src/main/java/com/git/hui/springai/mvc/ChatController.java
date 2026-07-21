package com.git.hui.springai.mvc;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author YiHui
 * @date 2025/7/11
 */
@RestController
public class ChatController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final ZhiPuAiChatModel chatModel;

    @Autowired
    public ChatController(ZhiPuAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @JsonPropertyOrder({"actor", "movies"})
    record ActorsFilms(String actor, List<String> movies) {
    }

    /**
     * 基于ChatClient实现返回结果的结构化映射
     *
     * @param actor
     * @return
     */
    @GetMapping("/ai/generate")
    public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        PromptTemplate template = new PromptTemplate("帮我返回五个{actor}导演的电影名，要求中文返回");
        Prompt prompt = template.create(Map.of("actor", actor));
        ChatClient.CallResponseSpec res = ChatClient.create(chatModel).prompt(prompt).call();
        ActorsFilms films = res.entity(ActorsFilms.class);
        return films;
    }

    @GetMapping("/ai/generate2")
    public ActorsFilms generate2(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        PromptTemplate promptTemplate = PromptTemplate.builder().renderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<').endDelimiterToken('>').build())
                .template("帮我返回五个<actor>导演的电影名，要求中文返回")
                .build();
        Prompt prompt = promptTemplate.create(Map.of("actor", actor));
        ChatClient chatClient = ChatClient.create(chatModel);
        ChatClient.CallResponseSpec call = chatClient.prompt(prompt).call();
        return call.entity(ActorsFilms.class);
    }

    /**
     * 基于BeanOutputConverter实现返回结果结构化映射
     *
     * @param actor
     * @return
     */
    @GetMapping("/ai/gen2")
    public ActorsFilms gen2(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        BeanOutputConverter<ActorsFilms> beanOutputConverter = new BeanOutputConverter<>(ActorsFilms.class);
        String format = beanOutputConverter.getFormat();

        PromptTemplate template = new PromptTemplate("""
                    帮我返回五个{actor}导演的电影名
                    {format}
                """);
        Prompt prompt = template.create(Map.of("actor", actor, "format", format));
        Generation generation = chatModel.call(prompt).getResult();
        if (generation == null) {
            return null;
        }
        System.out.println(generation.getOutput().getText());
        return beanOutputConverter.convert(generation.getOutput().getText());
    }

    /**
     * 基于BeanOutputConverter实现返回结果结构化映射2
     *
     * @param actor
     * @return
     */
    @GetMapping("/ai/gen3")
    public ActorsFilms gen3(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        BeanOutputConverter<ActorsFilms> beanOutputConverter = new BeanOutputConverter<>(ActorsFilms.class);
        String format = beanOutputConverter.getFormat(); // 直接在提示词中添加了结构化的返回结果格式，因此，大模型返回的数据结构，会按照这个格式进行解析
        PromptTemplate template = new PromptTemplate("帮我返回五个{actor}导演的电影名 {format} ");
        Prompt prompt = template.create(Map.of("actor", actor, "format", format));

        Generation generation = chatModel.call(prompt).getResult();
        if (generation == null) {
            return null;
        }
        return beanOutputConverter.convert(generation.getOutput().getText());
    }

    @GetMapping("/ai/genList")
    public List<ActorsFilms> genList(@RequestParam(value = "actor1", defaultValue = "周星驰") String actor1,
                                     @RequestParam(value = "actor2", defaultValue = "张艺谋") String actor2) {
        List<ActorsFilms> actorsFilms = ChatClient.create(chatModel).prompt()
                .user(u ->
                        u.text("帮我返回五个{actor1}和{actor2}导演的电影名，要求中文返回")
                                .params(Map.of("actor1", actor1, "actor2", actor2)))
                .call()
                .entity(new ParameterizedTypeReference<List<ActorsFilms>>() {
                });
        return actorsFilms;
    }

    @GetMapping("/ai/genMap")
    public Map genMap(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        Map<String, Object> actorsFilms = ChatClient.create(chatModel).prompt()
                .user(u ->
                        u.text("帮我返回五个{actor}导演的电影名，要求中文返回")
                                .param("actor", actor))
                .call()
                .entity(new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return actorsFilms;
    }

    @GetMapping("/ai/genMap3")
    public Map genMap3(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        Map<String, Object> map = ChatClient.create(chatModel).prompt().user(new Consumer<ChatClient.PromptUserSpec>() {
            @Override
            public void accept(ChatClient.PromptUserSpec promptUserSpec) {
                promptUserSpec.text("帮我返回五个{actor}导演的电影名，要求中文返回").param("actor", actor);
            }
        }).call().entity(new ParameterizedTypeReference<Map<String, Object>>() {
        });
        return map;
    }

    @GetMapping("/ai/genMap2")
    public Map genMap2(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        String format = mapOutputConverter.getFormat();
        PromptTemplate template = new PromptTemplate("""
                    帮我返回五个{actor}导演的电影名，要求中文返回
                    {format}
                """);
        Prompt prompt = template.create(Map.of("actor", actor, "format", format));
        Generation generation = chatModel.call(prompt).getResult();
        Map<String, Object> result = mapOutputConverter.convert(generation.getOutput().getText());
        return result;
    }

    @GetMapping("/ai/genMap4")
    public Map genMap4(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        MapOutputConverter mapOutputConverter = new MapOutputConverter();
        String format = mapOutputConverter.getFormat();
        PromptTemplate template = new PromptTemplate("帮我返回五个{actor}导演的电影名，要求中文返回, {format}");

        Prompt prompt = template.create(Map.of("actor", actor, "format", format));

        Generation result = chatModel.call(prompt).getResult();

        return mapOutputConverter.convert(result.getOutput().getText());
    }

    @GetMapping("/ai/genList1")
    public List<String> genList1(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        List<String> actorsFilms = ChatClient.create(chatModel).prompt()
                .user(u ->
                        u.text("帮我返回五个{actor}导演的电影名，要求中文返回")
                                .param("actor", actor))
                .call()
                .entity(new ListOutputConverter(new DefaultConversionService()));
        return actorsFilms;
    }

    @GetMapping("/ai/genList2")
    public List genList2(@RequestParam(value = "actor", defaultValue = "周星驰") String actor) {
        ListOutputConverter listOutputConverter = new ListOutputConverter(new DefaultConversionService());

        String format = listOutputConverter.getFormat();
        PromptTemplate template = new PromptTemplate("""
                    帮我返回五个{actor}导演的电影名，要求中文返回
                    {format}
                """);
        Prompt prompt = template.create(Map.of("actor", actor, "format", format));
        Generation generation = chatModel.call(prompt).getResult();
        List<String> result = listOutputConverter.convert(generation.getOutput().getText());
        return result;
    }
}