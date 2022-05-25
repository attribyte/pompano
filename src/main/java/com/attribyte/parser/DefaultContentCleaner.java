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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.util.Properties;

import static com.attribyte.parser.Util.childrenToString;
import static com.attribyte.parser.Util.firstMatch;
import static com.attribyte.parser.Safelists.contentSafelist;

public class DefaultContentCleaner implements ContentCleaner {

   @Override
   public Document transform(Document doc) {
      String defaultProtocol = getDocumentProtocol(doc.baseUri());
      massageLinks(doc, defaultProtocol);
      cleanAndMarkImages(doc, defaultProtocol);
      cleanAndMarkEmbeds(doc, defaultProtocol);
      cleanAndMarkTwitterBlockquotes(doc);
      doc = new Cleaner(DEFAULT_CONTENT_WHITELIST).clean(doc);
      return doc;
   }

   @Override
   public String toCleanContent(Document doc) {
      Element mainElement = firstMatch(doc.body(), "main");
      if(mainElement == null) {
         mainElement = firstMatch(doc.body(), "article");
      }
      if(mainElement == null) {
         mainElement = doc.body();
      }
      return childrenToString(mainElement).trim();
   }

   @Override
   public void init(final Properties props) {
      this.withImages = props.getProperty("withImages", "false").equalsIgnoreCase("true");
   }

   /**
    * Gets the protocol to be prepended to image source that start with {@code //}.
    * @param link The link.
    * @return The protocol.
    */
   protected String getDocumentProtocol(final String link) {
      String href = Strings.nullToEmpty(link).trim();
      if(href.startsWith("https:")) {
         return "https:";
      } else {
         return "http:";
      }
   }

   /**
    * Marks {@code iframes}
    * by placing a {@code q} tag before every image
    * with the {@code cite} attribute set to the frame source.
    * The class {@code iframe} is also added.
    * @param doc The document.
    * @param defaultProtocol The default protocol.
    * @return The number of modifications.
    */
   protected int cleanAndMarkEmbeds(Document doc, final String defaultProtocol) {
      try {
         int modified = 0;
         Elements iframes = doc.body().getElementsByTag("iframe");
         for(Element iframe : iframes) {
            String src = Strings.nullToEmpty(iframe.attr("src")).trim();
            if(src.isEmpty()) {
               modified++;
               iframe.remove();
            } else {
               if(src.startsWith("//")) {
                  src = defaultProtocol + src;
               }
               Element q = doc.createElement("q");
               q.attr("cite", src);
               q.attr("class", "iframe");
               iframe.before(q);
               modified++;
            }
         }
         return modified;
      } catch(Throwable e) {
         return 0;
      }
   }

   protected int cleanAndMarkTwitterBlockquotes(final Document doc) {
      int modified = 0;
      Elements twitterQuotes = doc.body().select("blockquote.twitter-tweet");
      for(Element twitterQuote : twitterQuotes) {
         String twitterLink = null;
         Elements anchors = twitterQuote.getElementsByTag("a");
         for(Element anchor : anchors) {
            String href = anchor.attr("href").trim();
            if(!href.isEmpty() && href.startsWith("https://twitter.com/") && href.contains("/status/")) {
               twitterLink = href;
               break;
            }
         }
         if(twitterLink != null) {
            twitterQuote.attr("cite", twitterLink);
            modified++;
         }
      }
      return modified;
   }

   /**
    * Cleans links, adding a protocol, if missing.
    * <p>
    *    Converts "mailto" links to {@code q.mailto} with value as {@code cite}.
    * </p>
    * @param doc The document.
    * @param defaultProtocol The default protocol for protocol-less links.
    * @return The number of modifications.
    */
   protected int massageLinks(final Document doc, final String defaultProtocol) {
      try {
         Elements anchors = doc.body().getElementsByTag("a");
         int modified = 0;
         for(Element anchor : anchors) {
            String href = Strings.nullToEmpty(anchor.attr("href")).trim();
            if(href.startsWith("//")) {
               anchor.attr("href", defaultProtocol + href);
               modified++;
            } else if(href.startsWith("mailto:")) {
               anchor.tagName("q");
               anchor.removeAttr("href");
               anchor.attr("cite", href);
               anchor.attr("class", "mailto");
               modified++;
            }
         }
         return modified;
      } catch(Throwable e) {
         return 0;
      }
   }

   /**
    * Removes any images without a source and marks
    * images (if {@code withImages=false}) by placing a {@code q} tag before every image
    * with the {@code cite} attribute set to the image source and
    * {@code class} set to {@code image}.
    * @param doc The document.
    * @param defaultProtocol The default protocol.
    * @return The number of changes.
    */
   protected int cleanAndMarkImages(final Document doc, final String defaultProtocol) {

      Elements images = doc.body().getElementsByTag("img");
      int count = 0;
      for(Element image : images) {
         String src = Strings.nullToEmpty(image.attr("src")).trim();
         if(src.isEmpty()) {
            count++;
            image.remove();
         } else if(!withImages) {
            if(src.startsWith("//")) {
               src = defaultProtocol + src;
            }
            String alt = image.attr("alt");
            String title = image.attr("title");
            Element q = doc.createElement("q");
            q.attr("cite", src);
            q.attr("class", "image");
            if(!alt.isEmpty()) {
               q.attr("alt", alt);
            }
            if(!title.isEmpty()) {
               q.attr("title", title);
            }
            image.before(q);
         }
      }
      return count;
   }

   private boolean withImages = false;
   private static final Safelist DEFAULT_CONTENT_WHITELIST = contentSafelist();
}