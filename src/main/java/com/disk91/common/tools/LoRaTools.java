package com.disk91.common.tools;

public class LoRaTools {


    /**
     * Generate a random DevEui with first bytes given by base (must be an HexString)
     * @param base
     * @return
     */
    public static String getRandomDevEui(String base) {
        if ( HexCodingTools.isHexString(base) ) {
            return base+HexCodingTools.getRandomHexString(16-base.length());
        }
        return null;
    }

    /**
     * Generate a random JoinEui with first bytes given by base (must be an HexString)
     * @param base
     * @return
     */
    public static String getRandomJoinEui(String base) {
        return getRandomDevEui(base);
    }

    /**
     * Generate a random AppKey
     * @return
     */
    public static String getRandomAppKey() {
        return HexCodingTools.getRandomHexString(32);
    }


}
