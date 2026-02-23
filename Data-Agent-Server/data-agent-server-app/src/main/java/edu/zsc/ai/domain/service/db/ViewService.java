package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

import java.util.List;

public interface ViewService {

    List<String> getViews(Long connectionId, String catalog, String schema, Long userId);

    String getViewDdl(Long connectionId, String catalog, String schema, String viewName, Long userId);

    void deleteView(Long connectionId, String catalog, String schema, String viewName, Long userId);

    TableDataResponse getViewData(Long connectionId, String catalog, String schema, String viewName, Long userId, Integer currentPage, Integer pageSize);
}
