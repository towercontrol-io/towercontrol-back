/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2018.
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

package com.disk91.common.api.security;

import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCache;
import com.disk91.users.services.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.io.Serial;
import java.security.Key;
import java.util.ArrayList;

@Service
@DependsOn("flywayConfiguration")
public class JWTAuthorizationFilter extends GenericFilterBean {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected static class MyGrantedAuthority implements GrantedAuthority {

        @Serial
        private static final long serialVersionUID = 0L;
        protected String authority;

        public MyGrantedAuthority(String auth) {
            this.authority = auth;
        }

        @Override
        public String getAuthority() {
            return authority;
        }
    }

    @Autowired
    private UserCache userCache;

    @Autowired
    private UserService userService;

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        // Make sure the request contains a Bearer or it is not for us
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // refuse authentication
            chain.doFilter(request, response);
            return;
        }

        // Verify Bearer and return user or error
        String token = "not init";
        try {
            token = authHeader.replace("Bearer ","");
            Claims claims = Jwts.parser()
                .keyLocator(new Locator<Key>() {
                    @Override
                    public Key locate(Header header) {
                        if (header instanceof JwsHeader jwsh) {
                            String user = (String)jwsh.get("sub");
                            String algo = jwsh.getAlgorithm();
                            if ( algo == null || algo.compareToIgnoreCase("HS512") != 0 ) {
                                log.error("[users] Bearer is signed with invalid algo !!! ");
                                return null;
                            }
                            if (user == null) return null;
                            try {
                                if ( user.startsWith("apikey_") ) {
                                    // this is an API key, we need to find the associated user
                                    // @TODO - We need a cache ?

                                } else {
                                    // this is a regular user token
                                    User u = userCache.getUser(user);
                                    return userService.generateKeyForUser(u);
                                }
                            } catch (ITNotFoundException x) {
                                return null;
                            }
                        }
                        log.error("[users] Invalid type of headers");
                        return null;
                    }})
                .build()
                .parseSignedClaims(token).getPayload();

            ArrayList<String> roles = (ArrayList<String>) claims.get("roles");
            ArrayList<MyGrantedAuthority> list = new ArrayList<>();
            if (roles != null) {
                for (String a : roles) {
                    MyGrantedAuthority g = new MyGrantedAuthority(a);
                    list.add(g);
                }
            }
            String user = claims.getSubject();
            try {
                User u = null;
                if ( user.startsWith("apikey_") ) {
                    // this is an API key, we need to find the associated user
                    // @TODO - We need a cache ?

                } else {
                    // standard user
                    u = userCache.getUser(user);
                }
                assert u != null;
                if ( u.isActive() && !u.isLocked() && u.getDeletionDate() == 0 ) {
                    // @TODO - Lets see if we have some other verification ?
                    // accept the authentication
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, list));
                }
            } catch (ITNotFoundException x) {
                log.error("[users] jwt attempt with non existing user!!! ");
                chain.doFilter(request, response);
                return;
            }
        } catch (ExpiredJwtException x) {
            //log.error("expired");
            // Expired
        } catch (SignatureException x) {
            //log.error("signature");
            // the signature of the JWT is invalid
        } catch (IllegalArgumentException x) {
            //log.error("arg");
            // corresponds to a non existing user inside the JWT
            // so can find a signature to be verified
        } catch (UnsupportedJwtException x) {
            // sounds like a wrong JWT
            //log.warn("[users] Invalid token: "+token);
        } catch (MalformedJwtException x) {
            // sounds like signature problem
            if ( token != null ) log.warn("[users] Invalid token: {}", token);
        } catch (Exception x) {
            log.error("[users] Invalid token: {}", token);
        }
        chain.doFilter(request, response);

    }

}
