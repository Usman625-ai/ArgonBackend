//package com.ecommerce.multivendor.config;
//
//import com.ecommerce.multivendor.dto.request.*;
//import com.ecommerce.multivendor.entity.*;
//import com.ecommerce.multivendor.enums.*;
//import com.ecommerce.multivendor.repository.*;
//import com.ecommerce.multivendor.service.impl.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Profile;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//
///**
// * Seeds the database with a small, internally-consistent demo dataset:
// * users -> categories -> 25 products (each tied to a real leaf category) ->
// * addresses -> coupons -> orders -> reviews -> wishlists.
// *
// * Every product gets 3 guaranteed-unique images, generated deterministically
// * from its own product key via picsum.photos/seed/{key}-{n}. Because the seed
// * is derived from the unique product key, no two products (and no two images
// * within a product) can ever collide — unlike a fixed pool of stock photos
// * that has to be reused/cycled once you run out of URLs.
// */
///**
// * SECURITY: This seeds a hardcoded admin account (admin@shopversee.com / Admin@123)
// * plus demo users/products/orders. It's gated to non-production profiles only —
// * it must NEVER run against a real deployment's database.
// *
// * Make sure your production deployment sets SPRING_PROFILES_ACTIVE=prod
// * (or any profile name other than the ones excluded below).
// */
//@Component
//@Profile("!prod")
//@RequiredArgsConstructor
//@Slf4j
//public class DataLoader implements CommandLineRunner {
//
//    private final AuthService authService;
//    private final CategoryService categoryService;
//    private final ProductService productService;
//    private final AddressService addressService;
//    private final CouponService couponService;
//    private final CartService cartService;
//    private final OrderService orderService;
//    private final ReviewService reviewService;
//    private final WishlistService wishlistService;
//    private final AdminService adminService;
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    private final Map<String, User> users = new LinkedHashMap<>();
//    private final Map<String, Long> catIds = new LinkedHashMap<>();
//    private final Map<String, Long> productIds = new LinkedHashMap<>();
//    private final Map<Long, Long> addressMap = new LinkedHashMap<>();
//
//    private final Random rnd = new Random(42);
//
//    private static final String PW = "Password@123";
//
//    // Seed identities — override via application.properties / env vars so real
//    // emails can be used without touching this file. Defaults match the
//    // original demo dataset.
//    @Value("${app.seed.admin.name:Usman Hussain}")
//    private String adminName;
//    @Value("${app.seed.admin.email:admin@shopversee.com}")
//    private String adminEmail;
//    @Value("${app.seed.admin.password:Admin@123}")
//    private String adminPassword;
//
//    @Value("${app.seed.seller1.email:seller1@shop.com}") private String seller1Email;
//    @Value("${app.seed.seller2.email:seller2@shop.com}") private String seller2Email;
//    @Value("${app.seed.seller3.email:seller3@shop.com}") private String seller3Email;
//    @Value("${app.seed.seller4.email:seller4@shop.com}") private String seller4Email;
//    @Value("${app.seed.seller5.email:seller5@shop.com}") private String seller5Email;
//    @Value("${app.seed.seller6.email:seller6@shop.com}") private String seller6Email;
//    @Value("${app.seed.seller7.email:seller7@shop.com}") private String seller7Email;
//    @Value("${app.seed.seller8.email:seller8@shop.com}") private String seller8Email;
//
//    @Override
//    public void run(String... args) {
//        if (userRepository.count() > 0) {
//            log.info("📦 DataLoader: data already exists — skipping.");
//            return;
//        }
//
//        log.info("🚀 DataLoader: starting with 25 products...");
//        long start = System.currentTimeMillis();
//
//        try {
//            step1_createUsers();
//            step2_approveAndSetupSellers();
//            step3_createCategories();
//            step4_createProducts();
//            step5_createAddresses();
//            step6_createCoupons();
//            step7_createOrders();
//            step8_createReviews();
//            step9_createWishlists();
//
//            long elapsed = System.currentTimeMillis() - start;
//            log.info("✅ DataLoader: complete in {}ms — {} users, {} categories, {} products",
//                    elapsed, users.size(), catIds.size(), productIds.size());
//        } catch (Exception e) {
//            log.error("❌ DataLoader failed: {}", e.getMessage(), e);
//        } finally {
//            SecurityContextHolder.clearContext();
//        }
//    }
//
//    private void step1_createUsers() {
//        log.info("  [1/9] Creating users...");
//
//        User admin = User.builder()
//                .name(adminName)
//                .email(adminEmail)
//                .password(passwordEncoder.encode(adminPassword))
//                .role(Role.ADMIN)
//                .active(true).verified(true)
//                .contactNumber("03353580298")
//                .build();
//        users.put("admin", userRepository.save(admin));
//
//        users.put("seller1", reg("Rahul Sharma", seller1Email, Role.SELLER, "TechZone Electronics"));
//        users.put("seller2", reg("Priya Patel", seller2Email, Role.SELLER, "Style Hub Fashion"));
//        users.put("seller3", reg("Tariq Ahmed", seller3Email, Role.SELLER, "HomeDecor Paradise"));
//        users.put("seller5", reg("Faisal Qureshi", seller5Email, Role.SELLER, "GadgetHub Pakistan"));
//        users.put("seller6", reg("Ayesha Raza", seller6Email, Role.SELLER, "Trendy Threads"));
//        users.put("seller7", reg("Hamza Iqbal", seller7Email, Role.SELLER, "FitLife Sports"));
//        users.put("seller4", reg("Zara Malik", seller4Email, Role.SELLER, "VipSetup"));
//        users.put("seller8", reg("Mahnoor Shah", seller8Email, Role.SELLER, "GlowUp Beauty"));
//
//        users.put("c1", reg("Amit Kumar", "customer1@shop.com", Role.CUSTOMER, null));
//        users.put("c2", reg("Sneha Joshi", "customer2@shop.com", Role.CUSTOMER, null));
//        users.put("c3", reg("Ali Hassan", "customer3@shop.com", Role.CUSTOMER, null));
//        users.put("c4", reg("Sara Ahmed", "customer4@shop.com", Role.CUSTOMER, null));
//        users.put("c5", reg("Usman Malik", "customer5@shop.com", Role.CUSTOMER, null));
//        users.put("c6", reg("Maria Khan", "customer6@shop.com", Role.CUSTOMER, null));
//        users.put("c7", reg("Fatima Noor", "customer7@shop.com", Role.CUSTOMER, null));
//        users.put("c8", reg("Bilal Sheikh", "customer8@shop.com", Role.CUSTOMER, null));
//    }
//
//    private User reg(String name, String email, Role role, String shopName) {
//        RegisterRequest r = new RegisterRequest();
//        r.setName(name); r.setEmail(email); r.setPassword(PW);
//        r.setRole(role); r.setShopName(shopName);
//        r.setContactNumber("03" + String.format("%09d", (long) (Math.random() * 1_000_000_000)));
//        authService.register(r);
//        User u = userRepository.findByEmail(email).orElseThrow();
//        u.setVerified(true);
//        return userRepository.save(u);
//    }
//
//    private void step2_approveAndSetupSellers() {
//        log.info("  [2/9] Approving sellers (2 left pending)...");
//
//        List<String> toApprove = List.of("seller1", "seller2", "seller3", "seller5", "seller6", "seller7");
//        for (String key : toApprove) {
//            adminService.approveSeller(users.get(key).getId());
//            users.put(key, userRepository.findById(users.get(key).getId()).orElseThrow());
//        }
//
//        setupShop(users.get("seller1"), "Pakistan's #1 authorized dealer for premium electronics.",
//                "https://picsum.photos/seed/shop-seller1-logo/200/200",
//                "https://picsum.photos/seed/shop-seller1-banner/1200/400", "1234567890123");
//        setupShop(users.get("seller2"), "Trendy Pakistani fashion for all.",
//                "https://picsum.photos/seed/shop-seller2-logo/200/200",
//                "https://picsum.photos/seed/shop-seller2-banner/1200/400", "9876543210987");
//        setupShop(users.get("seller3"), "Beautiful home decor and essentials.",
//                "https://picsum.photos/seed/shop-seller3-logo/200/200",
//                "https://picsum.photos/seed/shop-seller3-banner/1200/400", "1122334455667");
//        setupShop(users.get("seller5"), "Your one-stop shop for cameras, audio, and wearables.",
//                "https://picsum.photos/seed/shop-seller5-logo/200/200",
//                "https://picsum.photos/seed/shop-seller5-banner/1200/400", "2233445566778");
//        setupShop(users.get("seller6"), "Footwear and accessories that keep you on-trend.",
//                "https://picsum.photos/seed/shop-seller6-logo/200/200",
//                "https://picsum.photos/seed/shop-seller6-banner/1200/400", "3344556677889");
//        setupShop(users.get("seller7"), "Fitness gear and outdoor equipment for every adventure.",
//                "https://picsum.photos/seed/shop-seller7-logo/200/200",
//                "https://picsum.photos/seed/shop-seller7-banner/1200/400", "4455667788990");
//    }
//
//    private void setupShop(User seller, String desc, String logo, String banner, String gst) {
//        runAs(seller, () -> {
//            seller.setShopDescription(desc);
//            seller.setShopLogo(logo);
//            seller.setShopBanner(banner);
//            seller.setGstNumber(gst);
//            userRepository.save(seller);
//        });
//    }
//
//    private void step3_createCategories() {
//        log.info("  [3/9] Creating categories...");
//
//        catIds.put("electronics", mkCat("Electronics", null, "Gadgets & devices", "https://picsum.photos/seed/cat-electronics/300/300", 1));
//        catIds.put("fashion", mkCat("Fashion", null, "Clothing & accessories", "https://picsum.photos/seed/cat-fashion/300/300", 2));
//        catIds.put("home", mkCat("Home & Living", null, "Decor & furniture", "https://picsum.photos/seed/cat-home/300/300", 3));
//        catIds.put("sports", mkCat("Sports & Outdoors", null, "Fitness & outdoor gear", "https://picsum.photos/seed/cat-sports/300/300", 4));
//
//        catIds.put("smartphones", mkCat("Smartphones", catIds.get("electronics"), "Mobile phones", "https://picsum.photos/seed/cat-smartphones/300/300", 1));
//        catIds.put("laptops", mkCat("Laptops", catIds.get("electronics"), "Computers", "https://picsum.photos/seed/cat-laptops/300/300", 2));
//        catIds.put("audio", mkCat("Audio", catIds.get("electronics"), "Headphones & speakers", "https://picsum.photos/seed/cat-audio/300/300", 3));
//        catIds.put("cameras", mkCat("Cameras", catIds.get("electronics"), "Cameras & accessories", "https://picsum.photos/seed/cat-cameras/300/300", 4));
//        catIds.put("wearables", mkCat("Wearables", catIds.get("electronics"), "Smartwatches & bands", "https://picsum.photos/seed/cat-wearables/300/300", 5));
//
//        catIds.put("mens", mkCat("Men's Clothing", catIds.get("fashion"), "Menswear", "https://picsum.photos/seed/cat-mens/300/300", 1));
//        catIds.put("womens", mkCat("Women's Clothing", catIds.get("fashion"), "Womenswear", "https://picsum.photos/seed/cat-womens/300/300", 2));
//        catIds.put("footwear", mkCat("Footwear", catIds.get("fashion"), "Shoes & sandals", "https://picsum.photos/seed/cat-footwear/300/300", 3));
//
//        catIds.put("kitchen", mkCat("Kitchen", catIds.get("home"), "Cookware & appliances", "https://picsum.photos/seed/cat-kitchen/300/300", 1));
//        catIds.put("furniture", mkCat("Furniture", catIds.get("home"), "Sofas, tables & more", "https://picsum.photos/seed/cat-furniture/300/300", 2));
//
//        catIds.put("fitness", mkCat("Fitness Equipment", catIds.get("sports"), "Home gym gear", "https://picsum.photos/seed/cat-fitness/300/300", 1));
//        catIds.put("outdoor", mkCat("Outdoor Gear", catIds.get("sports"), "Camping & hiking", "https://picsum.photos/seed/cat-outdoor/300/300", 2));
//    }
//
//    private Long mkCat(String name, Long parentId, String desc, String img, int order) {
//        CategoryRequest r = new CategoryRequest();
//        r.setName(name); r.setDescription(desc); r.setImageUrl(img);
//        r.setParentId(parentId); r.setActive(true); r.setDisplayOrder(order);
//        return categoryService.createCategory(r).getId();
//    }
//
//    /**
//     * 25 products total, each mapped to a real leaf category that exists in
//     * catIds. Distribution: smartphones(3), laptops(3), audio(2), cameras(2),
//     * wearables(2), mens(2), womens(2), footwear(2), kitchen(2), furniture(2),
//     * fitness(2), outdoor(1) = 25.
//     */
//    private void step4_createProducts() {
//        log.info("  [4/9] Creating 25 products, each with 3 unique images...");
//
//        runAs(users.get("seller1"), () -> {
//            createProducts(users.get("seller1"), catIds.get("smartphones"),
//                    new String[]{"Galaxy S24 Ultra", "iPhone 15 Pro Max", "Pixel 8 Pro"},
//                    new String[]{"Samsung", "Apple", "Google"},
//                    new String[]{"s24", "iphone15", "pixel8"},
//                    159999, "smartphones");
//            createProducts(users.get("seller1"), catIds.get("laptops"),
//                    new String[]{"MacBook Pro 16 M3", "Dell XPS 15", "ThinkPad X1 Carbon"},
//                    new String[]{"Apple", "Dell", "Lenovo"},
//                    new String[]{"mbp16", "xps15", "x1c"},
//                    219999, "laptops");
//        });
//
//        runAs(users.get("seller5"), () -> {
//            createProducts(users.get("seller5"), catIds.get("audio"),
//                    new String[]{"Sony WH-1000XM5", "AirPods Pro 2"},
//                    new String[]{"Sony", "Apple"},
//                    new String[]{"xm5", "ap2"},
//                    34999, "audio");
//            createProducts(users.get("seller5"), catIds.get("cameras"),
//                    new String[]{"Canon EOS R50", "Sony Alpha A7 IV"},
//                    new String[]{"Canon", "Sony"},
//                    new String[]{"r50", "a7iv"},
//                    179999, "cameras");
//            createProducts(users.get("seller5"), catIds.get("wearables"),
//                    new String[]{"Apple Watch Series 10", "Samsung Galaxy Watch 7"},
//                    new String[]{"Apple", "Samsung"},
//                    new String[]{"aw10", "gw7"},
//                    49999, "wearables");
//        });
//
//        runAs(users.get("seller2"), () -> {
//            createProducts(users.get("seller2"), catIds.get("mens"),
//                    new String[]{"Oxford Formal Shirt", "Slim Fit Chinos"},
//                    new String[]{"StyleHub", "StyleHub"},
//                    new String[]{"oxford", "chinos"},
//                    3499, "mens");
//            createProducts(users.get("seller2"), catIds.get("womens"),
//                    new String[]{"Floral Wrap Dress", "Embroidered Kurti"},
//                    new String[]{"StyleHub", "StyleHub"},
//                    new String[]{"floral", "kurti"},
//                    4999, "womens");
//        });
//
//        runAs(users.get("seller6"), () -> {
//            createProducts(users.get("seller6"), catIds.get("footwear"),
//                    new String[]{"Nike Air Max Running", "Leather Formal Shoes"},
//                    new String[]{"Nike", "Trendy Threads"},
//                    new String[]{"airmax", "formal"},
//                    5999, "footwear");
//        });
//
//        runAs(users.get("seller3"), () -> {
//            createProducts(users.get("seller3"), catIds.get("kitchen"),
//                    new String[]{"Philips Air Fryer 5.6L", "Instant Pot Duo 7-in-1"},
//                    new String[]{"Philips", "Instant Pot"},
//                    new String[]{"fryer", "pot"},
//                    14999, "kitchen");
//            createProducts(users.get("seller3"), catIds.get("furniture"),
//                    new String[]{"3-Seater Fabric Sofa", "Wood Dining Table Set"},
//                    new String[]{"HomeDecor", "HomeDecor"},
//                    new String[]{"sofa", "dining"},
//                    24999, "furniture");
//        });
//
//        runAs(users.get("seller7"), () -> {
//            createProducts(users.get("seller7"), catIds.get("fitness"),
//                    new String[]{"Adjustable Dumbbell Set", "Premium Yoga Mat"},
//                    new String[]{"FitLife", "FitLife"},
//                    new String[]{"dumbbell", "yoga"},
//                    12999, "fitness");
//            createProducts(users.get("seller7"), catIds.get("outdoor"),
//                    new String[]{"4-Person Camping Tent"},
//                    new String[]{"FitLife"},
//                    new String[]{"tent"},
//                    8999, "outdoor");
//        });
//
//        log.info("      → {} products created", productIds.size());
//    }
//
//    private void createProducts(User seller, Long catId, String[] names, String[] brands,
//                                String[] keys, double basePrice, String categoryKey) {
//        for (int i = 0; i < names.length; i++) {
//            String brand = brands[i];
//            String key = keys[i];
//            double variance = 0.75 + rnd.nextDouble() * 0.5;
//            BigDecimal price = BigDecimal.valueOf(Math.round(basePrice * variance / 10.0) * 10);
//            double discPct = 0.08 + rnd.nextDouble() * 0.22;
//            BigDecimal discounted = price.multiply(BigDecimal.valueOf(1 - discPct)).setScale(0, RoundingMode.HALF_UP);
//            int stock = 15 + rnd.nextInt(85);
//            boolean featured = rnd.nextInt(5) == 0;
//
//            List<String> productImages = genImages(key);
//            String primaryImageUrl = productImages.get(0);
//
//            Long id = mkProd(seller, names[i],
//                    names[i] + " by " + brand + ". Genuine product with official warranty. Fast delivery across Pakistan. Easy 7-day returns.",
//                    brand + " — " + names[i], price, discounted, stock, brand, catId, featured,
//                    "[\"" + brand.toLowerCase() + "\",\"" + categoryKey + "\",\"premium\"]",
//                    "{\"color\":\"Multiple\",\"warranty\":\"1 Year\",\"origin\":\"Imported\"}",
//                    productImages, primaryImageUrl);
//
//            productIds.put(key, id);
//        }
//    }
//
//    /**
//     * Generates 3 guaranteed-unique image URLs for a product. Because the
//     * seed is the product's own unique key (plus an index 1-3), no product
//     * can ever share an image with another product, and no two images
//     * within the same product can collide either.
//     */
//    private List<String> genImages(String productKey) {
//        List<String> imgs = new ArrayList<>(3);
//        for (int j = 1; j <= 3; j++) {
//            imgs.add("https://picsum.photos/seed/" + productKey + "-" + j + "/800/800");
//        }
//        return imgs;
//    }
//
//    private Long mkProd(User seller, String name, String desc, String shortDesc,
//                        BigDecimal price, BigDecimal disc, int stock, String brand,
//                        Long catId, boolean featured, String tags, String specs,
//                        List<String> imageUrls, String primaryImageUrl) {
//        ProductRequest r = new ProductRequest();
//        r.setName(name); r.setDescription(desc); r.setShortDescription(shortDesc);
//        r.setPrice(price); r.setDiscountedPrice(disc); r.setStockQuantity(stock);
//        r.setBrand(brand); r.setCategoryId(catId); r.setFeatured(featured);
//        r.setTags(tags); r.setSpecifications(specs);
//        r.setImageUrls(imageUrls);
//        r.setPrimaryImageUrl(primaryImageUrl);
//        return productService.createProduct(r, seller).getId();
//    }
//
//    private void step5_createAddresses() {
//        log.info("  [5/9] Creating addresses...");
//        mkAddress("c1", "Amit Kumar", "03114444444", "House 42, Street 5", "Lahore", "Punjab", "54000");
//        mkAddress("c2", "Sneha Joshi", "03114444445", "Flat 7B, Clifton Block 4", "Karachi", "Sindh", "75500");
//        mkAddress("c3", "Ali Hassan", "03114444446", "House 12, F-10/2", "Islamabad", "ICT", "44000");
//        mkAddress("c4", "Sara Ahmed", "03114444447", "Street 3, DHA Phase 5", "Karachi", "Sindh", "75500");
//        mkAddress("c5", "Usman Malik", "03114444448", "House 88, Model Town", "Lahore", "Punjab", "54000");
//        mkAddress("c6", "Maria Khan", "03114444449", "Apartment 4, Gulshan-e-Iqbal", "Karachi", "Sindh", "75500");
//    }
//
//    private void mkAddress(String custKey, String fullName, String phone, String line1,
//                           String city, String state, String pincode) {
//        User c = users.get(custKey);
//        runAs(c, () -> {
//            AddressRequest r = new AddressRequest();
//            r.setFullName(fullName); r.setPhoneNumber(phone);
//            r.setAddressLine1(line1); r.setCity(city);
//            r.setState(state); r.setPincode(pincode); r.setDefaultAddress(true);
//            addressMap.put(c.getId(), addressService.addAddress(r, c).getId());
//        });
//    }
//
//    private void step6_createCoupons() {
//        log.info("  [6/9] Creating coupons...");
//        LocalDate from = LocalDate.now().plusDays(1);
//        LocalDate until = from.plusMonths(1);
//        mkCoupon("WELCOME20", "20% off for new customers", "PERCENTAGE", 20, 500, 1500.0, from, until, 1000, 1);
//        mkCoupon("FLAT500", "Flat Rs. 500 off", "FIXED", 500, 3000, null, from, until, 500, 2);
//        mkCoupon("SEASON10", "10% seasonal discount", "PERCENTAGE", 10, 1000, 1000.0, from, until, 2000, 3);
//    }
//
//    private void mkCoupon(String code, String desc, String type, double val, double minOrder,
//                          Double maxDisc, LocalDate from, LocalDate until, Integer limit, int perUser) {
//        CouponRequest r = new CouponRequest();
//        r.setCode(code); r.setDescription(desc); r.setDiscountType(DiscountType.valueOf(type));
//        r.setDiscountValue(BigDecimal.valueOf(val)); r.setMinOrderValue(BigDecimal.valueOf(minOrder));
//        r.setMaxDiscount(maxDisc != null ? BigDecimal.valueOf(maxDisc) : null);
//        r.setValidFrom(from); r.setValidUntil(until); r.setUsageLimit(limit); r.setPerUserLimit(perUser);
//        couponService.createCoupon(r);
//    }
//
//    private void step7_createOrders() {
//        log.info("  [7/9] Creating orders...");
//        placeOrder("c1", "s24", 1);
//        placeOrder("c1", "oxford", 2);
//        placeOrder("c2", "mbp16", 1);
//        placeOrder("c3", "floral", 1);
//        placeOrder("c4", "fryer", 1);
//        placeOrder("c5", "iphone15", 1);
//        placeOrder("c6", "chinos", 3);
//        placeOrder("c2", "pot", 2);
//    }
//
//    private void placeOrder(String custKey, String productKey, int qty) {
//        User c = users.get(custKey);
//        Long addressId = addressMap.get(c.getId());
//        if (addressId == null) return;
//        runAs(c, () -> {
//            CartRequest cr = new CartRequest();
//            cr.setProductId(productIds.get(productKey)); cr.setQuantity(qty);
//            cartService.addToCart(c, cr);
//            CheckoutRequest chr = new CheckoutRequest();
//            chr.setAddressId(addressId);
//            chr.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
//            orderService.checkout(chr, c);
//        });
//    }
//
//    private void step8_createReviews() {
//        log.info("  [8/9] Creating reviews...");
//        mkReview("c1", "s24", 5, "Amazing phone, camera is outstanding! Battery lasts 2 days easily.");
//        mkReview("c1", "oxford", 4, "Good quality fabric, fits true to size. Color slightly darker than photo.");
//        mkReview("c2", "mbp16", 5, "Blazing fast M3 chip, battery lasts all day for development work.");
//        mkReview("c2", "pot", 4, "Makes healthy cooking so much easier. 7 functions in one device.");
//        mkReview("c3", "floral", 5, "Beautiful design, exactly as pictured. Perfect for summer weddings.");
//        mkReview("c4", "fryer", 3, "Good but a bit noisy on high heat. Cleaning is easy though.");
//        mkReview("c5", "iphone15", 4, "Great phone, a little pricey but worth it for the ecosystem.");
//        mkReview("c6", "chinos", 5, "Perfect for office wear, highly recommend. Very comfortable fit.");
//        mkReview("c7", "mbp16", 5, "Best laptop I've owned for coding. The display is incredible.");
//        mkReview("c8", "floral", 4, "Nice material, delivery was quick. Will order again.");
//    }
//
//    private void mkReview(String custKey, String productKey, int rating, String comment) {
//        User c = users.get(custKey);
//        runAs(c, () -> {
//            ReviewRequest r = new ReviewRequest();
//            r.setProductId(productIds.get(productKey)); r.setRating(rating); r.setComment(comment);
//            reviewService.addReview(r, c);
//        });
//    }
//
//    private void step9_createWishlists() {
//        log.info("  [9/9] Creating wishlists...");
//        mkWishlist("c1", "mbp16");
//        mkWishlist("c2", "s24");
//        mkWishlist("c3", "iphone15");
//        mkWishlist("c4", "oxford");
//        mkWishlist("c5", "floral");
//        mkWishlist("c6", "mbp16");
//        mkWishlist("c7", "fryer");
//        mkWishlist("c8", "s24");
//    }
//
//    private void mkWishlist(String custKey, String productKey) {
//        User c = users.get(custKey);
//        runAs(c, () -> wishlistService.addToWishlist(productIds.get(productKey), c));
//    }
//
//    private void runAs(User user, Runnable action) {
//        try {
//            var auth = new UsernamePasswordAuthenticationToken(
//                    user.getEmail(), null,
//                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
//            );
//            SecurityContextHolder.getContext().setAuthentication(auth);
//            action.run();
//        } finally {
//            SecurityContextHolder.clearContext();
//        }
//    }
//}