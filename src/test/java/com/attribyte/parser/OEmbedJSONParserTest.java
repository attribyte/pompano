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
import com.attribyte.parser.entry.OEmbedJSONParser;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test the oEmbed parser.
 * @author Matt Hamer
 */
public class OEmbedJSONParserTest extends ResourceTest {

   @Test
   public void oembedPhoto() throws IOException {
      ParseResult res = new OEmbedJSONParser().parse(testResource("oembed.json"), "https://test.com/1", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("ZB8T0193", entry.title);
      assertEquals("https://test.com/1", entry.canonicalLink);
      assertEquals(1, entry.authors.size());
      Author author = entry.authors.get(0);
      assertEquals("Bees", author.name);
      assertEquals("http://www.flickr.com/photos/bees/", author.link);
      assertNotNull(entry.primaryImage);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals("http://farm4.static.flickr.com/3123/2341623661_7c99f48bbf_m.jpg", entry.primaryImage.get().link);
      assertEquals(240, entry.primaryImage.get().width);
      assertEquals(160, entry.primaryImage.get().height);
   }

   @Test
   public void oembedRich() throws IOException {
      ParseResult res = new OEmbedJSONParser().parse(testResource("oembed2.json"),
              "https://twitter.com/Interior/status/507185938620219395", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("https://twitter.com/Interior/status/507185938620219395", entry.canonicalLink);
      assertEquals(1, entry.authors.size());
      Author author = entry.authors.get(0);
      assertEquals("US Department of the Interior", author.name);
      assertEquals("https://twitter.com/Interior", author.link);
      assertNotNull(entry.cleanContent);
      assertTrue(entry.cleanContent.startsWith("<blockquote"));
      assertFalse(entry.cleanContent.contains("<script"));
   }

   @Test
   public void oembedRichThumbnail() throws IOException {
      ParseResult res = new OEmbedJSONParser().parse(testResource("oembed3.json"),
              "https://twitter.com/Interior/status/507185938620219395", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("My favorite cat from tonight's episode- a true winner. #newgirl", entry.title);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals("https://scontent-iad3-1.cdninstagram.com/vp/02e278a40db16aa7304374f2b7b62107/5B6386F2/t51.2885-15/e15/11262720_891453137565191_1495973619_n.jpg", entry.primaryImage.get().link);
      assertEquals(612, entry.primaryImage.get().width);
      assertEquals(613, entry.primaryImage.get().height);
   }
}
