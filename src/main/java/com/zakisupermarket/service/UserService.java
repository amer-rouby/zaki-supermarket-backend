package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.UserRequest;
import com.zakisupermarket.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers(Long storeId);

    UserResponse getUser(Long id, Long storeId);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request, Long storeId);

    void deleteUser(Long id, Long storeId);

    List<UserResponse> searchUsers(Long storeId, String query);

    Long getUsersCount(Long storeId);

    List<UserResponse> getActiveUsers(Long storeId);
}