package com.zakisupermarket.config;

import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            if (storeRepository.findByLicenseNumber("ST-2024-001").isPresent() ||
                    storeRepository.findByEmail("test@zakisupermarket.eg").isPresent()) {
                return;
            }

            Store store = Store.builder()
                    .name("Zaki Model Store")
                    .licenseNumber("ST-2024-001")
                    .email("test@zakisupermarket.eg")
                    .phone("01012345678")
                    .address("Cairo, Nasr City, Al Tayaran Street")
                    .subscriptionStatus(Store.SubscriptionStatus.ACTIVE)
                    .planType(Store.PlanType.PROFESSIONAL)
                    .build();

            storeRepository.save(store);
            User admin = User.builder()
                    .store(store)
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Ahmed Mohamed")
                    .phone("01012345678")
                    .role(User.UserRole.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);

            User pharmacist = User.builder()
                    .store(store)
                    .username("pharmacist")
                    .password(passwordEncoder.encode("pharm123"))
                    .fullName("Mohamed Ali")
                    .phone("01098765432")
                    .role(User.UserRole.PHARMACIST)
                    .isActive(true)
                    .build();

            userRepository.save(pharmacist);
            String[][] productsData = {
                    {"Full Cream Milk 1L", "1234567890123", "Dairy", "BOTTLE"},
                    {"White Bread", "1234567890124", "Bakery", "PIECE"},
                    {"Basmati Rice 5kg", "1234567890125", "Grains", "BAG"},
                    {"Sunflower Oil 1.5L", "1234567890126", "Cooking Oil", "BOTTLE"},
                    {"Fresh Eggs (30 pcs)", "1234567890127", "Dairy", "TRAY"},
                    {"White Sugar 1kg", "1234567890128", "Grocery", "BAG"},
                    {"Tomato Ketchup 500g", "1234567890129", "Condiments", "BOTTLE"},
                    {"Laundry Detergent 3kg", "1234567890130", "Household", "BOX"},
                    {"Bottled Water 1.5L", "1234567890131", "Beverages", "BOTTLE"},
                    {"Chicken Breast 1kg", "1234567890132", "Meat", "PACK"}
            };

            for (String[] prodData : productsData) {
                Product product = Product.builder()
                        .store(store)
                        .name(prodData[0])
                        .barcode(prodData[1])
                        .category(prodData[2])
                        .unitType(prodData[3])
                        .minStockLevel(10)
                        .sellPrice(new BigDecimal("25.00"))
                        .buyPrice(new BigDecimal("15.00"))
                        .build();

                productRepository.save(product);

                StockBatch batch = StockBatch.builder()
                        .product(product)
                        .store(store)
                        .batchNumber("BATCH-" + product.getId())
                        .quantityInitial(50)
                        .quantityCurrent(50)
                        .expiryDate(LocalDate.now().plusMonths(18))
                        .buyPrice(new BigDecimal("15.00"))
                        .sellPrice(new BigDecimal("25.00"))
                        .location("Shelf-" + (product.getId() % 5 + 1))
                        .status(StockBatch.BatchStatus.ACTIVE)
                        .build();

                stockBatchRepository.save(batch);
            }
        };
    }
}