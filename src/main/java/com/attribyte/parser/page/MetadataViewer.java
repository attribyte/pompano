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

package com.attribyte.parser.page;

import com.attribyte.parser.model.Page;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Download/view metadata for a page.
 * @author Matt Hamer
 */
public class MetadataViewer {

   public static void main(String[] args) throws Exception {

      if(args.length < 1) {
         throw new Exception("A 'url' must be specified");
      }

      Connection conn = Jsoup.connect(args[0]);
      conn.followRedirects(true);
      conn.ignoreHttpErrors(true);
      Document doc = conn.get();


      System.out.println("Raw");
      System.out.println("***");
      System.out.println();
      for(final Element meta : doc.getElementsByTag("meta")) {
         System.out.println(meta.toString());
      }
      System.out.println();

      System.out.println("Metadata");
      System.out.println("********");
      System.out.println();

      String title = doc.title();
      System.out.println("title=" + title);

      for(final Element meta : doc.getElementsByTag("meta")) {
         String name = meta.attr("name");
         if(name.isEmpty()) {
            name = meta.attr("property");
            if(!name.isEmpty()) {
               name = "property:" + name;
            } else {
               name = meta.attr("itemprop");
               if(!name.isEmpty()) {
                  name = "itemprop:" + name;
               }
            }
         } else {
            name = "name:" + name;
         }

         if(!name.isEmpty()) {
            System.out.println(String.format("%s, content=%s", name, meta.attr("content")));
         }
      }

      System.out.println();
      System.out.println("Links");
      System.out.println("*****");
      System.out.println();
      doc.getElementsByTag("link").forEach(elem -> {
         System.out.println(String.format("%s (rel=%s, type=%s)",
                 elem.attr("href"),
                 elem.attr("rel"),
                 elem.attr("type")
         ));
      });

      System.out.println();
      System.out.println("Parser");
      System.out.println("******");
      System.out.println();

      Page parsedPage = HTMLPageParser.parse(doc, args[0]);
      System.out.println("Best Site Name: " + parsedPage.siteName);
      System.out.println("Best Title: " + parsedPage.title);
      if(!parsedPage.author.isEmpty()) {
         System.out.println("Best Author: " + parsedPage.author);
      } else {
         System.out.println("Best Author: [unknown]");
      }
      System.out.println("Best Summary: " + parsedPage.summary);
      System.out.println("Best Canonical Link: " + parsedPage.canonicalLink);
      if(parsedPage.publishTime != null) {
         System.out.println("Publish Time: " + parsedPage.publishTime);
      } else {
         System.out.println("Publish Time: [unknown]");
      }

      parsedPage.metaImages.forEach(image -> {
         System.out.println(String.format("Meta Image: %s (height=%d, width=%d", image.link, image.height, image.width));
      });

      parsedPage.feedLinks().forEach(link -> {
         System.out.println(String.format("Feed Link: %s (rel=%s, type=%s, title=%s)", link.href, link.rel, link.type, link.title));
      });

      parsedPage.iconLinks().forEach(link -> {
         System.out.println(String.format("Icon Link: %s (rel=%s, type=%s, title=%s)", link.href, link.rel, link.type, link.title));
      });

      parsedPage.links("amphtml", null).forEach(link -> {
         System.out.println("Atom Link: " + link.href);
      });


   }
}
