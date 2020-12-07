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

import com.google.common.collect.ImmutableSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.File;
import java.util.List;

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
   public void before_after_preserved() throws Exception {
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

   @Test
   public void translate_inline_with_break() throws Exception {
      String content = "Leading text <h1>The <b>title</b>.</h1> Trailing text";
      Document doc = Jsoup.parseBodyFragment(content, "");
      ContentSplitter contentSplitter = new ContentSplitter("p",
              Whitelists.blockElementNames,
              ImmutableSet.copyOf(new String[] {
                      "ul",
                      "img",
                      "figure"
              }), null).convertToInlineWithBreak("b", "h1");


      Elements splitElements = contentSplitter.split(doc.body());
      System.out.println(splitElements.toString());
      assertEquals(2, splitElements.size());
      assertEquals("Leading text", splitElements.get(0).text());
      List<Node> children = splitElements.get(1).childNodes();
      assertEquals(3, children.size());
      assertElementText(children.get(0), "b", "The title.");
      assertElementText(children.get(1), "br", "");
      assertNodeText(children.get(2), "Trailing text");
   }


   @Test
   public void translate_nested() throws Exception {
      String content = "Leading text <div><p><span>Trailing <b>text</b></span></p></div>";
      Document doc = Jsoup.parseBodyFragment(content, "");
      ContentSplitter contentSplitter = new ContentSplitter("p",
              ImmutableSet.of("div", "span", "p"),
              ImmutableSet.copyOf(new String[] {
                      "ul",
                      "img",
                      "figure"
              }), null);


      Elements splitElements = contentSplitter.split(doc.body());
      System.out.println(splitElements.toString());
      assertEquals(2, splitElements.size());
      assertEquals("Leading text", splitElements.get(0).text());
      assertEquals("Trailing <b>text</b>", splitElements.get(1).html());
   }

   @Test
   public void image_in_anchor() throws Exception {
      String content = "<div><a href='https://attribyte.com'><img src='https://attribyte.com/img1.jpg'/></a></div>";
      Document doc = Jsoup.parseBodyFragment(content, "");
      ContentSplitter contentSplitter = new ContentSplitter("p",
              Whitelists.blockElementNames,
              ImmutableSet.of("img")
              , null);
      Elements splitElements = contentSplitter.split(doc.body());
      assertEquals(1, splitElements.size());
      assertEquals(1, splitElements.get(0).children().size());
      assertEquals("a", splitElements.get(0).child(0).tagName());
      assertEquals(1, splitElements.get(0).child(0).children().size());
      assertEquals("img", splitElements.get(0).child(0).child(0).tagName());
   }

   private void assertNodeText(final Node node, final String value) {
      assertTrue(node instanceof TextNode);
      assertEquals(value, ((TextNode)node).text().trim());
   }

   private void assertElementText(final Node node, final String tagName,
                                  final String value) {
      assertTrue(node instanceof Element);
      assertEquals(tagName, ((Element)node).tagName());
      assertEquals(value, ((Element)node).text());
   }
}
