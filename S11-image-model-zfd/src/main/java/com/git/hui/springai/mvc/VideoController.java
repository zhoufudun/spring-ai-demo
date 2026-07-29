package com.git.hui.springai.mvc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class VideoController {

    private static final String VIDEO_API_URL = "https://open.bigmodel.cn/api/paas/v4/videos/generations";

    private final RestTemplate restTemplate;

    @Value("${spring.ai.zhipuai.api-key}")
    private String apiKey;

    @Value("${spring.ai.zhipuai.video.options.model:cogvideox-flash}")
    private String videoModel;

    public VideoController() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 提交视频生成任务（异步），返回 task_id
     */
    @PostMapping("/genVideo")
    public Map<String, Object> genVideo(@RequestParam String msg) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", videoModel,
                "prompt", msg
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(VIDEO_API_URL, request, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = response.getBody();
        return result;
    }

    /**
     * 查询视频生成任务结果
     */
    @GetMapping("/genVideo/result")
    public Map<String, Object> genVideoResult(@RequestParam String id) {
        String url = VIDEO_API_URL + "/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = response.getBody();
        return result;
    }
}
