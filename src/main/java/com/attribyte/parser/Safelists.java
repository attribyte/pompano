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
import org.jsoup.safety.Safelist;

import java.util.Collection;

public class Safelists {

   /**
    * The default content safelist with images allowed.
    * @return The content safelist.
    */
   public static Safelist contentSafelistWithImages() {
      return contentSafelist()
              .addTags("img")
              .addAttributes("img", "src", "title", "alt", "width", "height")
              .addProtocols("img", "src", "http", "https");
   }

   /**
    * The default content safelist.
    * <p>
    *    Includes tags from the basic safelist as well as
    *    {@code h1, h2, h3, h4, h5, h6, table, tr, td, th, tbody, tfoot, thead, col, colgroup, figure, figcaption
    *    header, footer, aside, details, section, summary, time, article, main}
    * </p>
    * @return The safelist.
    */
   public static Safelist contentSafelist() {
      return basicSafelist()
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
    * All HTML 5 inline element names.
    */
   public static final ImmutableSet<String> inlineElementNames =
           ImmutableSet.copyOf(new String[] {
                   "a",
                   "abbr",
                   "acronym",
                   "audio",
                   "b",
                   "bdi",
                   "bdo",
                   "big",
                   "br",
                   "button",
                   "canvas",
                   "cite",
                   "code",
                   "data",
                   "datalist",
                   "del",
                   "dfn",
                   "em",
                   "embed",
                   "i",
                   "iframe",
                   "img",
                   "input",
                   "ins",
                   "kbd",
                   "label",
                   "map",
                   "mark",
                   "meter",
                   "noscript",
                   "object",
                   "output",
                   "picture",
                   "progress",
                   "q",
                   "ruby",
                   "s",
                   "samp",
                   "script",
                   "select",
                   "slot",
                   "small",
                   "span",
                   "strong",
                   "sub",
                   "sup",
                   "svg",
                   "template",
                   "textarea",
                   "time",
                   "u",
                   "tt",
                   "var",
                   "video",
                   "wbr"
           });

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
    * An immutable set of HTML 'event' attribute names.
    */
   public static final ImmutableSet<String> eventAttributeNames = ImmutableSet.copyOf(new String[] {
           "onafterprint",
           "onbeforeprint",
           "onbeforeunload",
           "onerror",
           "onhashchange",
           "onload",
           "onmessage",
           "onoffline",
           "ononline",
           "onpagehide",
           "onpageshow",
           "onpopstate",
           "onresize",
           "onstorage",
           "onunload",
           "onblur",
           "onchange",
           "oncontextmenu",
           "onfocus",
           "oninput",
           "oninvalid",
           "onreset",
           "onsearch",
           "onselect",
           "onsubmit",
           "onkeydown",
           "onkeypress",
           "onkeyup",
           "onclick",
           "ondblclick",
           "onmousedown",
           "onmousemove",
           "onmouseout",
           "onmouseover",
           "onmouseup",
           "onmousewheel",
           "onwheel",
           "ondrag",
           "ondragend",
           "ondragenter",
           "ondragleave",
           "ondragover",
           "ondragstart",
           "ondrop",
           "onscroll",
           "oncopy",
           "oncut",
           "onpaste",
           "onabort",
           "oncanplay",
           "oncanplaythrough",
           "oncuechange",
           "ondurationchange",
           "onemptied",
           "onended",
           "onerror",
           "onloadeddata",
           "onloadedmetadata",
           "onloadstart",
           "onpause",
           "onplay",
           "onplaying",
           "onprogress",
           "onratechange",
           "onseeked",
           "onseeking",
           "onstalled",
           "onsuspend",
           "ontimeupdate",
           "onvolumechange",
           "onwaiting",
           "onshow",
           "ontoggle"
   });

   /**
    * Creates a safelist that allows a specific set of tags and attributes (without regard to context).
    * @param allowTags A collection of tags to allow. Add {@code 'data-*'} to allow any HTML5 data attribute.
    * @param allowAttributes A collection of attributes to allow.
    * @return The safelist.
    */
   public static Safelist safelist(final Collection<String> allowTags,
                                   final Collection<String> allowAttributes) {

      return new Safelist() {

         private final ImmutableSet<String> tagNames = ImmutableSet.copyOf(allowTags.stream()
                 .map(String::toLowerCase).distinct().iterator()
         );

         private final ImmutableSet<String> attributeNames = ImmutableSet.copyOf(allowAttributes.stream()
                 .map(String::toLowerCase).distinct().iterator()
         );

         @Override
         protected boolean	isSafeTag(String tag) {
            return tagNames.contains(tag);
         }

         @Override
         protected boolean	isSafeAttribute(String tagName, Element el, Attribute attr) {
            final String attributeName = attr.getKey();
            return attributeNames.contains(attributeName) || (attributeName.startsWith("data-") && attributeNames.contains("data-*"));
         }
      };
   }

   /**
    * A safelist that includes only HTML5 block elements
    * with {@code class}, {@code id} and {@code data-*}attributes.
    * @return The safelist.
    */
   public static Safelist blockElements() {
      return safelist(blockElementNames, ImmutableList.of("id", "class", "data-*"));
   }

   /**
    * The basic safelist.
    * <p>
    *    Includes tags: {@code a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small,
    *    strike, del, s, strong, sub, sup, u, ul, mark, bdi}
    * </p>
    * @return The safelist.
    */
   public static Safelist basicSafelist() {

      return new Safelist()
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
