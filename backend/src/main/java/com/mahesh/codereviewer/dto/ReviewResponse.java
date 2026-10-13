package com.mahesh.codereviewer.dto;

import java.util.List;

public record ReviewResponse(
        String summary,
        int score,
        List<Issue> issues
) {
    public record Issue(
            String severity,
            Integer line,
            String title,
            String explanation,
            String suggestion
    ) {
    }
}
