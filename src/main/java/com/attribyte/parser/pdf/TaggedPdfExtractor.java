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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extracts structured content from tagged PDFs using the PDF structure tree.
 * <p>
 * Tagged PDFs (especially those from InDesign, Word, etc.) contain a structure tree
 * that maps content to semantic elements like paragraphs, headings, and text frames.
 * This extractor uses Story boundaries (InDesign text frames) or Sect/Art boundaries
 * as natural content splits, and paragraph style names for heading vs body classification.
 * </p>
 */
final class TaggedPdfExtractor {

   static final class TaggedStory {

      TaggedStory(String text, ImmutableList<String> paragraphStyles, int pageIndex) {
         this.text = text;
         this.paragraphStyles = paragraphStyles;
         this.pageIndex = pageIndex;
      }

      final String text;
      final ImmutableList<String> paragraphStyles;
      final int pageIndex;

      boolean hasHeadingStyle() {
         for(String style : paragraphStyles) {
            if(isHeadingStyle(style)) return true;
         }
         return false;
      }

      String extractTitle() {
         // Use heading-style paragraphs as title candidates.
         for(String style : paragraphStyles) {
            if(isHeadingStyle(style)) return null; // title is embedded, handled by toHtml
         }
         return null;
      }

      String toHtml() {
         if(text.isEmpty()) return "";
         // For now, wrap the whole story text as a paragraph.
         // The paragraph styles could drive more granular HTML,
         // but we'd need per-paragraph text boundaries for that.
         return "<p>" + escapeHtml(text) + "</p>\n";
      }

      boolean isSubstantial(int minContentLength) {
         return text.length() >= minContentLength;
      }

      boolean isJunk() {
         if(text.isEmpty()) return true;
         if(text.length() < 50) {
            // Short stories that are just page numbers, credits, or fragments.
            if(text.matches("^\\d{1,3}$")) return true;
            if(text.length() < 5) return true;
         }
         // TOC-only stories.
         if(paragraphStyles.stream().allMatch(s -> s.startsWith("TOC_"))) {
            return false; // Keep TOC — it has useful content.
         }
         // Photo credits.
         if(paragraphStyles.stream().allMatch(s -> s.contains("Credit") || s.contains("credit"))) {
            return true;
         }
         return false;
      }

      StoryCategory categorize() {
         if(paragraphStyles.isEmpty()) return StoryCategory.BODY;

         boolean allToc = true;
         boolean allCredit = true;
         boolean hasCover = false;
         boolean hasBody = false;
         boolean hasHeading = false;

         for(String style : paragraphStyles) {
            String lower = style.toLowerCase(Locale.ROOT);
            if(!lower.startsWith("toc_")) allToc = false;
            if(!lower.contains("credit") && !lower.contains("copyright")) allCredit = false;
            if(lower.contains("cover_")) hasCover = true;
            if(lower.contains("text_justified") || lower.contains("text_standard")
                    || lower.contains("text_indent") || lower.contains("text_bio")
                    || lower.contains("feature_text") || lower.contains("sidebar_text")) {
               hasBody = true;
            }
            if(isHeadingStyle(style)) hasHeading = true;
         }

         if(allToc) return StoryCategory.TOC;
         if(allCredit) return StoryCategory.CREDIT;
         if(hasCover && !hasBody) return StoryCategory.COVER;
         if(hasBody) return StoryCategory.BODY;
         if(hasHeading) return StoryCategory.HEADING;
         return StoryCategory.OTHER;
      }
   }

   enum StoryCategory {
      BODY,     // Article body text
      HEADING,  // Standalone heading/title
      TOC,      // Table of contents
      COVER,    // Cover page elements
      CREDIT,   // Photo/illustration credits
      OTHER     // Ads, metadata, etc.
   }

   /**
    * Check if a PDF document has a usable structure tree.
    */
   static boolean isTaggedPdf(PDDocument doc) {
      PDStructureTreeRoot root = doc.getDocumentCatalog().getStructureTreeRoot();
      if(root == null) return false;
      List<Object> kids = root.getKids();
      return kids != null && !kids.isEmpty();
   }

