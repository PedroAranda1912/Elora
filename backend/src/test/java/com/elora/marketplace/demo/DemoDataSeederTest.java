package com.elora.marketplace.demo;

import com.elora.marketplace.repository.ProductRepository;
import com.elora.marketplace.repository.ServiceRepository;
import com.elora.marketplace.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "elora.demo.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:elora-demo-seed;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
@ActiveProfiles("test")
class DemoDataSeederTest {
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired DemoDataSeeder seeder;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ServiceRepository services;

    @Test
    void vitrinesMostramTodasAsCategorias() throws Exception {
        JsonNode productPage = get("/api/products/search?size=48", null);
        assertThat(productPage.get("totalElements").asLong()).isEqualTo(36);
        assertThat(field(productPage.get("content"), "category")).hasSize(9);

        JsonNode servicePage = get("/api/services/search?size=48", null);
        assertThat(servicePage.get("totalElements").asLong()).isEqualTo(18);
        assertThat(field(servicePage.get("content"), "category")).hasSize(15);

        JsonNode projects = get("/api/projects?size=48", null);
        assertThat(projects.get("totalElements").asLong()).isEqualTo(7);
    }

    @Test
    void historicoDaPrestadoraGeraNivelEAvaliacoes() throws Exception {
        JsonNode branding = null;
        for (JsonNode service : get("/api/services/search?category=Design&size=48", null).get("content"))
            if (service.get("title").asText().startsWith("Identidade visual completa")) branding = service;
        assertThat(branding).isNotNull();
        assertThat(branding.get("providerLevel").asText()).isEqualTo("Experiente");
        assertThat(branding.get("completedWorkCount").asLong()).isEqualTo(11);
        assertThat(branding.get("packages")).hasSize(3);
    }

    @Test
    void contasDeDemonstracaoEntramETemHistorico() throws Exception {
        String marina = login("marina@elora.demo");
        assertThat(get("/api/orders/mine", marina)).hasSize(3);
        assertThat(get("/api/favorites", marina)).hasSize(8);
        assertThat(get("/api/negotiations/mine", marina)).hasSize(2);
        assertThat(get("/api/notifications/unread-count", marina).get("count").asLong()).isEqualTo(4);
        JsonNode conversations = get("/api/conversations", marina);
        assertThat(conversations).hasSize(3);
        long unread = 0;
        for (JsonNode conversation : conversations) unread += conversation.get("unreadCount").asLong();
        assertThat(unread).isEqualTo(2);

        String rafael = login("rafael@elora.demo");
        JsonNode dashboard = get("/api/dashboard", rafael);
        assertThat(dashboard.get("activeProducts").asLong()).isEqualTo(8);
        assertThat(dashboard.get("lowStockProducts").asLong()).isPositive();
        assertThat(get("/api/orders/sales", rafael)).hasSize(5);
    }

    @Test
    void imagensDosAnunciosExistemNoFrontend() {
        products.findAll().forEach(product ->
                assertThat(localFile(product.getImageUrl())).as(product.getName()).isRegularFile());
        services.findAll().forEach(service -> {
            assertThat(localFile(service.getImageUrl())).as(service.getTitle()).isRegularFile();
            if (service.getPortfolioUrls() != null)
                service.getPortfolioUrls().lines().forEach(url -> assertThat(localFile(url)).as(url).isRegularFile());
        });
        assertThat(Files.isRegularFile(FRONTEND.resolve("assets/demo/CREDITOS.md"))).isTrue();
    }

    @Test
    void rodarDeNovoNaoDuplicaDados() throws Exception {
        long usersBefore = users.count();
        long productsBefore = products.count();
        seeder.run(null);
        assertThat(users.count()).isEqualTo(usersBefore);
        assertThat(products.count()).isEqualTo(productsBefore);
    }

    private String login(String email) throws Exception {
        ResponseEntity<String> response = http.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "Elora@2026"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return json.readTree(response.getBody()).get("token").asText();
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        ResponseEntity<String> response = http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        return json.readTree(response.getBody());
    }

    private static final Path FRONTEND = Path.of("..", "frontend");

    /** "http://localhost:8080/assets/demo/x.jpg" → ../frontend/assets/demo/x.jpg */
    private static Path localFile(String url) {
        assertThat(url).contains("/assets/demo/");
        return FRONTEND.resolve(url.substring(url.indexOf("/assets/demo/") + 1));
    }

    private static Set<String> field(JsonNode items, String name) {
        Set<String> values = new HashSet<>();
        items.forEach(item -> values.add(item.get(name).asText()));
        return values;
    }
}
