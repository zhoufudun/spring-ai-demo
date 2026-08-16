package com.git.hui.springai.app.vectorstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStoreContent;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.filter.converter.SimpleVectorStoreFilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.core.io.Resource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

import javax.print.Doc;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/**
 * 基于文本匹配的VectorStore实现，不需要向量模型
 * 使用关键词匹配和文本相似度算法替代向量嵌入，用于简易的RAG验证场景
 */
public class TextBasedVectorStore_zfd extends AbstractObservationVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(TextBasedVectorStore_zfd.class);
    private final ObjectMapper objectMapper = ((JsonMapper.Builder) JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules())).build();
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final FilterExpressionConverter filterExpressionConverter = new SimpleVectorStoreFilterExpressionConverter();
    @Getter
    protected Map<String, SimpleVectorStoreContent> store = new ConcurrentHashMap();

    /**
     * 已经存储到向量库的document，用于幂等
     */
    private Set<String> persistMd5 = new CopyOnWriteArraySet<>();

    /**
     * 添加文档到向量数据库
     *
     * @param documents
     */
    @Override
    public void doAdd(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        // 创建一个新的可变列表副本
        List<Document> mutableDocuments = new ArrayList<>();
        for(Document document:documents){
            // 过滤掉重复的文档，避免二次写入，浪费空间
            if(!persistMd5.contains(document.getMetadata().get("md5"))){
                mutableDocuments.add(document);
            }
        }

        if (CollectionUtils.isEmpty(mutableDocuments)) {
            return;
        }

        // 文档分片


    }

    @Override
    public void doDelete(List<String> idList) {

    }

    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        return List.of();
    }

    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        return null;
    }

    /**
     * 已经存储到向量库的document
     */






    /**
     * 搜索向量数据库，根据相似度返回相关文档
     *
     * @param request
     * @return
     */



}