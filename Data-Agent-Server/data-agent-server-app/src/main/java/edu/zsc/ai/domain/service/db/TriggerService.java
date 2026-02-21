package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;

import java.util.List;

public interface TriggerService {

    List<TriggerMetadata> listTriggers(Long connectionId, String catalog, String schema, String tableName, Long userId);

    String getTriggerDdl(Long connectionId, String catalog, String schema, String triggerName, Long userId);

    void deleteTrigger(Long connectionId, String catalog, String schema, String triggerName, Long userId);
}