   /**
    * Extract stories from a tagged PDF.
    * @return List of tagged stories, or empty list if extraction fails.
    */
   static List<TaggedStory> extractStories(PDDocument doc) throws IOException {
      PDStructureTreeRoot root = doc.getDocumentCatalog().getStructureTreeRoot();
      if(root == null) return List.of();

      // Build page MCID → text maps for all pages.
      Map<Integer, Map<Integer, String>> pageMcidText = new HashMap<>();
      for(int p = 0; p < doc.getNumberOfPages(); p++) {
         PDPage page = doc.getPage(p);
         PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
         extractor.processPage(page);
         Map<Integer, String> mcidText = new HashMap<>();
         for(PDMarkedContent mc : extractor.getMarkedContents()) {
            collectMcText(mc, mcidText);
         }
         pageMcidText.put(p, mcidText);
      }

      // Walk the structure tree and collect stories.
      List<TaggedStory> stories = Lists.newArrayList();
      List<Object> kids = root.getKids();
      if(kids != null) {
         for(Object kid : kids) {
            walkForStories(kid, doc, pageMcidText, stories);
         }
      }

      return stories;
   }

   /**
    * Generate a debug report of the tagged structure.
    */
   static String debugTaggedStructure(PDDocument doc, List<TaggedStory> stories) {
      StringBuilder out = new StringBuilder();
      out.append("Tagged PDF: YES (").append(stories.size()).append(" stories extracted)\n");

      int kept = 0, dropped = 0;
      for(TaggedStory story : stories) {
         if(story.isJunk()) dropped++;
         else kept++;
      }
      out.append("Kept: ").append(kept).append(", Dropped (junk): ").append(dropped).append("\n");

      out.append("\n--- Stories ---\n");
      for(int i = 0; i < stories.size(); i++) {
         TaggedStory story = stories.get(i);
         StoryCategory cat = story.categorize();
         String junk = story.isJunk() ? " [JUNK]" : "";
         String text = story.text;
         if(text.length() > 80) text = text.substring(0, 80) + "...";
         out.append(String.format("  %3d  p%-3d  %-8s%s  (%d chars) \"%s\"\n",
                 i, story.pageIndex + 1, cat, junk, story.text.length(), text));
         if(!story.paragraphStyles.isEmpty()) {
            out.append("       styles: ").append(story.paragraphStyles).append("\n");
         }
      }

      return out.toString();
   }

   private static void walkForStories(Object node, PDDocument doc,
                                       Map<Integer, Map<Integer, String>> pageMcidText,
                                       List<TaggedStory> stories) {
      if(!(node instanceof PDStructureElement)) return;
      PDStructureElement el = (PDStructureElement) node;
      String type = el.getStructureType();

      // Story = InDesign text frame, the natural content boundary.
      if("Story".equals(type)) {
         StringBuilder text = new StringBuilder();
         List<String> paraStyles = Lists.newArrayList();
         int[] pageRef = {-1};
         resolveElementText(el, doc, pageMcidText, text, paraStyles, pageRef);
         String content = text.toString().trim();
         stories.add(new TaggedStory(content, ImmutableList.copyOf(paraStyles),
                 pageRef[0] >= 0 ? pageRef[0] : 0));
         return;
      }

      // Recurse into Document, Article, Sect, etc. to find Stories.
      List<Object> kids = el.getKids();
      if(kids != null) {
         for(Object kid : kids) {
            walkForStories(kid, doc, pageMcidText, stories);
         }
      }
   }

   private static void resolveElementText(PDStructureElement el, PDDocument doc,
                                           Map<Integer, Map<Integer, String>> pageMcidText,
                                           StringBuilder text, List<String> paraStyles,
                                           int[] pageRef) {
      String type = el.getStructureType();

      // Record paragraph style names — skip structural wrappers.
      if(!STRUCTURAL_TYPES.contains(type) && !paraStyles.contains(type)) {
         paraStyles.add(type);
      }

      // Resolve page index from this element.
      if(pageRef[0] < 0) {
         int idx = resolvePageIndex(el, doc);
         if(idx >= 0) pageRef[0] = idx;
      }

      // Extract MCID text references from the COS dictionary.
      COSBase kObj = el.getCOSObject().getDictionaryObject(COSName.K);
      if(kObj != null) {
         extractMcidText(kObj, el, doc, pageMcidText, text);
      }

      // Recurse into child structure elements.
      List<Object> kids = el.getKids();
      if(kids != null) {
         for(Object kid : kids) {
            if(kid instanceof PDStructureElement) {
               resolveElementText((PDStructureElement) kid, doc, pageMcidText, text, paraStyles, pageRef);
            }
         }
      }
   }

