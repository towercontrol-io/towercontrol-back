/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.common.api.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag(name = "Action Result", description = "common format for better front-end integration")
public class ActionResult {

    public static enum ACTION_RESULT  {
        OK,                // action complete with sucess (200)
        CREATED,           // created (201)
        ACCEPTED,          // action accepted  - async (202)
        NOCONTENT,         // no Data (204)
        PARTIAL,           // partial success (206)
        BADREQUEST,        // bad request (400)
        NOTFOUND,          // not found (404)
        FORBIDDEN,         // forbidden (403)
        EXISTS,            // already exists (409)
        TEAPOT,            // I'm a teapot (418)
        TOO_EARLY,         // too early (425)
        TOO_MANY_REQUESTS, // too many requests (429)
        UNKNOWN            // Unknown (300)
    }

    @Schema(
            description = "Result of a given action",
            example = "SUCCESS",
            allowableValues = "OK, CREATED, ACCEPTED, NOCONTENT, PARTIAL, BADREQUEST, NOTFOUND, FORBIDDEN, TEAPOT, TOO_EARLY, TOO_MANY_REQUESTS, UNKNOWN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected ACTION_RESULT status;

    @Schema(
            description = "Associated custom code, http code",
            example = "200",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int status_code;

    @Schema(
            description = "Associated custom message ready for i18n",
            example = "err-user-creation-email-already-exist",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String message = "";


    // ------------------------------------

    public static ActionResult OK(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.OK);
        r.setStatus_code(200);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_OK(String m) {
        return new ResponseEntity<>(ActionResult.OK(m), HttpStatus.OK);
    }

    public static ActionResult CREATED(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.CREATED);
        r.setStatus_code(201);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_CREATED(String m) {
        return new ResponseEntity<>(ActionResult.CREATED(m), HttpStatus.CREATED);
    }

    public static ActionResult ACCEPTED(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.ACCEPTED);
        r.setStatus_code(202);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_ACCEPTED(String m) {
        return new ResponseEntity<>(ActionResult.ACCEPTED(m), HttpStatus.ACCEPTED);
    }


    public static ActionResult NOCONTENT(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.NOCONTENT);
        r.setStatus_code(204);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_NOCONTENT(String m) {
        return new ResponseEntity<>(ActionResult.NOCONTENT(m), HttpStatus.NO_CONTENT);
    }

    public static ActionResult PARTIAL(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.PARTIAL);
        r.setStatus_code(206);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_PARTIAL(String m) {
        return new ResponseEntity<>(ActionResult.PARTIAL(m), HttpStatus.PARTIAL_CONTENT);
    }

    public static ActionResult BADREQUEST(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.BADREQUEST);
        r.setStatus_code(400);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_BADREQUEST(String m) {
        return new ResponseEntity<>(ActionResult.BADREQUEST(m), HttpStatus.BAD_REQUEST);
    }


    public static ActionResult NOTFOUND(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.NOTFOUND);
        r.setStatus_code(404);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_NOTFOUND(String m) {
        return new ResponseEntity<>(ActionResult.NOTFOUND(m), HttpStatus.NOT_FOUND);
    }

    public static ActionResult FORBIDDEN(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.FORBIDDEN);
        r.setStatus_code(403);
        r.setMessage(m);
        return r;
    }

    public static ActionResult EXISTS(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.EXISTS);
        r.setStatus_code(409);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_FORBIDDEN(String m) {
        return new ResponseEntity<>(ActionResult.FORBIDDEN(m), HttpStatus.FORBIDDEN);
    }

    public static ActionResult TEAPOT(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.TEAPOT);
        r.setStatus_code(418);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_TEAPOT(String m) {
        return new ResponseEntity<>(ActionResult.TEAPOT(m), HttpStatus.I_AM_A_TEAPOT);
    }


    public static ActionResult TOO_EARLY(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.TOO_EARLY);
        r.setStatus_code(425);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_TOO_EARLY(String m) {
        return new ResponseEntity<>(ActionResult.TOO_EARLY(m), HttpStatus.TOO_EARLY);
    }


    public static ActionResult TOO_MANY_REQUESTS(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.TOO_MANY_REQUESTS);
        r.setStatus_code(429);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_TOO_MANY_REQUESTS(String m) {
        return new ResponseEntity<>(ActionResult.TOO_MANY_REQUESTS(m), HttpStatus.TOO_MANY_REQUESTS);
    }

    public static ActionResult UNKNOWN(String m){
        ActionResult r = new ActionResult();
        r.setStatus(ACTION_RESULT.UNKNOWN);
        r.setStatus_code(300);
        r.setMessage(m);
        return r;
    }

    public static ResponseEntity<ActionResult> r_UNKNOWN(String m) {
        return new ResponseEntity<>(ActionResult.UNKNOWN(m), HttpStatus.MULTIPLE_CHOICES);
    }



    // ------------------------------------

    public ACTION_RESULT getStatus() {
        return status;
    }

    public void setStatus(ACTION_RESULT status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }
}