package com.elora.marketplace.demo;

import com.elora.marketplace.demo.DemoData.*;
import com.elora.marketplace.model.*;
import com.elora.marketplace.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Popula o banco com dados de demonstração (contas, anúncios, pedidos, avaliações, conversas...).
 * Só roda com {@code ELORA_DEMO_SEED=true} e não faz nada se alguma conta de demonstração já existir,
 * então pode ficar ligado sem duplicar dados. As fotos ficam em {@code frontend/assets/demo/} e são
 * gravadas com a URL de {@code elora.demo.seed.image-base-url}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "elora.demo.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private static final String DATA_FILE = "demo/demo-data.json";
    private static final String IMAGE_DIR = "/assets/demo/";

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository users;
    private final ProductRepository products;
    private final ServiceRepository services;
    private final PortfolioItemRepository portfolioItems;
    private final ProjectRepository projects;
    private final ProposalRepository proposals;
    private final OrderRepository orders;
    private final ServiceOrderRepository serviceOrders;
    private final ReviewRepository reviews;
    private final ProductNegotiationRepository negotiations;
    private final ListingQuestionRepository questions;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final FavoriteRepository favorites;
    private final MarketplaceNotificationRepository notifications;

    @Value("${elora.demo.seed.password:Elora@2026}")
    private String demoPassword;

    // URL absoluta, como a dos uploads: os campos de imagem dos formulários de edição exigem http(s).
    @Value("${elora.demo.seed.image-base-url:http://localhost:8080}")
    private String imageBaseUrl;

    private final Map<String, AppUser> userByKey = new HashMap<>();
    private final Map<String, Product> productByKey = new HashMap<>();
    private final Map<String, ServiceOffer> serviceByKey = new HashMap<>();
    private final Map<String, Project> projectByKey = new HashMap<>();
    private LocalDateTime now;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        DemoData data = load();
        if (data.users().stream().anyMatch(user -> users.existsByEmailIgnoreCase(user.email()))) {
            log.info("Dados de demonstração já existem no banco; nada foi alterado.");
            return;
        }
        now = LocalDateTime.now();
        seedUsers(data.users());
        seedProducts(data.products());
        seedServices(data.services());
        seedPortfolio(data.portfolio());
        seedProjects(data.projects());
        seedOrders(data.orders());
        seedServiceOrders(data.serviceOrders());
        seedNegotiations(data.negotiations());
        seedQuestions(data.questions());
        seedConversations(data.conversations());
        seedFavorites(data.favorites());
        seedNotifications(data.notifications());

        log.info("Dados de demonstração criados: {} contas, {} produtos, {} serviços, {} projetos, {} pedidos e {} contratações.",
                data.users().size(), data.products().size(), data.services().size(), data.projects().size(),
                data.orders().size(), data.serviceOrders().size());
        log.info("Contas de demonstração (senha {}): {}", demoPassword,
                String.join(", ", data.users().stream().map(UserSeed::email).toList()));
    }

    private DemoData load() throws IOException {
        try (InputStream input = new ClassPathResource(DATA_FILE).getInputStream()) {
            return objectMapper.readValue(input, DemoData.class);
        }
    }

    private void seedUsers(List<UserSeed> seeds) {
        String passwordHash = passwordEncoder.encode(demoPassword);
        for (int index = 0; index < seeds.size(); index++) {
            UserSeed seed = seeds.get(index);
            AppUser user = new AppUser();
            user.setName(seed.name());
            user.setEmail(seed.email().toLowerCase());
            user.setCpf(uniqueCpf(index));
            user.setPassword(passwordHash);
            user.setCity(seed.city());
            user.setPhone(seed.phone());
            user.setBio(seed.bio());
            user.setCreatedAt(ago(seed.ago()));
            userByKey.put(seed.key(), users.save(user));
        }
    }

    private void seedProducts(List<ProductSeed> seeds) {
        for (ProductSeed seed : seeds) {
            AppUser owner = user(seed.owner());
            Product product = new Product();
            product.setOwner(owner);
            product.setName(seed.name());
            product.setCategory(seed.category());
            product.setDescription(seed.description());
            product.setPrice(seed.price());
            product.setStock(seed.stock());
            product.setConditionType(seed.condition());
            product.setStatus(seed.status() == null ? "ATIVO" : seed.status());
            product.setImageUrl(image(seed.key()));
            product.setLocation(owner.getCity());
            product.setTags(seed.tags());
            product.setCreatedAt(ago(seed.ago()));
            productByKey.put(seed.key(), products.save(product));
        }
    }

    private void seedServices(List<ServiceSeed> seeds) {
        for (ServiceSeed seed : seeds) {
            AppUser owner = user(seed.owner());
            ServiceOffer service = new ServiceOffer();
            service.setOwner(owner);
            service.setTitle(seed.title());
            service.setCategory(seed.category());
            service.setDescription(seed.description());
            service.setStatus("ATIVO");
            service.setImageUrl(image(seed.key()));
            service.setExperienceLevel(seed.experience());
            service.setServiceMode(seed.mode());
            service.setLocation(owner.getCity());
            service.setTags(seed.tags());
            service.setFaq(seed.faq() == null ? null : String.join("\n", seed.faq()));
            service.setPortfolioUrls(seed.gallery() == null ? null
                    : String.join("\n", seed.gallery().stream().map(this::image).toList()));
            service.setCreatedAt(ago(seed.ago()));
            for (int order = 0; order < seed.packages().size(); order++) {
                PackageSeed packageSeed = seed.packages().get(order);
                ServicePackage item = new ServicePackage();
                item.setService(service);
                item.setTier(packageSeed.tier());
                item.setName(packageSeed.name());
                item.setPrice(packageSeed.price());
                item.setDeliveryDays(packageSeed.days());
                item.setRevisions(packageSeed.revisions());
                item.setDescription(packageSeed.description());
                item.setDeliverables(packageSeed.deliverables());
                item.setSortOrder(order);
                service.getPackages().add(item);
            }
            // O preço e o prazo do anúncio são os do pacote mais barato, como mostram as listagens.
            ServicePackage cheapest = service.getPackages().get(0);
            service.setPrice(cheapest.getPrice());
            service.setDeliveryDays(cheapest.getDeliveryDays());
            serviceByKey.put(seed.key(), services.save(service));
        }
    }

    private void seedPortfolio(List<PortfolioSeed> seeds) {
        for (PortfolioSeed seed : seeds) {
            PortfolioItem item = new PortfolioItem();
            item.setOwner(user(seed.owner()));
            item.setTitle(seed.title());
            item.setDescription(seed.description());
            item.setCategory(seed.category());
            item.setTechnologies(seed.technologies());
            item.setImageUrl(image(seed.image()));
            item.setCreatedAt(ago(seed.ago()));
            item.setUpdatedAt(item.getCreatedAt());
            portfolioItems.save(item);
        }
    }

    private void seedProjects(List<ProjectSeed> seeds) {
        for (ProjectSeed seed : seeds) {
            AppUser client = user(seed.client());
            Project project = new Project();
            project.setClient(client);
            project.setTitle(seed.title());
            project.setDescription(seed.description());
            project.setCategory(seed.category());
            project.setBudget(seed.budget());
            project.setDeadlineDays(seed.deadlineDays());
            project.setLocation(seed.location() == null ? client.getCity() : seed.location());
            project.setServiceMode(seed.mode());
            project.setStatus(seed.status());
            project.setCreatedAt(ago(seed.ago()));
            project.setUpdatedAt(project.getCreatedAt());
            project = projects.save(project);
            projectByKey.put(seed.key(), project);
            for (ProposalSeed proposalSeed : seed.proposals()) {
                Proposal proposal = new Proposal();
                proposal.setProject(project);
                proposal.setProvider(user(proposalSeed.provider()));
                proposal.setAmount(proposalSeed.amount());
                proposal.setDeadlineDays(proposalSeed.deadlineDays());
                proposal.setMessage(proposalSeed.message());
                proposal.setExperience(proposalSeed.experience());
                proposal.setStatus(proposalSeed.status());
                proposal.setCreatedAt(ago(proposalSeed.ago()));
                proposal.setUpdatedAt(proposal.getCreatedAt());
                proposals.save(proposal);
            }
        }
    }

    private void seedOrders(List<OrderSeed> seeds) {
        for (OrderSeed seed : seeds) {
            AppUser buyer = user(seed.buyer());
            AppOrder order = new AppOrder();
            order.setBuyer(buyer);
            order.setStatus(seed.status());
            order.setPaymentMethod(seed.payment());
            order.setDeliveryAddress(seed.address());
            order.setCreatedAt(ago(seed.ago()));
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemSeed itemSeed : seed.items()) {
                Product product = product(itemSeed.product());
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(itemSeed.quantity());
                item.setUnitPrice(product.getPrice());
                item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemSeed.quantity())));
                item.setFulfillmentStatus(fulfillmentFor(seed.status()));
                order.getItems().add(item);
                total = total.add(item.getSubtotal());
            }
            order.setTotalAmount(total);
            AppOrder saved = orders.save(order);
            if (seed.reviews() == null) continue;
            for (ProductReviewSeed reviewSeed : seed.reviews()) {
                Review review = new Review();
                review.setReviewer(buyer);
                review.setReviewedUser(user(reviewSeed.seller()));
                review.setProductOrder(saved);
                review.setRating(reviewSeed.rating());
                review.setComment(reviewSeed.comment());
                review.setCreatedAt(notAfterNow(saved.getCreatedAt().plusDays(3)));
                reviews.save(review);
            }
        }
    }

    private void seedServiceOrders(List<ServiceOrderSeed> seeds) {
        for (ServiceOrderSeed seed : seeds) {
            AppUser client = user(seed.client());
            ServiceOffer service = service(seed.service());
            ServicePackage selected = service.getPackages().stream()
                    .filter(item -> item.getTier().equals(seed.tier())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Pacote " + seed.tier() + " não existe em " + seed.service()));
            ServiceOrder order = new ServiceOrder();
            order.setClient(client);
            order.setProvider(service.getOwner());
            order.setService(service);
            order.setSelectedPackage(selected);
            order.setAgreedPackageName(selected.getName());
            order.setDeliveryDays(selected.getDeliveryDays());
            order.setAgreedPrice(selected.getPrice());
            order.setRequestDescription(seed.description());
            order.setStatus(seed.status());
            order.setCreatedAt(ago(seed.ago()));
            order.setUpdatedAt("SOLICITADO".equals(seed.status()) ? order.getCreatedAt()
                    : notAfterNow(order.getCreatedAt().plusDays(Math.min(selected.getDeliveryDays(), 7))));
            ServiceOrder saved = serviceOrders.save(order);
            if (seed.review() == null) continue;
            Review review = new Review();
            review.setReviewer(client);
            review.setReviewedUser(service.getOwner());
            review.setServiceOrder(saved);
            review.setRating(seed.review().rating());
            review.setComment(seed.review().comment());
            review.setCreatedAt(saved.getUpdatedAt());
            reviews.save(review);
        }
    }

    private void seedNegotiations(List<NegotiationSeed> seeds) {
        for (NegotiationSeed seed : seeds) {
            Product product = product(seed.product());
            ProductNegotiation negotiation = new ProductNegotiation();
            negotiation.setProduct(product);
            negotiation.setBuyer(user(seed.buyer()));
            negotiation.setSeller(product.getOwner());
            negotiation.setBuyerOffer(seed.offer());
            negotiation.setBuyerMessage(seed.message());
            negotiation.setSellerCounteroffer(seed.counter());
            negotiation.setSellerMessage(seed.sellerMessage());
            negotiation.setStatus(seed.status());
            negotiation.setCreatedAt(ago(seed.ago()));
            negotiation.setUpdatedAt("PENDENTE".equals(seed.status()) ? negotiation.getCreatedAt()
                    : notAfterNow(negotiation.getCreatedAt().plusHours(2)));
            negotiations.save(negotiation);
        }
    }

    private void seedQuestions(List<QuestionSeed> seeds) {
        for (QuestionSeed seed : seeds) {
            ListingQuestion question = new ListingQuestion();
            AppUser owner;
            if ("PRODUCT".equals(seed.type())) {
                Product product = product(seed.listing());
                question.setProduct(product);
                owner = product.getOwner();
            } else {
                ServiceOffer service = service(seed.listing());
                question.setService(service);
                owner = service.getOwner();
            }
            question.setAsker(user(seed.asker()));
            question.setQuestion(seed.question());
            question.setCreatedAt(ago(seed.ago()));
            if (seed.answer() != null) {
                question.setAnswer(seed.answer());
                question.setResponder(owner);
                question.setAnsweredAt(notAfterNow(question.getCreatedAt().plusHours(1)));
            }
            questions.save(question);
        }
    }

    private void seedConversations(List<ConversationSeed> seeds) {
        for (ConversationSeed seed : seeds) {
            AppUser first = user(seed.a());
            AppUser second = user(seed.b());
            // A API guarda o participante de menor id em A; seguir a regra evita conversas duplicadas.
            boolean firstIsLower = first.getId() < second.getId();
            Conversation conversation = new Conversation();
            conversation.setParticipantA(firstIsLower ? first : second);
            conversation.setParticipantB(firstIsLower ? second : first);
            if (seed.product() != null) conversation.setProduct(product(seed.product()));
            if (seed.service() != null) conversation.setService(service(seed.service()));
            if (seed.project() != null) conversation.setProject(project(seed.project()));
            conversation.setCreatedAt(ago(seed.messages().get(0).ago()));
            conversation.setUpdatedAt(ago(seed.messages().get(seed.messages().size() - 1).ago()));
            Conversation saved = conversations.save(conversation);
            for (MessageSeed messageSeed : seed.messages()) {
                Message message = new Message();
                message.setConversation(saved);
                message.setSender(user(messageSeed.from()));
                message.setContent(messageSeed.text());
                message.setCreatedAt(ago(messageSeed.ago()));
                message.setRead(!Boolean.TRUE.equals(messageSeed.unread()));
                messages.save(message);
            }
        }
    }

    private void seedFavorites(List<FavoriteSeed> seeds) {
        for (FavoriteSeed seed : seeds) {
            Favorite favorite = new Favorite();
            favorite.setUser(user(seed.user()));
            favorite.setTargetType(seed.type());
            favorite.setTargetId("PRODUCT".equals(seed.type()) ? product(seed.item()).getId() : service(seed.item()).getId());
            favorites.save(favorite);
        }
    }

    private void seedNotifications(List<NotificationSeed> seeds) {
        for (NotificationSeed seed : seeds) {
            MarketplaceNotification notification = new MarketplaceNotification();
            notification.setUser(user(seed.user()));
            notification.setType(seed.type());
            notification.setTitle(seed.title());
            notification.setMessage(seed.message());
            notification.setLink(seed.link());
            notification.setCreatedAt(ago(seed.ago()));
            if (Boolean.TRUE.equals(seed.read())) notification.setReadAt(notAfterNow(notification.getCreatedAt().plusMinutes(10)));
            notifications.save(notification);
        }
    }

    private String fulfillmentFor(String orderStatus) {
        return switch (orderStatus) {
            case "ENVIADO" -> "ENVIADO";
            case "CONCLUIDO" -> "CONCLUIDO";
            case "CANCELADO" -> "CANCELADO";
            default -> "PENDENTE";
        };
    }

    private String image(String key) { return imageBaseUrl.replaceAll("/+$", "") + IMAGE_DIR + key + ".jpg"; }

    private AppUser user(String key) { return Objects.requireNonNull(userByKey.get(key), () -> "Conta de demonstração desconhecida: " + key); }
    private Product product(String key) { return Objects.requireNonNull(productByKey.get(key), () -> "Produto de demonstração desconhecido: " + key); }
    private ServiceOffer service(String key) { return Objects.requireNonNull(serviceByKey.get(key), () -> "Serviço de demonstração desconhecido: " + key); }
    private Project project(String key) { return Objects.requireNonNull(projectByKey.get(key), () -> "Projeto de demonstração desconhecido: " + key); }

    private LocalDateTime notAfterNow(LocalDateTime value) { return value.isAfter(now) ? now : value; }

    /** Converte "3d", "5h" ou "40m" na data correspondente no passado. */
    private LocalDateTime ago(String value) {
        if (value == null || value.isBlank()) return now;
        long amount = Long.parseLong(value.substring(0, value.length() - 1));
        return switch (value.charAt(value.length() - 1)) {
            case 'd' -> now.minusDays(amount);
            case 'h' -> now.minusHours(amount);
            case 'm' -> now.minusMinutes(amount);
            default -> throw new IllegalArgumentException("Intervalo inválido em demo-data.json: " + value);
        };
    }

    /** Gera um CPF válido (com dígitos verificadores) que ainda não esteja em uso. */
    private String uniqueCpf(int index) {
        for (long attempt = 0; ; attempt++) {
            String base = String.format("%09d", 314_159_000L + index * 7_919L + attempt * 104_729L);
            int first = checkDigit(base, 10);
            int second = checkDigit(base + first, 11);
            String cpf = base + first + second;
            if (!users.existsByCpf(cpf)) return cpf;
        }
    }

    private static int checkDigit(String digits, int firstWeight) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) sum += (digits.charAt(i) - '0') * (firstWeight - i);
        int digit = 11 - sum % 11;
        return digit >= 10 ? 0 : digit;
    }
}
