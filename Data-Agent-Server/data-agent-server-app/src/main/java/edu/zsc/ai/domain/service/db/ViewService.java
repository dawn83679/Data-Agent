package edu.zsc.ai.domain.service.db;

import java.util.List;

public interface ViewService {

    List<String> listViews(Long connectionId, String catalog, String schema, Long userId);

    String getViewDdl(Long connectionId, String catalog, String schema, String viewName, Long userId);

    void deleteView(Long connectionId, String catalog, String schema, String viewName, Long userId);
}
