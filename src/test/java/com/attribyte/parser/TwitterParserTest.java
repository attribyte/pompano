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

import com.attribyte.parser.entry.TwitterAPIParser;
import com.attribyte.parser.model.Entry;
import com.attribyte.parser.model.Resource;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test the twitter parser.
 */
public class TwitterParserTest extends ResourceTest {

   @Test
   public void timeline() throws IOException {
      ParseResult res = new TwitterAPIParser().parse(testResource("twitter_timeline.json"), "", new DefaultContentCleaner());
      assertNotNull(res);
      assertFalse(res.hasErrors());
      assertTrue(res.resource.isPresent());
      Resource parsedResource = res.resource.get();
      for(Entry entry : res.resource.get().entries) {
         System.out.println(entry);
         System.out.println();
         System.out.println();
      }
   }

}