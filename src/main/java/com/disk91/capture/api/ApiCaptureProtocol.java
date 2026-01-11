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
package com.disk91.capture.api;

import com.disk91.capture.api.interfaces.CaptureProtocolResponseItf;
import com.disk91.capture.services.CaptureEndpointService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag( name = "Capture protocol management", description = "List capture protocols" )
@CrossOrigin
@RequestMapping(value = "/capture/1.0/protocol")
@RestController
public class ApiCaptureProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureEndpointService captureEndpointService;

    /**
     * List the existing protocols existing in the platform.
     * @param request
     * @return
     */
    @Operation(
            summary = "List available protocols",
            description = "This endpoint allow to get the capture protocol definitions and requirements. These information are " +
                    "mandatory to correctly display or setup a endpoint. Any user can access these information. This is a read only " +
                    "information, the protocols are defined by the installed software drivers.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Protocols definitions",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = CaptureProtocolResponseItf.class))))
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getCaptureProtocolsList(
            HttpServletRequest request
    ) {
         List<CaptureProtocolResponseItf> ret = captureEndpointService.listProtocols();
         return new ResponseEntity<>(ret, HttpStatus.OK);
    }

}