   private static void extractMcidText(COSBase kObj, PDStructureElement el, PDDocument doc,
                                        Map<Integer, Map<Integer, String>> pageMcidText,
                                        StringBuilder text) {
      if(kObj instanceof COSInteger) {
         int mcid = ((COSInteger) kObj).intValue();
         int pageIdx = resolvePageIndex(el, doc);
         if(pageIdx >= 0) appendMcidText(pageMcidText, pageIdx, mcid, text);
      } else if(kObj instanceof COSDictionary) {
         COSDictionary d = (COSDictionary) kObj;
         COSName mcidName = COSName.getPDFName("MCID");
         if(d.containsKey(mcidName)) {
            int mcid = d.getInt(mcidName);
            int pageIdx = resolvePageIndex(el, doc);
            // Check if dict has its own Pg reference.
            COSBase pg = d.getDictionaryObject(COSName.getPDFName("Pg"));
            if(pg instanceof COSDictionary) {
               for(int i = 0; i < doc.getNumberOfPages(); i++) {
                  if(doc.getPage(i).getCOSObject() == pg) {
                     pageIdx = i;
                     break;
                  }
               }
            }
            if(pageIdx >= 0) appendMcidText(pageMcidText, pageIdx, mcid, text);
         }
      } else if(kObj instanceof COSArray) {
         COSArray arr = (COSArray) kObj;
         for(int i = 0; i < arr.size(); i++) {
            extractMcidText(arr.get(i), el, doc, pageMcidText, text);
         }
      }
   }

   private static int resolvePageIndex(PDStructureElement el, PDDocument doc) {
      PDPage page = el.getPage();
      if(page != null) {
         for(int i = 0; i < doc.getNumberOfPages(); i++) {
            if(doc.getPage(i).getCOSObject() == page.getCOSObject()) return i;
         }
      }
      return -1;
   }

   private static void appendMcidText(Map<Integer, Map<Integer, String>> pageMcidText,
                                       int pageIdx, int mcid, StringBuilder text) {
      Map<Integer, String> mcidMap = pageMcidText.get(pageIdx);
      if(mcidMap != null) {
         String t = mcidMap.get(mcid);
         if(t != null && !t.isEmpty()) {
            if(text.length() > 0) text.append(" ");
            text.append(t);
         }
      }
   }

   private static void collectMcText(PDMarkedContent mc, Map<Integer, String> mcidText) {
      int mcid = mc.getMCID();
      StringBuilder sb = new StringBuilder();
      for(Object content : mc.getContents()) {
         if(content instanceof TextPosition) {
            sb.append(((TextPosition) content).getUnicode());
         } else if(content instanceof PDMarkedContent) {
            Map<Integer, String> inner = new HashMap<>();
            collectMcText((PDMarkedContent) content, inner);
            inner.values().forEach(sb::append);
         }
      }
      String t = sb.toString().trim();
      if(!t.isEmpty() && mcid >= 0) {
         mcidText.put(mcid, t);
      }
   }

   static boolean isHeadingStyle(String style) {
      String lower = style.toLowerCase(Locale.ROOT);
      return lower.contains("head") || lower.contains("subhead")
              || lower.contains("headline") || lower.contains("opener_head")
              || lower.contains("section_opener") || lower.contains("dept_head");
   }

   private static String escapeHtml(String text) {
      return text.replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;");
   }

   private static final Set<String> STRUCTURAL_TYPES = Set.of(
           "Story", "Sect", "Span", "Link", "Reference", "Note",
           "Table", "TBody", "TR", "TD", "TH", "L", "LI", "Lbl", "LBody",
           "Document", "Article", "Art", "Figure", "key_insights_list"
   );

   // Default used only by debug output; actual threshold comes from PdfParserConfig.
   static final int DEFAULT_MIN_STORY_CONTENT_LENGTH = 500;
}
