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

package com.attribyte.parser.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.attribyte.parser.Util.domain;

/**
 * An immutable oembed provider.
 */
public class OEmbedProvider {

   public static class Endpoint {

      /**
       * Creates an endpoint.
       * @param url The endpoint URL.
       * @param schemes The collection of schemes.
       */
      public Endpoint(final String url, final Collection<String> schemes) {
         this.schemes = schemes != null ? ImmutableList.copyOf(schemes) : ImmutableList.of();
         this.url = url;
         List<Pattern> patterns = Lists.newArrayListWithExpectedSize(this.schemes.size());
         for(String scheme : this.schemes) {
            patterns.add(Pattern.compile(
                    scheme
                            .replace(".", "\\.")
                            .replace("*", ".*")
                    )
            );
         }
         this.patterns = ImmutableList.copyOf(patterns);
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this)
                 .add("schemes", schemes)
                 .add("url", url)
                 .toString();
      }

      /**
       * Determine if a URL matches any of the supported schemes.
       * @param url The URL.
       * @return Does this endpoint support the URL?
       */
      public final boolean matches(final String url) {
         for(Pattern pattern : patterns) {
            if(pattern.matcher(url).matches()) {
               return true;
            }
         }
         return false;
      }

      /**
       * A list of patterns for each scheme.
       */
      public final ImmutableList<Pattern> patterns;

      /**
       * The immutable list of schemes.
       */
      public final ImmutableList<String> schemes;

      /**
       * The endpoint URL.
       */
      public final String url;
   }

   /**
    * Creates an oembed provider.
    * @param name The provider name.
    * @param url The provider URL.
    * @param endpoints The collection of endpoints.
    */
   public OEmbedProvider(final String name, final String url, final Collection<Endpoint> endpoints) {
      this.name = name;
      this.url = url;
      this.endpoints = endpoints != null ? ImmutableList.copyOf(endpoints) : ImmutableList.of();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("name", name)
              .add("url", url)
              .add("endpoints", endpoints)
              .toString();
   }

   /**
    * The provider name.
    */
   public final String name;

   /**
    * The provider URL.
    */
   public final String url;

   /**
    * The immutable list of endpoints.
    */
   public final ImmutableList<Endpoint> endpoints;


   /**
    * Builds a map of endpoints vs the domain they support.
    * @param providers A collection of providers.
    * @return The map of endpoints vs domain.
    */
   public static ImmutableMap<String, ImmutableList<Endpoint>> domainMap(final Collection<OEmbedProvider> providers) {
      Map<String, List<Endpoint>> builder = Maps.newHashMap();
      providers.forEach(provider -> {
         provider.endpoints.forEach(endpoint -> {
            endpoint.schemes.forEach(scheme -> {
               String testURL = scheme.replace('*', 'x');
               testURL = testURL.replace("{format}", "json");
               String domain = domain(testURL);
               if(domain != null) {
                  List<Endpoint> endpoints = builder.computeIfAbsent(domain, k -> Lists.newArrayList());
                  endpoints.add(endpoint);
               }
            });
         });
      });

      ImmutableMap.Builder<String, ImmutableList<Endpoint>> endpointMap = ImmutableMap.builder();
      builder.forEach((k, v) -> {
         endpointMap.put(k, ImmutableList.copyOf(v));
      });
      return endpointMap.build();
   }


   /**
    * Creates a map containing the default published providers.
    * @return The map of providers vs URL.
    * @throws IOException on parse/load error.
    */
   public static ImmutableMap<String, OEmbedProvider> publishedProviders() throws IOException {
      return publishedProviders(DEFAULT_PROVIDERS_URL);
   }


   /**
    * Creates a map containing published providers from a URL.
    * @return The map of providers vs URL.
    * @throws IOException on parse/load error.
    */
   public static ImmutableMap<String, OEmbedProvider> publishedProviders(final String url) throws IOException {
      InputStream is = new URL(url).openStream();
      try {
         return fromJSON(new ObjectMapper().readTree(is));
      } catch(MalformedURLException mue) {
         throw new IOException(String.format("Invalid URL, '%s'", url), mue);
      } finally {
         if(is != null) {
            is.close();
         }
      }
   }

   /**
    * Creates a map of provider vs URL from the format supported by {@code https://oembed.com/providers.json}.
    * @param node The root node.
    * @return The map of provider vs URL.
    */
   public static ImmutableMap<String, OEmbedProvider> fromJSON(final JsonNode node) {
      if(node.isArray()) {
         ImmutableMap.Builder<String, OEmbedProvider> providerBuilder = ImmutableMap.builder();
         for(final JsonNode providerNode : node) {
            String providerName = providerNode.path("provider_name").asText();
            String providerURL = providerNode.path("provider_url").asText();
            if(!providerURL.isEmpty()) {
               JsonNode endpointsNode = providerNode.path("endpoints");
               if(endpointsNode.isArray()) {
                  List<Endpoint> endpoints = Lists.newArrayListWithExpectedSize(1024);
                  for(JsonNode endpointNode : endpointsNode) {
                     String endpointURL = endpointNode.path("url").asText();
                     if(!endpointURL.isEmpty()) {
                        JsonNode schemesNode = endpointNode.path("schemes");
                        if(schemesNode.isArray()) {
                           List<String> schemes = Lists.newArrayListWithExpectedSize(4);
                           for(JsonNode schemeNode : schemesNode) {
                              String scheme = schemeNode.asText();
                              if(!scheme.isEmpty()) {
                                 schemes.add(scheme);
                              }
                           }
                           endpoints.add(new Endpoint(endpointURL, schemes));
                        }
                     }
                  }
                  providerBuilder.put(providerURL, new OEmbedProvider(providerName, providerURL, endpoints));
               }
            }
         }
         return providerBuilder.build();
      } else {
         return ImmutableMap.of();
      }
   }

   /**
    * The standard oembed providers endpoint URL ({@value}).
    */
   public static final String DEFAULT_PROVIDERS_URL = "https://oembed.com/providers.json";
}