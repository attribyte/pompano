package com.attribyte.parser.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

/**
 * An immutable author.
 * @author Matt Hamer
 */
public class Author {

   /**
    * Builds immutable authors.
    */
   public static class Builder {

      /**
       * Gets the id.
       * @return The id.
       */
      public String getId() {
         return id;
      }

      /**
       * Sets the id.
       * @param id The id.
       * @return A self-reference.
       */
      public Builder setId(final String id) {
         this.id = id;
         return this;
      }

      /**
       * Gets the name.
       * @return The name.
       */
      public String getName() {
         return name;
      }

      /**
       * Gets the email.
       * @return The email.
       */
      public String getEmail() {
         return email;
      }

      /**
       * Sets the email.
       * @param email The Email.
       * @return A self-reference.
       */
      public Builder setEmail(final String email) {
         this.email = email;
         return this;
      }

      /**
       * Gets the link.
       * @return The link.
       */
      public String getLink() {
         return link;
      }

      /**
       * Sets the link.
       * @param link The link.
       * @return A self-reference.
       */
      public Builder setLink(final String link) {
         this.link = link;
         return this;
      }

      /**
       * Gets the display name.
       * @return The display name.
       */
      public String getDisplayName() {
         return displayName;
      }

      /**
       * Sets the display name.
       * @param displayName The display name.
       * @return A self-reference.
       */
      public Builder setDisplayName(final String displayName) {
         this.displayName  = displayName;
         return this;
      }

      /**
       * Gets the description.
       * @return The description.
       */
      public String getDescription() {
         return description;
      }

      /**
       * Sets the description.
       * @param description The description.
       * @return A self-reference.
       */
      public Builder setDescription(final String description) {
         this.description = description;
         return this;
      }

      /**
       * Builds an immutable author.
       * @return The author.
       */
      public Author build() {
         return new Author(id, name, email, link, displayName, description);
      }

      /**
       * Creates a builder with the required name.
       * @param name The name.
       */
      private Builder(final String name) {
         this.name = name;
         this.id = "";
         this.email = "";
         this.link = "";
         this.displayName = "";
         this.description = "";
      }

      /**
       * Creates a builder.
       * @param id the id.
       * @param name the name.
       * @param email the email.
       * @param link the link.
       * @param displayName The display name.
       * @param description The description.
       */
      private Builder(final String id, final String name,
                      final String email, final String link,
                      final String displayName, final String description) {
         this.id = id;
         this.name = name;
         this.email = email;
         this.link = link;
         this.displayName = displayName;
         this.description = description;
      }

      private String id;
      private String name;
      private String email;
      private String link;
      private String displayName;
      private String description;
   }

   /**
    * Creates an author.
    * @param id The id.
    * @param name The name.
    * @param email The email.
    * @param link The link.
    * @param displayName A display name.
    * @param description A description.
    */
   private Author(final String id, final String name, final String email, final String link,
                  final String displayName, final String description) {
      this.id = Strings.nullToEmpty(id);
      this.name = name;
      this.email = Strings.nullToEmpty(email);
      this.link = Strings.nullToEmpty(link);
      this.displayName = Strings.nullToEmpty(displayName);
      this.description = Strings.nullToEmpty(description);
   }

   /**
    * Creates a builder.
    * @param name The (required) name for the author.
    * @return The builder.
    * @throws UnsupportedOperationException If name is {@code null} or empty.
    */
   public static Builder builder(final String name) throws UnsupportedOperationException {
      if(Strings.nullToEmpty(name).trim().isEmpty()) {
         throw new UnsupportedOperationException("The 'name' must not be null or empty");
      }
      return new Builder(name);
   }

   /**
    * Creates a builder populated from an existing author.
    * @param author The author.
    * @return The builder.
    */
   public static Builder builder(final Author author) {
      return new Builder(author.id, author.name, author.email, author.link, author.displayName, author.description);
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("id", id)
              .add("name", name)
              .add("email", email)
              .add("link", link)
              .add("displayName", displayName)
              .add("description", description)
              .toString();
   }

   /**
    * The unique id or an empty string if none.
    */
   public final String id;

   /**
    * The author name. Never {@code null} or empty.
    */
   public final String name;

   /**
    * The author email or an empty string if none.
    */
   public final String email;

   /**
    * The author link or an empty string if none.
    */
   public final String link;

   /**
    * An author name for display.
    */
   public final String displayName;

   /**
    * A description for the author.
    */
   public final String description;

}
