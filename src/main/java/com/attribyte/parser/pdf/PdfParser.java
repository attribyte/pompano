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
import com.google.common.collect.Lists;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Parses PDF files into pompano {@link Entry} objects.
 * <p>
 * Uses PDFBox to extract text and document metadata (title, author, creation date).
 * </p>
 */
public class PdfParser {

   /**
    * Parse a PDF into sections based on the document outline (bookmarks).
    * <p>
    * If the PDF has no outline or only one bookmark, returns a single entry (same as {@link #parse(byte[], String)}).
    * Otherwise, each top-level bookmark becomes a separate entry with the text extracted from its page range.
    * </p>
    * @param pdfBytes The PDF file content.
    * @param filename The original filename (used as title fallback).
    * @return The list of section entries.
    * @throws IOException on parse error.
    */
   public static List<Entry> parseSections(byte[] pdfBytes, String filename) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {
         PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
         if(outline == null || !outline.hasChildren()) {
            return List.of(parse(pdfBytes, filename));
         }

         PDDocumentInformation info = doc.getDocumentInformation();
         int totalPages = doc.getNumberOfPages();

         // Collect bookmark start pages.
         List<PDOutlineItem> items = Lists.newArrayList();
         for(PDOutlineItem item : outline.children()) {
            items.add(item);
         }

         if(items.size() <= 1) {
            return List.of(parse(pdfBytes, filename));
         }

         String docTitle = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim()) : null;
         if(docTitle == null) {
            if(filename != null) {
               int dot = filename.lastIndexOf('.');
               docTitle = dot > 0 ? filename.substring(0, dot) : filename;
            } else {
               docTitle = "Untitled PDF";
            }
         }

         String author = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getAuthor()).trim()) : null;
         long publishedMillis = 0;
         if(info != null && info.getCreationDate() != null) {
            publishedMillis = info.getCreationDate().getTimeInMillis();
         }

         List<Entry> entries = Lists.newArrayListWithExpectedSize(items.size());
         for(int i = 0; i < items.size(); i++) {
            PDOutlineItem item = items.get(i);
            String sectionTitle = Strings.nullToEmpty(item.getTitle()).trim();
            if(sectionTitle.isEmpty()) {
               sectionTitle = "Section " + (i + 1);
            }

            int startPage = resolvePageNumber(item, doc);
            if(startPage < 1) startPage = 1;

            int endPage;
            if(i + 1 < items.size()) {
               endPage = resolvePageNumber(items.get(i + 1), doc);
               if(endPage < 1) endPage = totalPages;
               // End page is exclusive of the next section's start.
               if(endPage > startPage) {
                  endPage = endPage - 1;
               }
            } else {
               endPage = totalPages;
            }

            if(endPage < startPage) endPage = startPage;

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPage);
            stripper.setEndPage(endPage);
            String text = stripper.getText(doc);

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

            Entry.Builder builder = new Entry.Builder();
            builder.setTitle(sectionTitle);
            builder.setCleanContent(html.toString());
            if(author != null) {
               builder.addAuthor(Author.builder(author).build());
            }
            if(publishedMillis > 0) {
               builder.setPublishedTimestamp(publishedMillis);
            }
            builder.addMetadata("section_index", String.valueOf(i));
            builder.addMetadata("document_title", docTitle);
            builder.addMetadata("page_start", String.valueOf(startPage));
            builder.addMetadata("page_end", String.valueOf(endPage));

            entries.add(builder.build());
         }

         return entries;
      }
   }

   /**
    * Resolve the 1-based page number for an outline item.
    * Returns -1 if the destination cannot be resolved.
    */
   private static int resolvePageNumber(PDOutlineItem item, PDDocument doc) {
      try {
         if(item.getDestination() instanceof PDPageDestination) {
            PDPageDestination dest = (PDPageDestination) item.getDestination();
            int pageIndex = dest.retrievePageNumber();
            return pageIndex >= 0 ? pageIndex + 1 : -1;
         }
         // Try action-based destination
         if(item.getAction() != null) {
            // PDFBox 2.x: action destinations are harder to resolve, fall back
            return -1;
         }
      } catch(IOException e) {
         // Ignore
      }
      return -1;
   }

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
