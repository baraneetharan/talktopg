package com.kgisl.talktopg;

import java.util.List;
import java.util.Map;

/**
 * Response wrapper for query results.
 */
public record QueryResponse(
    boolean success,
    String sql,
    List<Map<String, Object>> data,
    String error
) {
    public static QueryResponse success(String sql, List<Map<String, Object>> data) {
        return new QueryResponse(true, sql, data, null);
    }

    public static QueryResponse failure(String error) {
        return new QueryResponse(false, null, null, error);
    }
}
