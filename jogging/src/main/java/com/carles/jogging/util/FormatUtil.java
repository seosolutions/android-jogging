package com.carles.jogging.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.concurrent.TimeUnit;

/**
 * Created by carles1 on 20/05/14.
 */
public class FormatUtil {

    private static final DateTimeFormatter time = DateTimeFormat.forPattern("hh:mm:ss");
    private static final DateTimeFormatter date = DateTimeFormat.forPattern("dd/MM/yyyy");
    private static final DateTimeFormatter datetime = DateTimeFormat.forPattern("dd/MM/yyyy hh:mm:ss");

    public static String date(long millis) {
        return date.print(millis);
    }

    public static String datetime(long millis) {
        return datetime.print(millis);
    }

    public static String timePattern(long millis) { return time.print(millis); }
    /**
     * Formats a timestamp to a user readable format containing the time in hours, minutes, and seconds
     */
    public static String time(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%1d:%02d:%02d", hours, minutes, seconds);
    }

//    public static Integer textDistanceToMeters(Context context, String src) {
//        Integer ret;
//        String errorMsg = "Trying to convert" + src + " into kilometers";
//        try {
//            String parsed = src.replaceAll("[^\\d.]", "");
//            if (parsed.isEmpty()) {
//                throw new JoggingAppError(errorMsg);
//            }
//
//            ret = Integer.valueOf(parsed) * 1000;
//        } catch (NumberFormatException e) {
//            Log.e(TAG, errorMsg);
//            throw new JoggingAppError(errorMsg);
//        }
//        return ret;
//    }

}
