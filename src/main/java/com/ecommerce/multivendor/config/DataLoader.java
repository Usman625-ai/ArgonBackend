package com.ecommerce.multivendor.config;

import com.ecommerce.multivendor.dto.request.*;
import com.ecommerce.multivendor.entity.*;
import com.ecommerce.multivendor.enums.*;
import com.ecommerce.multivendor.repository.*;
import com.ecommerce.multivendor.service.impl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final AuthService         authService;
    private final CategoryService     categoryService;
    private final ProductService      productService;
    private final AddressService      addressService;
    private final CouponService       couponService;
    private final CartService         cartService;
    private final OrderService        orderService;
    private final ReviewService       reviewService;
    private final WishlistService     wishlistService;
    private final AdminService        adminService;
    private final NotificationService notificationService;

    private final UserRepository  userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, User> users      = new LinkedHashMap<>();
    private final Map<String, Long> catIds     = new LinkedHashMap<>();
    private final Map<String, Long> productIds = new LinkedHashMap<>();
    private final Map<Long,   Long> addressMap = new LinkedHashMap<>();

    private static final String PW = "Password@123";

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("📦 DataLoader: data already exists — skipping.");
            return;
        }

        log.info("🚀 DataLoader: starting…");
        try {
            step1_createUsers();
            step2_approveAndSetupSellers();
            step3_createCategories();
            step4_createProducts();
            step5_createAddresses();
            step6_createCoupons();
            step7_createOrders();
            step8_createReviews();
            step9_createWishlists();
            log.info("✅ DataLoader: complete — {} users, {} categories, {} products",
                    users.size(), catIds.size(), productIds.size());
        } catch (Exception e) {
            log.error("❌ DataLoader failed: {}", e.getMessage(), e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void step1_createUsers() {
        log.info("  [1/9] Creating users…");

        User admin = User.builder()
                .name("usman hussain")
                .email("mrusmanhussan101@gmail.com")
                .password(passwordEncoder.encode("alishbausman19"))
                .role(Role.ADMIN)
                .active(true).verified(true)
                .contactNumber("03353580298")
                .build();
        users.put("admin", userRepository.save(admin));

        // Sellers now include shopName during registration
        users.put("seller1", reg("Rahul Sharma",  "seller1@shop.com", Role.SELLER, "TechZone Electronics"));
        users.put("seller2", reg("Priya Patel",   "seller2@shop.com", Role.SELLER, "Style Hub Fashion"));
        users.put("seller3", reg("Tariq Ahmed",   "seller3@shop.com", Role.SELLER, "HomeDecor Paradise"));
        users.put("seller4", reg("Zara Malik",    "seller4@shop.com", Role.SELLER, "VipSetup"));

        users.put("c1", reg("Amit Kumar",   "customer1@shop.com", Role.CUSTOMER, null));
        users.put("c2", reg("Sneha Joshi",  "customer2@shop.com", Role.CUSTOMER, null));
        users.put("c3", reg("Ali Hassan",   "customer3@shop.com", Role.CUSTOMER, null));
        users.put("c4", reg("Sara Ahmed",   "customer4@shop.com", Role.CUSTOMER, null));
        users.put("c5", reg("Usman Malik",  "customer5@shop.com", Role.CUSTOMER, null));
        users.put("c6", reg("Maria Khan",   "customer6@shop.com", Role.CUSTOMER, null));
        users.put("c7", reg("Fatima Noor",  "customer7@shop.com", Role.CUSTOMER, null));
        users.put("c8", reg("Bilal Sheikh", "customer8@shop.com", Role.CUSTOMER, null));
    }

    private User reg(String name, String email, Role role, String shopName) {
        RegisterRequest r = new RegisterRequest();
        r.setName(name);
        r.setEmail(email);
        r.setPassword(PW);
        r.setRole(role);
        r.setShopName(shopName);
        r.setContactNumber("03" + String.format("%09d", (long)(Math.random() * 1_000_000_000)));
        authService.register(r);
        User u = userRepository.findByEmail(email).orElseThrow();
        u.setVerified(true);
        return userRepository.save(u);
    }

    private void step2_approveAndSetupSellers() {
        log.info("  [2/9] Approving sellers…");
        adminService.approveSeller(users.get("seller1").getId());
        adminService.approveSeller(users.get("seller2").getId());
        adminService.approveSeller(users.get("seller3").getId());

        for (String key : List.of("seller1","seller2","seller3")) {
            users.put(key, userRepository.findById(users.get(key).getId()).orElseThrow());
        }

        runAs(users.get("seller1"), () -> {
            User s = users.get("seller1");
            s.setShopDescription("Pakistan's #1 authorized dealer for premium electronics.");
            s.setShopLogo("https://images.unsplash.com/photo-1518770660439-4636190af475?w=200");
            s.setShopBanner("https://images.unsplash.com/photo-1550009158-9ebf69173e03?w=800");
            s.setGstNumber("1234567890123");
            userRepository.save(s);
        });
        runAs(users.get("seller2"), () -> {
            User s = users.get("seller2");
            s.setShopDescription("Trendy Pakistani fashion for all.");
            s.setShopLogo("https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=200");
            s.setShopBanner("https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=800");
            s.setGstNumber("9876543210987");
            userRepository.save(s);
        });
        runAs(users.get("seller3"), () -> {
            User s = users.get("seller3");
            s.setShopDescription("Beautiful home decor and essentials.");
            s.setShopLogo("https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=200");
            userRepository.save(s);
        });
    }

    private void step3_createCategories() {
        log.info("  [3/9] Creating categories…");
        catIds.put("electronics", mkCat("Electronics", null, "Gadgets", "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=300", 1));
        catIds.put("fashion", mkCat("Fashion", null, "Clothing", "https://images.unsplash.com/photo-1445205170230-053b83016050?w=300", 2));
        catIds.put("home", mkCat("Home & Living", null, "Decor", "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=300", 3));

        catIds.put("smartphones", mkCat("Smartphones", catIds.get("electronics"), "Mobiles", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=300", 1));
        catIds.put("laptops", mkCat("Laptops", catIds.get("electronics"), "Computers", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=300", 2));
        catIds.put("mens", mkCat("Men's Clothing", catIds.get("fashion"), "Menswear", "https://images.unsplash.com/photo-1516257984-b1b4d707412e?w=300", 1));
        catIds.put("womens", mkCat("Women's Clothing", catIds.get("fashion"), "Womenswear", "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=300", 2));
        catIds.put("kitchen", mkCat("Kitchen", catIds.get("home"), "Cookware", "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=300", 1));
    }

    private Long mkCat(String name, Long parentId, String desc, String img, int order) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name); r.setDescription(desc); r.setImageUrl(img);
        r.setParentId(parentId); r.setActive(true); r.setDisplayOrder(order);
        return categoryService.createCategory(r).getId();
    }

    private void step4_createProducts() {
        log.info("  [4/9] Creating products…");
        User s1 = users.get("seller1"), s2 = users.get("seller2"), s3 = users.get("seller3");

        runAs(s1, () -> {
            productIds.put("s24", mkProd(s1, "Samsung Galaxy S24 Ultra", "Flagship phone", "200MP Camera", new BigDecimal("124999"), new BigDecimal("109999"), 50, "Samsung", catIds.get("smartphones"), true, "[]", "{}", "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600"));
            productIds.put("macbook", mkProd(s1, "MacBook Pro M3", "Pro Laptop", "M3 Chip", new BigDecimal("199990"), new BigDecimal("189990"), 20, "Apple", catIds.get("laptops"), true, "[]", "{}", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600"));
        });

        runAs(s2, () -> {
            productIds.put("shirt", mkProd(s2, "Oxford Shirt", "Classic Shirt", "Cotton", new BigDecimal("2499"), new BigDecimal("1799"), 100, "StyleHub", catIds.get("mens"), true, "[]", "{}", "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=600"));
            productIds.put("dress", mkProd(s2, "Floral Dress", "Wrap Dress", "Summer", new BigDecimal("3299"), new BigDecimal("2499"), 50, "StyleHub", catIds.get("womens"), true, "[]", "{}", "https://images.unsplash.com/photo-1612336307429-8a898d10e223?w=600"));
        });

        runAs(s3, () -> {
            productIds.put("airfryer", mkProd(s3, "Philips Air Fryer", "Healthy cooking", "5.6L", new BigDecimal("18999"), new BigDecimal("15999"), 30, "Philips", catIds.get("kitchen"), true, "[]", "{}", "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=600"));
        });
    }

    private Long mkProd(User seller, String name, String desc, String shortDesc, BigDecimal price, BigDecimal disc, int stock, String brand, Long catId, boolean featured, String tags, String specs, String imgUrl) {
        ProductRequest r = new ProductRequest();
        r.setName(name); r.setDescription(desc); r.setShortDescription(shortDesc);
        r.setPrice(price); r.setDiscountedPrice(disc); r.setStockQuantity(stock);
        r.setBrand(brand); r.setCategoryId(catId); r.setFeatured(featured);
        r.setTags(tags); r.setSpecifications(specs); r.setImageUrls(List.of(imgUrl));
        return productService.createProduct(r, seller).getId();
    }

    private void step5_createAddresses() {
        log.info("  [5/9] Creating addresses…");
        User c1 = users.get("c1");
        runAs(c1, () -> {
            AddressRequest r = new AddressRequest();
            r.setFullName("Amit Kumar"); r.setPhoneNumber("03114444444");
            r.setAddressLine1("House 42, Street 5"); r.setCity("Lahore");
            r.setState("Punjab"); r.setPincode("54000"); r.setDefaultAddress(true);
            addressMap.put(c1.getId(), addressService.addAddress(r, c1).getId());
        });
    }

    private void step6_createCoupons() {
        log.info("  [6/9] Creating coupons…");
        LocalDateTime from = LocalDateTime.now().minusMinutes(5);
        LocalDateTime until = LocalDateTime.now().plusYears(1);
        mkCoupon("WELCOME20", "20% off", "PERCENTAGE", 20, 500, 500.0, from, until, 1000, 1);
    }

    private void mkCoupon(String code, String desc, String type, double val, double minOrder, Double maxDisc, LocalDateTime from, LocalDateTime until, Integer limit, int perUser) {
        CouponRequest r = new CouponRequest();
        r.setCode(code); r.setDescription(desc); r.setDiscountType(DiscountType.valueOf(type));
        r.setDiscountValue(BigDecimal.valueOf(val)); r.setMinOrderValue(BigDecimal.valueOf(minOrder));
        r.setMaxDiscount(maxDisc != null ? BigDecimal.valueOf(maxDisc) : null);
        r.setValidFrom(from); r.setValidUntil(until); r.setUsageLimit(limit); r.setPerUserLimit(perUser);
        couponService.createCoupon(r);
    }

    private void step7_createOrders() {
        log.info("  [7/9] Creating orders…");
        User c1 = users.get("c1");
        runAs(c1, () -> {
            CartRequest cr = new CartRequest();
            cr.setProductId(productIds.get("s24")); cr.setQuantity(1);
            cartService.addToCart(c1, cr);

            CheckoutRequest chr = new CheckoutRequest();
            chr.setAddressId(addressMap.get(c1.getId()));
            chr.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            orderService.checkout(chr, c1);
        });
    }

    private void step8_createReviews() {
        log.info("  [8/9] Creating reviews…");
        User c1 = users.get("c1");
        runAs(c1, () -> {
            ReviewRequest r = new ReviewRequest();
            r.setProductId(productIds.get("s24")); r.setRating(5); r.setComment("Amazing phone!");
            reviewService.addReview(r, c1);
        });
    }

    private void step9_createWishlists() {
        log.info("  [9/9] Creating wishlists…");
        User c1 = users.get("c1");
        runAs(c1, () -> wishlistService.addToWishlist(productIds.get("macbook"), c1));
    }

    private void runAs(User user, Runnable action) {
        try {
            var auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}