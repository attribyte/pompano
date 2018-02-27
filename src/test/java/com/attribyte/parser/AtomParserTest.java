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

import com.attribyte.parser.entry.AtomParser;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.io.IOException;

import static com.attribyte.parser.Util.childrenToString;
import static org.junit.Assert.*;

/**
 * Test the Atom parser.
 * @author Matt Hamer
 */
public class AtomParserTest extends ResourceTest {

   @Test
   public void basic() throws IOException {
      ParseResult res = new AtomParser().parse(testResource("atom.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals("Example Feed", parsedResource.title);
      assertEquals("A subtitle", parsedResource.subtitle);
      //assertEquals("", parsedResource.siteLink);
      assertEquals("2003-12-13T18:30:02Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(parsedResource.updatedTimestamp));
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("Atom-Powered Robots Run Amok", entry.title);
      assertEquals("http://example.org/2003/12/13/atom03.html", entry.canonicalLink);
      assertEquals("2003-12-13T18:30:02Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.publishedTimestamp));
      assertEquals("2003-12-14T18:30:02Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.updatedTimestamp));
      assertEquals("<p>This is the entry content.</p>", entry.cleanContent);
      assertTrue(entry.originalContent().isPresent());
      assertEquals("<p>This is the entry content.</p>", childrenToString(entry.originalContent().get().body()));
      assertNotNull(entry.authors);
      assertEquals(1, entry.authors.size());
      Author author = entry.authors.get(0);
      assertEquals("John Doe", author.name);
      assertEquals("johndoe@example.com", author.email);
   }
}
