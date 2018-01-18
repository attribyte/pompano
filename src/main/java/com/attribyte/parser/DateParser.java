/*
 * Copyright 2018 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.attribyte.parser;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * Date/time parsing methods.
 * @author Matt Hamer.
 */
public class DateParser {

   /**
    * Parses a date/time in ISO8601 format.
    * @param dateTimeStr The string.
    * @return The timestamp.
    * @throws ParseException on invalid format.
    */
   public static long parseISO8601(final String dateTimeStr) throws ParseException {
      try {
         return ISO8601_FORMATTER.parseMillis(dateTimeStr);
      } catch(IllegalArgumentException iae) {
         throw new ParseException(iae.getMessage(), 0);
      }
   }

   /**
    * Try to parse date/time in ISO8601 format.
    * @param dateTimeStr The string.
    * @return The timestamp or {@code null} if invalid.
    */
   public static Long tryParseISO8601(final String dateTimeStr) {
      try {
         return parseISO8601(dateTimeStr);
      } catch(ParseException pe) {
         return null;
      }
   }

   /**
    * The ISO 8601 formatter.
    */
   public static final DateTimeFormatter ISO8601_FORMATTER = ISODateTimeFormat.dateOptionalTimeParser();

   /**
    * Try to parse date/time in ISO8601 format.
    * @param dateTimeStr The string.
    * @return The timestamp or {@code null} if invalid.
    */
   public static Long tryParseRFC822(final String dateTimeStr) {
      try {
         return parseRFC822(dateTimeStr);
      } catch(ParseException pe) {
         return null;
      }
   }

