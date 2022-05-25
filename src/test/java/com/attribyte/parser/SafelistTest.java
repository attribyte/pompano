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
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for safelists.
 * @author Matt Hamer
 */
public class SafelistTest {

   @Test
   public void standardBlockElements() {
      String html = "<html><head><title>Title</title></head><body><div><p onfocus='bad' data-test='test-data' id='testid' class='testclass'><em>test</em></p></div></body></html>";
      Document doc = Jsoup.parse(html);
      Document cleanDoc = new Cleaner(Safelists.blockElements()).clean(doc);
      Element div = cleanDoc.body().selectFirst("div");
      assertNotNull(div);
      Element p = cleanDoc.body().selectFirst("p");
      assertNotNull(p);
      assertEquals(p.attr("data-test"), "test-data");
      assertEquals(p.attr("id"), "testid");
      assertEquals(p.attr("class"), "testclass");
      assertTrue(p.attr("onfocus").isEmpty());
   }

   @Test
   public void customElements() {
      Safelist safelist = Safelists.safelist(ImmutableSet.of("p"), ImmutableSet.of("id", "data-*"));
      String html = "<html><head><title>Title</title></head><body><div><p onfocus='bad' data-test='test-data' id='testid' class='testclass'><em>test</em></p></div></body></html>";
      Document doc = Jsoup.parse(html);
      Document cleanDoc = new Cleaner(safelist).clean(doc);
      Element div = cleanDoc.body().selectFirst("div");
      assertNull(div);
      Element p = cleanDoc.body().selectFirst("p");
      assertNotNull(p);
      assertEquals(p.attr("data-test"), "test-data");
      assertEquals(p.attr("id"), "testid");
      assertTrue(p.attr("class").isEmpty());
      assertTrue(p.attr("onfocus").isEmpty());
   }

   @Test
   public void customAttributes() {
      Safelist safelist = Safelists.safelist(Safelists.blockElementNames, ImmutableSet.of("id", "data-*"));
      String html = "<html><head><title>Title</title></head><body><p onfocus='bad' data-test='test-data' id='testid' class='testclass'><em>test</em></p></body></html>";
      Document doc = Jsoup.parse(html);
      Document cleanDoc = new Cleaner(safelist).clean(doc);
      Element p = cleanDoc.body().selectFirst("p");
      assertNotNull(p);
      assertEquals(p.attr("data-test"), "test-data");
      assertEquals(p.attr("id"), "testid");
      assertTrue(p.attr("class").isEmpty());
      assertTrue(p.attr("onfocus").isEmpty());
   }
}
