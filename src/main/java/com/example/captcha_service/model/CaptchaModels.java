package com.example.captcha_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CaptchaModels {

    // --- General Enums ---
    public enum Difficulty {
        L1, L2, L3
    }

    // --- Mathematical CAPTCHA ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateMathRequest {
        private boolean asImage = false;
        private Difficulty difficulty;
    }

    @Data
    @AllArgsConstructor
    public static class GenerateMathResponse {
        private String captchaId;
        private String question;
        private String imageBase64;
        private long expiresIn;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateMathRequest {
        private String captchaId;
        private int answer;
    }

    // --- Image Challenge CAPTCHA ---

    @Data
    @AllArgsConstructor
    public static class GenerateImageResponse {
        private String captchaId;
        private String prompt; // e.g., "Select all images containing a tree"
        private List<String> images; // List of image URLs
        private long expiresIn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateImageRequest {
        private String captchaId;
        private List<Integer> selectedIndices;
    }

    // --- Common Validation Response ---
    @Data
    @AllArgsConstructor
    public static class ValidateResponse {
        private boolean success;
        private String message;
    }
}

