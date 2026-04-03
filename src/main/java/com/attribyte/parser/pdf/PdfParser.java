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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.List;

/**
 * Parses PDF files into pompano {@link Entry} objects.
 * <p>
 * Uses PDFBox to extract text with structural analysis — detecting paragraphs via
 * PDFBox's built-in spatial heuristics, and headings/titles/authors via font size analysis.
 * </p>
 */
public class PdfParser {

   /**
    * Parse a PDF into sections based on the document outline (bookmarks) or heading structure.
    * <p>
    * Fallback chain:
    * <ol>
    *   <li>PDF outline with 2+ bookmarks → split by bookmarks (with structural paragraph detection)</li>
    *   <li>No outline → structural analysis → if 2+ section breaks, split at level-1 headings</li>
    *   <li>No section breaks → single entry (same as {@link #parse(byte[], String)})</li>
    * </ol>
    * </p>
    * @param pdfBytes The PDF file content.
    * @param filename The original filename (used as title fallback).
    * @return The list of section entries.
    * @throws IOException on parse error.
    */
   public static List<Entry> parseSections(byte[] pdfBytes, String filename) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {
         PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

         // Path 1: PDF outline with 2+ bookmarks.
         if(outline != null && outline.hasChildren()) {
            List<PDOutlineItem> items = Lists.newArrayList();
            for(PDOutlineItem item : outline.children()) {
               items.add(item);
            }

            if(items.size() >= 2) {
               return parseSectionsByOutline(doc, items, filename);
            }
         }

         // Path 2: Structural analysis — split by level-1 headings.
         PdfStructure structure = extractStructure(doc);
         if(structure.sectionBreaks.size() >= 2) {
            return parseSectionsByStructure(doc, structure, filename);
         }

         // Path 3: Single entry.
         return List.of(buildSingleEntry(doc, structure, filename));
      }
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
         PdfStructure structure = extractStructure(doc);
         return buildSingleEntry(doc, structure, filename);
      }
   }

   private static Entry buildSingleEntry(PDDocument doc, PdfStructure structure, String filename) {
      PDDocumentInformation info = doc.getDocumentInformation();
      Entry.Builder builder = new Entry.Builder();

      // Title: structural detection, then PDF metadata, then filename.
      String title = structure.detectedTitle;
      if(title == null && info != null) {
         title = Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim());
      }
      if(title == null) {
         title = titleFromFilename(filename);
      }
      builder.setTitle(title);

      // Author: structural detection, then PDF metadata.
      String author = structure.detectedAuthor;
      if(author == null && info != null) {
         author = Strings.emptyToNull(Strings.nullToEmpty(info.getAuthor()).trim());
      }
      if(author != null) {
         builder.addAuthor(Author.builder(author).build());
      }

      // Creation date.
      if(info != null) {
         Calendar creationDate = info.getCreationDate();
         if(creationDate != null) {
            builder.setPublishedTimestamp(creationDate.getTimeInMillis());
         }
      }

      builder.setCleanContent(structure.toHtml());
      return builder.build();
   }

   private static List<Entry> parseSectionsByOutline(PDDocument doc, List<PDOutlineItem> items,
                                                     String filename) throws IOException {
      PDDocumentInformation info = doc.getDocumentInformation();
      int totalPages = doc.getNumberOfPages();

      String docTitle = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim()) : null;
      if(docTitle == null) {
         docTitle = titleFromFilename(filename);
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
            if(endPage > startPage) {
               endPage = endPage - 1;
            }
         } else {
            endPage = totalPages;
         }

         if(endPage < startPage) endPage = startPage;

         PdfStructure sectionStructure = extractStructure(doc, startPage, endPage);
         String html = sectionStructure.toHtml();

         Entry.Builder builder = new Entry.Builder();
         builder.setTitle(sectionTitle);
         builder.setCleanContent(html);
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

   private static List<Entry> parseSectionsByStructure(PDDocument doc, PdfStructure structure,
                                                       String filename) {
      PDDocumentInformation info = doc.getDocumentInformation();

      String docTitle = structure.detectedTitle;
      if(docTitle == null && info != null) {
         docTitle = Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim());
      }
      if(docTitle == null) {
         docTitle = titleFromFilename(filename);
      }

      String author = structure.detectedAuthor;
      if(author == null && info != null) {
         author = Strings.emptyToNull(Strings.nullToEmpty(info.getAuthor()).trim());
      }

      long publishedMillis = 0;
      if(info != null && info.getCreationDate() != null) {
         publishedMillis = info.getCreationDate().getTimeInMillis();
      }

      List<Entry> entries = Lists.newArrayListWithExpectedSize(structure.sectionBreaks.size());

      for(int i = 0; i < structure.sectionBreaks.size(); i++) {
         int fromBlock = structure.sectionBreaks.get(i);
         int toBlock = i + 1 < structure.sectionBreaks.size()
                 ? structure.sectionBreaks.get(i + 1)
                 : structure.blocks.size();

         PdfStructure.PdfBlock headingBlock = structure.blocks.get(fromBlock);
         String sectionTitle = headingBlock.text;

         // Render blocks after the heading.
         String html = structure.toHtml(fromBlock + 1, toBlock);

         Entry.Builder builder = new Entry.Builder();
         builder.setTitle(sectionTitle);
         builder.setCleanContent(html);
         if(author != null) {
            builder.addAuthor(Author.builder(author).build());
         }
         if(publishedMillis > 0) {
            builder.setPublishedTimestamp(publishedMillis);
         }
         builder.addMetadata("section_index", String.valueOf(i));
         builder.addMetadata("document_title", docTitle);
         builder.addMetadata("page_start", String.valueOf(headingBlock.startPage));

         entries.add(builder.build());
      }

      // If there are blocks before the first section break, prepend as entry 0.
      int firstBreak = structure.sectionBreaks.get(0);
      if(firstBreak > 0) {
         String preambleHtml = structure.toHtml(0, firstBreak);
         if(!preambleHtml.trim().isEmpty()) {
            Entry.Builder builder = new Entry.Builder();
            builder.setTitle(docTitle);
            builder.setCleanContent(preambleHtml);
            if(author != null) {
               builder.addAuthor(Author.builder(author).build());
            }
            if(publishedMillis > 0) {
               builder.setPublishedTimestamp(publishedMillis);
            }
            builder.addMetadata("section_index", "preamble");
            builder.addMetadata("document_title", docTitle);
            entries.add(0, builder.build());
         }
      }

      return entries;
   }

   private static PdfStructure extractStructure(PDDocument doc) throws IOException {
      StructuralTextStripper stripper = new StructuralTextStripper();
      StringWriter writer = new StringWriter();
      stripper.writeText(doc, writer);
      List<PdfLine> lines = stripper.getLines();
      return PdfStructure.analyze(lines, doc.getNumberOfPages());
   }

   private static PdfStructure extractStructure(PDDocument doc, int startPage, int endPage) throws IOException {
      StructuralTextStripper stripper = new StructuralTextStripper();
      stripper.setStartPage(startPage);
      stripper.setEndPage(endPage);
      StringWriter writer = new StringWriter();
      stripper.writeText(doc, writer);
      List<PdfLine> lines = stripper.getLines();
      return PdfStructure.analyze(lines, doc.getNumberOfPages());
   }

   private static int resolvePageNumber(PDOutlineItem item, PDDocument doc) {
      try {
         if(item.getDestination() instanceof PDPageDestination) {
            PDPageDestination dest = (PDPageDestination) item.getDestination();
            int pageIndex = dest.retrievePageNumber();
            return pageIndex >= 0 ? pageIndex + 1 : -1;
         }
         if(item.getAction() != null) {
            return -1;
         }
      } catch(IOException e) {
         // Ignore
      }
      return -1;
   }

   /**
    * Generate a debug report of the structural analysis for a PDF.
    * @param pdfBytes The PDF file content.
    * @param filename The original filename.
    * @return A human-readable debug report.
    * @throws IOException on parse error.
    */
   public static String debugStructure(byte[] pdfBytes, String filename) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {
         PDDocumentInformation info = doc.getDocumentInformation();
         int totalPages = doc.getNumberOfPages();

         StructuralTextStripper stripper = new StructuralTextStripper();
         StringWriter sw = new StringWriter();
         stripper.writeText(doc, sw);
         List<PdfLine> lines = stripper.getLines();
         PdfStructure structure = PdfStructure.analyze(lines, totalPages);

         StringBuilder out = new StringBuilder();
         out.append("=== PDF Debug: ").append(filename).append(" ===\n");
         out.append("Pages: ").append(totalPages).append("\n");

         // PDF metadata.
         if(info != null) {
            String metaTitle = Strings.nullToEmpty(info.getTitle()).trim();
            String metaAuthor = Strings.nullToEmpty(info.getAuthor()).trim();
            if(!metaTitle.isEmpty()) out.append("Metadata title: ").append(metaTitle).append("\n");
            if(!metaAuthor.isEmpty()) out.append("Metadata author: ").append(metaAuthor).append("\n");
         }

         // Outline.
         PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
         if(outline != null && outline.hasChildren()) {
            int count = 0;
            for(PDOutlineItem item : outline.children()) count++;
            out.append("Outline bookmarks: ").append(count).append("\n");
         } else {
            out.append("Outline: none\n");
         }

         // Structural detection results.
         out.append("\n--- Detected ---\n");
         out.append("Title: ").append(structure.detectedTitle != null ? structure.detectedTitle : "(none)").append("\n");
         out.append("Author: ").append(structure.detectedAuthor != null ? structure.detectedAuthor : "(none)").append("\n");
         out.append("Section breaks: ").append(structure.sectionBreaks.size()).append("\n");

         // Lines.
         out.append("\n--- Lines (").append(lines.size()).append(") ---\n");
         for(int i = 0; i < lines.size(); i++) {
            PdfLine line = lines.get(i);
            out.append(String.format("  %3d  p%-2d  %5.1fpt  %s%s  \"%s\"\n",
                    i, line.pageNumber, line.fontSize,
                    line.isBold ? "B" : " ",
                    line.paragraphStart ? "P" : " ",
                    truncate(line.text, 80)));
         }

         // Blocks.
         out.append("\n--- Blocks (").append(structure.blocks.size()).append(") ---\n");
         for(int i = 0; i < structure.blocks.size(); i++) {
            PdfStructure.PdfBlock block = structure.blocks.get(i);
            String marker = "";
            if(block.type == PdfStructure.BlockType.HEADING) {
               marker = " [H" + block.headingLevel + "]";
               if(structure.sectionBreaks.contains(i)) {
                  marker += " [SECTION BREAK]";
               }
            }
            out.append(String.format("  %3d  p%-2d  %-9s%s  \"%s\"\n",
                    i, block.startPage, block.type, marker,
                    truncate(block.text, 80)));
         }

         return out.toString();
      }
   }

   private static String truncate(String s, int max) {
      return s.length() <= max ? s : s.substring(0, max) + "...";
   }

   private static String titleFromFilename(String filename) {
      if(filename != null) {
         int dot = filename.lastIndexOf('.');
         return dot > 0 ? filename.substring(0, dot) : filename;
      }
      return "Untitled PDF";
   }
}