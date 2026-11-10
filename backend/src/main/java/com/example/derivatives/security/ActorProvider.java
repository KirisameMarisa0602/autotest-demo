package com.example.derivatives.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorProvider {

    public String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "SYSTEM";
        }
        Object details = authentication.getDetails();
        if (details instanceof DemoIdentityFilter.DemoRequestContext context) {
            return context.user() + "/" + context.role();
        }
        return authentication.getName();
    }

    public String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "SYSTEM";
        }
        Object details = authentication.getDetails();
        if (details instanceof DemoIdentityFilter.DemoRequestContext context) {
            return context.role();
        }
        return String.valueOf(authentication.getCredentials());
    }
}