   /**
    * Lenient parse RFC822.
    * <p>
    *   Some examples...
    *    Wed, 02 Oct 2002 08:00:00 EST
    *    Wed, 02 Oct 2002 13:00:00 GMT
    *    Wed, 02 Oct 2002 15:00:00 +0200
    *    Tue, 17 Jul 2012 19:56:08 +0000
    * </p>
    * @param dateTimeStr The date/time string.
    * @return The timestamp.
    * @throws ParseException on invalid format.
    */
   public static final long parseRFC822(String dateTimeStr) throws ParseException {

      StringBuilder tmpBuf = new StringBuilder();

      char[] chars = dateTimeStr.toCharArray();
      int pos = 0;
      int len = chars.length;

      //Find the first digit position - ignore the day of week and any leading spaces

      while(pos < len && !CharMatcher.digit().matches(chars[pos])) {
         pos++;
      }

      if(pos == len) {
         throw new ParseException("Not an RFC822 timestamp", 0);
      }

      pos = appendToSpace(pos, chars, tmpBuf);

      Integer maybeDay = Ints.tryParse(tmpBuf.toString());
      if(maybeDay == null) {
         throw new ParseException("Unable to parse day: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      int day = maybeDay;

      pos = eatSpaces(pos, chars);
      tmpBuf.setLength(0);
      pos = appendToSpace(pos, chars, tmpBuf);

      int month = monthInt(tmpBuf.toString());
      if(month == 0) {
         throw new ParseException("Invalid month: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      pos = eatSpaces(pos, chars);
      tmpBuf.setLength(0);
      pos = appendToSpace(pos, chars, tmpBuf);

      Integer maybeYear = Ints.tryParse(tmpBuf.toString());
      if(maybeYear == null) {
         throw new ParseException("Unable to parse year: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      int year = maybeYear;

      pos = eatSpaces(pos, chars);
      if(pos >= len)
         return getTimestampMillis(year, month, day, 0, 0, 0);

      tmpBuf.setLength(0);
      pos = appendToTimeDelim(pos, chars, tmpBuf);

      Integer maybeHour = Ints.tryParse(tmpBuf.toString());
      if(maybeHour == null) {
         throw new ParseException("Unable to parse hour: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      int hour = maybeHour;

      pos = eatSpaces(pos, chars);
      if(pos >= len)
         return getTimestampMillis(year, month, day, hour, 0, 0);

      tmpBuf.setLength(0);
      pos = appendToTimeDelim(pos, chars, tmpBuf);

      Integer maybeMin = Ints.tryParse(tmpBuf.toString());
      if(maybeMin == null) {
         throw new ParseException("Unable to parse minutes: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      int min = maybeMin;

      pos = eatSpaces(pos, chars);
      if(pos >= len)
         return getTimestampMillis(year, month, day, hour, min, 0);

      tmpBuf.setLength(0);
      pos = appendToSpace(pos, chars, tmpBuf);

      Float maybeSec = Floats.tryParse(tmpBuf.toString());
      if(maybeSec == null) {
         throw new ParseException("Unable to parse seconds: "+tmpBuf.toString() + "("+dateTimeStr+")", pos);
      }

      int sec = maybeSec.intValue();

      pos = eatSpaces(pos, chars);
      if(pos >= len)
         return getTimestampMillis(year, month, day, hour, min, sec);

      char currChar;
      tmpBuf.setLength(0);

      char char0 = chars[pos++];
      tmpBuf.append(char0);

      pos = appendToSpace(pos, chars, tmpBuf);

      long offset = 0L;
      long gmtTime = getTimestampMillis(year, month, day, hour, min, sec);

      String tzStr = tmpBuf.toString();

      switch(char0) {
         case '-':
         case '+':
         case '0':
         case '1':
         case '2':
         case '3':
         case '4':
         case '5':
         case '6':
         case '7':
         case '8':
         case '9':
            if(char0 == '+' || char0 == '-') {
               tzStr = tzStr.substring(1);
            }

            int tzStrLen = tzStr.length();

            int endPos = 2;
            if(endPos > tzStrLen) {
               endPos = tzStrLen;
            }
            String offsetHourStr = tzStr.substring(0, endPos);
            Integer maybeOffsetHours = Ints.tryParse(offsetHourStr);
            if(maybeOffsetHours == null) {
               throw new ParseException("Invalid offset hours: "+tzStr, pos);
            }

            int offsetHours = maybeOffsetHours;

            offset = offsetHours * 3600L * 1000L;

            int startPos = endPos;

            if(startPos < tzStrLen && tzStr.charAt(startPos) == ':') { //Allow ":' in timezone
               startPos++;
            }

            if(startPos < tzStrLen) {
               endPos = startPos + 2;
               if(endPos > tzStrLen) {
                  endPos = tzStrLen;
               }

               String offsetMinStr = tzStr.substring(startPos, endPos);
               Integer maybeOffsetMins = Ints.tryParse(offsetMinStr);
               if(maybeOffsetMins == null) {
                  throw new ParseException("Invalid offset hours: "+tzStr, pos);
               }
               int offsetMins = maybeOffsetMins;

               offset = offset + offsetMins * 60L * 1000L;
            }

            if(char0 != '-')
               offset = offset * -1L;
            break;
         case 'Z':
         case 'z':
            return getTimestampMillis(year, month, day, hour, min, sec); //GMT
         default:
            Long offsetLong = tzOffsetMap.get(tzStr);
            if(offsetLong != null) {
               offset = offsetLong;
            } else {
               TimeZone tz = TimeZone.getTimeZone(tzStr);
               if(tz != null) {
                  offset = tz.getOffset(gmtTime);
               }
            }
      }

      return gmtTime + offset;
   }

   /**
    * Gets the index for a month name or {@code 0} if invalid.
    * @param month The month as a string.
    * @return The month index.
    */
   private static int monthInt(final String month) {
      switch(month.toLowerCase()) {
         case "jan" :
         case "january":
            return 1;
         case "feb" :
         case "february":
            return 2;
         case "mar" :
         case "march":
            return 3;
         case "apr" :
         case "april":
            return 4;
         case "may" :
            return 5;
         case "jun" :
         case "june":
            return 6;
         case "jul" :
         case "july":
            return 7;
         case "aug" :
         case "august":
            return 8;
         case "sep" :
         case "september":
            return 9;
         case "oct" :
         case "october":
            return 10;
         case "nov" :
         case "november":
            return 11;
         case "dec" :
         case "december":
            return 12;
         default:
            return 0;
      }
   }

   /**
    * Eat spaces in the input buffer.
    * @param pos The start position.
    * @param chars The input buffer.
    * @return The position of the first non-space.
    */
   private static int eatSpaces(int pos, final char[] chars) {
      while(pos < chars.length && CharMatcher.whitespace().matches(chars[pos])) {
         pos++;
      }
      return pos;
   }

   /**
    * Append characters to the buffer up to a space.
    * @param pos The start position.
    * @param chars The input buffer.
    * @param buf The output buffer.
    * @return The end position in the input buffer.
    */
   private static int appendToSpace(int pos, final char[] chars, final StringBuilder buf) {
      while(pos < chars.length && !CharMatcher.whitespace().matches(chars[pos])) {
         buf.append(chars[pos]);
         pos++;
      }
      return pos;
   }

   /**
    * Append characters to the buffer up to a delimiter (space or ':').
    * <p>
    *    Eats the first character.
    * </p>
    * @param pos The start position.
    * @param chars The input buffer.
    * @param buf The output buffer.
    * @return The end position in the input buffer.
    */
   private static int appendToTimeDelim(int pos, final char[] chars, final StringBuilder buf) {
      char currChar;
      while(pos < chars.length) {
         currChar = chars[pos++];
         switch(currChar) {
            case ':':
            case ' ':
               return pos;
            default:
               buf.append(currChar);
         }
      }
      return pos;
   }

   /**
    * A map of offset vs old-school time zone.
    */
   private static final ImmutableMap<String, Long> tzOffsetMap = new ImmutableMap.Builder<String, Long>()
      .put("UT", 0L)
      .put("GMT", 0L)
      .put("EST", 5L * 3600L * 1000L)
      .put("EDT", 4L * 3600L * 1000L)
      .put("CST", 6L * 3600L * 1000L)
      .put("CDT", 5L * 3600L * 1000L)
      .put("MST", 7L * 3600L * 1000L)
      .put("MDT", 6L * 3600L * 1000L)
      .put("PST", 8L * 3600L * 1000L)
      .put("PDT", 7L * 3600L * 1000L)
      .put("A", -1L * 3600L * 1000L)
      .put("M", -1L * 3600L * 1000L)
      .put("N",  3600L * 1000L)
      .put("Y",  12L * 3600L * 1000L).build();

   /**
    * Number of days elapsed in one non-leap year at the beginning of each month.
    */
   private static final long[] DMonth = {
           0L,
           31L,
           59L,
           90L,
           120L,
           151L,
           181L,
           212L,
           243L,
           273L,
           304L,
           334L
   };

   /**
    * Gets the timestamp in millis given year, month, etc...
    * @param year The year.
    * @param month The month (1 == Jan)
    * @param day The day of the month.
    * @param hour The hour.
    * @param min The minute.
    * @param sec The second.
    * @return The timestamp.
    */
   public static final long getTimestampMillis(final int year, final int month, final int day,
                                               final int hour, final int min, final int sec) {
      long iday = 365L * (year - 1970L) + DMonth[month - 1] + (day - 1L); // Full days since 1/1/70
      iday = iday + (year - 1969L) / 4L; //Leap days since 1/1/70

      if(month > 2 && ((year % 4) == 0)) { //If leap year and past Feb, add this year's leap day
         iday++;
      }

      return((long)sec + 60L * ((long)min + 60L * (hour + 24L * iday))) * 1000L;
   }
}