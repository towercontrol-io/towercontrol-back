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
package com.disk91.common.tools;


/**
 * This class is a generic class that will serve as the foundation for the Intracom services to offer common services
 * and abstract the underlying communication mechanism. The general principle is to have a class that allows the emission
 * of messages on a bus (here, a database) and for the different services to poll the sent messages. This bus does not
 * expect responses from the services processing the messages back to the message sender. One possible implementation
 * could later be an MQTT broker.
 */
public class Intracom {

    // Common public services and actions
    public static final String INTRACOM_SERVICE_COMMON  = "common";
    public static final String INTRACOM_COMMON_xxxx = "xxxxx";

    public static final String INTRACOM_SERVICE_USERS  = "users";




}
