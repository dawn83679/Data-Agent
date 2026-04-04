package edu.zsc.ai.config.ai;

import java.util.Collections;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

@Configuration
@EnableConfigurationProperties({MemoryProperties.class, AiEmbeddingProperties.class})
public class EmbeddingStoreConfig {

    private static final String EMBEDDING_TABLE = "ai_memory_embedding";
    private static final int INDEX_LIST_SIZE = 100;

    @Bean
    public EmbeddingStore<TextSegment> memoryEmbeddingStore(DataSource dataSource,
                                                            AiEmbeddingProperties embeddingProperties) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(EMBEDDING_TABLE)
                .dimension(embeddingProperties.getDimension())
                .createTable(true)
                .useIndex(true)
                .indexListSize(INDEX_LIST_SIZE)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(Collections.singletonList("metadata JSONB NULL"))
                        .indexes(Collections.emptyList())
                        .build())
                .build();
    }
}
