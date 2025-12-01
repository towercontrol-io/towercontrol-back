package com.disk91.common.tools;

import com.disk91.common.tools.exceptions.ITParseException;

import java.util.Calendar;

public class DateConverters {

    // from 2022-11-14T20:05:16+00:00 to timestamp
    public static long StringDateToMs(String d) throws ITParseException {
        try {
            Calendar calendar = jakarta.xml.bind.DatatypeConverter.parseDateTime(d);
            return calendar.getTimeInMillis();
        } catch (Exception x) {
            throw new ITParseException(x.getMessage());
        }
    }
}
