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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Tests for utility methods.
 * @author Matt Hamer
 */
public class ContentSplitterTest extends ResourceTest {

   @Test
   public void split0() throws Exception {
      Document doc = Jsoup.parse(testResource("split_test_0.html"));
      ContentSplitter contentSplitter = new ContentSplitter("p",
              Whitelists.blockElementNames,
              ImmutableSet.copyOf(new String[] {
                      "ul",
                      "img",
                      "figure"
                      //"h1", "h2", "h3", "h4", "h5", "h6"
              }), null).convertToInlineWithBreak("b", "h1", "h2", "h3", "h4", "h5", "h6");

      System.out.println(contentSplitter.split(doc.body()));
   }

   @Test
   public void split_before_after_preserved() throws Exception {
      String content = "Leading text <img src='https://attribyte.com/img1.jpg'/> Trailing text";
      Document doc = Jsoup.parseBodyFragment(content, "");
      ContentSplitter contentSplitter = new ContentSplitter("p",
              Whitelists.blockElementNames,
              ImmutableSet.copyOf(new String[] {
                      "ul",
                      "img",
                      "figure"
              }), null);


      Elements splitElements = contentSplitter.split(doc.body());
      assertEquals(3, splitElements.size());
      assertEquals("Leading text", splitElements.get(0).text());
      assertEquals("img", splitElements.get(1).tagName());
      assertEquals("Trailing text", splitElements.get(2).text());
   }
}
