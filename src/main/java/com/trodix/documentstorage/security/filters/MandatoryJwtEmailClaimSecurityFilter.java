package com.trodix.documentstorage.security.filters;

import com.trodix.documentstorage.security.exceptions.InvalidUserException;
import com.trodix.documentstorage.security.services.AuthenticationService;
import com.trodix.documentstorage.security.utils.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that denies all request that not contains a Jwt with an email claim
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class MandatoryJwtEmailClaimSecurityFilter extends HttpFilter {

    private final AuthenticationService authService;

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        try {
            authService.getEmail();
        } catch (InvalidUserException e) {
            log.info("The JWT token did not contained the mandatory {} claim.", Claims.EMAIL.value);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (RuntimeException e) {
            // case when no security on the requested url
        }

        chain.doFilter(request, response);
    }

}
