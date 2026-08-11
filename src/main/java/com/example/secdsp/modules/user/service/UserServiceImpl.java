package com.example.secdsp.modules.user.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.user.dto.internal.UserInfo;
import com.example.secdsp.modules.user.dto.request.AssignRoleRequest;
import com.example.secdsp.modules.user.dto.request.UpdateProfileRequest;
import com.example.secdsp.modules.user.dto.response.UserProfileResponse;
import com.example.secdsp.modules.user.dto.response.UserSummaryResponse;
import com.example.secdsp.modules.user.entity.Role;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.entity.UserStatus;
import com.example.secdsp.modules.user.mapper.UserMapper;
import com.example.secdsp.modules.user.repository.RoleRepository;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        User user = findActiveUser(requireCurrentUserId());
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = findActiveUser(requireCurrentUserId());

        if (StringUtils.hasText(request.getPhone())
            && !request.getPhone().equals(user.getPhone())
            && userRepository.existsByPhoneAndIdNot(request.getPhone(), user.getId())) {
            log.warn("Phone already exists: {}", request.getPhone());
            throw new BusinessException("Phone already exists", HttpStatus.CONFLICT);
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable)
            .map(userMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id) {
        User user = findActiveUser(id);
        return userMapper.toProfileResponse(user);
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }

    private User findActiveUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /** Giải phóng email/username/phone để có thể đăng ký lại sau soft-delete. */
    private void releaseUserUniqueFields(User user) {
        Long id = user.getId();
        user.setEmail("deleted-" + id + "@sedsp.local");
        user.setUsername("deleted-" + id);
        user.setPhone(null);
        userRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void assignRole(
        Long userId,
        AssignRoleRequest request
    ) {

        User user = findActiveUser(userId);

        Long currentUserId = requireCurrentUserId();

        if (currentUserId.equals(userId)
            && UserRole.ADMIN.name().equals(user.getRole().getName())
            && !UserRole.ADMIN.name().equalsIgnoreCase(request.getRole().toString())) {

            throw new BusinessException(
                "You cannot remove your own admin role",
                HttpStatus.BAD_REQUEST
            );
        }

        Role role = roleRepository.findByName(
                request.getRole().name())
            .orElseThrow(() ->
                             new ResourceNotFoundException(
                                 "Role",
                                 request.getRole()
                             ));

        user.setRole(role);

        log.info(
            "User {} role changed to {}",
            userId,
            role.getName()
        );
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {

        User user = findActiveUser(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(
                "User is already active",
                HttpStatus.BAD_REQUEST
            );
        }

        user.setStatus(UserStatus.ACTIVE);

        log.info("User {} activated", userId);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {

        Long currentUserId = requireCurrentUserId();

        if (currentUserId.equals(userId)) {
            throw new BusinessException(
                "You cannot deactivate your own account",
                HttpStatus.BAD_REQUEST
            );
        }

        User user = findActiveUser(userId);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(
                "User is already inactive",
                HttpStatus.BAD_REQUEST
            );
        }

        user.setStatus(UserStatus.INACTIVE);

        log.info("User {} deactivated", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        Long currentUserId = requireCurrentUserId();

        if (currentUserId.equals(userId)) {
            throw new BusinessException(
                "You cannot delete your own account",
                HttpStatus.BAD_REQUEST
            );
        }

        User user = findActiveUser(userId);

        if (UserRole.ADMIN.name().equals(user.getRole().getName())
            && userRepository.countByRoleName(UserRole.ADMIN.name()) <= 1) {
            throw new BusinessException(
                "Cannot delete the last admin account",
                HttpStatus.BAD_REQUEST
            );
        }

        String originalEmail = user.getEmail();
        releaseUserUniqueFields(user);
        userRepository.delete(user);

        log.info("User {} ({}) soft-deleted by admin {}", userId, originalEmail, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getUserInfo(Long id) {
        return userMapper.toUserInfo(findActiveUser(id));
    }
}
