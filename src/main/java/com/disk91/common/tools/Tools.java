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
package com.disk91.common.tools;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tools for the common library
 */

public class Tools {

    /**
     * Check if the given email has a valid syntax
     * @param email
     * @return true if the email has a valid syntax
     */
    public static boolean isValidEmailSyntax(String email) {
        return email.matches("^(?=.{1,64}@)[A-Za-z0-9_+-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
    }

    /**
     * Check the email format based on a list of filters (regex)
     * @param email
     * @param filters
     * @return
     */
    public static boolean isAcceptedEmailSyntax(String email, String filters) {
        // filters is a list of regEx separated by , corresponding to rejected patterns
        String [] _filters = filters.split(",");
        for (String filter : _filters) {
            if (email.matches(filter)) return false;
        }
        return true;
    }

    /**
     * Check the IP forrmat based on a list of IP filters (regex) and return true if the IP is accepted
     * @param ip
     * @param filters
     * @return
     */
    public static boolean isAcceptedIP(String ip, String filters) {
        // filters is a list of regEx separated by, corresponding to accepted patterns
        String [] _filters = filters.split(",");
        for (String filter : _filters) {
            if (ip.matches(filter)) return true;
        }
        return false;
    }

    /**
     * From a parameter like xxxx,yyyy,zzzz get an arraylist [ xxxx, yyyy, zzzz ]
     * @param param
     * @return
     */
    public static ArrayList<String> getStringListFromParam(String param) {
        String[] _params = param.split(",");
        return new ArrayList<>(Arrays.asList(_params));
    }

    /**
     * Return true if the given string is in the given list (comma separated)
     * @param str
     * @param list
     * @return
     */
    public static boolean isStringInList(String str,String list) {
        if ( list == null || list.isEmpty() ) return false;
        List<String> arrayList = getStringListFromParam(list);
        for ( String s : arrayList ) {
            if ( s.compareTo(str) == 0 ) return true;
        }
        return false;
    }

    /**
     * Get the remote IP from the request, taking into account possible reverse proxy header
     * @param req
     * @return IP address as a String
     */
    public static String getRemoteIp(HttpServletRequest req) {
        return (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : req.getRemoteAddr();
    }
}
