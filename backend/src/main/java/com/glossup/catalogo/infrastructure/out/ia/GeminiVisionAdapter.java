package com.glossup.catalogo.infrastructure.out.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.catalogo.domain.port.out.GeminiVisionPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class GeminiVisionAdapter implements GeminiVisionPort {

    @Value("${glossup.ai.gemini.api-key}")
    private String apiKey;

    @Value("${glossup.ai.gemini.model}")
    private String model;

    @Value("${glossup.ai.gemini.endpoint}")
    private String endpoint;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<String> extraerIngredientes(byte[] imagenBytes, String mimeType) {
        String base64 = Base64.getEncoder().encodeToString(imagenBytes);
        String url    = endpoint + "/" + model + ":generateContent?key=" + apiKey;
        String body   = buildBody(base64, mimeType);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new RuntimeException("Gemini Vision respondió HTTP " + res.statusCode() + ": " + res.body());
            }
            return parsear(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupción en la petición a Gemini Vision", e);
        } catch (IOException e) {
            throw new RuntimeException("Error de red con Gemini Vision: " + e.getMessage(), e);
        }
    }

    private String buildBody(String base64, String mimeType) {
        String prompt = """
            Eres un experto en nomenclatura INCI (International Nomenclature of Cosmetic Ingredients).
            Analiza la imagen de esta etiqueta cosmética.
            Devuelve ÚNICAMENTE los nombres de ingredientes INCI, uno por línea, en minúsculas.
            Sin porcentajes, sin fechas, sin explicaciones, sin texto adicional.
            Si no hay ingredientes visibles, responde: NINGUNO
            """;
        String p;
        try {
            p = mapper.writeValueAsString(prompt);
        } catch (Exception e) {
            p = "\"" + prompt.replace("\"", "\\\"") + "\"";
        }

        return """
            {
              "contents":[{"parts":[
                {"text":%s},
                {"inline_data":{"mime_type":"%s","data":"%s"}}
              ]}],
              "generationConfig":{"temperature":0.1,"maxOutputTokens":1024}
            }
            """.formatted(p, mimeType, base64);
    }

    private List<String> parsear(String responseBody) throws IOException {
        JsonNode root = mapper.readTree(responseBody);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("").trim();

        List<String> result = new ArrayList<>();
        for (String linea : text.split("\\r?\\n")) {
            String limpio = linea.trim().toLowerCase().replaceAll("^[-•*\\d.]+\\s*", "");
            if (!limpio.isEmpty() && !limpio.equals("ninguno")) {
                result.add(limpio);
            }
        }
        return result;
    }
}
