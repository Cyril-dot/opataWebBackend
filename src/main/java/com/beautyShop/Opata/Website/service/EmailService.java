package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.entity.GeneralProduct;
import com.beautyShop.Opata.Website.entity.Product;
import com.beautyShop.Opata.Website.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String SHOP_NAME    = "Opata Beauty Shop";
    private static final String FOOTER_TEXT  = "Opata Beauty Shop — Your Favourite Clothing & Fashion Destination";
    private static final String HEADER_BG    = "#c2185b";
    private static final String ACCENT_COLOR = "#880e4f";

    // ─────────────────────────────────────────────────────────
    // CHAT NOTIFICATIONS
    // ─────────────────────────────────────────────────────────

    @Async
    public void notifyAdminOfUserMessage(String adminEmail, String adminName,
                                         String customerName, String productName,
                                         String messageContent, Long chatRoomId) {

        String subject = SHOP_NAME + " | 💬 New message from " + customerName
                + " about \"" + productName + "\"";

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">💬 New Customer Message</h2>
                  <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
                </div>
                <div style="padding:24px;">
                  <p>Hi <strong>%s</strong>,</p>
                  <p><strong>%s</strong> sent you a message about <strong>"%s"</strong>:</p>
                  <div style="background:#fce4ec; padding:16px; border-left:4px solid %s; border-radius:4px; margin:16px 0;">
                    <p style="margin:0; font-size:15px;">"%s"</p>
                  </div>
                  <a href="http://localhost:8080/chat/%d"
                     style="background:%s; color:white; padding:12px 24px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:8px;">
                    Reply to Customer
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
              </div>
            </body></html>
            """.formatted(
                HEADER_BG, SHOP_NAME,
                adminName, customerName, productName,
                ACCENT_COLOR, messageContent,
                chatRoomId, ACCENT_COLOR,
                FOOTER_TEXT
        );

        sendHtmlEmail(adminEmail, subject, body);
    }

    @Async
    public void notifyUserOfAdminReply(String userEmail, String userName,
                                       String adminName, String productName,
                                       String messageContent, Long chatRoomId) {

        String subject = SHOP_NAME + " | 💬 " + adminName + " replied about \"" + productName + "\"";

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">💬 You have a new reply!</h2>
                  <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
                </div>
                <div style="padding:24px;">
                  <p>Hi <strong>%s</strong>,</p>
                  <p><strong>%s</strong> replied to your enquiry about <strong>"%s"</strong>:</p>
                  <div style="background:#fce4ec; padding:16px; border-left:4px solid #28a745; border-radius:4px; margin:16px 0;">
                    <p style="margin:0; font-size:15px;">"%s"</p>
                  </div>
                  <a href="http://localhost:8080/chat/%d"
                     style="background:#28a745; color:white; padding:12px 24px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:8px;">
                    View Conversation
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
              </div>
            </body></html>
            """.formatted(
                HEADER_BG, SHOP_NAME,
                userName, adminName, productName,
                messageContent, chatRoomId,
                FOOTER_TEXT
        );

        sendHtmlEmail(userEmail, subject, body);
    }

    // ─────────────────────────────────────────────────────────
    // CLOTHING PRODUCT ANNOUNCEMENT
    // ─────────────────────────────────────────────────────────

    @Async
    public void announceNewProductToAllUsers(List<User> allUsers,
                                             Product product,
                                             String adminShopName) {

        String subject = SHOP_NAME + " | 🆕 New arrival: " + product.getName() + " just dropped!";

        String sizesText = (product.getAvailableSizes() != null && !product.getAvailableSizes().isEmpty())
                ? product.getAvailableSizes().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("N/A")
                : "N/A";

        String colorsText = (product.getAvailableColors() != null && !product.getAvailableColors().isEmpty())
                ? product.getAvailableColors().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("N/A")
                : "N/A";

        String discountBadge = buildDiscountBadge(product.getDiscountPercentage());
        String priceBlock    = buildPriceBlock(product.getPrice(), product.getFinalPrice(), product.getDiscountPercentage());

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">✨ New Fashion Drop!</h2>
                  <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
                </div>
                %s
                <div style="padding:24px;">
                  %s
                  <h2 style="margin:0 0 8px;">%s</h2>
                  %s
                  <p style="color:#555; line-height:1.6; margin-bottom:16px;">%s</p>
                  <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Category</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Sub-Category</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Brand</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Sizes</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Colors</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Material</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Style</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:8px 0; color:#888;">In Stock</td>
                      <td style="padding:8px 0; font-weight:bold;">%d units</td>
                    </tr>
                  </table>
                  <a href="http://localhost:8080/products/%d"
                     style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:24px; font-size:15px; font-weight:bold;">
                    🛍️ Shop Now
                  </a>
                </div>
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
              </div>
            </body></html>
            """.formatted(
                HEADER_BG, SHOP_NAME,
                imageTag(product.getPrimaryImageUrl()),
                discountBadge,
                product.getName(),
                priceBlock,
                product.getDescription() != null ? product.getDescription() : "Check out our latest fashion piece!",
                product.getCategory(),
                product.getSubCategory() != null ? product.getSubCategory().name() : "N/A",
                product.getBrand() != null ? product.getBrand() : "N/A",
                sizesText,
                colorsText,
                product.getMaterial() != null ? product.getMaterial() : "N/A",
                product.getStyle() != null ? product.getStyle() : "N/A",
                product.getStock(),
                product.getId(),
                ACCENT_COLOR,
                FOOTER_TEXT
        );

        allUsers.forEach(user -> sendHtmlEmail(user.getEmail(), subject, body));
        System.out.println("📧 Clothing announcement sent to " + allUsers.size() + " users for: " + product.getName());
    }

    // ─────────────────────────────────────────────────────────
    // GENERAL PRODUCT ANNOUNCEMENT
    // ─────────────────────────────────────────────────────────

    @Async
    public void announceNewGeneralProductToAllUsers(List<User> allUsers,
                                                    GeneralProduct product,
                                                    String adminShopName) {

        String subject = SHOP_NAME + " | 🆕 New product: " + product.getName() + " now available!";

        String discountBadge = buildDiscountBadge(product.getDiscountPercentage());
        String priceBlock    = buildPriceBlock(product.getPrice(), product.getFinalPrice(), product.getDiscountPercentage());

        // Build tags string e.g. "sale, trending, new arrival"
        String tagsText = (product.getTags() != null && !product.getTags().isEmpty())
                ? String.join(", ", product.getTags())
                : "N/A";

        // Build attributes rows for the table — one row per key-value pair
        StringBuilder attributeRows = new StringBuilder();
        if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
            product.getAttributes().forEach((key, value) ->
                    attributeRows.append("""
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">%s</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    """.formatted(key, value))
            );
        }

        String body = """
            <html><body style="font-family: Arial, sans-serif; color: #333;">
              <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">

                <!-- Header -->
                <div style="background:%s; padding:20px; color:white;">
                  <h2 style="margin:0;">🛒 New Product Available!</h2>
                  <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
                </div>

                <!-- Product Image -->
                %s

                <!-- Product Details -->
                <div style="padding:24px;">
                  %s
                  <h2 style="margin:0 0 8px;">%s</h2>
                  %s
                  <p style="color:#555; line-height:1.6; margin-bottom:16px;">%s</p>

                  <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Category</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Sub-Category</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Brand</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">SKU</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Unit</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">Tags</td>
                      <td style="padding:8px 0; font-weight:bold;">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #eee;">
                      <td style="padding:8px 0; color:#888;">In Stock</td>
                      <td style="padding:8px 0; font-weight:bold;">%d units</td>
                    </tr>
                    %s
                  </table>

                  <a href="http://localhost:8080/general-products/%d"
                     style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                            text-decoration:none; display:inline-block; margin-top:24px;
                            font-size:15px; font-weight:bold;">
                    🛍️ View Product
                  </a>
                </div>

                <!-- Footer -->
                <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
              </div>
            </body></html>
            """.formatted(
                HEADER_BG,
                SHOP_NAME,
                imageTag(product.getPrimaryImageUrl()),
                discountBadge,
                product.getName(),
                priceBlock,
                product.getDescription() != null ? product.getDescription() : "Check out this amazing new product!",
                product.getCategory(),
                product.getSubCategory() != null ? product.getSubCategory() : "N/A",
                product.getBrand() != null ? product.getBrand() : "N/A",
                product.getSku() != null ? product.getSku() : "N/A",
                product.getUnit() != null ? product.getUnit() : "N/A",
                tagsText,
                product.getStock(),
                attributeRows.toString(),
                product.getId(),
                ACCENT_COLOR,
                FOOTER_TEXT
        );

        allUsers.forEach(user -> sendHtmlEmail(user.getEmail(), subject, body));
        System.out.println("📧 General product announcement sent to " + allUsers.size() + " users for: " + product.getName());
    }

    // ─────────────────────────────────────────────────────────
