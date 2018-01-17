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

package com.attribyte.parser.sitemap;

import com.attribyte.parser.Util;
import com.attribyte.parser.model.SitemapLink;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static com.attribyte.parser.Util.map;

/**
 * Parse a site map.
 * @author Matt Hamer
 */
public class SitemapParser {

   /**
    * Parse a sitemap.
    * @param content The content as a string.
    * @param sitemapLink The link to the content, if any.
    * @return A list of links.
    * @throws ParseException on invalid date/time.
    */
   public static List<SitemapLink> parse(final String content, final String sitemapLink) throws ParseException {
      if(content.contains("<urlset")) {
         Document doc = Jsoup.parse(content, sitemapLink, org.jsoup.parser.Parser.xmlParser());
         Elements urls = doc.getElementsByTag("url");
         if(urls.size() == 0) {
            return ImmutableList.of();
         }

         List<SitemapLink> links = Lists.newArrayListWithExpectedSize(urls.size());
         for(Element url : urls) {
            Map<String, Element> children = map(url, tagNames);
            String link = children.getOrDefault("loc", Util.EMPTY_ELEMENT).text().trim();
            if(!link.isEmpty()) {
               String lastmod = children.getOrDefault("lastmod", Util.EMPTY_ELEMENT).text();
               String changefreq = children.getOrDefault("changefreq", Util.EMPTY_ELEMENT).text();
               links.add(new SitemapLink(link, lastmod, changefreq));
            }
         }
         return links;
      } else {
         return parseSimple(content);
      }
   }

   /**
    * Parse a "simple" sitemap file (one URL per line).
    * @param response The response body as a string.
    * @return The list of links.
    */
   public static List<SitemapLink> parseSimple(final String response) {
      List<String> lines = Splitter.on(CharMatcher.invisible())
              .trimResults()
              .omitEmptyStrings()
              .limit(500)
              .splitToList(response);
      List<SitemapLink> links = Lists.newArrayListWithExpectedSize(lines.size());
      for(String line : lines) {
         if(!line.startsWith("#")) {
            links.add(new SitemapLink(line, 0, SitemapLink.ChangeFrequency.NEVER));
         }
      }
      return links;
   }

   /**
    * Expected tag names for a sitemap entry.
    */
   private static ImmutableSet<String> tagNames = ImmutableSet.of("loc", "lastmod", "changefreq");
}