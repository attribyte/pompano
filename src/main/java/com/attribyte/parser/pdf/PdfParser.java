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

import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.google.common.base.Strings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.Calendar;

/**
 * Parses PDF files into pompano {@link Entry} objects.
 * <p>
 * Uses PDFBox to extract text and document metadata (title, author, creation date).
 * </p>
 */
public class PdfParser {

   /**
    * Parse a PDF from raw bytes.
    * @param pdfBytes The PDF file content.
    * @param filename The original filename (used as title fallback).
    * @return The parsed entry.
    * @throws IOException on parse error.
    */
   public static Entry parse(byte[] pdfBytes, String filename) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {
         PDDocumentInformation info = doc.getDocumentInformation();

         Entry.Builder builder = new Entry.Builder();

         // Title: PDF metadata, then filename
         String title = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim()) : null;
         if(title == null) {
            if(filename != null) {
               int dot = filename.lastIndexOf('.');
               title = dot > 0 ? filename.substring(0, dot) : filename;
            } else {
               title = "Untitled PDF";
            }
         }
         builder.setTitle(title);

         // Author
         if(info != null) {
            String author = Strings.emptyToNull(Strings.nullToEmpty(info.getAuthor()).trim());
            if(author != null) {
               builder.addAuthor(Author.builder(author).build());
            }
         }

         // Creation date
         if(info != null) {
            Calendar creationDate = info.getCreationDate();
            if(creationDate != null) {
               builder.setPublishedTimestamp(creationDate.getTimeInMillis());
            }
         }

         // Extract text
         PDFTextStripper stripper = new PDFTextStripper();
         String text = stripper.getText(doc);

         // Convert to simple HTML paragraphs
         StringBuilder html = new StringBuilder();
         String[] paragraphs = text.split("\\n\\s*\\n");
         for(String para : paragraphs) {
            String trimmed = para.trim();
            if(!trimmed.isEmpty()) {
               html.append("<p>");
               html.append(escapeHtml(trimmed));
               html.append("</p>\n");
            }
         }

         builder.setCleanContent(html.toString());
         return builder.build();
      }
   }

   private static String escapeHtml(String text) {
      return text.replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;");
   }
}
