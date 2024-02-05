package org.gradle.backendpostgresqlapi.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateConverterUtil {

	private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);

	public static Date parseStringToDate(String dateToParse) throws ParseException {
		return dateFormat.parse(dateToParse);
	}

	public static String formatDateToString(Date dateToFormat) {
		return dateFormat.format(dateToFormat);
	}

	public static String formatMillisecondsDateToString(long millisecondsDate) {
		return dateFormat.format(millisecondsDate);
	}
}
