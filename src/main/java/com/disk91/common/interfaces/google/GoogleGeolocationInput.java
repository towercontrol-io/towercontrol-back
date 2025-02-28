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
package com.disk91.common.interfaces.google;

import java.util.ArrayList;
import java.util.List;

// see - https://developers.google.com/maps/documentation/geolocation/intro
public class GoogleGeolocationInput {

    public class CellTower {
        public int cellId;
        public int locationAreaCode;
        public int mobileCountryCode;
        public int mobileNetworkCode;
        public int age;
        public int signalStrength;
        public int timingAdvance;
    }

    public class WifiAccessPoint {
        public String macAddress;
        //public int signalStrength;
        //public int age;
        //public int channel;
        //public int signalToNoiseRatio;
    }

    //public int homeMobileCountryCode;
    //public int homeMobileNetworkCode;
    //public String radioType;
    //public String carrier;
    //public String considerIp;
    public String considerIp;
    //public List<CellTower> cellTowers;
    public List<WifiAccessPoint> wifiAccessPoints;


    public void initWifi() {
        this.wifiAccessPoints = new ArrayList<WifiAccessPoint>();
        this.considerIp = "false";
    }

    public void addMac(String mac) {
        WifiAccessPoint w = new WifiAccessPoint();
        w.macAddress = mac;
        this.wifiAccessPoints.add(w);
    }



}

