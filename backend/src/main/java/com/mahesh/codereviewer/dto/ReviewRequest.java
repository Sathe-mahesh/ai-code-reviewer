package com.mahesh.codereviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank(message = "Code must not be empty")
        @Size(max = 20000, message = "Code is too long (max 20,000 characters)")
        String code,
        String language
) {
}
