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
}