package com.kgisl.talktopg;


import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Spring Boot equivalent of the .NET NorthwindServicefromDB class.
 *
 * Responsibilities (identical to the original):
 *  1. Retrieve all table schemas from the PostgreSQL information_schema
 *  2. Send schema + natural-language query to GPT-4 to generate SQL
 *  3. Extract the SQL from the model response (```sql … ```)
 *  4. Allow only SELECT queries (security guard)
 *  5. Execute the query and return structured results
 */
@Service
public class DBService {

    private static final Logger log = LoggerFactory.getLogger(DBService.class);

    private final JdbcTemplate jdbc;
    private final OpenAIClient openAIClient;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public DBService(JdbcTemplate jdbc, OpenAIClient openAIClient) {
        this.jdbc = jdbc;
        this.openAIClient = openAIClient;
    }

    // ─────────────────────────────────────────────────────────────────
    // Public API  —  replaces AnswerFromDB(string query)
    // ─────────────────────────────────────────────────────────────────

    public QueryResponse answerFromDB(String naturalLanguageQuery) {
        try {
            // Step 1: Retrieve schema (replaces GetAllTableSchemas())
            String schema = getAllTableSchemas();
            log.debug("Retrieved schema:\n{}", schema);

            // Step 2: Ask GPT-4 to write the SQL
            String prompt = schema + "\n" + naturalLanguageQuery;
            String rawResponse = callOpenAI(prompt);
            log.debug("OpenAI raw response:\n{}", rawResponse);

            // Step 3: Extract SQL from ```sql ... ``` block
            String sqlQuery = extractSqlQuery(rawResponse);
            log.info("Generated SQL: {}", sqlQuery);

            if (sqlQuery.isBlank()) {
                return QueryResponse.failure("Could not extract a SQL query from the model response.");
            }

            // Step 4: Security guard — SELECT only
            if (!sqlQuery.stripLeading().toUpperCase().startsWith("SELECT")) {
                return QueryResponse.failure("Only SELECT queries are supported.");
            }

            // Step 5: Execute and return
            List<Map<String, Object>> rows = executeQuery(sqlQuery);
            return QueryResponse.success(sqlQuery, rows);

        } catch (Exception ex) {
            log.error("Error processing query: {}", naturalLanguageQuery, ex);
            return QueryResponse.failure("Internal error: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Schema retrieval  —  replaces GetAllTableSchemas() / GetTableSchema()
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieves the schema for every public table in the database.
     * Uses JdbcTemplate instead of raw Npgsql connections.
     */
    private String getAllTableSchemas() {
        List<String> tableNames = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class
        );

        StringBuilder allSchemas = new StringBuilder();
        for (String tableName : tableNames) {
            allSchemas.append(getTableSchema(tableName)).append("\n");
        }
        return allSchemas.toString();
    }

    /**
     * Returns column names and data types for a single table.
     * Replaces GetTableSchema(string tableName).
     */
    private String getTableSchema(String tableName) {
        List<Map<String, Object>> columns = jdbc.queryForList(
                "SELECT column_name, data_type " +
                "FROM information_schema.columns " +
                "WHERE table_name = ? " +
                "ORDER BY ordinal_position",
                tableName
        );

        StringBuilder schema = new StringBuilder();
        schema.append("Table: ").append(tableName).append("\n");
        schema.append("Columns:\n");
        for (Map<String, Object> col : columns) {
            schema.append("- ")
                  .append(col.get("column_name"))
                  .append(" (")
                  .append(col.get("data_type"))
                  .append(")\n");
        }
        return schema.toString();
    }

    // ─────────────────────────────────────────────────────────────────
    // OpenAI call  —  replaces client.CompleteAsync(...)
    // ─────────────────────────────────────────────────────────────────

    private String callOpenAI(String prompt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .addUserMessage(prompt)
                .build();

        ChatCompletion completion = openAIClient.chat().completions().create(params);

        return completion.choices().stream()
                .findFirst()
                .map(c -> c.message().content().orElse(""))
                .orElse("");
    }

    // ─────────────────────────────────────────────────────────────────
    // SQL extraction  —  replaces ExtractSqlQuery(string response)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extracts the SQL query from a GPT response that wraps it in:
     * ```sql
     * SELECT ...
     * ```
     */
    static String extractSqlQuery(String response) {
        if (response == null || response.isBlank()) return "";

        String lower = response.toLowerCase();
        int startIndex = lower.indexOf("```sql");
        if (startIndex == -1) return "";

        startIndex += 6; // skip past "```sql"
        int endIndex = response.indexOf("```", startIndex);
        if (endIndex == -1) return "";

        return response.substring(startIndex, endIndex).trim();
    }

    // ─────────────────────────────────────────────────────────────────
    // Query execution  —  replaces ExecuteQuery(string query)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Executes the SQL query and returns rows as a list of column-name → value maps.
     * JdbcTemplate.queryForList(sql) already returns List<Map<String, Object>>.
     */
    private List<Map<String, Object>> executeQuery(String sql) {
        return jdbc.queryForList(sql);
    }
}