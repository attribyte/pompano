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

import com.attribyte.parser.entry.RSSParser;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test the RSS parser.
 * @author Matt Hamer
 */
public class RSSParserTest extends ResourceTest {

   @Test
   public void minimal() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals("The channel title", parsedResource.title);
      assertEquals("The channel description", parsedResource.description);
      assertEquals("Copyright 2018, Attribyte LLC", parsedResource.rights);
      assertEquals("https://attribyte.com", parsedResource.siteLink);
      assertEquals("2002-09-30T11:00:00Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(parsedResource.publishedTimestamp));
      assertEquals("2002-09-30T12:00:00Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(parsedResource.updatedTimestamp));
      assertTrue(parsedResource.entries.size() > 0);
      Entry entry = findEntry("https://attribyte.com/test1", parsedResource);
      assertNotNull(entry);
      assertEquals("The First Item", entry.title);
      assertEquals("https://attribyte.com/test1", entry.canonicalLink);
      assertNotNull(entry.originalContent());
      assertTrue(entry.originalContent().isPresent());
      assertEquals("The first item.", entry.originalContent().get().text());
      assertEquals("The first item.", entry.cleanContent);
      assertEquals("2002-09-29T23:48:33Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.publishedTimestamp));
      assertNotNull(entry.authors);
      assertEquals(0, entry.authors.size());
   }

   @Test
   public void dcAuthor() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertTrue(parsedResource.entries.size() > 1);
      Entry entry = findEntry("https://attribyte.com/test2", parsedResource);
      assertNotNull(entry);
      assertEquals(1, entry.authors.size());
      assertEquals("Matt Hamer", entry.authors.get(0).name);
   }

   @Test
   public void emailAuthor() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test3", parsedResource);
      assertNotNull(entry);
      assertEquals(1, entry.authors.size());
      assertEquals("blah@example.com", entry.authors.get(0).email);
      assertEquals("Mary Smith", entry.authors.get(0).name);
   }

   @Test
   public void dcPubDate() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test3", parsedResource);
      assertNotNull(entry);
      assertEquals("2002-09-29T23:48:33Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.publishedTimestamp));
   }

   @Test
   public void tags() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test4", parsedResource);
      assertNotNull(entry);
      assertEquals(3, entry.tags.size());
      assertEquals("chickens", entry.tags.get(0));
      assertEquals("death", entry.tags.get(1));
      assertEquals("disease", entry.tags.get(2));
   }

   @Test
   public void guidPermalink() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test5", parsedResource);
      assertNotNull(entry);
   }

   @Test
   public void feedburnerOrigLink() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test6", parsedResource);
      assertNotNull(entry);
      assertEquals(1, entry.altLinks.size());
   }

   @Test
   public void mediaImage() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test6", parsedResource);
      assertNotNull(entry);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals(1, entry.images.size());
      assertEquals("https://images.example.com/test.jpg", entry.primaryImage.get().link);
      assertEquals(720, entry.primaryImage.get().height);
      assertEquals(1280, entry.primaryImage.get().width);
      assertEquals("The image title", entry.primaryImage.get().title);
   }

   @Test
   public void enclosureImage() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test7", parsedResource);
      assertNotNull(entry);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals(1, entry.images.size());
      assertEquals("https://images.example.com/test.jpg", entry.primaryImage.get().link);
   }

   @Test
   public void mediaGroupImage() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rss2.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assert(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      Entry entry = findEntry("https://attribyte.com/test8", parsedResource);
      assertNotNull(entry);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals(1, entry.images.size());
      assertEquals("https://images.example.com/test.jpg", entry.primaryImage.get().link);
      assertEquals(720, entry.primaryImage.get().height);
      assertEquals(1280, entry.primaryImage.get().width);
      assertEquals("The image title", entry.primaryImage.get().title);
   }

   @Test
   public void rdf() throws IOException {
      ParseResult res = new RSSParser().parse(testResource("rssrdf.xml"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals("The Channel Title", parsedResource.title);
      assertEquals("The Channel Description", parsedResource.description);
      assertEquals("https://example.com/feed", parsedResource.siteLink);
      assertEquals(2, parsedResource.entries.size());
      Entry entry = findEntry("https://example.com/test1", parsedResource);
      assertNotNull(entry);
      assertEquals("Test Title 1", entry.title);
      assertEquals("https://example.com/test1", entry.canonicalLink);
      assertTrue(entry.originalContent().isPresent());
      assertEquals("Test description 1", entry.originalContent().get().text());
      assertEquals("Test description 1", entry.cleanContent);
      assertEquals("2002-09-29T23:48:33Z", ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(entry.publishedTimestamp));

      entry = findEntry("https://example.com/test2", parsedResource);
      assertNotNull(entry);
      assertEquals("Test Title 2", entry.title);
      assertEquals("https://example.com/test2", entry.canonicalLink);
      assertTrue(entry.originalContent().isPresent());
      assertEquals("Test description 2", entry.originalContent().get().text());
      assertEquals("Test description 2", entry.cleanContent);
   }

   private static Entry findEntry(final String link, final Resource parsedResource) {
      for(Entry entry : parsedResource.entries) {
         if(entry.canonicalLink.equals(link)) {
            return entry;
         }
      }
      return null;
   }

}