// ORDER CHAT NOTIFICATION — sent to USER when admin opens an order chat
// ─────────────────────────────────────────────────────────

    @Async
    public void notifyUserOfOrderChat(String userEmail, String userName,
                                      String adminName, Long orderId,
                                      Long chatRoomId) {

        String subject = SHOP_NAME + " | 📦 Update on your Order #" + orderId;

        String body = """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
            <div style="background:%s; padding:20px; color:white;">
              <h2 style="margin:0;">📦 Order Update</h2>
              <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
            </div>
            <div style="padding:24px;">
              <p>Hi <strong>%s</strong>,</p>
              <p><strong>%s</strong> from %s has sent you a message regarding your <strong>Order #%d</strong>.</p>
              <p style="color:#555;">Click the button below to view the message and reply.</p>
              <a href="http://localhost:8080/chat/%d"
                 style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                        text-decoration:none; display:inline-block; margin-top:16px;
                        font-size:15px; font-weight:bold;">
                View Message
              </a>
            </div>
            <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
          </div>
        </body></html>
        """.formatted(
                HEADER_BG, SHOP_NAME,
                userName, adminName, SHOP_NAME, orderId,
                chatRoomId, ACCENT_COLOR,
                FOOTER_TEXT
        );

        sendHtmlEmail(userEmail, subject, body);
    }

    // ── NOTIFY ADMIN OF NEW DELIVERY REQUEST ─────────────────────
    @Async
    public void notifyAdminOfDeliveryRequest(String adminEmail, String adminName,
                                             String customerName, Long deliveryId,
                                             String deliveryAddress) {

        String subject = SHOP_NAME + " | 🚚 New delivery request #" + deliveryId + " from " + customerName;

        String body = """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
            <div style="background:%s; padding:20px; color:white;">
              <h2 style="margin:0;">🚚 New Delivery Request</h2>
              <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
            </div>
            <div style="padding:24px;">
              <p>Hi <strong>%s</strong>,</p>
              <p><strong>%s</strong> has submitted a new delivery request.</p>
              <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                <tr style="border-bottom:1px solid #eee;">
                  <td style="padding:8px 0; color:#888;">Delivery ID</td>
                  <td style="padding:8px 0; font-weight:bold;">#%d</td>
                </tr>
                <tr style="border-bottom:1px solid #eee;">
                  <td style="padding:8px 0; color:#888;">Customer</td>
                  <td style="padding:8px 0; font-weight:bold;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0; color:#888;">Delivery Address</td>
                  <td style="padding:8px 0; font-weight:bold;">%s</td>
                </tr>
              </table>
              <a href="http://localhost:8080/admin/deliveries/%d"
                 style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                        text-decoration:none; display:inline-block; margin-top:24px;
                        font-size:15px; font-weight:bold;">
                View Delivery Request
              </a>
            </div>
            <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
          </div>
        </body></html>
        """.formatted(
                HEADER_BG, SHOP_NAME,
                adminName, customerName,
                deliveryId, customerName, deliveryAddress,
                deliveryId, ACCENT_COLOR,
                FOOTER_TEXT
        );

        sendHtmlEmail(adminEmail, subject, body);
    }

    // ── NOTIFY USER OF DELIVERY STATUS UPDATE ────────────────────
    @Async
    public void notifyUserOfDeliveryStatusUpdate(String userEmail, String userName,
                                                 Long deliveryId, String oldStatus,
                                                 String newStatus, String trackingNumber) {

        String subject = SHOP_NAME + " | 📦 Your Delivery #" + deliveryId + " is now " + newStatus;

        // Status color — green for delivered, red for failed/cancelled, pink for others
        String statusColor = switch (newStatus) {
            case "DELIVERED"  -> "#28a745";
            case "FAILED", "CANCELLED" -> "#e53935";
            default -> ACCENT_COLOR;
        };

        // Only show tracking row if tracking number exists
        String trackingRow = (trackingNumber != null && !trackingNumber.isBlank())
                ? """
              <tr style="border-bottom:1px solid #eee;">
                <td style="padding:8px 0; color:#888;">Tracking Number</td>
                <td style="padding:8px 0; font-weight:bold;">%s</td>
              </tr>
              """.formatted(trackingNumber)
                : "";

        String body = """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
            <div style="background:%s; padding:20px; color:white;">
              <h2 style="margin:0;">📦 Delivery Update</h2>
              <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
            </div>
            <div style="padding:24px;">
              <p>Hi <strong>%s</strong>,</p>
              <p>There's an update on your delivery!</p>
              <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                <tr style="border-bottom:1px solid #eee;">
                  <td style="padding:8px 0; color:#888;">Delivery ID</td>
                  <td style="padding:8px 0; font-weight:bold;">#%d</td>
                </tr>
                <tr style="border-bottom:1px solid #eee;">
                  <td style="padding:8px 0; color:#888;">Previous Status</td>
                  <td style="padding:8px 0; color:#888;">%s</td>
                </tr>
                <tr style="border-bottom:1px solid #eee;">
                  <td style="padding:8px 0; color:#888;">New Status</td>
                  <td style="padding:8px 0; font-weight:bold; color:%s;">%s</td>
                </tr>
                %s
              </table>
              <a href="http://localhost:8080/deliveries/%d"
                 style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                        text-decoration:none; display:inline-block; margin-top:24px;
                        font-size:15px; font-weight:bold;">
                Track My Delivery
              </a>
            </div>
            <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
          </div>
        </body></html>
        """.formatted(
                HEADER_BG, SHOP_NAME,
                userName,
                deliveryId,
                oldStatus,
                statusColor, newStatus,
                trackingRow,
                deliveryId, ACCENT_COLOR,
                FOOTER_TEXT
        );

        sendHtmlEmail(userEmail, subject, body);
    }

    // ── NOTIFY USER OF NEW DELIVERY CHAT ─────────────────────────
    @Async
    public void notifyUserOfDeliveryChat(String userEmail, String userName,
                                         String adminName, Long deliveryId,
                                         Long chatRoomId) {

        String subject = SHOP_NAME + " | 🚚 Message about your Delivery #" + deliveryId;

        String body = """
        <html><body style="font-family: Arial, sans-serif; color: #333;">
          <div style="max-width:600px; margin:auto; border:1px solid #ddd; border-radius:8px; overflow:hidden;">
            <div style="background:%s; padding:20px; color:white;">
              <h2 style="margin:0;">🚚 Delivery Message</h2>
              <p style="margin:4px 0 0; opacity:0.8; font-size:13px;">%s</p>
            </div>
            <div style="padding:24px;">
              <p>Hi <strong>%s</strong>,</p>
              <p><strong>%s</strong> has sent you a message about your <strong>Delivery #%d</strong>.</p>
              <p style="color:#555;">Click below to view the message and reply.</p>
              <a href="http://localhost:8080/chat/%d"
                 style="background:%s; color:white; padding:14px 32px; border-radius:6px;
                        text-decoration:none; display:inline-block; margin-top:16px;
                        font-size:15px; font-weight:bold;">
                View Message
              </a>
            </div>
            <div style="padding:12px 24px; background:#f9f9f9; color:#888; font-size:12px;">%s</div>
          </div>
        </body></html>
        """.formatted(
                HEADER_BG, SHOP_NAME,
                userName, adminName, deliveryId,
                chatRoomId, ACCENT_COLOR,
                FOOTER_TEXT
        );

        sendHtmlEmail(userEmail, subject, body);
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    /** Renders a red discount badge or empty string if no discount. */
    private String buildDiscountBadge(java.math.BigDecimal discountPercentage) {
        if (discountPercentage == null || discountPercentage.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "";
        }
        return "<div style='display:inline-block; background:#e53935; color:white; padding:4px 12px;" +
                "border-radius:20px; font-size:13px; margin-bottom:12px;'>🔥 " + discountPercentage + "% OFF</div>";
    }

    /** Renders price block — strikethrough original + bold final if discounted, else just price. */
    private String buildPriceBlock(java.math.BigDecimal price,
                                   java.math.BigDecimal finalPrice,
                                   java.math.BigDecimal discountPercentage) {
        if (discountPercentage != null && discountPercentage.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return "<p style='margin:0 0 16px;'>"
                    + "<span style='text-decoration:line-through; color:#aaa; font-size:16px;'>₵" + price + "</span>"
                    + "&nbsp;&nbsp;"
                    + "<span style='color:" + ACCENT_COLOR + "; font-size:24px; font-weight:bold;'>₵" + finalPrice + "</span>"
                    + "</p>";
        }
        return "<p style='color:" + ACCENT_COLOR + "; font-size:24px; font-weight:bold; margin:0 0 16px;'>₵" + price + "</p>";
    }

    /** Renders a product image tag or empty string if no image. */
    private String imageTag(String imageUrl) {
        return imageUrl != null
                ? "<img src='" + imageUrl + "' style='width:100%; max-height:350px; object-fit:cover;'/>"
                : "";
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("📧 Email sent to: " + to + " | Subject: " + subject);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}