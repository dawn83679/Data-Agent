package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;

import java.util.List;

public interface TriggerService {

    List<TriggerMetadata> getTriggers(DbContext db, String tableName);

    List<TriggerMetadata> searchTriggers(DbContext db, String tableName, String triggerNamePattern);

    String getTriggerDdl(DbContext db, String triggerName);

    void deleteTrigger(DbContext db, String triggerName);
}
