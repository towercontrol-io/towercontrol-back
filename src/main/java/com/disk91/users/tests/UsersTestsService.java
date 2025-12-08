package com.disk91.users.tests;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tests.CommonTestsService;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import com.disk91.users.services.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

@Service
public class UsersTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonTestsService commonTestsService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserRegistrationRepository userRegistrationRepository;

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserRegistrationService userRegistrationService;

    @Autowired
    protected UserCreationService userCreationService;

    @Autowired
    protected CrossUserWrapperService crossUserWrapperService;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    public static String testNormalUserEmail = "john.doe@test.foo.bar";
    public static String testNormalUserPassword = "";
    public static String testNormalUserLogin = "";
    public static User testNormalUser = null;

    /**
     * This function is creating tests for the Users module
     * It will create some user like if coming from the API and try to bypass the security potentially
     * the purpose is to make sure the module is working as expected
     * @throws ITParseException
     */
    public void runTests() throws ITParseException {

        HttpServletRequest req = new HttpServletRequest() {
            @Override
            public String getAuthType() {
                return "";
            }

            @Override
            public Cookie[] getCookies() {
                return new Cookie[0];
            }

            @Override
            public long getDateHeader(String s) {
                return 0;
            }

            @Override
            public String getHeader(String s) {
                return "";
            }

            @Override
            public Enumeration<String> getHeaders(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return null;
            }

            @Override
            public int getIntHeader(String s) {
                return 0;
            }

            @Override
            public String getMethod() {
                return "";
            }

            @Override
            public String getPathInfo() {
                return "";
            }

            @Override
            public String getPathTranslated() {
                return "";
            }

            @Override
            public String getContextPath() {
                return "";
            }

            @Override
            public String getQueryString() {
                return "";
            }

            @Override
            public String getRemoteUser() {
                return "";
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public String getRequestedSessionId() {
                return "";
            }

            @Override
            public String getRequestURI() {
                return "";
            }

            @Override
            public StringBuffer getRequestURL() {
                return null;
            }

            @Override
            public String getServletPath() {
                return "";
            }

            @Override
            public HttpSession getSession(boolean b) {
                return null;
            }

            @Override
            public HttpSession getSession() {
                return null;
            }

            @Override
            public String changeSessionId() {
                return "";
            }

            @Override
            public boolean isRequestedSessionIdValid() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromCookie() {
                return false;
            }

            @Override
            public boolean isRequestedSessionIdFromURL() {
                return false;
            }

            @Override
            public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
                return false;
            }

            @Override
            public void login(String s, String s1) throws ServletException {

            }

            @Override
            public void logout() throws ServletException {

            }

            @Override
            public Collection<Part> getParts() throws IOException, ServletException {
                return List.of();
            }

            @Override
            public Part getPart(String s) throws IOException, ServletException {
                return null;
            }

            @Override
            public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
                return null;
            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getAttributeNames() {
                return null;
            }

            @Override
            public String getCharacterEncoding() {
                return "";
            }

            @Override
            public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

            }

            @Override
            public int getContentLength() {
                return 0;
            }

            @Override
            public long getContentLengthLong() {
                return 0;
            }

            @Override
            public String getContentType() {
                return "";
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return null;
            }

            @Override
            public String getParameter(String s) {
                return "";
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return null;
            }

            @Override
            public String[] getParameterValues(String s) {
                return new String[0];
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Map.of();
            }

            @Override
            public String getProtocol() {
                return "";
            }

            @Override
            public String getScheme() {
                return "";
            }

            @Override
            public String getServerName() {
                return "";
            }

            @Override
            public int getServerPort() {
                return 0;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return null;
            }

            @Override
            public String getRemoteAddr() {
                return "";
            }

            @Override
            public String getRemoteHost() {
                return "";
            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public Enumeration<Locale> getLocales() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(String s) {
                return null;
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalName() {
                return "";
            }

            @Override
            public String getLocalAddr() {
                return "";
            }

            @Override
            public int getLocalPort() {
                return 0;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public AsyncContext startAsync() throws IllegalStateException {
                return null;
            }

            @Override
            public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
                return null;
            }

            @Override
            public boolean isAsyncStarted() {
                return false;
            }

            @Override
            public boolean isAsyncSupported() {
                return false;
            }

            @Override
            public AsyncContext getAsyncContext() {
                return null;
            }

            @Override
            public DispatcherType getDispatcherType() {
                return null;
            }

            @Override
            public String getRequestId() {
                return "";
            }

            @Override
            public String getProtocolRequestId() {
                return "";
            }

            @Override
            public ServletConnection getServletConnection() {
                return null;
            }
        };

        // Test use the public API and this need to be authorized, later we could manage different type of creation
        // so it will be possible to test the different cases
        // @TODO
        if (!usersConfig.isUsersRegistrationSelf()) {
            commonTestsService.error("[users] Self registration is disabled, cannot run tests, please enable it in config");
            throw new ITParseException("[users] failed in new user creation");
        }

        commonTestsService.info("[users] Attempt to create {} user",testNormalUserEmail);
        testNormalUserLogin = User.encodeLogin(testNormalUserEmail);
        String testNormalUserEmailEnc = UserRegistration.getEncodedEmail(testNormalUserEmail,commonConfig.getEncryptionKey());

        // check if a request is pending
        UserRegistration ur = userRegistrationRepository.findOneUserRegistrationByEmail(testNormalUserEmailEnc);
        if ( ur != null ) {
            commonTestsService.info("[users] UserRequest {} already exist, deleting it",testNormalUserEmail);
            userRegistrationRepository.delete(ur);
        }

        // check if exist (like previous test)
        User u = userRepository.findOneUserByLogin(testNormalUserLogin);
        if ( u != null ) {
            commonTestsService.info("[users] User {} already exist, deleting it",testNormalUserEmail);
            userRepository.delete(u);
            userCache.flushUser(u.getLogin());
        }

        // Create the request for this user, we may have no issue
        UserAccountRegistrationBody jd = new UserAccountRegistrationBody();
        jd.setEmail(testNormalUserEmail);
        jd.setRegistrationCode("");
        try {
            userRegistrationService.requestAccountCreation(jd, req);
        } catch ( ITTooManyException x) {
            commonTestsService.error("[users] {} already existing even if deleted or pending", testNormalUserEmail);
            throw new ITParseException("[users] failed in new user creation");
        } catch (ITRightException x) {
            commonTestsService.error("[users] Right Exception");
            throw new ITParseException("[users] failed in new user creation");
        } catch (ITParseException x) {
            commonTestsService.error("[users] {} format seems invalid", testNormalUserEmail);
            throw new ITParseException("[users] failed in new user creation");
        }

        // get the associated registration key
        UserRegistration exists = userRegistrationRepository.findOneUserRegistrationByEmail(testNormalUserEmailEnc);
        if ( exists == null ) {
            commonTestsService.error("[users] UserRegistration for {} not found",testNormalUserEmail);
            throw new ITParseException("[users] failed to confirm user registration");
        }

        commonTestsService.success("[users] User {} creation request passed successfully",testNormalUserEmail);

        // Validate the registration
        testNormalUserPassword = RandomString.getRandomAZString(15)+"!1";
        UserAccountCreationBody ucb = new UserAccountCreationBody();
        ucb.setEmail(testNormalUserEmail);
        ucb.setPassword(testNormalUserPassword);
        ucb.setConditionValidation(true);
        ucb.setValidationID(exists.getValidationId());
        if ( usersConfig.isUsersNceEnableCaptcha() ) {
            if ( crossUserWrapperService.isNceEnabled() ) {
                // only for NCE
                try {
                    // We expect the call to fail due to captcha missing
                    userCreationService.createUserSelf(ucb, req);
                    commonTestsService.error("[users] {} created even with captcha not validated", testNormalUserEmail);
                    throw new ITParseException("[users] failed in new user creation");
                } catch (ITTooManyException | ITParseException x) {
                    commonTestsService.error("[users] {} creation failed for an unexpected reason {}", testNormalUserEmail, x.getMessage());
                    throw new ITParseException("[users] failed in new user creation");
                } catch (ITRightException ignored) {
                    // normal behavior
                }
            }
        }
        commonTestsService.success("[users] Captcha requirement for {} enforced successfully",testNormalUserEmail);

        crossUserWrapperService.userRegistrationForceCaptcha(exists.getValidationId());
        try {
            userCreationService.createUserSelf(ucb, req);
        } catch (ITTooManyException | ITParseException | ITRightException x) {
            commonTestsService.error("[users] {} creation failed for an unexpected reason {}", testNormalUserEmail,x.getMessage());
            throw new ITParseException("[users] failed in new user creation");
        }

        try {
            testNormalUser = userCache.getUser(testNormalUserLogin);
            // Add right for other tests to that user
            // ROLE_BACKEND_CAPTURE for capture tests
            commonTestsService.info("[users] Adding ROLE_BACKEND_CAPTURE to {}",testNormalUserEmail);
            testNormalUser.getRoles().add(UsersRolesCache.StandardRoles.ROLE_BACKEND_CAPTURE.getRoleName());
            testNormalUser.getRoles().add(UsersRolesCache.StandardRoles.ROLE_GLOBAL_CAPTURE.getRoleName());
            userCache.saveUser(testNormalUser);
        } catch (ITNotFoundException x) {
            commonTestsService.error("[users] {} not found after creation",testNormalUserEmail);
            throw new ITParseException("[users] failed in new user creation");
        }
        commonTestsService.success("[users] User {} created successfully with ID {}",testNormalUserEmail,testNormalUserLogin);

        // Test login and print the JWT for HTTP tests
        commonTestsService.info("[users] Attempt to login {}",testNormalUserEmail);
        UserLoginBody loginBody = new UserLoginBody();
        loginBody.setEmail(testNormalUserEmail);

        // Do a test with a wrong password
        loginBody.setPassword("wrongPassword");
        try {
            userService.userLogin(
                    loginBody,
                    req
            );
            commonTestsService.error("[users] {} login passed with wrong password",testNormalUserEmail);
            throw new ITParseException("[users] failed in new user signin");
        } catch (ITRightException x) {
            // normal behavior
            commonTestsService.success("[users] {} login failed as expected for wrong password",testNormalUserEmail);
        } catch (Exception e) {
            commonTestsService.error("[users] {} login failed for an unexpected reason",testNormalUserEmail);
            throw new ITParseException("[users] failed in new user signin");
        }

        loginBody.setPassword(testNormalUserPassword);
        try {
            UserLoginResponse resp = userService.userLogin(
                    loginBody,
                    req
            );
            commonTestsService.info("[users] {} login JWT: {}",testNormalUserEmail, resp.getJwtToken());
        } catch (Exception e) {
            commonTestsService.error("[users] {} login failed after creation",testNormalUserEmail);
            throw new ITParseException("[users] failed in new user signin");
        }

        commonTestsService.success("[users] User {} logged successfully ",testNormalUserEmail);
    }

}
