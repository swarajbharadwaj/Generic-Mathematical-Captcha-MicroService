package com.example.captcha_service.service;

import com.example.captcha_service.config.CacheConfig;
import com.example.captcha_service.model.CaptchaModels.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
public class CaptchaService {

    private final Cache captchaCache;
    private final Random random = new Random();

    @Value("${captcha.ttl.seconds}")
    private int captchaTtl;
    
    @Value("${captcha.difficulty.default}")
    private String defaultDifficultyStr;

    // --- NEW: Static Image URL Store ---
    // Using a reliable placeholder service (placehold.co) to avoid rate limiting issues.
    private static final Map<String, List<String>> STATIC_IMAGE_URLS = Map.of(
        "tree", List.of(
            "https://placehold.co/150x150/a1de93/333?text=Tree",
            "https://placehold.co/150x150/a1de93/333?text=Tree&font=lora",
            "https://placehold.co/150x150/a1de93/333?text=Tree&font=playfair-display"
        ),
        "car", List.of(
            "https://placehold.co/150x150/f4b3b3/333?text=Car",
            "https://placehold.co/150x150/f4b3b3/333?text=Car&font=lora",
            "https://placehold.co/150x150/f4b3b3/333?text=Car&font=playfair-display"
        ),
        "bicycle", List.of(
            "https://placehold.co/150x150/b3cde4/333?text=Bicycle",
            "https://placehold.co/150x150/b3cde4/333?text=Bicycle&font=lora",
            "https://placehold.co/150x150/b3cde4/333?text=Bicycle&font=playfair-display"
        ),
        "boat", List.of(
            "https://placehold.co/150x150/f3e6b3/333?text=Boat",
            "https://placehold.co/150x150/f3e6b3/333?text=Boat&font=lora",
            "https://placehold.co/150x150/f3e6b3/333?text=Boat&font=playfair-display"
        )
    );
    private static final List<String> CATEGORY_KEYS = new ArrayList<>(STATIC_IMAGE_URLS.keySet());


    public CaptchaService(CacheManager cacheManager) {
        this.captchaCache = Objects.requireNonNull(cacheManager.getCache(CacheConfig.CAPTCHA_CACHE));
    }

    // --- Image Challenge Methods ---

    public GenerateImageResponse generateImageCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        int gridSize = 9;

        String targetCategory = CATEGORY_KEYS.get(random.nextInt(CATEGORY_KEYS.size()));
        String prompt = "Select all images containing a " + targetCategory;

        List<String> imageUrls = new ArrayList<>();
        List<Integer> correctIndices = new ArrayList<>();
        
        List<Integer> gridPositions = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) gridPositions.add(i);
        Collections.shuffle(gridPositions);

        int numCorrect = 3 + random.nextInt(2); // 3 or 4 correct images

        for (int i = 0; i < gridSize; i++) {
            int position = gridPositions.get(i);
            
            if (i < numCorrect) {
                // This is a target image
                imageUrls.add(getRandomUrlForCategory(targetCategory));
                correctIndices.add(position);
            } else {
                // This is a distractor image
                String distractorCategory;
                do {
                    distractorCategory = CATEGORY_KEYS.get(random.nextInt(CATEGORY_KEYS.size()));
                } while (distractorCategory.equals(targetCategory));
                imageUrls.add(getRandomUrlForCategory(distractorCategory));
            }
        }
        
        // Create a final shuffled list based on the position mapping
        List<String> finalImageUrls = new ArrayList<>(Collections.nCopies(gridSize, ""));
        for(int i=0; i < gridSize; i++){
            finalImageUrls.set(gridPositions.get(i), imageUrls.get(i));
        }

        captchaCache.put(captchaId, correctIndices);

        return new GenerateImageResponse(captchaId, prompt, finalImageUrls, captchaTtl);
    }
    
    private String getRandomUrlForCategory(String category) {
        List<String> urls = STATIC_IMAGE_URLS.get(category);
        return urls.get(random.nextInt(urls.size()));
    }
    
    @SuppressWarnings("unchecked")
    public ValidateResponse validateImageCaptcha(ValidateImageRequest request) {
        List<Integer> correctIndices = captchaCache.get(request.getCaptchaId(), List.class);
        
        if (correctIndices == null) {
            return new ValidateResponse(false, "CAPTCHA expired or invalid.");
        }

        List<Integer> selectedIndices = new ArrayList<>(request.getSelectedIndices());
        Collections.sort(correctIndices);
        Collections.sort(selectedIndices);
        
        if (correctIndices.equals(selectedIndices)) {
            captchaCache.evict(request.getCaptchaId());
            return new ValidateResponse(true, "CAPTCHA validation successful.");
        } else {
            return new ValidateResponse(false, "Incorrect selection.");
        }
    }


    // --- Mathematical CAPTCHA Methods (Unchanged) ---

    public GenerateMathResponse generateMathCaptcha(GenerateMathRequest request) {
        Difficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.valueOf(defaultDifficultyStr);
        int num1, num2, answer;
        String question;

        switch (difficulty) {
            case L2:
                num1 = random.nextInt(90) + 10;
                num2 = random.nextInt(90) + 10;
                break;
            case L3:
                 int a = random.nextInt(5) + 2;
                 int b = random.nextInt(10) + 1;
                 int result = a * (random.nextInt(5) + 1) + b;
                 question = String.format("%dx + %d = %d", a, b, result);
                 answer = (result - b) / a;
                 return createMathCaptchaResponse(request, question, answer);
            default: // L1
                num1 = random.nextInt(10);
                num2 = random.nextInt(10);
                break;
        }

        int operation = random.nextInt(3);
        switch (operation) {
            case 1: 
                if (num1 < num2) { int temp = num1; num1 = num2; num2 = temp; }
                question = String.format("%d - %d", num1, num2);
                answer = num1 - num2;
                break;
            case 2:
                question = String.format("%d × %d", num1, num2);
                answer = num1 * num2;
                break;
            default: // 0
                question = String.format("%d + %d", num1, num2);
                answer = num1 + num2;
                break;
        }
        return createMathCaptchaResponse(request, question, answer);
    }

    private GenerateMathResponse createMathCaptchaResponse(GenerateMathRequest request, String question, int answer) {
        String captchaId = UUID.randomUUID().toString();
        captchaCache.put(captchaId, answer);
        String imageBase64 = null;
        if (request.isAsImage()) {
            try {
                imageBase64 = generateTextImage(question);
            } catch (IOException e) { System.err.println("Error generating CAPTCHA image: " + e.getMessage()); }
        }
        return new GenerateMathResponse(captchaId, question, imageBase64, captchaTtl);
    }

    public ValidateResponse validateMathCaptcha(ValidateMathRequest request) {
        Integer answer = captchaCache.get(request.getCaptchaId(), Integer.class);
        if (answer == null) {
            return new ValidateResponse(false, "CAPTCHA expired or invalid.");
        }
        if (answer.equals(request.getAnswer())) {
            captchaCache.evict(request.getCaptchaId());
            return new ValidateResponse(true, "CAPTCHA validation successful.");
        } else {
            return new ValidateResponse(false, "Incorrect answer.");
        }
    }

    private String generateTextImage(String text) throws IOException {
        int width = 180, height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2);
        g.drawString(text, x, y);
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 5; i++) g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}

