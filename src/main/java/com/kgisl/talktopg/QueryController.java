package com.kgisl.talktopg;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling natural language database queries.
 */
@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final DBService dbService;

    public QueryController(DBService dbService) {
        this.dbService = dbService;
    }

    /**
     * Accepts a natural language query and returns the SQL and results.
     */
    @PostMapping
    public ResponseEntity<?> query(@RequestBody QueryRequest request) {
        QueryResponse response = dbService.answerFromDB(request.query());
        if (response.success()) {
            // Extract only concatenated_text values from each row
            List<String> textValues = response.data().stream()
                    .map(row -> (String) row.get("concatenated_text"))
                    .filter(Objects::nonNull)
                    .toList();
            return ResponseEntity.ok(textValues);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Request body for natural language queries.
     */
    public record QueryRequest(String query) {
    }
}
