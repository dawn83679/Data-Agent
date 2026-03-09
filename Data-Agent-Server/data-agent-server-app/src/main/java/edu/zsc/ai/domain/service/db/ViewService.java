package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.TableDataResponse;

import java.util.List;

public interface ViewService {

    List<String> getViews(DbContext db);

    List<String> searchViews(DbContext db, String viewNamePattern);

    long countViews(DbContext db, String viewNamePattern);

    long countViewRows(DbContext db, String viewName);

    String getViewDdl(DbContext db, String viewName);

    void deleteView(DbContext db, String viewName);

    TableDataResponse getViewData(DbContext db, String viewName, Integer currentPage, Integer pageSize);

    TableDataResponse getViewData(DbContext db, String viewName,
            Integer currentPage, Integer pageSize, String whereClause, String orderByColumn, String orderByDirection);
}
