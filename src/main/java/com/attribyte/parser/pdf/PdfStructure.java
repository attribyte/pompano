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

import java.util.Arrays;
import java.util.List;

final class PdfStructure {

   enum BlockType { PARAGRAPH, HEADING }

   static final class PdfBlock {

      PdfBlock(BlockType type, String text, int headingLevel, int startPage) {
         this.type = type;
         this.text = text;
         this.headingLevel = headingLevel;
         this.startPage = startPage;
      }

      final BlockType type;
      final String text;
      final int headingLevel;
      final int startPage;
   }

   PdfStructure(String detectedTitle, String detectedAuthor,
                ImmutableList<PdfBlock> blocks, ImmutableList<Integer> sectionBreaks) {
      this.detectedTitle = detectedTitle;
      this.detectedAuthor = detectedAuthor;
      this.blocks = blocks;
      this.sectionBreaks = sectionBreaks;
   }

   final String detectedTitle;
   final String detectedAuthor;
   final ImmutableList<PdfBlock> blocks;
   final ImmutableList<Integer> sectionBreaks;

   static PdfStructure analyze(List<PdfLine> lines, int totalPages) {
      if(lines.isEmpty()) {
         return new PdfStructure(null, null, ImmutableList.of(), ImmutableList.of());
      }

      // Step 1 — Compute body font size (mode of all lines).
      float bodyFontSize = computeBodyFontSize(lines);
      if(bodyFontSize <= 0) bodyFontSize = 12.0f;

      // Step 2 — Group lines into blocks using paragraphStart flags.
      List<LineGroup> groups = groupLines(lines);

      // Step 4 — Title detection (page 1, top area, largest font).
      String detectedTitle = null;
      String detectedAuthor = null;
      int titleGroupEnd = 0;

      float page1MaxY = 0;
      for(PdfLine line : lines) {
         if(line.pageNumber == 1 && line.yPosition > page1MaxY) {
            page1MaxY = line.yPosition;
         }
      }
      float topHalfThreshold = page1MaxY / 2.0f;

      // Find largest font size in top half of page 1.
      float largestTitleSize = 0;
      for(LineGroup group : groups) {
         PdfLine first = group.lines.get(0);
         if(first.pageNumber != 1 || first.yPosition > topHalfThreshold) break;
         if(first.fontSize > largestTitleSize) {
            largestTitleSize = first.fontSize;
         }
      }

      if(largestTitleSize > bodyFontSize * 1.3f) {
         // Collect consecutive groups at this size in the top area.
         StringBuilder titleBuilder = new StringBuilder();
         for(int i = 0; i < groups.size(); i++) {
            LineGroup group = groups.get(i);
            PdfLine first = group.lines.get(0);
            if(first.pageNumber != 1 || first.yPosition > topHalfThreshold) break;
            if(Math.abs(first.fontSize - largestTitleSize) < 0.5f) {
               if(titleBuilder.length() > 0) titleBuilder.append(' ');
               titleBuilder.append(group.getText());
               titleGroupEnd = i + 1;
            } else if(titleBuilder.length() > 0) {
               break;
            }
         }
         String candidate = titleBuilder.toString().trim();
         if(!candidate.isEmpty()) {
            detectedTitle = candidate;
         }
      }

      // Step 5 — Author detection (page 1, below title).
      if(detectedTitle != null && titleGroupEnd < groups.size()) {
         for(int i = titleGroupEnd; i < Math.min(titleGroupEnd + 3, groups.size()); i++) {
            LineGroup group = groups.get(i);
            PdfLine first = group.lines.get(0);
            if(first.pageNumber != 1) break;
            String text = group.getText().trim();
            if(text.isEmpty()) continue;
            if(looksLikeAuthor(text, bodyFontSize, first.fontSize)) {
               detectedAuthor = text;
               titleGroupEnd = i + 1;
               break;
            }
         }
      }

      // Step 3 — Classify blocks as headings or paragraphs.
      List<PdfBlock> blocks = Lists.newArrayListWithExpectedSize(groups.size());
      List<Integer> sectionBreaks = Lists.newArrayList();

      for(int i = 0; i < groups.size(); i++) {
         LineGroup group = groups.get(i);
         String text = group.getText();
         PdfLine first = group.lines.get(0);

         // Skip title/author lines — they're metadata, not body blocks.
         if(i < titleGroupEnd && first.pageNumber == 1) {
            continue;
         }

         if(isHeading(text, first.fontSize, first.isBold, bodyFontSize, group.lines.size())) {
            int level = headingLevel(first.fontSize, first.isBold, bodyFontSize);
            blocks.add(new PdfBlock(BlockType.HEADING, text, level, first.pageNumber));
            if(level == 1) {
               sectionBreaks.add(blocks.size() - 1);
            }
         } else {
            blocks.add(new PdfBlock(BlockType.PARAGRAPH, text, 0, first.pageNumber));
         }
      }

      return new PdfStructure(detectedTitle, detectedAuthor,
              ImmutableList.copyOf(blocks), ImmutableList.copyOf(sectionBreaks));
   }

