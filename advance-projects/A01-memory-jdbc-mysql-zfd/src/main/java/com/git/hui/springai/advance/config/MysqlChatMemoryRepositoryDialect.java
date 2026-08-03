package com.git.hui.springai.advance.config;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

/**
 * 1、若需要调整表名、字段名，替换下面的这个类，然后注册到 JdbcChatMemoryRepository 中即可
 * 2、重点说明：换表名、字段名还好，如果我想额外存储用户的token使用情况，可行吗？ 从源码来看，不可行，上面的 Dialect 中的insert，已经绑定了字段，无法进行动态扩展
 *       原因：JdbcChatMemoryRepository 类写死了 保存的时候保存哪些字段
 */
public class MysqlChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {
    @Override
    public String getSelectMessagesSql() {
        return "SELECT content, type FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ? ORDER BY `timestamp`";
    }

    @Override
    public String getInsertMessageSql() {
        return "INSERT INTO SPRING_AI_CHAT_MEMORY (conversation_id, content, type, `timestamp`) VALUES (?, ?, ?, ?)";
    }

    @Override
    public String getSelectConversationIdsSql() {
        return "SELECT DISTINCT conversation_id FROM SPRING_AI_CHAT_MEMORY";
    }

    @Override
    public String getDeleteMessagesSql() {
        return "DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?";
    }
}
