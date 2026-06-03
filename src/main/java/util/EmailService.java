package util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class EmailService {
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final HttpClient http;

    public EmailService() {
        Properties props = AppProperties.load();
        this.apiKey = String.valueOf(props.getProperty("brevo.apiKey", "")).trim();
        this.fromEmail = String.valueOf(props.getProperty("brevo.fromEmail", "")).trim();
        this.fromName = String.valueOf(props.getProperty("brevo.fromName", "StyleHub")).trim();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !fromEmail.isBlank();
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        if (!isConfigured()) {
            System.err.println("Brevo not configured; reset email not sent.");
            return;
        }
        String subject = "StyleHub: код відновлення пароля";
        String text = "Ваш код для відновлення пароля: " + code + "\n"
                + "Код дійсний 15 хвилин.\n\n"
                + "Якщо це були не ви — просто ігноруйте цей лист.";

        String payload = buildJsonPayload(toEmail, subject, text);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(12))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                System.err.println("Brevo send failed: status=" + res.statusCode() + " body=" + res.body());
            }
        } catch (Exception e) {
            System.err.println("Brevo send error: " + e.getMessage());
        }
    }

    private String buildJsonPayload(String toEmail, String subject, String text) {
        return "{"
                + "\"sender\":{\"name\":\"" + esc(fromName) + "\",\"email\":\"" + esc(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + esc(toEmail) + "\"}],"
                + "\"subject\":\"" + esc(subject) + "\","
                + "\"textContent\":\"" + esc(text) + "\""
                + "}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

