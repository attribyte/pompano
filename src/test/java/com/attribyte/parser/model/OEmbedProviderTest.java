/*
 * Copyright 2019 Attribyte, LLC
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

package com.attribyte.parser.model;

import com.attribyte.parser.ResourceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static com.attribyte.parser.Util.domain;

public class OEmbedProviderTest extends ResourceTest {

   @Test
   public void matchSuffixPattern() throws IOException {
      String resource = testResource("oembed_providers.json");
      Map<String, OEmbedProvider> providerMap = OEmbedProvider.fromJSON(jsonReader.readTree(resource));
      ImmutableMap<String, ImmutableList<OEmbedProvider.Endpoint>> domainEndpoints = OEmbedProvider.domainMap(providerMap.values());
      String link = "https://open.spotify.com/track/298gs9ATwr2rD9tGYJKlQR";
      String domain = domain(link);
      List<OEmbedProvider.Endpoint> endpoints = domainEndpoints.get(domain);
      assertNotNull(endpoints);
      assertEquals(1, endpoints.size());
      OEmbedProvider.Endpoint endpoint = endpoints.get(0);
      assertTrue(endpoint.matches(link));
   }

   @Test
   public void matchPrefixPattern() throws IOException {
      String resource = testResource("oembed_providers.json");
      Map<String, OEmbedProvider> providerMap = OEmbedProvider.fromJSON(jsonReader.readTree(resource));
      ImmutableMap<String, ImmutableList<OEmbedProvider.Endpoint>> domainEndpoints = OEmbedProvider.domainMap(providerMap.values());
      String link = "https://doh.flickr.com/photos/86832534@N03/42728148584/";
      String domain = domain(link);
      List<OEmbedProvider.Endpoint> endpoints = domainEndpoints.get(domain);
      assertNotNull(endpoints);
      assertEquals(1, endpoints.size());
      OEmbedProvider.Endpoint endpoint = endpoints.get(0);
      assertTrue(endpoint.matches(link));
   }

   /**
    * The JSON reader.
    */
   private final ObjectReader jsonReader = new ObjectMapper().reader();
}
