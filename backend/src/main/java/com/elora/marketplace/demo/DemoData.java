package com.elora.marketplace.demo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estrutura do arquivo {@code demo/demo-data.json}. Os registros se referenciam por chave
 * (por exemplo, {@code "owner": "rafael"} ou {@code "product": "p-notebook"}), e o
 * {@link DemoDataSeeder} resolve essas chaves para as entidades salvas.
 * O campo {@code ago} indica há quanto tempo o registro foi criado: "3d", "5h" ou "40m".
 */
public record DemoData(
        List<UserSeed> users,
        List<ProductSeed> products,
        List<ServiceSeed> services,
        List<PortfolioSeed> portfolio,
        List<ProjectSeed> projects,
        List<OrderSeed> orders,
        List<ServiceOrderSeed> serviceOrders,
        List<NegotiationSeed> negotiations,
        List<QuestionSeed> questions,
        List<ConversationSeed> conversations,
        List<FavoriteSeed> favorites,
        List<NotificationSeed> notifications) {

    public record UserSeed(String key, String name, String email, String city, String phone, String bio, String ago) {}

    public record ProductSeed(String key, String owner, String name, String category, String condition,
                              BigDecimal price, Integer stock, String status, String tags, String description, String ago) {}

    public record ServiceSeed(String key, String owner, String title, String category, String experience, String mode,
                              String tags, String description, List<String> faq, List<String> gallery,
                              List<PackageSeed> packages, String ago) {}

    public record PackageSeed(String tier, String name, BigDecimal price, Integer days, Integer revisions,
                              String description, String deliverables) {}

    public record PortfolioSeed(String owner, String title, String description, String category,
                                String technologies, String image, String ago) {}

    public record ProjectSeed(String key, String client, String title, String description, String category,
                              BigDecimal budget, Integer deadlineDays, String location, String mode, String status,
                              List<ProposalSeed> proposals, String ago) {}

    public record ProposalSeed(String provider, BigDecimal amount, Integer deadlineDays, String message,
                               String experience, String status, String ago) {}

    public record OrderSeed(String buyer, String status, String payment, String address, List<OrderItemSeed> items,
                            List<ProductReviewSeed> reviews, String ago) {}

    public record OrderItemSeed(String product, Integer quantity) {}

    public record ProductReviewSeed(String seller, Integer rating, String comment) {}

    public record ServiceOrderSeed(String client, String service, String tier, String status, String description,
                                   ReviewSeed review, String ago) {}

    public record ReviewSeed(Integer rating, String comment) {}

    public record NegotiationSeed(String product, String buyer, BigDecimal offer, String message, String status,
                                  BigDecimal counter, String sellerMessage, String ago) {}

    public record QuestionSeed(String type, String listing, String asker, String question, String answer, String ago) {}

    public record ConversationSeed(String a, String b, String product, String service, String project,
                                   List<MessageSeed> messages) {}

    public record MessageSeed(String from, String text, Boolean unread, String ago) {}

    public record FavoriteSeed(String user, String type, String item) {}

    public record NotificationSeed(String user, String type, String title, String message, String link,
                                   Boolean read, String ago) {}
}
