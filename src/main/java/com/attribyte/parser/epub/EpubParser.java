package com.attribyte.parser.epub;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.attribyte.parser.DateParser.tryParseISO8601;
import static com.attribyte.parser.Util.unzip;

public class EpubParser {

   public static void main(final String[] args) throws Exception {
      File epubFile = new File("/home/matt/test/pg64454-images.epub");
      //File epubFile = new File("/home/matt/test/test.epub");
      File targetDir = new File("/home/matt/test/target");
      List<EpubDocument> docs =
              parse(epubFile, targetDir, file -> System.out.println("Processing " + file.getAbsolutePath()));
      docs.forEach(doc -> System.out.println(doc.toString()));
   }

   public static final ImmutableSet<String> supportedMimeTypes =
           ImmutableSet.of("application/epub+zip");

   /**
    * Parse an epub file.
    * @param epubFile The file.
    * @param targetDir The target output directory.
    * @param fileConsumer If non-null, extracted files are reported to this function.
    * @return The list of documents (each has metadata and a spine).
    * @throws IOException on error.
    */
   public static List<EpubDocument> parse(final File epubFile,
                                          final File targetDir,
                                          final Consumer<File> fileConsumer) throws IOException {
      unzip(epubFile, targetDir, fileConsumer);
      File metaDir = new File(targetDir, "META-INF");
      if(!metaDir.exists()) {
         throw new IOException("Missing 'META-INF' directory");
      }

      File mimetypeFile = new File(targetDir, "mimetype");
      if(!mimetypeFile.exists()) {
         throw new IOException("Missing 'mimetype' file");
      }

      String checkType = new String(Files.toByteArray(mimetypeFile), Charsets.US_ASCII);
      if(!supportedMimeTypes.contains(checkType.trim().toLowerCase())) {
         throw new IOException(String.format("Unsupported mime type, '%s'", checkType));
      }

      List<File> rootFiles = rootFiles(metaDir);
      if(rootFiles.isEmpty()) {
         throw new IOException("At least one 'rootfile' must be present in the container");
      }

      List<EpubDocument> documents = Lists.newArrayListWithExpectedSize(2);
      for(File file : rootFiles) {
         try(FileInputStream fis = new FileInputStream(file)) {
            Document packageDoc = Jsoup.parse(fis, Charsets.UTF_8.name(), "", Parser.xmlParser());
            Metadata metadata = new Metadata(packageDoc);
            Map<String, ManifestItem> files = manifestItems(file.getParentFile(), packageDoc);
            Spine spine = spine(packageDoc, files);
            documents.add(new EpubDocument(metadata, spine));
         }
      }
      return documents;
   }

   /**
    * Extract the root files.
    * @param metaDir The meta directory.
    * @return The list of files.
    * @throws IOException on error.
    */
   private static List<File> rootFiles(final File metaDir) throws IOException {
      File containerFile = new File(metaDir, "container.xml");
      if(!containerFile.exists()) {
         throw new IOException("Missing 'META-INF/container.xml'");
      }

      List<File> rootFiles = Lists.newArrayListWithExpectedSize(2);
      try(FileInputStream fis = new FileInputStream(containerFile)) {
         Document doc = Jsoup.parse(fis, Charsets.UTF_8.name(), "", Parser.xmlParser());
         Elements elements = doc.select("rootfile[full-path]");
         for(Element element : elements) {
            String path = element.attr("full-path").trim();
            if(path.isEmpty()) {
               continue;
            } else if(path.startsWith("/")) {
               path = path.substring(1);
            }
            File rootFile = new File(metaDir.getParent(), path);
            if(!rootFile.exists()) {
               throw new IOException(String.format("Missing file, '%s'", rootFile.getAbsolutePath()));
            }
            rootFiles.add(rootFile);
         }
      }

      return rootFiles;
   }

   /**
    * An Epub format document.
    */
   public static class EpubDocument {

      public EpubDocument(final Metadata metadata,
                          final Spine spine) {
         this.metadata = metadata;
         this.spine = spine;
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this)
                 .add("metadata", metadata)
                 .add("spine", spine)
                 .toString();
      }

      /**
       * The metadata.
       */
      public final Metadata metadata;

