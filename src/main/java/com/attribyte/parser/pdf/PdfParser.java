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
 * Fallback chain for multi-entry splitting:
 * <ol>
 *   <li>Tagged PDF structure tree (InDesign Story boundaries) → split by stories</li>
 *   <li>PDF outline with 2+ bookmarks → split by bookmarks</li>
 *   <li>Structural analysis (font sizes) → split at level-1 headings</li>
 *   <li>Single entry</li>
 * </ol>
 * </p>
 */
public class PdfParser {

   /**
    * Parse a PDF into sections using default configuration.
    */
   public static List<Entry> parseSections(byte[] pdfBytes, String filename) throws IOException {
      return parseSections(pdfBytes, filename, PdfParserConfig.DEFAULT);
   }

   /**
    * Parse a PDF into sections with custom configuration.
    * @param pdfBytes The PDF file content.
    * @param filename The original filename (used as title fallback).
    * @param config Parser configuration for tuning heuristics.
    * @return The list of section entries.
    * @throws IOException on parse error.
    */
   public static List<Entry> parseSections(byte[] pdfBytes, String filename,
                                            PdfParserConfig config) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {

         // Flatten form fields so their values appear in text extraction.
         org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm acroForm =
                 doc.getDocumentCatalog().getAcroForm();
         if(acroForm != null) {
            acroForm.flatten();
         }

         // Path 1: Tagged PDF structure tree.
         if(TaggedPdfExtractor.isTaggedPdf(doc)) {
            List<TaggedPdfExtractor.TaggedStory> stories = TaggedPdfExtractor.extractStories(doc);
            List<Entry> entries = parseSectionsByTags(doc, stories, filename, config);
            if(entries.size() >= 2) {
               return entries;
            }
            // Fall through if tagged extraction produced only 0-1 useful entries.
         }

         PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();

         // Path 2: PDF outline with 2+ bookmarks.
         if(outline != null && outline.hasChildren()) {
            List<PDOutlineItem> items = Lists.newArrayList();
            for(PDOutlineItem item : outline.children()) {
               items.add(item);
            }

            if(items.size() >= 2) {
               return parseSectionsByOutline(doc, items, filename, config);
            }
         }

         // Path 3: Structural analysis — split by level-1 headings.
         PdfStructure structure = extractStructure(doc, config);
         if(structure.sectionBreaks.size() >= config.minSectionBreaks) {
            return parseSectionsByStructure(doc, structure, filename);
         }

         // Path 4: Single entry.
         return List.of(buildSingleEntry(doc, structure, filename));
      }
   }

   /**
    * Parse a PDF from raw bytes using default configuration.
    */
   public static Entry parse(byte[] pdfBytes, String filename) throws IOException {
      return parse(pdfBytes, filename, PdfParserConfig.DEFAULT);
   }

   /**
    * Parse a PDF from raw bytes with custom configuration.
    * @param pdfBytes The PDF file content.
    * @param filename The original filename (used as title fallback).
    * @param config Parser configuration for tuning heuristics.
    * @return The parsed entry.
    * @throws IOException on parse error.
    */
   public static Entry parse(byte[] pdfBytes, String filename, PdfParserConfig config) throws IOException {
      try(PDDocument doc = PDDocument.load(pdfBytes)) {
         PdfStructure structure = extractStructure(doc, config);
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

   private static List<Entry> parseSectionsByTags(PDDocument doc,
                                                   List<TaggedPdfExtractor.TaggedStory> stories,
                                                   String filename, PdfParserConfig config) {
      PDDocumentInformation info = doc.getDocumentInformation();

      String docTitle = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getTitle()).trim()) : null;
      if(docTitle == null) {
         docTitle = titleFromFilename(filename);
      }

      String author = info != null ? Strings.emptyToNull(Strings.nullToEmpty(info.getAuthor()).trim()) : null;
      long publishedMillis = 0;
      if(info != null && info.getCreationDate() != null) {
         publishedMillis = info.getCreationDate().getTimeInMillis();
      }

      int minContent = config.minStoryContentLength;

      // First pass: collect eligible stories and look for heading → body pairs.
      List<TaggedPdfExtractor.TaggedStory> eligible = Lists.newArrayList();
      for(TaggedPdfExtractor.TaggedStory story : stories) {
         if(story.isJunk()) continue;
         TaggedPdfExtractor.StoryCategory cat = story.categorize();
         if(cat == TaggedPdfExtractor.StoryCategory.CREDIT
                 || cat == TaggedPdfExtractor.StoryCategory.COVER) {
            continue;
         }
         eligible.add(story);
      }

      List<Entry> entries = Lists.newArrayList();
      for(int i = 0; i < eligible.size(); i++) {
         TaggedPdfExtractor.TaggedStory story = eligible.get(i);

         // Skip non-substantial stories unless they're headings that can pair with the next story.
         if(!story.isSubstantial(minContent)) {
            if(story.hasHeadingStyle() && i + 1 < eligible.size()) {
               TaggedPdfExtractor.TaggedStory next = eligible.get(i + 1);
               if(next.isSubstantial(minContent) && Math.abs(next.pageIndex - story.pageIndex) <= 1) {
                  continue; // Heading consumed when we process the next story.
               }
            }
            continue;
         }

         String html = story.toHtml();
         if(html.isEmpty()) continue;

         // Look backward for a heading story to use as title.
         String title = null;
         if(i > 0) {
            TaggedPdfExtractor.TaggedStory prev = eligible.get(i - 1);
            if(!prev.isSubstantial(minContent) && prev.hasHeadingStyle()
                    && Math.abs(story.pageIndex - prev.pageIndex) <= 1) {
               title = prev.text.trim();
               if(title.length() > 120) {
                  title = title.substring(0, 117).trim() + "...";
               }
            }
         }

         // Fallback: use heading style text within this story, or truncate content.
         if(title == null && story.hasHeadingStyle()) {
            String text = story.text;
            int nl = text.indexOf('\n');
            if(nl < 0) nl = text.indexOf(". ");
            if(nl > 0 && nl <= 120) {
               title = text.substring(0, nl).trim();
            }
         }
         if(title == null) {
            title = story.text.length() > 80
                    ? story.text.substring(0, 77).trim() + "..."
                    : story.text;
         }

         Entry.Builder builder = new Entry.Builder();
         builder.setTitle(title);
         builder.setCleanContent(html);
         if(author != null) {
            builder.addAuthor(Author.builder(author).build());
         }
         if(publishedMillis > 0) {
            builder.setPublishedTimestamp(publishedMillis);
         }
         builder.addMetadata("section_index", String.valueOf(entries.size()));
         builder.addMetadata("document_title", docTitle);
         builder.addMetadata("page_start", String.valueOf(story.pageIndex + 1));
         builder.addMetadata("split_method", "tagged");

         entries.add(builder.build());
      }

      return entries;
   }

   private static List<Entry> parseSectionsByOutline(PDDocument doc, List<PDOutlineItem> items,
                                                     String filename, PdfParserConfig config)
           throws IOException {
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

         PdfStructure sectionStructure = extractStructure(doc, startPage, endPage, config);
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

   private static PdfStructure extractStructure(PDDocument doc, PdfParserConfig config)
           throws IOException {
      StructuralTextStripper stripper = new StructuralTextStripper();
      StringWriter writer = new StringWriter();
      stripper.writeText(doc, writer);
      List<PdfLine> lines = stripper.getLines();
      return PdfStructure.analyze(lines, doc.getNumberOfPages(), config);
   }

   private static PdfStructure extractStructure(PDDocument doc, int startPage, int endPage,
                                                 PdfParserConfig config) throws IOException {
      StructuralTextStripper stripper = new StructuralTextStripper();
      stripper.setStartPage(startPage);
      stripper.setEndPage(endPage);
      StringWriter writer = new StringWriter();
      stripper.writeText(doc, writer);
      List<PdfLine> lines = stripper.getLines();
      return PdfStructure.analyze(lines, doc.getNumberOfPages(), config);
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

         // Tagged PDF.
         boolean isTagged = TaggedPdfExtractor.isTaggedPdf(doc);
         out.append("Tagged PDF: ").append(isTagged ? "YES" : "NO").append("\n");

         if(isTagged) {
            List<TaggedPdfExtractor.TaggedStory> stories = TaggedPdfExtractor.extractStories(doc);
            out.append("\n").append(TaggedPdfExtractor.debugTaggedStructure(doc, stories));
         }

         // Heuristic structural analysis.
         StructuralTextStripper stripper = new StructuralTextStripper();
         StringWriter sw = new StringWriter();
         stripper.writeText(doc, sw);
         List<PdfLine> lines = stripper.getLines();
         PdfStructure structure = PdfStructure.analyze(lines, totalPages);

         out.append("\n--- Heuristic Analysis ---\n");
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
