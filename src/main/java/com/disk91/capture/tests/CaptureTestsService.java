package com.disk91.capture.tests;

import com.disk91.capture.api.interfaces.CaptureEndpointCreationBody;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.repositories.CaptureEndpointRepository;
import com.disk91.capture.services.CaptureEndpointCache;
import com.disk91.capture.services.CaptureEndpointService;
import com.disk91.capture.services.CaptureIngestService;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.KeyValues;
import com.disk91.common.tests.CommonTestsService;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.entities.sub.DevAttribute;
import com.disk91.devices.mdb.entities.sub.DevSubState;
import com.disk91.devices.mdb.entities.sub.DeviceHistoryReason;
import com.disk91.devices.services.DeviceCache;
import com.disk91.users.tests.UsersTestsService;
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
public class CaptureTestsService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final String TEST_ENDPOINT_NAME = "Test Chirpstack V4 Endpoint";

    @Autowired
    protected CommonTestsService commonTestsService;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected CaptureEndpointService captureEndpointService;

    @Autowired
    protected CaptureEndpointRepository captureEndpointRepository;

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @Autowired
    protected CaptureIngestService captureIngestService;

    @Autowired
    protected DeviceCache deviceCache;

    public static String captureDeviceId = "";
    private CaptureEndpoint ce = null;


    /**
     * This function is creating tests for the Users module
     * It will create some user like if coming from the API and try to bypass the security potentially
     * the purpose is to make sure the module is working as expected
     * @throws ITParseException
     */
    public void runTests() throws ITParseException {

        // Create a endpoint service for chirpstack protocol when not already existing
        // Delete it when existing

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
                return new Principal() {
                    @Override
                    public String getName() {
                        return UsersTestsService.testNormalUserLogin;
                    }
                };
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

        ce = captureEndpointRepository.findFirstByName(TEST_ENDPOINT_NAME);
        if ( ce != null ) {
            commonTestsService.info("[capture] Removing the test endpoint previously created");
            // delete it
            captureEndpointRepository.delete(ce);
        }

        CaptureEndpoint endp = null;
        try {
            commonTestsService.info("[capture] Creating the default Chirpstack V4 endpoint");
            CaptureEndpointCreationBody reqBody = new CaptureEndpointCreationBody();
            reqBody.setName(TEST_ENDPOINT_NAME);
            reqBody.setDescription("test endpoint for Chirpstack V4");
            reqBody.setEncrypted(false);
            reqBody.setProtocolId("system-lorawan-helium-chirpstack-v4");
            reqBody.setForceWideOpen(false);
            reqBody.setCustomConfig(new ArrayList<>());
            reqBody.getCustomConfig().add(new CustomField("protocol-server-api-endpoint","https://console.heyiot.xyz/api"));

            endp = captureEndpointService.createCaptureEndpoint(
                req,
                reqBody
            );

            commonTestsService.success("[capture] Default Chirpstack V4 endpoint created successfully with RefId {}", endp.getRef());
        } catch (ITRightException | ITNotFoundException  | ITParseException e) {
            commonTestsService.error("[capture] Error creating default Chirpstack V4 endpoint {} ", e.getMessage());
            throw new ITParseException("[capture] Error creating default Chirpstack V4 endpoint: " + e.getMessage());
        }

        // make sure we can get the endpoint
        try {
            captureEndpointCache.getCaptureEndpoint(endp.getRef());
            commonTestsService.success("[capture] Created endpoint retrieved successfully from cache");
        } catch (ITNotFoundException x) {
            commonTestsService.error("[capture] Error getting created endpoint {} ", x.getMessage());
            throw new ITParseException("[capture] Error getting created endpoint: " + x.getMessage());
        }

        // Deleting previous test device if any
        try {
            List<Device> devices =  deviceCache.getDevicesByDataStream("stream-test-device-capture-001");
            if ( devices != null && !devices.isEmpty() ) {
                for ( Device d : devices ) {
                    commonTestsService.info("[capture] Removing previous test device in cache with id {}", d.getId());
                    deviceCache.deleteDevice(d, DeviceHistoryReason.NO_REASON);
                }
            }
        } catch (ITNotFoundException ignored) {}

        // Create a test device
        commonTestsService.info("[capture] Creating one test device in cache");
        Device dev = new Device();
        dev.setCreationBy("test-capture-module");
        dev.setVersion(Device.DEVICE_VERSION);
        dev.setPublicId("test-device-capture-001");
        dev.setDataStreamId("stream-test-device-capture-001");
        dev.setName("Test Device for Capture Module");
        dev.setDevState(DeviceState.OPEN);
        dev.setSubState(DevSubState.initNone());
        dev.setCreationOnMs(Now.NowUtcMs());
        dev.setDataEncrypted(false);
        dev.setCommunicationIds(new ArrayList<>());
        DevAttribute a = new DevAttribute();
        a.addOneSimpleParam("LoRa", "deveui", "6081f9dde602cd71");
        dev.getCommunicationIds().add(a);
        dev.setAssociatedGroups(new ArrayList<>());
        deviceCache.saveDevice(dev, DeviceHistoryReason.NO_REASON);

        // check device presence
        try {
            List<Device> devices =  deviceCache.getDevicesByDataStream("stream-test-device-capture-001");
            if ( devices == null || devices.isEmpty() ) {
                commonTestsService.error("[capture] Test device not found after creation");
                throw new ITParseException("[capture] Test device not found after creation");
            } else {
                captureDeviceId = devices.getFirst().getId();
                commonTestsService.success("[capture] Test device found successfully after creation");
            }
        } catch (ITNotFoundException x) {
            commonTestsService.error("[capture] Test device not found after creation");
            throw new ITParseException("[capture] Test device not found after creation");
        }

        // Send a test payload
        try {
            commonTestsService.info("[capture] Sending a test payload to the created endpoint");
            captureIngestService.ingestData(
                    req,
                    TEST_PAYLOAD.getBytes(),
                    endp.getRef()
            );
            commonTestsService.success("[capture] Test payload sent successfully to the created endpoint");
        } catch (ITRightException | ITParseException | ITNotFoundException | ITTooManyException e) {
            commonTestsService.error("[capture] Error sending test payload to created endpoint {} ", e.getMessage());
            throw new ITParseException("[capture] Error sending test payload to created endpoint: " + e.getMessage());
        }

    }

    public void cleanTests() {
        // Delete test device
        try {
            List<Device> devices =  deviceCache.getDevicesByDataStream("stream-test-device-capture-001");
            if ( devices != null && !devices.isEmpty() ) {
                for ( Device d : devices ) {
                    commonTestsService.info("[capture] Removing previous test device in cache with id {}", d.getId());
                    deviceCache.deleteDevice(d, DeviceHistoryReason.NO_REASON);
                }
            }
        } catch (ITNotFoundException ignored) {}

        // Delete endpoint
        if ( ce != null ) captureEndpointRepository.delete(ce);
    }



    public final String TEST_PAYLOAD = """
            {
                 "deduplicationId":"49403db7-8722-49b4-8d49-c3b119c534e5",
                 "time":"2023-05-29T19:50:10+00:00",
                 "deviceInfo":{
                     "tenantId":"c9316fdb-d2fe-453d-bd28-4917f7e227ce",
                     "tenantName":"migration",
                     "applicationId":"ace3452a-87f3-4a86-b9a8-fe8507b47ff3",
                     "applicationName":"test",
                     "deviceProfileId":"7fc8491b-ab6a-4871-9eb9-a57431b982a0",
                     "deviceProfileName":"(EU868) Migration OTAA Without label",
                     "deviceName":"disk91_test1",
                     "devEui":"6081f9dde602cd71",
                     "deviceClassEnabled":"CLASS_A",
                     "tags":{
                         "label":"Without label"
                     }
                 },
                 "devAddr":"480007a0",
                 "dr":3,
                 "adr":false,
                 "fCnt":22772,
                 "fPort":1,
                 "confirmed":false,
                 "data":"HQ==",
                 "object":{
                     "other":"123",
                     "temp":22.5
                 },
                 "rxInfo":[
                     {
                         "gatewayId":"c986398a305dee5a",
                         "uplinkId":65489,
                         "gwTime":"2024-01-07T11:05:31+00:00",
                         "nsTime":"2024-01-07T11:05:31.577525935+00:00",
                         "rssi":-41,
                         "snr":7.8,
                         "location" : {
                         },
                         "context":"EbkTFA==",
                         "metadata":{
                             "region_common_name":"EU868",
                             "region_config_id":"eu868",
                             "gateway_h3index" : "61105",
                             "gateway_lat" : "45.80",
                             "gateway_long" : "3.09",
                             "gateway_name" : "mythical-xxx...",
                             "gateway_id":"11o8R9inbpc...3XA",
                             "lat": 0.0,
                             "lon": 0.0
                         },
                         "crcStatus":"CRC_OK"
                     },
                     {
                         "gatewayId":"3c408850a5b4f27c",
                         "uplinkId":11888,
                         "gwTime":"2024-02-27T18:07:11+00:00",
                         "nsTime":"2024-02-27T18:07:11.115026956+00:00",
                         "rssi":-22,
                         "snr":8.0,
                         "context":"yIKGwQ==",
                         "metadata":{
                             "region_common_name":"EU868",
                             "region_name":"eu868"
                         }
                     }
                 ],
                 "txInfo":{
                     "frequency":867500000,
                     "modulation":{
                         "lora":{
                             "bandwidth":125000,
                             "spreadingFactor":9,
                             "codeRate":"CR_4_5",
                             "polarizationInversion":false
                         }
                     }
                 }
             }
    """;
}