   String toHtml() {
      return toHtml(0, blocks.size());
   }

   String toHtml(int fromBlock, int toBlock) {
      StringBuilder html = new StringBuilder();
      int end = Math.min(toBlock, blocks.size());
      for(int i = fromBlock; i < end; i++) {
         PdfBlock block = blocks.get(i);
         switch(block.type) {
            case HEADING:
               String tag = block.headingLevel == 1 ? "h2" : "h3";
               html.append('<').append(tag).append('>');
               html.append(escapeHtml(block.text));
               html.append("</").append(tag).append(">\n");
               break;
            case PARAGRAPH:
               html.append("<p>");
               html.append(escapeHtml(block.text));
               html.append("</p>\n");
               break;
         }
      }
      return html.toString();
   }

   private static float computeBodyFontSize(List<PdfLine> lines) {
      // Mode (most frequent font size).
      java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
      for(PdfLine line : lines) {
         int key = Math.round(line.fontSize * 10);
         counts.merge(key, 1, Integer::sum);
      }
      int maxCount = 0;
      int modeKey = 120; // default 12pt
      for(java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
         if(e.getValue() > maxCount || (e.getValue() == maxCount && e.getKey() < modeKey)) {
            maxCount = e.getValue();
            modeKey = e.getKey();
         }
      }
      return modeKey / 10.0f;
   }

   private static List<LineGroup> groupLines(List<PdfLine> lines) {
      List<LineGroup> groups = Lists.newArrayList();
      List<PdfLine> current = Lists.newArrayList();

      for(PdfLine line : lines) {
         if(!current.isEmpty()) {
            PdfLine prev = current.get(current.size() - 1);
            boolean newBlock = line.paragraphStart || line.pageNumber != prev.pageNumber;
            if(newBlock) {
               groups.add(new LineGroup(current));
               current = Lists.newArrayList();
            }
         }
         current.add(line);
      }
      if(!current.isEmpty()) {
         groups.add(new LineGroup(current));
      }
      return groups;
   }

   private static boolean isHeading(String text, float fontSize, boolean isBold,
                                    float bodyFontSize, int lineCount) {
      if(text.length() > 120 || lineCount > 2) return false;

      boolean sizeQualifies = fontSize >= bodyFontSize * 1.2f;
      boolean boldQualifies = isBold && fontSize >= bodyFontSize * 1.0f;

      return sizeQualifies || boldQualifies;
   }

   private static int headingLevel(float fontSize, boolean isBold, float bodyFontSize) {
      if(fontSize >= bodyFontSize * 1.5f) return 1;
      if(isBold && fontSize >= bodyFontSize * 1.3f) return 1;
      return 2;
   }

   private static boolean looksLikeAuthor(String text, float bodyFontSize, float fontSize) {
      if(text.length() > 60) return false;
      if(text.endsWith(".")) return false;

      String[] words = text.split("\\s+");
      if(words.length < 1 || words.length > 4) return false;

      boolean hasCapitalized = false;
      for(String word : words) {
         if(!word.isEmpty() && Character.isUpperCase(word.charAt(0))) {
            hasCapitalized = true;
         }
      }

      return hasCapitalized;
   }

   private static String escapeHtml(String text) {
      return text.replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;");
   }

   private static class LineGroup {

      LineGroup(List<PdfLine> lines) {
         this.lines = ImmutableList.copyOf(lines);
      }

      String getText() {
         StringBuilder sb = new StringBuilder();
         for(PdfLine line : lines) {
            if(sb.length() > 0) sb.append(' ');
            sb.append(line.text);
         }
         return sb.toString();
      }

      final ImmutableList<PdfLine> lines;
   }
}