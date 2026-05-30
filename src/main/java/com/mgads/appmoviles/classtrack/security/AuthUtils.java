package com.mgads.appmoviles.classtrack.security;

import com.mgads.appmoviles.classtrack.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthUtils {

    private AuthUtils() {}

    public static String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return ((UserDetails) auth.getPrincipal()).getUsername();
    }
}
