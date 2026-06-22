package com.disk91.common.tools;

import com.disk91.common.tools.exceptions.ITParseException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;

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


    // from 2022-11-14T20:05:16+00:00 to timestamp (ms since epoch)
    public static long StringNsDateToMs(String d) throws ITParseException {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(d);
            return odt.toInstant().toEpochMilli();
        } catch (DateTimeParseException x) {
            throw new ITParseException(x.getMessage());
        } catch (Exception x) {
            throw new ITParseException(x.getMessage());
        }
    }


    // ex: "2024-01-07T11:05:31.577525935+00:00" -> nanosecondes = 577525935, millisecondes = 577000000,
    // return 577525935 - 577000000 = 525935
    public static int StringNsDateToNsRemaining(String d) throws ITParseException {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(d);
            int nanos = odt.getNano(); // 0..999_999_999
            return nanos % 1_000_000; // remainder after milliseconds (0..999_999)
        } catch (DateTimeParseException x) {
            throw new ITParseException(x.getMessage());
        } catch (Exception x) {
            throw new ITParseException(x.getMessage());
        }
    }

    // from timestamp to HH:MM ; the rest of the date is getting lost
    // appends " (UTC)" when tz is null
    public static String timestampToTime(long timestamp, TimeZone tz) {
        TimeZone zone = (tz != null) ? tz : TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(timestamp);
        String result = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        return (tz == null) ? result + " (UTC)" : result;
    }

    // from timestamp to YYYY-MM-DD HH:MM ; the rest of the date is getting lost
    // appends " (UTC)" when tz is null
    public static String timestampToDateTime(long timestamp, TimeZone tz) {
        TimeZone zone = (tz != null) ? tz : TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(timestamp);
        String result = String.format("%04d-%02d-%02d %02d:%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE));
        return (tz == null) ? result + " (UTC)" : result;
    }


}
