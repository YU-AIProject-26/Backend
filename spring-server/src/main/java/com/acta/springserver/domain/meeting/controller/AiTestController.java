package com.acta.springserver.domain.meeting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequiredArgsConstructor // 스프링 본사님, 밑에 `final` 붙은 애들 좀 가져다주세요!
@RequestMapping("/api/test-ai")
public class AiTestController {

    private final WebClient customWebClient;
    @PostMapping(value = "/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> testAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());

            // 파이썬(8000)을 호출
            Map result = customWebClient.post()
                    .uri("http://localhost:8000/api/analyze-meeting")
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // 15분까지는 얌전히 기다려 줍니다!

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI 서버 통신 실패: " + e.getMessage()));
        }
    }
}