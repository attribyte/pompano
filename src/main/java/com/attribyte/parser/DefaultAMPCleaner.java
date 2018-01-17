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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Properties;

public class DefaultAMPCleaner implements ContentCleaner {

   public Document transform(final Document doc) {
      Elements images = doc.getElementsByTag("amp-img");
      images.forEach(image -> image.tagName("img"));

      Elements videos = doc.getElementsByTag("amp-video");
      videos.forEach(video -> {
         String poster = video.attr("poster");
         if(!poster.isEmpty()) {
            Element newImage = doc.createElement("img");
            newImage.attr("src", poster);
            String width = video.attr("width");
            String height = video.attr("height");
            if(!width.isEmpty() && !height.isEmpty()) {
               newImage.attr("width", width);
               newImage.attr("height", height);
            }
            video.after(newImage);
         }
      });
      return doc;
   }

   /**
    * Converts the document to stored content.
    * @param doc The document.
    * @return The stored content.
    */
   public String toCleanContent(Document doc) {
      return defaultContentCleaner.toCleanContent(doc);
   }

   @Override
   public void init(final Properties props) {
   }

   private final DefaultContentCleaner defaultContentCleaner = new DefaultContentCleaner();
}