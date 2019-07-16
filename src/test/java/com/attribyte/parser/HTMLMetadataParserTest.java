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

import com.attribyte.parser.entry.HTMLMetadataParser;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Resource;
import com.attribyte.parser.model.Video;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test the Atom parser.
 * @author Matt Hamer
 */
public class HTMLMetadataParserTest extends ResourceTest {

   @Test
   public void jsonld() throws IOException {
      ParseResult res = new HTMLMetadataParser().parse(testResource("testjsonld.html"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("This is the title", entry.title);
      assertEquals("https://example.com/post/1", entry.canonicalLink);
      assertEquals(1, entry.authors.size());
      Author author = entry.authors.get(0);
      assertEquals("Test Author", author.name);
   }

   @Test
   public void ogMetadata() throws IOException {
      ParseResult res = new HTMLMetadataParser().parse(testResource("ogmetadata.html"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      assertEquals(1, parsedResource.entries.size());
      Entry entry = parsedResource.entries.get(0);
      assertEquals("Cory Booker on Twitter", entry.title);
      assertEquals("https://twitter.com/CoryBooker/status/953362403055587329", entry.canonicalLink);
      assertEquals("“When ignorance and bigotry are allied with power, it’s a dangerous force in our country. To not stand up to this; to be silent— is to be a part of the problem. https://t.co/Gtx65dxTIF”", entry.summary);
      assertTrue(entry.primaryImage.isPresent());
      assertEquals("https://pbs.twimg.com/ext_tw_video_thumb/953362228379639809/pu/img/y07DFCthzy93GZXV.jpg", entry.primaryImage.get().link);
      assertEquals("Alt Text 1", entry.primaryImage.get().altText);
      assertEquals(2, entry.images.size());
      Image secondImage = entry.images.get(1);
      assertEquals("https://pbs.twimg.com/ext_tw_video_thumb/953362228379639809/pu/img/y07DFCthzy93GZXV-2.jpg", secondImage.link);
      assertEquals("Alt Text 2", secondImage.altText);
      assertFalse(entry.videos.isEmpty());
      assertEquals(2, entry.videos.size());
      assertTrue(entry.primaryVideo.isPresent());
      Video primaryVideo = entry.primaryVideo.get();
      assertEquals("https://twitter.com/i/videos/953362403055587329?embed_source=facebook", primaryVideo.link);
      assertEquals(652, primaryVideo.width);
      assertEquals(720, primaryVideo.height);
      assertEquals("text/html", primaryVideo.mediaType);
      Video secondVideo = entry.videos.get(1);
      assertEquals("https://twitter.com/i/videos/953362403055587329-2", secondVideo.link);
      assertEquals("text/xml", secondVideo.mediaType);
      assertEquals(400, secondVideo.width);
      assertEquals(800, secondVideo.height);
      assertTrue(entry.primaryAudio.isPresent());
      assertEquals("https://example.com/test.mp3", entry.primaryAudio.get().link);
      assertEquals("audio/mp3", entry.primaryAudio.get().type);
   }
}