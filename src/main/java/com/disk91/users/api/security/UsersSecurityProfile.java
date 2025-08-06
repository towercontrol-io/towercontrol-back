/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.users.api.security;

import com.disk91.common.api.security.JWTAuthenticationProvider;
import com.disk91.common.api.security.JWTAuthorizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class UsersSecurityProfile {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JWTAuthenticationProvider jwtAuthenticationProvider;

    @Autowired
    private JWTAuthorizationFilter jwtAuthorizationFilter;

    @Order(1)
    @Bean
    public SecurityFilterChain usersFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/users/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtAuthorizationFilter, BasicAuthenticationFilter.class)
                .authenticationProvider(jwtAuthenticationProvider)
                .authorizeHttpRequests((authz) -> authz
                        // Allows registration link
                        .requestMatchers("/users/1.0/registration/register").permitAll()
                        .requestMatchers("/users/1.0/creation/create").permitAll()
                        // Allow sign in link
                        .requestMatchers("/users/1.0/session/signin").permitAll()
                        // Allow password reset link
                        .requestMatchers("/users/1.0/profile/password/reset").permitAll()
                        .requestMatchers("/users/1.0/profile/password/request").permitAll()
                        // Allow user config api
                        .requestMatchers("/users/1.0/config").permitAll()
                        // Allow all OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        // Others
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
