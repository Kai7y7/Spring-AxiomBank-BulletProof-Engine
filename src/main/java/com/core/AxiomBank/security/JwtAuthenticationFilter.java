package com.core.AxiomBank.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtCore jwtCore;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtCore jwtCore, UserDetailsService userDetailsService) {
        this.jwtCore = jwtCore;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/auth/logout") || path.equals("/auth/login") || path.equals("/auth/register");
    }

///THIS FILTER WILL BE EXECUTED EVERY TIME CLIENT SENDS
/// A REQUEST IT VALIDATES JWT FROM COOKIES AND PLACES AUTHORIZATION
/// INSIDE SPRING SECURITY CONTEXT HOLDER
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        for (Cookie cookie : cookies) {
            if ("access-token".equals(cookie.getName())) {
                jwt = cookie.getValue();
                break;
            }
        }


        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }


        Claims claims = jwtCore.validateJwtAndReturnClaims(jwt);


        String username = claims.getSubject();


        UserDetails userDetails =
                userDetailsService.loadUserByUsername(username);


        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );


        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);


        filterChain.doFilter(request, response);
    }
}

