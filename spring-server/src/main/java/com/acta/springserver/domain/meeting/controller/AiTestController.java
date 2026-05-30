package com.acta.springserver.domain.meeting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.client.MultipartBodyBuilder;
import java.util.Map;

@RestController
@RequestMapping("/api/test-ai")
public class AiTestController {

    private final WebClient webClient;

    // 💡 스프링에게 요구하지 않고(Builder 주입 X), 여기서 WebClient를 직접 생성해버립니다!
    public AiTestController() {
        this.webClient = WebClient.create("http://localhost:8000");
    }

    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> testAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map result = webClient.post()
                    .uri("/api/analyze-meeting") 
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class) 
                    .block();

            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI 서버 통신 실패: " + e.getMessage()));
        }
    }
}