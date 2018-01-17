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
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import org.attribyte.api.http.Response;

import java.io.IOException;

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
    * Examines the response body and headers to attempt to determine
    * the format of the content to be parsed.
    * @param response The HTTP response.
    * @return The format or {@code UNKNOWN} if detection failed.
    * @throws IOException on body or header read failure.
    */
   public static Format detect(final Response response) throws IOException {

      final ByteString responseBytes = response.getBody();
      if(responseBytes == null) {
         return Format.UNKNOWN;
      }

      final String body = CharMatcher.whitespace().trimFrom(responseBytes.toStringUtf8());
      final String contentType = CharMatcher.whitespace().trimFrom(Strings.nullToEmpty(response.getContentType()));
      if(body.endsWith("</rss>") || body.endsWith("</rdf:RDF>")) {
         return Format.RSS;
      } else if(body.endsWith("</feed>")) {
         return Format.ATOM;
      } else if(body.endsWith("</urlset>")) {
         return Format.SITEMAP;
      } else if(body.contains("<html amp>") || body.contains("<html ⚡>")) {
         return Format.AMP;
      } else if(body.endsWith("</html>") || contentType.startsWith("text/html")) {
         return Format.HTML;
      } else {
         return Format.UNKNOWN;
      }
   }
}