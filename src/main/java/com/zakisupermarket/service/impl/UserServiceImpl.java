package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.UserRequest;
import com.zakisupermarket.dto.response.UserResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(Long storeId) {
        return userRepository.findByStoreId(storeId)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id, Long storeId) {
        User user = userRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (request.getUsername() != null) {
            request.setUsername(request.getUsername().trim());
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("Store not found"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .store(store)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: {} for store {}", saved.getUsername(), store.getId());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request, Long storeId) {
        User user = userRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUsername() != null) {
            request.setUsername(request.getUsername().trim());
        }

        // Check if new username conflicts
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if new email conflicts
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : user.getIsActive());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        log.info("User updated: {} for store {}", updated.getUsername(), storeId);
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long storeId) {
        User user = userRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("User deleted (soft): {} for store {}", user.getUsername(), storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(Long storeId, String query) {
        return userRepository.searchByStoreIdAndUsername(storeId, query)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUsersCount(Long storeId) {
        return userRepository.countByStoreId(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers(Long storeId) {
        return userRepository.findActiveByStoreId(storeId)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}