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

package com.attribyte.parser.markdown;

import com.attribyte.parser.ContentCleaner;
import com.attribyte.parser.ParseResult;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import org.junit.Test;

import static org.junit.Assert.*;

public class MarkdownParserTest {

   private final MarkdownParser parser = new MarkdownParser();

   @Test
   public void testWithFrontMatter() {
      String md = "---\n" +
              "title: Test Article\n" +
              "author: Jane Doe\n" +
              "date: 2025-06-15T10:30:00Z\n" +
              "tags:\n" +
              "  - science\n" +
              "  - nature\n" +
              "summary: A test summary.\n" +
              "---\n" +
              "\n" +
              "# Hello World\n" +
              "\n" +
              "This is **bold** and *italic* text.\n";

      ParseResult result = parser.parse(md, null, ContentCleaner.NOOP);
      assertFalse(result.hasErrors());
      assertTrue(result.resource.isPresent());

      Resource resource = result.resource.get();
      assertFalse(resource.entries.isEmpty());
      Entry entry = resource.entries.get(0);

      assertEquals("Test Article", entry.title);
      assertEquals("A test summary.", entry.summary);
      assertEquals(1, entry.authors.size());
      assertEquals("Jane Doe", entry.authors.get(0).name);
      assertEquals(2, entry.tags.size());
      assertTrue(entry.tags.contains("science"));
      assertTrue(entry.tags.contains("nature"));
      assertTrue(entry.publishedTimestamp > 0);
      assertTrue(entry.cleanContent.contains("<strong>bold</strong>"));
      assertTrue(entry.cleanContent.contains("<em>italic</em>"));
   }

   @Test
   public void testWithoutFrontMatter() {
      String md = "# Simple Document\n\nJust some text.\n";

      ParseResult result = parser.parse(md, "test.md", ContentCleaner.NOOP);
      assertFalse(result.hasErrors());
      assertTrue(result.resource.isPresent());

      Entry entry = result.resource.get().entries.get(0);
      // No front matter title, so title is empty
      assertTrue(entry.title.isEmpty());
      assertTrue(entry.cleanContent.contains("Simple Document"));
      assertTrue(entry.cleanContent.contains("Just some text."));
      assertEquals("test.md", entry.canonicalLink);
   }

   @Test
   public void testEmptyContent() {
      ParseResult result = parser.parse("", null, ContentCleaner.NOOP);
      assertFalse(result.hasErrors());
      assertTrue(result.resource.isPresent());
   }
}