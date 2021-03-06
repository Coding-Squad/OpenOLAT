/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.commons.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.property.ExDate;

public class CalendarUtils {
	private static final OLog log = Tracing.createLoggerFor(CalendarUtils.class);
	private static final SimpleDateFormat ical4jFormatter = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat occurenceDateTimeFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

	public static String getTimeAsString(Date date, Locale locale) {
		return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date);
	}
	
	public static String getDateTimeAsString(Date date, Locale locale) {
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(date);
	}

	/**
	 * Create a calendar instance that uses mondays or sundays as the first day of
	 * the week depending on the given locale and sets the week number 1 to the
	 * first week in the year that has four days of january.
	 * 
	 * @param local the locale to define if a week starts on sunday or monday
	 * @return a calendar instance
	 */
	public static Calendar createCalendarInstance(Locale locale) {
		// use Calendar.getInstance(locale) that sets first day of week
		// according to locale or let user decide in GUI
		Calendar cal = Calendar.getInstance(locale);
		// manually set min days to 4 as we are used to have it
		cal.setMinimalDaysInFirstWeek(4);						
		return cal;
	}	
	
	public static Calendar getStartOfDayCalendar(Locale locale) {
		Calendar cal = createCalendarInstance(locale);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	
	
	public static Date endOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal = getEndOfDay(cal);
		return cal.getTime();
	}
	
	public static Calendar getEndOfDay(Calendar cal) {
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	public static Date getDate(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DATE, day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	
	public static String getRecurrence(String rule) {
		if (rule != null) {
			try {
				Recur recur = new Recur(rule);
				String frequency = recur.getFrequency();
				WeekDayList wdl = recur.getDayList();
				Integer interval = recur.getInterval();
				if((wdl != null && wdl.size() > 0)) {
					// we only support one rule with daylist
					return KalendarEvent.WORKDAILY;
				} else if(interval != null && interval == 2) {
					// we only support one rule with interval
					return KalendarEvent.BIWEEKLY;
				} else {
					// native supportet rule
					return frequency;
				}
			} catch (ParseException e) {
				log.error("cannot restore recurrence rule", e);
			}
		}
		
		return null;
	}
	

	

	
	/**
	 * Create list with excluded dates based on the exclusion rule.
	 * @param recurrenceExc
	 * @return list with excluded dates
	 */
	public static List<Date> getRecurrenceExcludeDates(String recurrenceExc) {
		List<Date> recurExcDates = new ArrayList<>();
		if(recurrenceExc != null && !recurrenceExc.equals("")) {
			try {
				net.fortuna.ical4j.model.ParameterList pl = new net.fortuna.ical4j.model.ParameterList();
				ExDate exdate = new ExDate(pl, recurrenceExc);
				DateList dl = exdate.getDates();
				for( Object date : dl ) {
					Date excDate = (Date)date;
					recurExcDates.add(excDate);
				}
			} catch (ParseException e) {
				log.error("cannot restore recurrence exceptions", e);
			}
		}
		
		return recurExcDates;
	}
	
	/**
	 * Create exclusion rule based on list with dates.
	 * @param dates
	 * @return string with exclude rule
	 */
	public static String getRecurrenceExcludeRule(List<Date> dates) {
		if(dates != null && dates.size() > 0) {
			DateList dl = new DateList();
			for( Date date : dates ) {
				net.fortuna.ical4j.model.Date dd = CalendarUtils.createDate(date);
				dl.add(dd);
			}
			ExDate exdate = new ExDate(dl);
			return exdate.getValue();
		}
		
		return null;
	}
	
	public static net.fortuna.ical4j.model.Date createDate(Date date) {
		try {
			String toString;
			synchronized(ical4jFormatter) {//cluster_OK only to optimize memory/speed
				toString = ical4jFormatter.format(date);
			}
			return new net.fortuna.ical4j.model.Date(toString);
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static String formatRecurrenceDate(Date date, boolean allDay) {
		try {
			String toString;
			if(allDay) {
				synchronized(ical4jFormatter) {//cluster_OK only to optimize memory/speed
					toString = ical4jFormatter.format(date);
				}
			} else {
				synchronized(occurenceDateTimeFormat) {//cluster_OK only to optimize memory/speed
					toString = occurenceDateTimeFormat.format(date);
				}
			}
			return toString;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Date removeTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);  
		cal.set(Calendar.MINUTE, 0);  
		cal.set(Calendar.SECOND, 0);  
		cal.set(Calendar.MILLISECOND, 0);  
		return cal.getTime();
	}
}