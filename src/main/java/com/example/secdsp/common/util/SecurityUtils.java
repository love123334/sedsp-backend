package com.example.secdsp.common.util;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.security.user.UserDetailsImpl;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Object principal =
            SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof UserDetailsImpl user) {
            return user.getId();
        }

        throw new UnauthorizedException();
    }
}
