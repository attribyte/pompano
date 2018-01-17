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

import com.attribyte.parser.entry.AmpParser;
import com.attribyte.parser.entry.AtomParser;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.io.IOException;

import static com.attribyte.parser.Util.childrenToString;
import static org.junit.Assert.*;

/**
 * Test the AMP parser.
 * @author Matt Hamer
 */
public class AmpParserTest extends ResourceTest {

   @Test
   public void ldJson() throws IOException {
      ParseResult res = new AmpParser().parse(testResource("amp.html"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("The test title", entry.title);
      assertEquals("https://example.com/test1.html", entry.canonicalLink);
      assertEquals("The test description", entry.summary);
      assertEquals("1997-05-05T12:02:41Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.publishedTimestamp));
      assertEquals("1997-05-05T13:02:41Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.updatedTimestamp));
      assertEquals(1, entry.authors.size());
      assertEquals("Test Author", entry.authors.get(0).name);
      assertTrue(entry.primaryImage.isPresent());
      Image primaryImage = entry.primaryImage.get();
      assertEquals("http://cdn.ampproject.org/leader.jpg", primaryImage.link);
      assertEquals(2000, primaryImage.height);
      assertEquals(800, primaryImage.width);
   }
}
