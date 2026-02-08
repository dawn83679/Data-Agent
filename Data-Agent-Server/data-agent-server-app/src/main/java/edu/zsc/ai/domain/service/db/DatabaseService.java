package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.util.exception.BusinessException;

import java.util.List;

public interface DatabaseService {

    List<String> listDatabases(Long connectionId);
}
