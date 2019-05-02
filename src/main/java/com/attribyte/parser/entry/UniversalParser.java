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

package com.attribyte.parser.entry;

import com.attribyte.parser.ContentCleaner;
import com.attribyte.parser.Detector;
import com.attribyte.parser.ParseError;
import com.attribyte.parser.ParseResult;
import com.attribyte.parser.Parser;

public class UniversalParser implements Parser {

   /**
    * Creates a universal parser with unknown content type.
    */
   public UniversalParser() {
      this.contentType = "";
   }

   /**
    * Creates a universal parser with a known content type.
    * @param contentType The content type.
    */
   public UniversalParser(final String contentType) {
      this.contentType = contentType;
   }

   @Override
   public ParseResult parse(final String content, final String sourceLink, final ContentCleaner contentCleaner) {
      switch(Detector.detect(content, contentType)) {
         case RSS:
            return new RSSParser().parse(content, sourceLink, contentCleaner);
         case ATOM:
            return new AtomParser().parse(content, sourceLink, contentCleaner);
         case AMP:
            return new AmpParser().parse(content, sourceLink, contentCleaner);
         case HTML:
            return new HTMLMetadataParser().parse(content, sourceLink, contentCleaner);
         default:
            return new ParseResult(name(), new ParseError("Unable to auto-detect parser"));
      }

   }

   @Override
   public String name() {
      return "universal";
   }

   /**
    * The content type, if known.
    */
   private final String contentType;

}
