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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PdfStructureTest {

   @Test
   public void testParagraphGrouping() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("First paragraph line one.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("First paragraph line two.", 12f, 686f, 50f, false, 1, false),
              new PdfLine("Second paragraph line one.", 12f, 658f, 50f, false, 1, true),
              new PdfLine("Second paragraph line two.", 12f, 644f, 50f, false, 1, false)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals(2, structure.blocks.size());
      assertEquals(PdfStructure.BlockType.PARAGRAPH, structure.blocks.get(0).type);
      assertTrue(structure.blocks.get(0).text.contains("line one"));
      assertTrue(structure.blocks.get(0).text.contains("line two"));
      assertEquals(PdfStructure.BlockType.PARAGRAPH, structure.blocks.get(1).type);
   }

   @Test
   public void testHeadingByFontSize() {
      // Body at 12pt, heading at 18pt (1.5x = level 1).
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Body paragraph one.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Body paragraph one cont.", 12f, 686f, 50f, false, 1, false),
              new PdfLine("Chapter Title", 18f, 650f, 50f, false, 1, true),
              new PdfLine("Body paragraph two.", 12f, 620f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals(3, structure.blocks.size());
      assertEquals(PdfStructure.BlockType.PARAGRAPH, structure.blocks.get(0).type);
      assertEquals(PdfStructure.BlockType.HEADING, structure.blocks.get(1).type);
      assertEquals(1, structure.blocks.get(1).headingLevel);
      assertEquals("Chapter Title", structure.blocks.get(1).text);
      assertEquals(PdfStructure.BlockType.PARAGRAPH, structure.blocks.get(2).type);
   }

   @Test
   public void testHeadingLevel2() {
      // Body at 12pt, heading at 15pt (1.25x = level 2).
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Body text.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Subsection Title", 15f, 660f, 50f, false, 1, true),
              new PdfLine("More body text.", 12f, 630f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals(3, structure.blocks.size());
      assertEquals(PdfStructure.BlockType.HEADING, structure.blocks.get(1).type);
      assertEquals(2, structure.blocks.get(1).headingLevel);
   }

   @Test
   public void testBoldHeading() {
      // Bold text at body font size qualifies as heading.
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Regular body text paragraph.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Regular body text continued.", 12f, 686f, 50f, false, 1, false),
              new PdfLine("Bold Section", 12f, 650f, 50f, true, 1, true),
              new PdfLine("More regular body text.", 12f, 620f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals(3, structure.blocks.size());
      assertEquals(PdfStructure.BlockType.HEADING, structure.blocks.get(1).type);
   }

   @Test
   public void testLongTextNotHeading() {
      // Text > 120 chars should not be classified as heading even with large font.
      String longText = "This is a very long line of text that goes well beyond the hundred and twenty character limit that we use to distinguish headings from body text in paragraph form.";
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Body text.", 12f, 700f, 50f, false, 1, true),
              new PdfLine(longText, 18f, 660f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 620f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      // Long text treated as paragraph, not heading.
      for(PdfStructure.PdfBlock block : structure.blocks) {
         if(block.text.equals(longText)) {
            assertEquals(PdfStructure.BlockType.PARAGRAPH, block.type);
         }
      }
   }

   @Test
   public void testTitleDetection() {
      // Large title text on page 1 top area.
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("My Document Title", 24f, 100f, 50f, false, 1, true),
              new PdfLine("Body paragraph.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals("My Document Title", structure.detectedTitle);
   }

   @Test
   public void testTitleNotDetectedWhenSmall() {
      // Title font not significantly larger than body — no detection.
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Smallish Title", 13f, 100f, 50f, false, 1, true),
              new PdfLine("Body paragraph.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertNull(structure.detectedTitle);
   }

   @Test
   public void testMultiLineTitleDetection() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("A Long Document Title", 24f, 100f, 50f, false, 1, true),
              new PdfLine("That Spans Two Lines", 24f, 130f, 50f, false, 1, false),
              new PdfLine("Body paragraph.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals("A Long Document Title That Spans Two Lines", structure.detectedTitle);
   }

   @Test
   public void testAuthorDetection() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Document Title", 24f, 100f, 50f, false, 1, true),
              new PdfLine("John Smith", 14f, 150f, 50f, false, 1, true),
              new PdfLine("Body paragraph text here.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body text.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals("Document Title", structure.detectedTitle);
      assertEquals("John Smith", structure.detectedAuthor);
   }

   @Test
   public void testAuthorNotDetectedWhenSentence() {
      // Text ending with period should not be treated as author.
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Document Title", 24f, 100f, 50f, false, 1, true),
              new PdfLine("This is a subtitle sentence.", 14f, 150f, 50f, false, 1, true),
              new PdfLine("Body text.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertNull(structure.detectedAuthor);
   }

   @Test
   public void testSectionBreaks() {
      // Two level-1 headings should create two section breaks.
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Intro paragraph.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Chapter One", 18f, 600f, 50f, false, 1, true),
              new PdfLine("Chapter one content.", 12f, 570f, 50f, false, 1, true),
              new PdfLine("Chapter Two", 18f, 400f, 50f, false, 2, true),
              new PdfLine("Chapter two content.", 12f, 370f, 50f, false, 2, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 2);
      assertEquals(2, structure.sectionBreaks.size());
   }

   @Test
   public void testToHtml() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Body text.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Section Title", 18f, 600f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 570f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      String html = structure.toHtml();
      assertTrue(html.contains("<p>Body text.</p>"));
      assertTrue(html.contains("<h2>Section Title</h2>"));
      assertTrue(html.contains("<p>More body.</p>"));
   }

   @Test
   public void testToHtmlRange() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("First.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Second.", 12f, 660f, 50f, false, 1, true),
              new PdfLine("Third.", 12f, 620f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      String html = structure.toHtml(1, 2);
      assertFalse(html.contains("First."));
      assertTrue(html.contains("Second."));
      assertFalse(html.contains("Third."));
   }

   @Test
   public void testEmptyInput() {
      PdfStructure structure = PdfStructure.analyze(List.of(), 0);
      assertNull(structure.detectedTitle);
      assertNull(structure.detectedAuthor);
      assertTrue(structure.blocks.isEmpty());
      assertTrue(structure.sectionBreaks.isEmpty());
   }

   @Test
   public void testPageBoundaryStartsNewBlock() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Page 1 text.", 12f, 700f, 50f, false, 1, true),
              new PdfLine("Page 2 text.", 12f, 700f, 50f, false, 2, false)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 2);
      assertEquals(2, structure.blocks.size());
   }

   @Test
   public void testTitleExcludedFromBlocks() {
      List<PdfLine> lines = Arrays.asList(
              new PdfLine("Big Title", 24f, 100f, 50f, false, 1, true),
              new PdfLine("Body text here.", 12f, 300f, 50f, false, 1, true),
              new PdfLine("More body.", 12f, 400f, 50f, false, 1, true)
      );

      PdfStructure structure = PdfStructure.analyze(lines, 1);
      assertEquals("Big Title", structure.detectedTitle);
      // Title should not appear in blocks.
      for(PdfStructure.PdfBlock block : structure.blocks) {
         assertNotEquals("Big Title", block.text);
      }
   }
}
