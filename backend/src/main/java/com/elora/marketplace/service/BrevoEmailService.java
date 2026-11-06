package com.elora.marketplace.service;

import com.elora.marketplace.model.AppOrder;
import com.elora.marketplace.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BrevoEmailService {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailService(
            ObjectMapper objectMapper,
            @Value("${elora.email.brevo.api-key:}") String apiKey,
            @Value("${elora.email.brevo.sender-email:}") String senderEmail,
            @Value("${elora.email.brevo.sender-name:Elora}") String senderName) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public void sendOrderConfirmation(AppOrder order) {
        if (apiKey.isBlank() || senderEmail.isBlank()) {
            log.info("E-mail do pedido {} não enviado: configure ELORA_BREVO_API_KEY e ELORA_BREVO_SENDER_EMAIL.", order.getId());
            return;
        }

        String items = order.getItems().stream()
                .map(this::formatItem)
                .collect(Collectors.joining("\n"));
        String body = "Olá, " + order.getBuyer().getName() + "!\n\n"
                + "Seu pedido #" + order.getId() + " foi registrado na Elora.\n\n"
                + "Itens:\n" + items + "\n\n"
                + "Total: " + currency(order.getTotalAmount()) + "\n\n"
                + "Esta compra foi registrada em modo de demonstração. Nenhuma cobrança real foi feita.\n"
                + "Acompanhe o pedido na sua conta Elora.";

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", new Object[]{Map.of("email", order.getBuyer().getEmail(), "name", order.getBuyer().getName())},
                    "subject", "Pedido #" + order.getId() + " registrado na Elora",
                    "textContent", body));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Brevo respondeu com HTTP {} ao enviar e-mail do pedido {}: {}",
                        response.statusCode(), order.getId(), response.body());
            } else {
                log.info("Brevo aceitou o e-mail de confirmação do pedido {}.", order.getId());
            }
        } catch (Exception exception) {
            // Falha no serviço de e-mail não desfaz o pedido já salvo.
            log.error("Não foi possível enviar o e-mail do pedido {} pelo Brevo.", order.getId(), exception);
        }
    }

    private String formatItem(OrderItem item) {
        return "- " + item.getProduct().getName() + " × " + item.getQuantity()
                + " — " + currency(item.getSubtotal());
    }

    private String currency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(amount);
    }
}
