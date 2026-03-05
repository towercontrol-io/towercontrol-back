/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2020.
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
package com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers;

import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2CoverageGlobalResponse;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnection;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Summary
 *
 * This class manage the sigfox Coverage Component from the SigfoxApi
 * ----------------------------------------------------------------------------------
 * Requires:
 *   This class requieres SpringBoot framework
 *   This class requieres
 *     compile("org.apache.httpcomponents:httpcore:4.4.6")
 *     compile("commons-codec:commons-codec:1.10")
 * ----------------------------------------------------------------------------------
 * Support :
 *
 *
 * ----------------------------------------------------------------------------------
 *
 * @author Paul Pinault
 */
public class CoverageWrapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected String apiLogin;
    protected String apiPassword;

    public CoverageWrapper(
            String _apiLogin,
            String _apiPassword
    ) {
        this.apiLogin = _apiLogin;
        this.apiPassword = _apiPassword;
    }

    // ========================================================================
    // Get a the number of antennas covering a specific position
    public int getSigfoxCoverageRedundancy(double lat, double lng, boolean indoor) {
        try {

            ITSigfoxConnection<String, SigfoxApiv2CoverageGlobalResponse> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            SigfoxApiv2CoverageGlobalResponse coverage = request.execute(
                    "GET",
                    "/api/v2/coverages/global/predictions",
                    "lat="+lat+"&lng="+lng+"&radius="+500,
                    null,
                    null,
                    SigfoxApiv2CoverageGlobalResponse.class
            );

            if ( coverage != null ) {
                if ( coverage.isLocationCovered() ) {
                    int count = 0;
                    for (double v:coverage.getMargins()) {
                        if ( indoor ) v -= 20;
                        if ( v > 10 ) count++;
                    }
                    return count;
                }
            }
        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}",x.errorMessage );
        }
        return 0;
    }

    // ========================================================================
    // Get a signal margin on  a specific position
    public SigfoxApiv2CoverageGlobalResponse getSigfoxCoverageMargins(double lat, double lng) {
        try {

            ITSigfoxConnection<String, SigfoxApiv2CoverageGlobalResponse> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            return request.execute(
                    "GET",
                    "/api/v2/coverages/global/predictions",
                    "lat="+lat+"&lng="+lng+"&radius="+500,
                    null,
                    null,
                    SigfoxApiv2CoverageGlobalResponse.class
            );

        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}", x.errorMessage);
        }
        SigfoxApiv2CoverageGlobalResponse r = new SigfoxApiv2CoverageGlobalResponse();
        r.setLocationCovered(false);
        return r;
    }


}
