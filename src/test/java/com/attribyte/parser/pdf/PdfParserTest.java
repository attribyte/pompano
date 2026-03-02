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

package com.attribyte.parser.pdf;

import com.attribyte.parser.model.Entry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.Assert.*;

public class PdfParserTest {

   @Test
   public void testParseWithMetadata() throws Exception {
      byte[] pdfBytes = createTestPdf("Test PDF Title", "Test Author", "Hello from PDF.");

      Entry entry = PdfParser.parse(pdfBytes, "test.pdf");
      assertEquals("Test PDF Title", entry.title);
      assertEquals(1, entry.authors.size());
      assertEquals("Test Author", entry.authors.get(0).name);
      assertTrue(entry.cleanContent.contains("Hello from PDF."));
   }

   @Test
   public void testParseFallbackToFilename() throws Exception {
      byte[] pdfBytes = createTestPdf(null, null, "Some content.");

      Entry entry = PdfParser.parse(pdfBytes, "my-document.pdf");
      assertEquals("my-document", entry.title);
      assertTrue(entry.authors.isEmpty());
      assertTrue(entry.cleanContent.contains("Some content."));
   }

   @Test
   public void testParseParagraphs() throws Exception {
      byte[] pdfBytes = createTestPdf("Title", null, "First paragraph.");

      Entry entry = PdfParser.parse(pdfBytes, "test.pdf");
      assertTrue(entry.cleanContent.contains("<p>"));
   }

   @Test
   public void testParseSectionsNoOutline() throws Exception {
      byte[] pdfBytes = createTestPdf("Simple PDF", "Author", "Content here.");

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "simple.pdf");
      assertEquals(1, sections.size());
      assertEquals("Simple PDF", sections.get(0).title);
   }

   @Test
   public void testParseSectionsWithOutline() throws Exception {
      byte[] pdfBytes = createPdfWithBookmarks();

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "bookmarked.pdf");
      assertEquals(3, sections.size());

      assertEquals("Introduction", sections.get(0).title);
      assertEquals("0", sections.get(0).metadata.get("section_index"));
      assertEquals("Test Document", sections.get(0).metadata.get("document_title"));
      assertNotNull(sections.get(0).metadata.get("page_start"));

      assertEquals("Chapter One", sections.get(1).title);
      assertEquals("1", sections.get(1).metadata.get("section_index"));

      assertEquals("Conclusion", sections.get(2).title);
      assertEquals("2", sections.get(2).metadata.get("section_index"));
   }

   @Test
   public void testParseSectionsSingleBookmark() throws Exception {
      // A PDF with only one bookmark should fall back to single entry.
      byte[] pdfBytes = createPdfWithSingleBookmark();

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "single-bm.pdf");
      assertEquals(1, sections.size());
   }

   private static byte[] createPdfWithBookmarks() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDDocumentInformation info = new PDDocumentInformation();
         info.setTitle("Test Document");
         info.setAuthor("Tester");
         doc.setDocumentInformation(info);

         // Create 3 pages.
         for(int i = 0; i < 3; i++) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 12);
               cs.newLineAtOffset(50, 700);
               cs.showText("Page " + (i + 1) + " content.");
               cs.endText();
            }
         }

         // Create outline with 3 bookmarks.
         PDDocumentOutline outline = new PDDocumentOutline();
         String[] titles = {"Introduction", "Chapter One", "Conclusion"};
         for(int i = 0; i < 3; i++) {
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(titles[i]);
            PDPageFitDestination dest = new PDPageFitDestination();
            dest.setPage(doc.getPage(i));
            item.setDestination(dest);
            outline.addLast(item);
         }
         doc.getDocumentCatalog().setDocumentOutline(outline);

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createPdfWithSingleBookmark() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDPage page = new PDPage();
         doc.addPage(page);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText("Only content.");
            cs.endText();
         }

         PDDocumentOutline outline = new PDDocumentOutline();
         PDOutlineItem item = new PDOutlineItem();
         item.setTitle("Only Section");
         PDPageFitDestination dest = new PDPageFitDestination();
         dest.setPage(doc.getPage(0));
         item.setDestination(dest);
         outline.addLast(item);
         doc.getDocumentCatalog().setDocumentOutline(outline);

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createTestPdf(String title, String author, String text) throws Exception {
      try(PDDocument doc = new PDDocument()) {
         if(title != null || author != null) {
            PDDocumentInformation info = new PDDocumentInformation();
            if(title != null) info.setTitle(title);
            if(author != null) info.setAuthor(author);
            doc.setDocumentInformation(info);
         }

         PDPage page = new PDPage();
         doc.addPage(page);

         try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText(text);
            cs.endText();
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }
}
