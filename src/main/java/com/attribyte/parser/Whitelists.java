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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class Whitelists {

   /**
    * The default content whitelist with images allowed.
    * @return The content whitelist.
    */
   public static Whitelist contentWhitelistWithImages() {
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
   public static Whitelist contentWhitelist() {
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
    * An immutable set of the names of all HTML 5 block elements.
    */
   public static final ImmutableSet<String> blockElementNames =
           ImmutableSet.copyOf(
                   new String[] {
                           "address","article","aside",
                           "blockquote","canvas","dd","div","dl","dt","fieldset",
                           "figcaption","figure","footer","form",
                           "h1","h2","h3","h4","h5","h6",
                           "header","hr","li","main","nav","noscript",
                           "ol","output","p","pre","section","table","tfoot","ul","video"
                   }
           );

   /**
    * An immutable set of the names of all HTML 5 inline elements that are "safe"
    * (excludes {@code object} and {@code script}).
    */
   public static final ImmutableSet<String> safeInlineElementNames =
           ImmutableSet.copyOf(
                   new String[] {
                           "a","abbr","acronym","b","bdo","big","br",
                           "button","cite","code","dfn","em","i","img","input","kbd",
                           "label","map",
                           "q","samp","select",
                           "small","span","strong","sub","sup",
                           "textarea","time","tt","var",
                   }
           );
   /**
    * Creates a whitelist that allows a specific set of tags and attributes (without regard to context).
    * @param allowTags A collection of tags to allow.
    * @param allowAttributes A collection of attributes to allow.
    * @return The whitelist.
    */
   public static Whitelist whitelist(final Collection<String> allowTags, final Collection<String> allowAttributes) {

      return new Whitelist() {

         private final ImmutableSet<String> tagNames = ImmutableSet.copyOf(allowTags.stream()
                 .map(String::toLowerCase).distinct().collect(Collectors.toList())
         );

         private final ImmutableSet<String> attributeNames = ImmutableSet.copyOf(allowAttributes.stream()
                 .map(String::toLowerCase).distinct().collect(Collectors.toList())
         );

         @Override
         protected boolean	isSafeTag(String tag) {
            return tagNames.contains(tag);
         }

         @Override
         protected boolean	isSafeAttribute(String tagName, Element el, Attribute attr) {
            return attributeNames.contains(attr.getKey());
         }
      };
   }

   /**
    * A whitelist that includes only HTML5 block elements
    * with {@code class} and {@code id} attributes.
    * @return The whitelist.
    */
   public static Whitelist blockElements() {
      return whitelist(blockElementNames, ImmutableList.of("id", "class"));
   }

   /**
    * The basic whitelist.
    * <p>
    *    Includes tags: {@code a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small,
    *    strike, del, s, strong, sub, sup, u, ul, mark, bdi}
    * </p>
    * @return The whitelist.
    */
   public static Whitelist basicWhitelist() {

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
