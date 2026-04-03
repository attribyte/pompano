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

/**
 * Configuration for PDF parsing heuristics.
 * <p>
 * Defaults are tuned for typical documents (reports, papers, Word docs).
 * Magazine-style PDFs with dense InDesign layouts may need higher thresholds.
 * </p>
 */
public final class PdfParserConfig {

   /** Default configuration suitable for most documents. */
   public static final PdfParserConfig DEFAULT = new Builder().build();

   /** Minimum font size ratio (heading / body) to qualify as any heading. */
   public final float headingMinRatio;

   /** Minimum font size ratio to qualify as a level-1 (section break) heading. */
   public final float sectionBreakMinRatio;

   /** Minimum font size ratio for bold text to qualify as a level-1 heading. */
   public final float boldSectionBreakMinRatio;

   /** Minimum number of section breaks required to split by headings. */
   public final int minSectionBreaks;

   /** Minimum paragraph block text length to include in HTML output. */
   public final int minBlockTextLength;

   /** Minimum story content length (chars) for tagged PDF extraction. */
   public final int minStoryContentLength;

   /** Maximum heading text length. Longer text is treated as a paragraph. */
   public final int maxHeadingTextLength;

   /** Maximum number of lines for a heading block. */
   public final int maxHeadingLineCount;

   private PdfParserConfig(Builder builder) {
      this.headingMinRatio = builder.headingMinRatio;
      this.sectionBreakMinRatio = builder.sectionBreakMinRatio;
      this.boldSectionBreakMinRatio = builder.boldSectionBreakMinRatio;
      this.minSectionBreaks = builder.minSectionBreaks;
      this.minBlockTextLength = builder.minBlockTextLength;
      this.minStoryContentLength = builder.minStoryContentLength;
      this.maxHeadingTextLength = builder.maxHeadingTextLength;
      this.maxHeadingLineCount = builder.maxHeadingLineCount;
   }

   public static final class Builder {

      private float headingMinRatio = 1.2f;
      private float sectionBreakMinRatio = 1.8f;
      private float boldSectionBreakMinRatio = 1.5f;
      private int minSectionBreaks = 2;
      private int minBlockTextLength = 10;
      private int minStoryContentLength = 500;
      private int maxHeadingTextLength = 120;
      private int maxHeadingLineCount = 2;

      public Builder setHeadingMinRatio(float ratio) { this.headingMinRatio = ratio; return this; }
      public Builder setSectionBreakMinRatio(float ratio) { this.sectionBreakMinRatio = ratio; return this; }
      public Builder setBoldSectionBreakMinRatio(float ratio) { this.boldSectionBreakMinRatio = ratio; return this; }
      public Builder setMinSectionBreaks(int n) { this.minSectionBreaks = n; return this; }
      public Builder setMinBlockTextLength(int n) { this.minBlockTextLength = n; return this; }
      public Builder setMinStoryContentLength(int n) { this.minStoryContentLength = n; return this; }
      public Builder setMaxHeadingTextLength(int n) { this.maxHeadingTextLength = n; return this; }
      public Builder setMaxHeadingLineCount(int n) { this.maxHeadingLineCount = n; return this; }

      public PdfParserConfig build() {
         return new PdfParserConfig(this);
      }
   }
}
