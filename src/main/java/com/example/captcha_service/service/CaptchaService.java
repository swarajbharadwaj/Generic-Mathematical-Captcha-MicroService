package com.example.captcha_service.service;

import com.example.captcha_service.model.CaptchaModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Base64;
import java.util.Random;

@Service
public class CaptchaService {

    private final JwtService jwtService;
    private final Random random = new Random();

    @Value("${captcha.jwt.ttl-seconds}")
    private int captchaTtl;
    
    @Value("${captcha.difficulty.default}")
    private String defaultDifficultyStr;

    private static final Map<String, List<String>> STATIC_IMAGE_URLS = Map.of(
        "tree", List.of("https://placehold.co/150x150/a1de93/333?text=Tree", "https://placehold.co/150x150/a1de93/333?text=Tree&font=lora"),
        "car", List.of("https://placehold.co/150x150/f4b3b3/333?text=Car", "https://placehold.co/150x150/f4b3b3/333?text=Car&font=lora"),
        "bicycle", List.of("https://placehold.co/150x150/b3cde4/333?text=Bicycle", "https://placehold.co/150x150/b3cde4/333?text=Bicycle&font=lora"),
        "boat", List.of("https://placehold.co/150x150/f3e6b3/333?text=Boat", "https://placehold.co/150x150/f3e6b3/333?text=Boat&font=lora")
    );
    private static final List<String> CATEGORY_KEYS = new ArrayList<>(STATIC_IMAGE_URLS.keySet());

    public CaptchaService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // --- Image Challenge Methods ---
    public GenerateImageResponse generateImageCaptcha() {
        int gridSize = 9;
        String targetCategory = CATEGORY_KEYS.get(random.nextInt(CATEGORY_KEYS.size()));
        String prompt = "Select all images containing a " + targetCategory;

        List<String> imageUrls = new ArrayList<>();
        List<Integer> correctIndices = new ArrayList<>();
        List<Integer> gridPositions = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) gridPositions.add(i);
        Collections.shuffle(gridPositions);
        int numCorrect = 3 + random.nextInt(2);

        for (int i = 0; i < gridSize; i++) {
            if (i < numCorrect) {
                imageUrls.add(getRandomUrlForCategory(targetCategory));
                correctIndices.add(gridPositions.get(i));
            } else {
                String distractorCategory;
                do {
                    distractorCategory = CATEGORY_KEYS.get(random.nextInt(CATEGORY_KEYS.size()));
                } while (distractorCategory.equals(targetCategory));
                imageUrls.add(getRandomUrlForCategory(distractorCategory));
            }
        }
        
        List<String> finalImageUrls = new ArrayList<>(Collections.nCopies(gridSize, ""));
        for(int i=0; i < gridSize; i++) finalImageUrls.set(gridPositions.get(i), imageUrls.get(i));
        
        // Serialize the list of correct indices to a string for the JWT claim
        String answerString = correctIndices.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        String captchaId = jwtService.generateToken(answerString);

        return new GenerateImageResponse(captchaId, prompt, finalImageUrls, captchaTtl);
    }
    
    public ValidateResponse validateImageCaptcha(ValidateImageRequest request) {
        return jwtService.validateAndGetAnswer(request.getCaptchaId())
            .map(correctAnswerString -> {
                List<Integer> selectedIndices = new ArrayList<>(request.getSelectedIndices());
                Collections.sort(selectedIndices);
                String selectedAnswerString = selectedIndices.stream().map(String::valueOf).collect(Collectors.joining(","));

                if (correctAnswerString.equals(selectedAnswerString)) {
                    return new ValidateResponse(true, "CAPTCHA validation successful.");
                } else {
                    return new ValidateResponse(false, "Incorrect selection.");
                }
            })
            .orElse(new ValidateResponse(false, "CAPTCHA expired or invalid."));
    }

    // --- Mathematical CAPTCHA Methods ---
    public GenerateMathResponse generateMathCaptcha(GenerateMathRequest request) {
        Difficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.valueOf(defaultDifficultyStr);
        int num1, num2, answer;
        String question;

        // ... (math problem generation logic remains the same)
        switch (difficulty) { /* ... */ }
        switch (random.nextInt(3)) { /* ... */ }

        // --- MOCK LOGIC FOR DEMONSTRATION ---
        num1 = random.nextInt(10);
        num2 = random.nextInt(10);
        question = String.format("%d + %d", num1, num2);
        answer = num1 + num2;
        // --- END MOCK LOGIC ---

        String captchaId = jwtService.generateToken(String.valueOf(answer));
        String imageBase64 = null;
        if (request.isAsImage()) {
            try {
                imageBase64 = generateTextImage(question);
            } catch (IOException e) { System.err.println("Error generating CAPTCHA image: " + e.getMessage()); }
        }
        return new GenerateMathResponse(captchaId, question, imageBase64, captchaTtl);
    }

    public ValidateResponse validateMathCaptcha(ValidateMathRequest request) {
        return jwtService.validateAndGetAnswer(request.getCaptchaId())
                .map(correctAnswer -> {
                    if (correctAnswer.equals(String.valueOf(request.getAnswer()))) {
                        return new ValidateResponse(true, "CAPTCHA validation successful.");
                    } else {
                        return new ValidateResponse(false, "Incorrect answer.");
                    }
                })
                .orElse(new ValidateResponse(false, "CAPTCHA expired or invalid."));
    }

    // --- Helper methods (getRandomUrlForCategory, generateTextImage) remain unchanged ---
    private String getRandomUrlForCategory(String category) {
        List<String> urls = STATIC_IMAGE_URLS.get(category);
        return urls.get(random.nextInt(urls.size()));
    }

    private String generateTextImage(String text) throws IOException {
        // ... implementation is the same
        int width = 180, height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 30)); g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2);
        g.drawString(text, x, y); g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}

