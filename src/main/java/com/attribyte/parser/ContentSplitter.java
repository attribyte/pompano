/*
 * Copyright 2020 Attribyte, LLC
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.attribyte.parser.Whitelists.inlineElementNames;

/**
 * Split content into a single sequence of elements.
 */
public class ContentSplitter {

   private class ToInlineWithBreak implements Function<Element, List<Node>> {

      public ToInlineWithBreak(@NonNull final String tagName) {
         this.tagName = tagName;
      }

      public List<Node> apply(final Element elem) {
         List<Node> nodes = Lists.newArrayList();
         boolean hasElements = false;
         for(Node node : elem.childNodes()) {
            if(node instanceof Element) {
               hasElements = true;
               if(((Element)node).tagName().equals(tagName)) {
                  node.childNodes().forEach(childNode -> nodes.add(childNode.clone()));
               } else {
                  nodes.add(node.clone());
               }
            } else {
               nodes.add(node.clone());
            }
         }

         if(!hasElements) {
            Element containerElem = new Element(tagName);
            nodes.forEach(containerElem::appendChild);
            nodes.clear();
            nodes.add(containerElem);
            nodes.add(new Element("br"));
            return nodes;
         } else {
            Element containerElem = new Element("body");
            nodes.forEach(containerElem::appendChild);
            NodeCollector collector = new NodeCollector(tagName);
            containerElem.filter(collector);
            nodes.clear();
            nodes.addAll(collector.elements);
            nodes.add(new Element("br"));
            return nodes;
         }
      }

      private final String tagName;
   }

   /**
    * Create a mapping function that converts a tag to an inline tag followed by a break.
    * @param tagName The replacement tag name.
    * @return The nodes.
    */
   public ContentSplitter convertToInlineWithBreak(final String tagName, final String... mapTags) {
      Map<String, Function<Element, List<Node>>> newMapTags = Maps.newHashMap(this.mapTags);
      Function<Element, List<Node>> fn = new ToInlineWithBreak(tagName);
      for(String mapTag : mapTags) {
         newMapTags.put(mapTag, fn);
      }
      return withMapTags(newMapTags);
   }

   /**
    * Create a content splitter.
    * @param containerTag The tag for each split element.
    * @param ignoreTags Tags to ignore.
    * @param preserveTags Tags to preserve as-is.
    * @param mapTags Function that maps the tag to a list of nodes in the output.
    */
   public ContentSplitter(@NonNull final String containerTag,
                          @Nullable final Set<String> ignoreTags,
                          @Nullable final Set<String> preserveTags,
                          @Nullable final Map<String, Function<Element, List<Node>>> mapTags) {
      this.containerTag = containerTag;
      this.ignoreTags = ignoreTags != null ? ImmutableSet.copyOf(ignoreTags) : ImmutableSet.of();
      this.preserveTags = preserveTags != null ? ImmutableSet.copyOf(preserveTags) : ImmutableSet.of();
      this.mapTags = mapTags != null ? ImmutableMap.copyOf(mapTags) : ImmutableMap.of();
   }

   /**
    * A node collector that accumulates the elements.
    */
   private class NodeCollector implements NodeFilter {

      NodeCollector() {
         this(ContentSplitter.this.containerTag);
      }

      NodeCollector(final String containerTag) {
         this.containerTag = containerTag;
      }

      public NodeFilter.FilterResult head(Node node, int depth) {
         if(node instanceof TextNode) {
            if(!((TextNode)node).isBlank()) {
               inlineNodes.add(node.clone());
            }
            return FilterResult.CONTINUE;
         } else if(node instanceof Element) {
            final Element elementNode = (Element)node;
            final String tagName = elementNode.tagName().toLowerCase();
            if(tagName.equals("body")) {
               return FilterResult.CONTINUE;
            }
            Function<Element, List<Node>> mapFunction = mapTags.get(tagName);
            if(mapFunction != null) {
               inlineNodes.addAll(mapFunction.apply(elementNode));
               return FilterResult.SKIP_CHILDREN;
            } else if(preserveTags.contains(tagName)) {
               elements.add(elementNode.clone());
               return FilterResult.SKIP_CHILDREN;
            } else if(ignoreTags.contains(tagName)) {
               if(!inlineElementNames.contains(tagName)) {
                  addInlineNodes();
               }
               return FilterResult.CONTINUE;
            } else {
               inlineNodes.add(node.clone());
               return FilterResult.SKIP_CHILDREN;
            }
         } else {
            return FilterResult.SKIP_ENTIRELY;
         }
      }

      public NodeFilter.FilterResult tail(Node node, int depth) {
         return FilterResult.CONTINUE;
      }

      void addInlineNodes() {
         if(!inlineNodes.isEmpty()) {
            Element containerElement = new Element(containerTag);
            inlineNodes.forEach(containerElement::appendChild);
            elements.add(containerElement);
            inlineNodes.clear();
         }
      }

      final Elements elements = new Elements();
      final List<Node> inlineNodes = Lists.newArrayList();
      final String containerTag;
   }


   /**
    * Split into elements.
    * @param elem The element to split.
    * @return The split elements.
    */
   public Elements split(final Element elem) {
      NodeCollector collector = new NodeCollector();
      elem.filter(collector);
      return collector.elements;
   }



   /**
    * Create a copy of this content splitter with new container tag.
    * @param containerTag The container tag.
    * @return The content splitter.
    */
   public ContentSplitter withContainerTag(@NonNull final String containerTag) {
      if(containerTag.equals(this.containerTag)) {
         return this;
      }
      return new ContentSplitter(containerTag, ignoreTags, preserveTags, mapTags);
   }

   /**
    * Create a copy of this content splitter with different ignore tags.
    * @param ignoreTags The set of ignore tags.
    * @return The content splitter.
    */
   public ContentSplitter withIgnoreTags(@NonNull final Set<String> ignoreTags) {
      return new ContentSplitter(containerTag, ignoreTags, preserveTags, mapTags);
   }

   /**
    * Create a copy of this content splitter with different preserve tags.
    * @param preserveTags The set of preserve tags.
    * @return The content splitter.
    */
   public ContentSplitter withPreserveTags(@NonNull final Set<String> preserveTags) {
      return new ContentSplitter(containerTag, ignoreTags, preserveTags, mapTags);
   }

   /**
    * Create a copy of this content splitter with different map tag functions.
    * @param mapTags The mapping function map.
    * @return The content splitter.
    */
   public ContentSplitter withMapTags(@NonNull final Map<String, Function<Element, List<Node>>> mapTags) {
      return new ContentSplitter(containerTag, ignoreTags, preserveTags, mapTags);
   }

   private final String containerTag;
   private final ImmutableSet<String> ignoreTags;
   private final ImmutableSet<String> preserveTags;
   private final ImmutableMap<String, Function<Element, List<Node>>> mapTags;
}
