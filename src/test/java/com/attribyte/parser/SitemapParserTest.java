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

import com.attribyte.parser.model.SitemapLink;
import com.attribyte.parser.sitemap.SitemapParser;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the sitemap parser.
 * @author Matt Hamer
 */
public class SitemapParserTest extends ResourceTest {

   @Test
   public void xmlFormat() throws IOException, ParseException {
      List<SitemapLink> links = SitemapParser.parse(testResource("sitemap1.xml"), "https://example.com");
      assertNotNull(links);
      assertEquals(1, links.size());
      SitemapLink link1 = links.get(0);
      assertEquals("https://example.com/1", link1.url);
      assertEquals(SitemapLink.ChangeFrequency.MONTHLY, link1.changeFrequency);
      assertEquals("2005-01-01", ISODateTimeFormat.date().withZoneUTC().print(link1.lastModifiedTimestamp));
   }

   @Test
   public void textFormat() throws IOException, ParseException {
      List<SitemapLink> links = SitemapParser.parse(testResource("sitemap1.txt"), "https://example.com");
      assertNotNull(links);
      assertEquals(2, links.size());
      SitemapLink link1 = links.get(0);
      assertEquals("https://example.com/file1.html", link1.url);
      assertEquals(SitemapLink.ChangeFrequency.NEVER, link1.changeFrequency);
      assertEquals(0, link1.lastModifiedTimestamp);

      SitemapLink link2 = links.get(1);
      assertEquals("https://example.com/file2.html", link2.url);
      assertEquals(SitemapLink.ChangeFrequency.NEVER, link1.changeFrequency);
      assertEquals(0, link1.lastModifiedTimestamp);
   }
}