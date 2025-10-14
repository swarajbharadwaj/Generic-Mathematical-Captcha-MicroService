package com.example.captcha_service.controller;

import com.example.captcha_service.model.CaptchaModels.*;
import com.example.captcha_service.service.CaptchaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/captcha")
@CrossOrigin(origins = "*") 
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    // --- Endpoints for Mathematical CAPTCHA ---
    @PostMapping("/generate-math")
    public ResponseEntity<GenerateMathResponse> generateMath(@RequestBody(required = false) GenerateMathRequest request) {
        if(request == null){
            request = new GenerateMathRequest();
        }
        return ResponseEntity.ok(captchaService.generateMathCaptcha(request));
    }

    @PostMapping("/validate-math")
    public ResponseEntity<ValidateResponse> validateMath(@RequestBody ValidateMathRequest request) {
        if (request.getCaptchaId() == null) {
             return ResponseEntity.badRequest().body(new ValidateResponse(false, "captchaId is required."));
        }
        ValidateResponse response = captchaService.validateMathCaptcha(request);
        if(!response.isSuccess() && response.getMessage().contains("expired")){
            return new ResponseEntity<>(response, HttpStatus.GONE); 
        }
        return ResponseEntity.ok(response);
    }

    // --- Endpoints for Image Challenge CAPTCHA ---
    @PostMapping("/generate-image")
    public ResponseEntity<GenerateImageResponse> generateImage() {
        return ResponseEntity.ok(captchaService.generateImageCaptcha());
    }

    @PostMapping("/validate-image")
    public ResponseEntity<ValidateResponse> validateImage(@RequestBody ValidateImageRequest request) {
        if (request.getCaptchaId() == null || request.getSelectedIndices() == null) {
            return ResponseEntity.badRequest().body(new ValidateResponse(false, "captchaId and selectedIndices are required."));
        }
        ValidateResponse response = captchaService.validateImageCaptcha(request);
        if (!response.isSuccess() && response.getMessage().contains("expired")) {
            return new ResponseEntity<>(response, HttpStatus.GONE);
        }
        return ResponseEntity.ok(response);
    }
}

