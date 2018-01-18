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

import com.google.common.base.Strings;

import static com.attribyte.parser.Util.endsWithIgnoreInvisible;

/**
 * Attempt to detect the content format to select a parser.
 * @author Matt Hamer
 */
public interface Detector {

   public enum Format {
      /**
       * The RSS feed format (any version).
       */
      RSS,

      /**
       * The Atom feed format.
       */
      ATOM,

      /**
       * HTML pages.
       */
      HTML,

      /**
       * Google Amp pages.
       */
      AMP,

      /**
       * A sitemap.
       */
      SITEMAP,

      /**
       * Format is unknown.
       */
      UNKNOWN
   }

   /**
    * Examines the response body and content type to attempt to determine
    * the format of the content to be parsed.
    * @param body The response body.
    * @param contentType The content type header value, if known.
    * @return The format or {@code UNKNOWN} if detection failed.
    */
   public static Format detect(final String body, final String contentType) {

      if(endsWithIgnoreInvisible("</rss>", body) || endsWithIgnoreInvisible("</rdf:RDF>", body)) {
         return Format.RSS;
      } else if(endsWithIgnoreInvisible("</feed>", body)) {
         return Format.ATOM;
      } else if(endsWithIgnoreInvisible("</urlset>", body)) {
         return Format.SITEMAP;
      } else if(body.contains("<html amp>") || body.contains("<html ⚡>")) {
         return Format.AMP;
      } else if(endsWithIgnoreInvisible("</html>", body) || Strings.nullToEmpty(contentType).startsWith("text/html")) {
         return Format.HTML;
      } else {
         return Format.UNKNOWN;
      }
   }
}