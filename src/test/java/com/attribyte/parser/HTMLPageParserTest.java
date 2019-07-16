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

import com.attribyte.parser.model.Anchor;
import com.attribyte.parser.model.Audio;
import com.attribyte.parser.model.Image;
import com.attribyte.parser.model.Page;
import com.attribyte.parser.model.Video;
import com.attribyte.parser.page.HTMLPageParser;

import org.junit.Test;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for the HTML page parser.
 * @author Matt Hamer
 */
public class HTMLPageParserTest extends ResourceTest {

   @Test
   public void videosInMetadata() throws IOException {
      Page page = HTMLPageParser.parse(testResource("testvideo1.html"), "");
      assertEquals(3, page.metaVideos.size());
      Video video1 = page.metaVideos.get(0);
      assertEquals("http://example.com/test1.mp4", video1.link);
      assertEquals("video/mp4", video1.mediaType);
      assertEquals(100, video1.width);
      assertEquals(101, video1.height);

      Video video2 = page.metaVideos.get(1);
      assertEquals("http://example.com/test2.mp4", video2.link);
      assertEquals("video/mp42", video2.mediaType);
      assertEquals(200, video2.width);
      assertEquals(201, video2.height);
      Video video3 = page.metaVideos.get(2);
      assertEquals("http://example.com/test3.mp4", video3.link);
   }

   @Test
   public void videosInMetadataAndBody() throws IOException {
      Page page = HTMLPageParser.parse(testResource("testvideo2.html"), "https://example.com");
      assertEquals(1, page.metaVideos.size());
      Video video1 = page.metaVideos.get(0);
      assertEquals("https://example.com/test1.mp4", video1.link);
      assertEquals("video/mp4", video1.mediaType);
      assertEquals(100, video1.width);
      assertEquals(101, video1.height);

      assertEquals(3, page.videos.size());
      Video video2 = page.videos.get(1);
      assertEquals("https://example.com/movie.mp4", video2.link);
      assertEquals("video/mp4", video2.mediaType);
      assertEquals(200, video2.width);
      assertEquals(200, video2.height);
      assertEquals("The alt text", video2.altText);


      Video video3 = page.videos.get(2);
      assertEquals("https://example.com/movie.ogg", video3.link);
      assertEquals("video/ogg", video3.mediaType);
      assertEquals(200, video3.width);
      assertEquals(200, video3.height);
      assertEquals("The alt text", video3.altText);
      assertEquals("The title", video3.title);
   }

   @Test
   public void audioInMetadataAndBody() throws IOException {
      Page page = HTMLPageParser.parse(testResource("testaudio.html"), "https://example.com");
      assertEquals(1, page.metaAudios.size());
      Audio audio1 = page.metaAudios.get(0);
      assertEquals("https://example.com/test1.mp3", audio1.link);
      assertEquals("audio/mp3", audio1.type);

      assertEquals(2, page.audios.size());
      Audio audio2 = page.audios.get(1);
      assertEquals("https://example.com/test2.mp3", audio2.link);
      assertEquals("audio/mp3", audio2.type);
      assertEquals("The alt text", audio2.altText);
   }

   @Test
   public void externalLinks() throws IOException {
      Page page = HTMLPageParser.parse(testResource("testvideo1.html"), "https://example.com");
      assertEquals(1, page.externalAnchors().size());
      Anchor anchor1 = page.externalAnchors().get(0);
      assertEquals("https://example1.com/test1.html", anchor1.href);
      assertEquals("External Link 1 Text", anchor1.anchorText);
      assertEquals("External Link 1 Title", anchor1.title);
   }

   @Test
   public void bodyImages() throws IOException {
      Page page = HTMLPageParser.parse(testResource("testvideo1.html"), "https://example.com");
      assertEquals(1, page.images.size());
      Image image1 = page.images.get(0);
      assertEquals("https://example1.com/image1.jpg", image1.link);
      assertEquals("Body Image 1 Title", image1.title);
      assertEquals("Body Image 1 Alt", image1.altText);
      assertEquals(100, image1.height);
      assertEquals(200, image1.width);
   }
}