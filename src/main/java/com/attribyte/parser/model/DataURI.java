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

package com.attribyte.parser.model;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;

/**
 * A data {@code data:image/png;base64,iVBOR} URI.
 * @author Matt Hamer
 */
public class DataURI {

   /**
    * Creates a data URI from the {@code src} attribute.
    * @param src The {@code src} attribute.
    * @throws IllegalArgumentException if {@code src} is not in the data URI format or base64 is specified with invalid base64 as data.
    */
   public DataURI(final String src) throws IllegalArgumentException {

      if(!src.startsWith("data:")) {
         throw new IllegalArgumentException("Not a data URI");
      }

      int dataStart = src.indexOf(',');
      if(dataStart == -1) {
         throw new IllegalArgumentException("The data URI scheme requires the presence of a ','");
      }

      String mediaType = src.substring(5, dataStart);
      String data = dataStart < src.length() - 1 ? src.substring(dataStart + 1) : "";

      int base64Start = mediaType.indexOf(";base64");
      if(base64Start > 0) {
         this.mediaType = mediaType.substring(0, base64Start);
         this.base64Encoded = true;
         this.data = ByteString.copyFrom(BaseEncoding.base64().decode(data));
      } else {
         this.mediaType = mediaType;
         this.base64Encoded = false;
         this.data = ByteString.copyFromUtf8(data);
      }
   }

   /**
    * The media type - may be an empty string.
    */
   public final String mediaType;

   /**
    * The data as bytes.
    */
   public final ByteString data;

   /**
    * Was the data base64 encoded?
    */
   public final boolean base64Encoded;
}
