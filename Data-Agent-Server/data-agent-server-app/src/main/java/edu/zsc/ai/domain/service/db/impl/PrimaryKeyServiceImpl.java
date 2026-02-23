package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.domain.service.db.PrimaryKeyService;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import edu.zsc.ai.plugin.model.metadata.PrimaryKeyMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrimaryKeyServiceImpl implements PrimaryKeyService {

    private final IndexService indexService;

    @Override
    public List<PrimaryKeyMetadata> listPrimaryKeys(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        List<IndexMetadata> indexes = indexService.getIndexes(connectionId, catalog, schema, tableName, userId);
        return indexes.stream()
                .filter(IndexMetadata::isPrimaryKey)
                .map(idx -> new PrimaryKeyMetadata(idx.name(), idx.columns()))
                .toList();
    }
}
