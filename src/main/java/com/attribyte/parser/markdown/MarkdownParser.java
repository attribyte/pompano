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
import com.attribyte.parser.Parser;
import com.attribyte.parser.model.Author;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.List;
import java.util.Map;

import static com.attribyte.parser.DateParser.tryParseISO8601;

/**
 * Parses Markdown content with optional YAML front matter.
 * <p>
 * Supported front matter fields: {@code title}, {@code author}, {@code date},
 * {@code tags}, {@code topics}, {@code summary}.
 * </p>
 */
public class MarkdownParser implements Parser {

   private final com.vladsch.flexmark.parser.Parser parser;
   private final HtmlRenderer renderer;

   public MarkdownParser() {
      MutableDataSet options = new MutableDataSet();
      options.set(com.vladsch.flexmark.parser.Parser.EXTENSIONS,
              java.util.Collections.singletonList(YamlFrontMatterExtension.create()));
      this.parser = com.vladsch.flexmark.parser.Parser.builder(options).build();
      this.renderer = HtmlRenderer.builder(options).build();
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {
      Node document = parser.parse(content);

      AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
      visitor.visit(document);
      Map<String, List<String>> frontMatter = visitor.getData();

      String html = renderer.render(document);

      Entry.Builder entryBuilder = new Entry.Builder();

      // Title
      String title = firstValue(frontMatter, "title");
      if(title != null) {
         entryBuilder.setTitle(title);
      }

      // Author
      String author = firstValue(frontMatter, "author");
      if(author != null) {
         entryBuilder.addAuthor(Author.builder(author).build());
      }

      // Date
      String date = firstValue(frontMatter, "date");
      if(date != null) {
         Long millis = tryParseISO8601(date);
         if(millis != null) {
            entryBuilder.setPublishedTimestamp(millis);
         }
      }

      // Summary
      String summary = firstValue(frontMatter, "summary");
      if(summary != null) {
         entryBuilder.setSummary(summary);
      }

      // Tags
      List<String> tags = frontMatter.get("tags");
      if(tags != null) {
         for(String tag : tags) {
            String trimmed = tag.trim();
            if(!trimmed.isEmpty()) {
               entryBuilder.addTag(trimmed);
            }
         }
      }

      // Clean content
      entryBuilder.setCleanContent(html);
      if(sourceLink != null) {
         entryBuilder.setCanonicalLink(sourceLink);
      }

      Resource resource = new Resource.Builder()
              .addEntry(entryBuilder.build())
              .build();

      return new ParseResult(name(), resource);
   }

   @Override
   public String name() {
      return "markdown";
   }

   private static String firstValue(Map<String, List<String>> frontMatter, String key) {
      List<String> values = frontMatter.get(key);
      if(values == null || values.isEmpty()) return null;
      String val = values.get(0).trim();
      return val.isEmpty() ? null : val;
   }
}