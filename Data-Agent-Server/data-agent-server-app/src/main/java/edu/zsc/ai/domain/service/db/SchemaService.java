package edu.zsc.ai.domain.service.db;

import java.util.List;

public interface SchemaService {

    List<String> listSchemas(Long connectionId, String catalog);
}
