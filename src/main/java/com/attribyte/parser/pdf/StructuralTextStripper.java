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

import com.google.common.collect.Lists;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StructuralTextStripper extends PDFTextStripper {

   StructuralTextStripper() throws IOException {
      super();
   }

   List<PdfLine> getLines() {
      finalizeLine();
      return lines;
   }

   @Override
   public void processPage(PDPage page) throws IOException {
      finalizeLine();
      pageNumber++;
      nextLineIsParagraphStart = true;
      super.processPage(page);
   }

   @Override
   protected void writeParagraphStart() throws IOException {
      nextLineIsParagraphStart = true;
   }

   @Override
   protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
      for(TextPosition tp : textPositions) {
         String ch = tp.getUnicode();
         if(ch == null || ch.isEmpty()) continue;

         float size = tp.getFontSizeInPt();
         if(size <= 0) {
            size = tp.getFontSize() * tp.getYScale();
         }

         String fontName = tp.getFont() != null ? tp.getFont().getName() : "";

         lineText.append(ch);

         int sizeKey = Math.round(size * 10);
         fontSizeCounts.merge(sizeKey, 1, Integer::sum);

         boolean bold = fontName.toLowerCase().contains("bold");
         boldCounts.merge(bold, 1, Integer::sum);

         if(firstX < 0) {
            firstX = tp.getXDirAdj();
            firstY = tp.getYDirAdj();
         }
      }
   }

   @Override
   protected void writeLineSeparator() throws IOException {
      finalizeLine();
   }

   private void finalizeLine() {
      String text = lineText.toString().trim();
      if(!text.isEmpty()) {
         float dominantSize = 0f;
         int maxCount = 0;
         for(Map.Entry<Integer, Integer> e : fontSizeCounts.entrySet()) {
            if(e.getValue() > maxCount) {
               maxCount = e.getValue();
               dominantSize = e.getKey() / 10.0f;
            }
         }

         int boldCount = boldCounts.getOrDefault(true, 0);
         int nonBoldCount = boldCounts.getOrDefault(false, 0);
         boolean isBold = boldCount > nonBoldCount;

         lines.add(new PdfLine(text, dominantSize, firstY, firstX,
                 isBold, pageNumber, nextLineIsParagraphStart));
         nextLineIsParagraphStart = false;
      }

      lineText.setLength(0);
      fontSizeCounts.clear();
      boldCounts.clear();
      firstX = -1;
      firstY = -1;
   }

   private final List<PdfLine> lines = Lists.newArrayList();
   private final StringBuilder lineText = new StringBuilder();
   private final Map<Integer, Integer> fontSizeCounts = new HashMap<>();
   private final Map<Boolean, Integer> boldCounts = new HashMap<>();
   private float firstX = -1;
   private float firstY = -1;
   private int pageNumber = 0;
   private boolean nextLineIsParagraphStart = true;
}