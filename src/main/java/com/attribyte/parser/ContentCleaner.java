/*
 * Copyright 2018 Attribyte, LLC
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

package com.attribyte.parser;

import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import java.util.Properties;

import static com.attribyte.parser.Util.childrenToString;

public interface ContentCleaner {

   /**
    * A cleaner that does no transformation or cleaning.
    */
   public static ContentCleaner NOOP = new ContentCleaner() {
      @Override
      public Document transform(Document doc) {
         return doc;
      }

      @Override
      public String toCleanContent(Document doc) {
         return doc != null ? childrenToString(doc.body()) : "";
      }

      @Override
      public void init(final Properties props) {
      }
   };

   /**
    * Transforms the content of a document.
    * <p>
    *    The input document is modified, not copied!
    * </p>
    * @param doc The document
    * @return The input document.
    */
   public Document transform(Document doc);

   /**
    * Converts the document to a cleaned string.
    * @param doc The document.
    * @return The clean content.
    */
   public String toCleanContent(Document doc);

   /**
    * Initialize the cleaner.
    * @param props The properties.
    */
   public void init(final Properties props);

   /**
    * The default whitelist for full content.
    */
   static final Whitelist DEFAULT_CONTENT_WHITELIST = contentWhitelist();

   /**
    * The default whitelist for full content with images.
    */
   static final Whitelist DEFAULT_CONTENT_WHITELIST_WITH_IMAGES = contentWhitelistWithImages();

   /**
    * The default content with images allowed.
    * @return The content whitelist.
    */
   static Whitelist contentWhitelistWithImages() {
      return contentWhitelist()
              .addTags("img")
              .addAttributes("img", "src", "title", "alt", "width", "height")
              .addProtocols("img", "src", "http", "https");
   }

   /**
    * The default content whitelist.
    * <p>
    *    Includes tags from the basic whitelist as well as
    *    {@code h1, h2, h3, h4, h5, h6, table, tr, td, th, tbody, tfoot, thead, col, colgroup, figure, figcaption
    *    header, footer, aside, details, section, summary, time, article, main}
    * </p>
    * @return The whitelist.
    */
   static Whitelist contentWhitelist() {
      return basicWhitelist()
              .addTags(
                      "h1", "h2", "h3", "h4", "h5", "h6",
                      "table", "tr", "td", "th", "tbody", "tfoot", "thead", "col", "colgroup", "figure", "figcaption",
                      "header", "footer",
                      "aside", "details", "section",
                      "summary", "time", "article", "main"
              )
              .addAttributes("time", "datetime");
   }

   /**
    * The basic whitelist.
    */
   static final Whitelist BASIC_WHITELIST = basicWhitelist();


   /**
    * Gets the basic whitelist.
    * <p>
    *    Includes tags: {@code a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small,
    *    strike, del, s, strong, sub, sup, u, ul, mark, bdi}
    * </p>
    * @return The whitelist.
    */
   static Whitelist basicWhitelist() {

      return new Whitelist()
              .addTags(
                      "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
                      "i", "li", "ol", "p", "pre", "q", "small", "strike", "del", "s", "strong", "sub",
                      "sup", "u", "ul", "mark", "bdi")
              .addAttributes("a", "href")
              .addAttributes("blockquote", "cite")
              .addAttributes("q", "cite", "class", "alt", "title")
              .addProtocols("a", "href", "http", "https", "mailto")
              .addProtocols("blockquote", "cite", "http", "https")
              .addProtocols("cite", "cite", "http", "https");
   }
}