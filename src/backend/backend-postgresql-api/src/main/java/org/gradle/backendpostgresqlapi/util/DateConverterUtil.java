package org.gradle.backendpostgresqlapi.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

public class DateConverterUtil {

	private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm:ss";
	private static final String ZONE_ID = "Europe/Berlin";
	private static final DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.GERMANY);
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

	public static Date parseStringToDate(String dateToParse) throws ParseException {
		return dateFormat.parse(dateToParse);
	}

	public static String formatDateToString(Date dateToFormat) {
		return dateFormat.format(dateToFormat);
	}

	public static String formatMillisecondsDateToString(long millisecondsDate) {
		return dateFormat.format(millisecondsDate);
	}

	public static long calculateDifferenceInDays(String dateToCompare) {
		LocalDateTime parsedStringDate = LocalDateTime.parse(dateToCompare, dateTimeFormatter);
		LocalDateTime now = LocalDateTime.now(ZoneId.of(ZONE_ID));
		return ChronoUnit.DAYS.between(parsedStringDate, now);
	}
}
