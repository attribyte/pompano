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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for format detection.
 * @author Matt Hamer
 */
public class DetectorTest {

   @Test
   public void detectRSS() {
      String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss>Test</rss>  \t \n\n ";
      assertEquals(Detector.Format.RSS, Detector.detect(content, ""));
   }

   @Test
   public void detectRSSRDF() {
      String content = "\n\n<rdf:RDF>Test</rdf:RDF>  \t \n\n ";
      assertEquals(Detector.Format.RSS, Detector.detect(content, ""));
   }

   @Test
   public void detectAtom() {
      String content = "\n\n<feed>Test</feed>  \t \n\n ";
      assertEquals(Detector.Format.ATOM, Detector.detect(content, ""));
   }

   @Test
   public void detectAMP() {
      String content = "\n\n<html ⚡>Test</html>  \t \n\n ";
      assertEquals(Detector.Format.AMP, Detector.detect(content, ""));
   }

   @Test
   public void detectAMP2() {
      String content = "\n\n<html amp>Test</html>  \t \n\n ";
      assertEquals(Detector.Format.AMP, Detector.detect(content, ""));
   }

   @Test
   public void detectHTML() {
      String content = "\n\n<html>Test</html>  \t \n\n ";
      assertEquals(Detector.Format.HTML, Detector.detect(content, ""));
   }

   @Test
   public void detectHTML2() {
      String content = "\n\n<p>Test</p>  \t \n\n ";
      assertEquals(Detector.Format.HTML, Detector.detect(content, "text/html; charset=utf8"));
   }

   @Test
   public void detectSitemap() {
      String content = "\n\n<urlset>Test</urlset>  \t \n\n ";
      assertEquals(Detector.Format.SITEMAP, Detector.detect(content, ""));
   }
}
