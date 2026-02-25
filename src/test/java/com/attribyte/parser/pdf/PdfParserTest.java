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
import org.junit.Test;

import java.io.ByteArrayOutputStream;

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
