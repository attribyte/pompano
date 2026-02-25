/*
 * Copyright 2026 Attribyte Labs, LLC
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

package com.attribyte.parser.epub;

import com.attribyte.parser.model.Entry;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.*;

public class EpubParserTest {

   @Test
   public void testToEntry() throws Exception {
      // Build a minimal EpubDocument with metadata and an empty spine
      EpubParser.Metadata metadata = createMetadata("Test Book", ImmutableList.of("Author One"),
              ImmutableList.of("fiction", "adventure"), 1700000000000L);

      EpubParser.Spine spine = new EpubParser.Spine(ImmutableList.of(), ImmutableList.of(), null);
      EpubParser.EpubDocument doc = new EpubParser.EpubDocument(metadata, spine, null);

      Entry entry = EpubParser.toEntry(doc);
      assertEquals("Test Book", entry.title);
      assertEquals(1, entry.authors.size());
      assertEquals("Author One", entry.authors.get(0).name);
      assertEquals(2, entry.tags.size());
      assertTrue(entry.tags.contains("fiction"));
      assertTrue(entry.tags.contains("adventure"));
      assertTrue(entry.publishedTimestamp > 0);
   }

   @Test
   public void testToEntryEmptyTitle() throws Exception {
      EpubParser.Metadata metadata = createMetadata("", ImmutableList.of(), ImmutableList.of(), 0L);
      EpubParser.Spine spine = new EpubParser.Spine(ImmutableList.of(), ImmutableList.of(), null);
      EpubParser.EpubDocument doc = new EpubParser.EpubDocument(metadata, spine, null);

      Entry entry = EpubParser.toEntry(doc);
      assertEquals("Untitled EPUB", entry.title);
   }

   /**
    * Create a Metadata instance for testing via reflection, since the constructor is package-private.
    */
   private static EpubParser.Metadata createMetadata(String title, ImmutableList<String> creators,
                                                       ImmutableList<String> subjects, long publishedTimestamp) {
      try {
         // Use reflection to set the final fields since Metadata's constructor requires a packageDoc
         java.lang.reflect.Constructor<EpubParser.Metadata> ctor =
                 EpubParser.Metadata.class.getDeclaredConstructor(org.jsoup.nodes.Document.class);
         ctor.setAccessible(true);

         // Build a minimal OPF document that Metadata can parse
         String opf = "<?xml version='1.0' encoding='UTF-8'?>" +
                 "<package xmlns='http://www.idpf.org/2007/opf' unique-identifier='uid'>" +
                 "<metadata xmlns:dc='http://purl.org/dc/elements/1.1/'>" +
                 "<dc:identifier id='uid'>test-id</dc:identifier>" +
                 "<dc:title>" + title + "</dc:title>";
         for(String creator : creators) {
            opf += "<dc:creator>" + creator + "</dc:creator>";
         }
         for(String subject : subjects) {
            opf += "<dc:subject>" + subject + "</dc:subject>";
         }
         if(publishedTimestamp > 0) {
            // Convert millis to ISO date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            opf += "<dc:date>" + sdf.format(new java.util.Date(publishedTimestamp)) + "</dc:date>";
         }
         opf += "</metadata></package>";

         org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(opf, "", org.jsoup.parser.Parser.xmlParser());
         return ctor.newInstance(doc);
      } catch(Exception e) {
         throw new RuntimeException("Failed to create test Metadata", e);
      }
   }
}
