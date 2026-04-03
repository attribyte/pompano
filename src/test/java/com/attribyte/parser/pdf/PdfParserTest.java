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
      byte[] pdfBytes = createPdfWithSingleBookmark();

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "single-bm.pdf");
      assertEquals(1, sections.size());
   }

   @Test
   public void testParagraphBreaks() throws Exception {
      // Two text blocks with large vertical gap should become separate paragraphs.
      byte[] pdfBytes = createPdfWithParagraphGap();

      Entry entry = PdfParser.parse(pdfBytes, "paragraphs.pdf");
      String content = entry.cleanContent;
      // Should contain at least two <p> tags.
      int pCount = countOccurrences(content, "<p>");
      assertTrue("Expected 2+ paragraphs, got " + pCount, pCount >= 2);
   }

   @Test
   public void testHeadingDetection() throws Exception {
      // 18pt heading + 12pt body should produce an h2 tag.
      byte[] pdfBytes = createPdfWithHeading();

      Entry entry = PdfParser.parse(pdfBytes, "heading.pdf");
      assertTrue("Expected h2 or h3 in content: " + entry.cleanContent,
              entry.cleanContent.contains("<h2>") || entry.cleanContent.contains("<h3>"));
   }

   @Test
   public void testTitleDetection() throws Exception {
      // 24pt title, no metadata — should detect title from content.
      byte[] pdfBytes = createPdfWithVisualTitle();

      Entry entry = PdfParser.parse(pdfBytes, "visual-title.pdf");
      assertEquals("Important Research Paper", entry.title);
   }

   @Test
   public void testSectionSplittingByHeadings() throws Exception {
      // Multi-page PDF, no outline, clear headings → multiple entries.
      byte[] pdfBytes = createPdfWithHeadingSections();

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "sections.pdf");
      assertTrue("Expected 2+ sections from heading splitting, got " + sections.size(),
              sections.size() >= 2);
   }

   @Test
   public void testFallbackChain() throws Exception {
      // No outline + no headings → single entry.
      byte[] pdfBytes = createTestPdf("Plain Doc", null, "Just plain text.");

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "plain.pdf");
      assertEquals(1, sections.size());
   }

   @Test
   public void testOutlinePreferred() throws Exception {
      // Outline + detectable headings → outline wins.
      byte[] pdfBytes = createPdfWithBookmarksAndHeadings();

      List<Entry> sections = PdfParser.parseSections(pdfBytes, "both.pdf");
      // Outline has 2 bookmarks, so should split by outline (2 sections), not headings.
      assertEquals(2, sections.size());
      assertEquals("Part One", sections.get(0).title);
      assertEquals("Part Two", sections.get(1).title);
   }

   // --- Helper methods to create test PDFs ---

   private static byte[] createPdfWithParagraphGap() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDPage page = new PDPage();
         doc.addPage(page);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText("First paragraph of text.");
            cs.endText();

            // Large gap (simulating paragraph break).
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 600);
            cs.showText("Second paragraph of text.");
            cs.endText();
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createPdfWithHeading() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDPage page = new PDPage();
         doc.addPage(page);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Body text first (establishes 12pt as body size).
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText("This is regular body text at twelve points.");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 680);
            cs.showText("More regular body text to establish baseline.");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 660);
            cs.showText("And yet more body text here.");
            cs.endText();

            // Heading at 18pt.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 18);
            cs.newLineAtOffset(50, 600);
            cs.showText("Major Section Heading");
            cs.endText();

            // Body text after heading.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 570);
            cs.showText("Content under the heading.");
            cs.endText();
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createPdfWithVisualTitle() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         // No metadata title set.
         PDPage page = new PDPage();
         doc.addPage(page);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Large title.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 24);
            cs.newLineAtOffset(50, 750);
            cs.showText("Important Research Paper");
            cs.endText();

            // Body text (needs several lines to establish 12pt as mode).
            for(int i = 0; i < 5; i++) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 12);
               cs.newLineAtOffset(50, 600 - (i * 20));
               cs.showText("Body text line " + (i + 1) + " of this document.");
               cs.endText();
            }
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createPdfWithHeadingSections() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         // Page 1 — body + heading at 22pt (1.83x body = level 1).
         PDPage page1 = new PDPage();
         doc.addPage(page1);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
            for(int i = 0; i < 4; i++) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 12);
               cs.newLineAtOffset(50, 700 - (i * 20));
               cs.showText("Intro body text line " + (i + 1) + ".");
               cs.endText();
            }

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 22);
            cs.newLineAtOffset(50, 550);
            cs.showText("First Major Section");
            cs.endText();

            for(int i = 0; i < 3; i++) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 12);
               cs.newLineAtOffset(50, 500 - (i * 20));
               cs.showText("Section one content line " + (i + 1) + ".");
               cs.endText();
            }
         }

         // Page 2 — another heading.
         PDPage page2 = new PDPage();
         doc.addPage(page2);
         try(PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 22);
            cs.newLineAtOffset(50, 700);
            cs.showText("Second Major Section");
            cs.endText();

            for(int i = 0; i < 3; i++) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 12);
               cs.newLineAtOffset(50, 650 - (i * 20));
               cs.showText("Section two content line " + (i + 1) + ".");
               cs.endText();
            }
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         doc.save(baos);
         return baos.toByteArray();
      }
   }

   private static byte[] createPdfWithBookmarksAndHeadings() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDDocumentInformation info = new PDDocumentInformation();
         info.setTitle("Dual Doc");
         doc.setDocumentInformation(info);

         // Two pages with headings.
         for(int p = 0; p < 2; p++) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try(PDPageContentStream cs = new PDPageContentStream(doc, page)) {
               cs.beginText();
               cs.setFont(PDType1Font.HELVETICA, 18);
               cs.newLineAtOffset(50, 700);
               cs.showText("Heading on Page " + (p + 1));
               cs.endText();

               for(int i = 0; i < 3; i++) {
                  cs.beginText();
                  cs.setFont(PDType1Font.HELVETICA, 12);
                  cs.newLineAtOffset(50, 650 - (i * 20));
                  cs.showText("Body text on page " + (p + 1) + " line " + (i + 1) + ".");
                  cs.endText();
               }
            }
         }

         // Add outline with 2 bookmarks.
         PDDocumentOutline outline = new PDDocumentOutline();
         String[] titles = {"Part One", "Part Two"};
         for(int i = 0; i < 2; i++) {
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

   private static byte[] createPdfWithBookmarks() throws Exception {
      try(PDDocument doc = new PDDocument()) {
         PDDocumentInformation info = new PDDocumentInformation();
         info.setTitle("Test Document");
         info.setAuthor("Tester");
         doc.setDocumentInformation(info);

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

   private static int countOccurrences(String str, String sub) {
      int count = 0;
      int idx = 0;
      while((idx = str.indexOf(sub, idx)) != -1) {
         count++;
         idx += sub.length();
      }
      return count;
   }
}