      /**
       * The spine.
       */
      public final Spine spine;
   }

   /**
    * The document metadata.
    */
   public static final class Metadata {

      /**
       * Creates metadata from the package doc.
       * @param packageDoc The package doc.
       * @throws IOException on parse error.
       */
      private Metadata(final Document packageDoc) throws IOException {
         Element packageElem = packageDoc.selectFirst("package");
         if(packageElem == null) {
            throw new IOException("The package document must contain a 'package' element");
         }
         String uniqueIdentifierId = packageElem.attr("unique-identifier");
         Element metadataElem = packageElem.selectFirst("metadata");

         if(metadataElem == null) {
            throw new IOException("The package document must contain a 'metadata' element");
         }

         String identifier = "";
         if(!uniqueIdentifierId.isEmpty()) {
            Element identifierElem = metadataElem.getElementById(uniqueIdentifierId);
            if(identifierElem != null) {
               identifier = identifierElem.text();
            }
         }

         this.identifier = identifier;

         Element modifiedElem = metadataElem.selectFirst("meta[property=dcterms:modified]");
         if(modifiedElem == null) {
            this.modifiedTimestamp = 0L;
         } else {
            Long modifiedTimestamp = tryParseISO8601(modifiedElem.text());
            this.modifiedTimestamp = modifiedTimestamp != null ? modifiedTimestamp : 0L;
         }

         Element publishedElem = metadataElem.selectFirst("dc|date");
         if(publishedElem == null) {
            this.publishedTimestamp = 0L;
         } else {
            Long publishedTimestamp = tryParseISO8601(publishedElem.text());
            this.publishedTimestamp = publishedTimestamp != null ? publishedTimestamp : 0L;
         }

         this.titles = dcElements(metadataElem, "title");
         this.title = titles.isEmpty() ? "" : this.titles.get(0);

         Element languageElem = metadataElem.selectFirst("dc|language");
         this.language = languageElem != null ? languageElem.text() : "";

         this.rights = dcElements(metadataElem, "rights");
         this.creators = dcElements(metadataElem, "creator");
         this.contributors = dcElements(metadataElem, "contributor");
         this.subjects = dcElements(metadataElem, "subject");
      }

      /**
       * Gets DC elements.
       * @param metadataElem The metadata element.
       * @param name The name.
       * @return The list of elements.
       */
      private static ImmutableList<String> dcElements(final Element metadataElem, final String name) {
         Elements elements = metadataElem.select("dc|" + name);
         if(elements.isEmpty()) {
            return ImmutableList.of();
         } else if(elements.size() == 1) {
            return ImmutableList.of(elements.get(0).text());
         } else {
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            elements.forEach(elem -> {
               builder.add(elem.text());
            });
            return builder.build();
         }
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this)
                 .add("identifier", identifier)
                 .add("publishedTimestamp", publishedTimestamp)
                 .add("rights", rights)
                 .add("modifiedTimestamp", modifiedTimestamp)
                 .add("title", title)
                 .add("titles", titles)
                 .add("language", language)
                 .add("creators", creators)
                 .add("contributors", contributors)
                 .add("subjects", subjects)
                 .toString();
      }

      /**
       * The identifier.
       */
      public final String identifier;

      /**
       * The primary title.
       */
      public final String title;

      /**
       * All titles.
       */
      public final ImmutableList<String> titles;

      /**
       * All rights.
       */
      public final ImmutableList<String> rights;

      /**
       * The language.
       */
      public final String language;

      /**
       * The creators.
       */
      public final ImmutableList<String> creators;

      /**
       * The contributors.
       */
      public final ImmutableList<String> contributors;

      /**
       * The subjects.
       */
      public final ImmutableList<String> subjects;

      /**
       * The last modified timestamp.
       */
      public final long modifiedTimestamp;

      /**
       * The time the document was published.
       */
      public final long publishedTimestamp;

   }

   /**
    * A document spine.
    */
   public static final class Spine {

      /**
       * Creates the spine.
       * @param linear The linear elements.
       * @param nonLinear The non-linear elements.
       * @param items The items.
       */
      Spine(final List<ManifestItem> linear,
            final List<ManifestItem> nonLinear,
            final Map<String, ManifestItem> items) {
         this.linear = linear != null ? ImmutableList.copyOf(linear) : ImmutableList.of();
         this.nonLinear = nonLinear != null ? ImmutableList.copyOf(nonLinear) : ImmutableList.of();
         this.items = items != null ? ImmutableMap.copyOf(items) : ImmutableMap.of();
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this)
                 .add("linear", linear)
                 .add("nonLinear", nonLinear)
                 .add("items", items)
                 .toString();
      }

      /**
       * @return A list containing all items from both linear and non-linear lists that are images.
       */
      public List<ManifestItem> images() {
         List<ManifestItem> images = Lists.newArrayList();
         linear.forEach(item -> {
            if(item.mediaType.startsWith("image/")) {
               images.add(item);
            }
         });

         nonLinear.forEach(item -> {
            if(item.mediaType.startsWith("image/")) {
               images.add(item);
            }
         });

         return images;
      }

      /**
       * @return All items in the linear list that are XHTML.
       */
      public List<ManifestItem> linearXHTML() {
         List<ManifestItem> items = Lists.newArrayList();
         linear.forEach(item -> {
            if(item.mediaType.equalsIgnoreCase("application/xhtml+xml")) {
               items.add(item);
            }
         });
         return items;
      }

      /**
       * @return All items in the non-linear list that are XHTML.
       */
      public List<ManifestItem> nonLinearXHTML() {
         List<ManifestItem> items = Lists.newArrayList();
         nonLinear.forEach(item -> {
            if(item.mediaType.equalsIgnoreCase("application/xhtml+xml")) {
               items.add(item);
            }
         });
         return items;
      }

      /**
       * The linear elements.
       */
      public final ImmutableList<ManifestItem> linear;

      /**
       * The non-linear elements.
       */
      public final ImmutableList<ManifestItem> nonLinear;

      /**
       * A map of manifest file vs id.
       */
      public final ImmutableMap<String, ManifestItem> items;
   }

   /**
    * A manifest item in the spine.
    */
   public static final class ManifestItem {

      /**
       * Creates the file.
       * @param id The id.
       * @param file The file.
       * @param mediaType The media type for the file.
       */
      ManifestItem(final String id, final String name,
                   final File file, final String mediaType) {
         this.id = id;
         this.name = name;
         this.file = file;
         this.mediaType = mediaType;
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this)
                 .add("id", id)
                 .add("name", name)
                 .add("file", file)
                 .add("mediaType", mediaType)
                 .toString();
      }

      /**
       * Gets the parsed XHTML document for this item, if any.
       * @return The document or {@code null} if not XHTML.
       * @throws IOException on parse error.
       */
      public Document document() throws IOException {
         if(mediaType.equalsIgnoreCase("application/xhtml+xml")) {
            try(FileInputStream fis = new FileInputStream(file)) {
               return Jsoup.parse(fis, Charsets.UTF_8.name(), "", Parser.xmlParser());
            }
         } else {
            return null;
         }
      }

      /**
       * Gets the raw bytes for this item.
       * @return The bytes.
       */
      public byte[] bytes() throws IOException {
         try(FileInputStream fis = new FileInputStream(file)) {
            return ByteStreams.toByteArray(fis);
         }
      }

      /**
       * Externally process the bytes for this item supplied as a stream.
       * @param processor The processor function.
       * @throws IOException On error.
       */
      public void processByteStream(final Consumer<InputStream> processor) throws IOException {
         try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            processor.accept(bis);
         }
      }

      /**
       * Gets the size of this item in bytes.
       * @return The size.
       */
      public long size() {
         return file.length();
      }

      /**
       * The id.
       */
      public final String id;

      /**
       * The name.
       */
      public final String name;

      /**
       * The file.
       */
      private final File file;

      /**
       * The media type.
       */
      public final String mediaType;
   }

   /**
    * Builds the spine.
    * @param opfDoc The OPF doc.
    * @param items The items map.
    * @return The spine.
    * @throws IOException on error.
    */
   private static Spine spine(final Document opfDoc,
                      final Map<String, ManifestItem> items) throws IOException {

      List<ManifestItem> linear = Lists.newArrayList();
      List<ManifestItem> nonLinear = Lists.newArrayList();

      for(Element element : opfDoc.select("spine > itemref")) {
         boolean isLinear = element.attr("linear").equalsIgnoreCase("yes");
         String id = element.attr("idref");
         ManifestItem item = items.get(id);
         if(item == null) {
            throw new IOException(String.format("Missing spine item, '%s'", id));
         } else if(isLinear) {
            linear.add(item);
         } else {
            nonLinear.add(item);
         }
      }

      return new Spine(linear, nonLinear, items);
   }

   /**
    * Create a map of manifest items vs id.
    * @param rootDir The root directory for the underlying files.
    * @param opfDoc The OPF document.
    * @return The map of items.
    * @throws IOException on error.
    */
   private static Map<String, ManifestItem> manifestItems(final File rootDir, final Document opfDoc) throws IOException {

      Map<String, ManifestItem> files = Maps.newLinkedHashMap();

      for(Element element : opfDoc.select("manifest > item")) {
         String href = element.attr("href").trim();
         if(href.isEmpty()) {
            continue;
         } else if(href.startsWith("/")) {
            href = href.substring(1);
         }

         File file = new File(rootDir, href);
         if(!file.exists()) {
            throw new IOException(String.format("Missing file, '%s'", file.getAbsolutePath()));
         }

         String id = element.attr("id").trim();
         if(id.isEmpty()) {
            throw new IOException(String.format("Missing id for, '%s'", file.getAbsolutePath()));
         }

         String mediaType = element.attr("media-type").trim();

         files.put(id, new ManifestItem(id, href, file, mediaType));
      }

      return files;
   }
}