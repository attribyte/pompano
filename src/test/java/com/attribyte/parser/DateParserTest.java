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

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;
import static com.attribyte.parser.DateParser.parseISO8601;
import static com.attribyte.parser.DateParser.parseRFC822;

/**
 * Tests for the various date/time parsers.
 * @author Matt Hamer
 */
public class DateParserTest {

   @Test
   public void validISO8601() throws ParseException  {
      long timestamp = parseISO8601("2003-12-13T18:30:02Z");
      assertTrue(timestamp > 0);
      assertEquals("2003-12-13T18:30:02Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(timestamp));
   }

   @Test
   public void invalidISO8601() {
      try {
         parseISO8601("x2003-12-13T18:30:02Z");
         fail("Expecting parse exception to be thrown");
      } catch(ParseException pe) {
         //Expected
      }
   }

   @Test
   public void validISO8601WithMillis() throws ParseException {
      long timestamp = parseISO8601("2003-12-13T18:30:02.145Z");
      assertTrue(timestamp > 0);
      assertEquals("2003-12-13T18:30:02.145Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validISO8601WithOffset() throws ParseException {
      long timestamp = parseISO8601("2003-12-13T10:30:02.145-0600");
      assertTrue(timestamp > 0);
      assertEquals("2003-12-13T16:30:02.145Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validISO8601NoSeconds() throws ParseException {
      long timestamp = parseISO8601("2003-12-13T18:30Z");
      assertTrue(timestamp > 0);
      assertEquals("2003-12-13T18:30:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 01:00:00 EST");
      assertEquals("2002-10-02T06:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void invalidRFC822() {
      try {
         parseRFC822("Wed, 02 Invalid 2002 01:00:00 EST");
         fail("Expecting parse exception to be thrown");
      } catch(ParseException pe) {
         //Expected
      }
   }

   @Test
   public void validRFC822Offset() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 01:00:00 -0500");
      assertEquals("2002-10-02T06:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822OffsetColon() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 01:00:00 -05:00");
      assertEquals("2002-10-02T06:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822OffsetPlus() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 03:00:00 +01:00");
      assertEquals("2002-10-02T02:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822OffsetMillis() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 03:00:00.000 +0100");
      assertEquals("2002-10-02T02:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822ShortOffset() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 03:00:00.000 +1");
      assertEquals("2002-10-02T02:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822GMT() throws ParseException {
      long timestamp = parseRFC822("Wed, 02 Oct 2002 03:00:00 GMT");
      assertEquals("2002-10-02T03:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }

   @Test
   public void validRFC822NoLeadingZero() throws ParseException {
      long timestamp = parseRFC822("Wed, 2 Oct 2002 3:00:00 GMT");
      assertEquals("2002-10-02T03:00:00.000Z", ISODateTimeFormat.dateTime().withZoneUTC().print(timestamp));
   }
}
