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

import com.attribyte.parser.entry.TwitterAPIParser;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import com.attribyte.parser.model.Video;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Test the twitter parser.
 */
public class TwitterParserTest extends ResourceTest {

   @Test
   public void timeline() throws IOException {
      ParseResult res = new TwitterAPIParser().parse(testResource("twitter_timeline3.json"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      for(Entry entry : res.resource.get().entries) {
         printEntry(entry);
      }
   }


   private void printEntry(final Entry entry) {
      System.out.println("id=" + entry.id);
      System.out.println("canonicalLink=" + entry.canonicalLink);
      System.out.println("published=" + new Date(entry.publishedTimestamp));
      System.out.println("content=" + entry.cleanContent);
      entry.authors.forEach(author -> {
         System.out.println("author.name=" + author.name);
         System.out.println("author.displayName=" + author.displayName);
         System.out.println("author.description=" + author.description);
         System.out.println("author.link=" + author.link);
         if(author.image != null) {
            System.out.println("author.image.link=" + author.image.link);
         }
         entry.tags.forEach(tag -> {
            System.out.println("tag=" + tag);
         });
      });

      entry.citations.forEach(citation -> {
         System.out.println("entry.citation.href=" + citation.href);
      });

      entry.images.forEach(image -> {
         System.out.println("entry.image.link=" + image.link);
      });

      System.out.println();
      entry.videos.forEach(this::printVideo);
   }

   private void printVideo(final Video video) {
      System.out.println("video.id=" + video.id);
      System.out.println("video.link=" + video.link);
      if(video.image != null) {
         System.out.println("video.image.link=" + video.image.link);
      }
      System.out.println("video.mediaType=" + video.mediaType);
      if(video.aspect != null) {
         System.out.println("video.aspect=" + video.aspect);
      }
      System.out.println("video.bitrate=" + video.bitrate);
      System.out.println("video.durationMillis=" + video.durationMillis);

      if(!video.variants.isEmpty()) {
         System.out.println();
         System.out.println("Variants");
         System.out.println("********");
         System.out.println();
         video.variants.forEach(this::printVideo);
      } else {
         System.out.println();
      }
   }
}
