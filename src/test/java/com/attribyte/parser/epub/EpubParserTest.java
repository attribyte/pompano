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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

   @Test
   public void testParseChaptersSingleChapter() throws Exception {
      byte[] epub = createTestEpub(1);
      List<Entry> chapters = EpubParser.parseChapters(epub, "single.epub");
      assertEquals(1, chapters.size());
   }

   @Test
   public void testParseChaptersMultipleChapters() throws Exception {
      byte[] epub = createTestEpub(3);
      List<Entry> chapters = EpubParser.parseChapters(epub, "multi.epub");
      assertEquals(3, chapters.size());

      assertEquals("Chapter One", chapters.get(0).title);
      assertEquals("Chapter Two", chapters.get(1).title);
      assertEquals("Chapter Three", chapters.get(2).title);

      // Check metadata.
      assertEquals("0", chapters.get(0).metadata.get("chapter_index"));
      assertEquals("Test Book", chapters.get(0).metadata.get("book_title"));
      assertEquals("1", chapters.get(1).metadata.get("chapter_index"));

      // Check authors propagated.
      assertEquals(1, chapters.get(0).authors.size());
      assertEquals("Test Author", chapters.get(0).authors.get(0).name);
   }

   /**
    * Create a minimal EPUB zip for testing.
    * Each chapter is a separate XHTML file with an h1 title.
    */
   private static byte[] createTestEpub(int chapterCount) throws Exception {
      String[] chapterNames = {"One", "Two", "Three", "Four", "Five"};
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try(ZipOutputStream zos = new ZipOutputStream(baos)) {
         // mimetype (must be first, uncompressed)
         ZipEntry mimetypeEntry = new ZipEntry("mimetype");
         mimetypeEntry.setMethod(ZipEntry.STORED);
         byte[] mimetypeBytes = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
         mimetypeEntry.setSize(mimetypeBytes.length);
         mimetypeEntry.setCompressedSize(mimetypeBytes.length);
         java.util.zip.CRC32 crc = new java.util.zip.CRC32();
         crc.update(mimetypeBytes);
         mimetypeEntry.setCrc(crc.getValue());
         zos.putNextEntry(mimetypeEntry);
         zos.write(mimetypeBytes);
         zos.closeEntry();

         // META-INF/container.xml
         zos.putNextEntry(new ZipEntry("META-INF/container.xml"));
         zos.write(("<?xml version='1.0'?>" +
                 "<container xmlns='urn:oasis:names:tc:opendocument:xmlns:container' version='1.0'>" +
                 "<rootfiles><rootfile full-path='OEBPS/content.opf' media-type='application/oebps-package+xml'/>" +
                 "</rootfiles></container>").getBytes(StandardCharsets.UTF_8));
         zos.closeEntry();

         // Build manifest and spine items.
         StringBuilder manifest = new StringBuilder();
         StringBuilder spine = new StringBuilder();
         for(int i = 0; i < chapterCount; i++) {
            String id = "ch" + i;
            manifest.append("<item id='").append(id).append("' href='ch").append(i)
                    .append(".xhtml' media-type='application/xhtml+xml'/>");
            spine.append("<itemref idref='").append(id).append("'/>");
         }

         // OEBPS/content.opf
         String opf = "<?xml version='1.0' encoding='UTF-8'?>" +
                 "<package xmlns='http://www.idpf.org/2007/opf' unique-identifier='uid' version='3.0'>" +
                 "<metadata xmlns:dc='http://purl.org/dc/elements/1.1/'>" +
                 "<dc:identifier id='uid'>test-epub-id</dc:identifier>" +
                 "<dc:title>Test Book</dc:title>" +
                 "<dc:creator>Test Author</dc:creator>" +
                 "<dc:subject>testing</dc:subject>" +
                 "</metadata>" +
                 "<manifest>" + manifest + "</manifest>" +
                 "<spine>" + spine + "</spine>" +
                 "</package>";
         zos.putNextEntry(new ZipEntry("OEBPS/content.opf"));
         zos.write(opf.getBytes(StandardCharsets.UTF_8));
         zos.closeEntry();

         // Chapter XHTML files.
         for(int i = 0; i < chapterCount; i++) {
            String name = i < chapterNames.length ? chapterNames[i] : String.valueOf(i + 1);
            String xhtml = "<?xml version='1.0' encoding='UTF-8'?>" +
                    "<html xmlns='http://www.w3.org/1999/xhtml'>" +
                    "<head><title>Chapter " + name + "</title></head>" +
                    "<body><h1>Chapter " + name + "</h1>" +
                    "<p>Content of chapter " + name + ".</p></body></html>";
            zos.putNextEntry(new ZipEntry("OEBPS/ch" + i + ".xhtml"));
            zos.write(xhtml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
         }
      }
      return baos.toByteArray();
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
