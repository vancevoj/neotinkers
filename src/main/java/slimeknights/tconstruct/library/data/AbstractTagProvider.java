package slimeknights.tconstruct.library.data;

import com.google.common.collect.Maps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.Target;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.data.GenericDataProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic class for generating tags at any location even for non-registries.
 * TODO: make updates based on {@link net.minecraft.data.tags.TagsProvider} changes, if any.
 */
public abstract class AbstractTagProvider<T> extends GenericDataProvider {
  /** Mod ID for the tags */
  private final String modId;
  /** Predicate to validate non-optional values. If the contents only exist in datapacks, they should be defined as optional */
  private final Predicate<Identifier> staticValuePredicate;
  /** Function to get a key from a value */
  private final Function<T,Identifier> keyGetter;
  /** Checks for tags in other datapacks, from {@link net.neoforged.neoforge.data.event.GatherDataEvent#getResourceManager(net.minecraft.server.packs.PackType)} with {@code SERVER_DATA}. If null, required references to other datapacks are considered missing. */
  @Nullable
  protected final ResourceManager resourceManager;
  /** Folder containing the tags, used to find tags in other datapacks */
  private final String folder;

  protected final Map<Identifier, TagBuilder> builders = Maps.newLinkedHashMap();

  protected AbstractTagProvider(PackOutput packOutput, String modId, String folder, Function<T,Identifier> keyGetter, Predicate<Identifier> staticValuePredicate, @Nullable ResourceManager resourceManager) {
    super(packOutput, Target.DATA_PACK, folder);
    this.modId = modId;
    this.keyGetter = keyGetter;
    this.staticValuePredicate = staticValuePredicate;
    this.resourceManager = resourceManager;
    this.folder = folder;
  }

  /** Creates all tag instances */
  protected abstract void addTags();

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    this.builders.clear();
    this.addTags();
    return allOf(this.builders.entrySet().stream().map(entry -> {
      List<TagEntry> tagEntries = entry.getValue().build();
      List<TagEntry> invalidEntries = tagEntries.stream()
                                                .filter((value) -> !value.verifyIfPresent(staticValuePredicate, this.builders::containsKey))
                                                .filter(this::missing)
                                                .toList();
      Identifier id = entry.getKey();
      if (!invalidEntries.isEmpty()) {
        return CompletableFuture.failedFuture(new IllegalArgumentException(String.format("Couldn't define tag %s as it is missing following references: %s", id, invalidEntries.stream().map(Objects::toString).collect(Collectors.joining(",")))));
      } else {
        TagBuilder builder = entry.getValue();
        return saveJson(cache, id, TagFile.CODEC, new TagFile(tagEntries, builder.shouldReplace(), builder.getRemoveEntries().toList()));
      }
    }));
  }

  /** Checks if a given reference exists in another data pack */
  private boolean missing(TagEntry reference) {
    if (reference.isRequired()) {
      // forge has a separate element resource type here to allow generating tags to non-static values. We don't currently handle non-static tag value validation but its worth considering
      return resourceManager == null || resourceManager.getResource(reference.getId().withPath(path -> folder + '/' + path + ".json")).isEmpty();
    }
    return false;
  }


  /* Make builders */

  /** Prepares a tag builder */
  protected TagAppender<T> tag(TagKey<T> pTag) {
    return new TagAppender<>(modId, this.getOrCreateRawBuilder(pTag), keyGetter);
  }

  /** Raw method to make a builder */
  protected TagBuilder getOrCreateRawBuilder(TagKey<T> pTag) {
    return this.builders.computeIfAbsent(pTag.location(), location -> TagBuilder.create());
  }

  /** Vanillas tag appender does not let us easily replace the key getter, so replace it */
  @SuppressWarnings({"UnusedReturnValue", "unused"})  // API
  public record TagAppender<T>(String modID, TagBuilder internalBuilder, Function<T,Identifier> keyGetter) {
    /** Adds a value to the tag */
    public TagAppender<T> add(T value) {
      this.internalBuilder.addElement(keyGetter.apply(value));
      return this;
    }

    /** Adds a list of values to the tag */
    @SafeVarargs
    public final TagAppender<T> add(T... values) {
      Stream.of(values).map(keyGetter).forEach(this.internalBuilder::addElement);
      return this;
    }

    /** Adds a resource location to the tag */
    public TagAppender<T> add(Identifier... ids) {
      for (Identifier id : ids) {
        this.internalBuilder.addElement(id);
      }
      return this;
    }

    /** Adds an optional ID to the tag */
    public TagAppender<T> addOptional(Identifier... ids) {
      for (Identifier id : ids) {
        this.internalBuilder.addOptionalElement(id);
      }
      return this;
    }

    /** Adds an tag to the tag */
    @SafeVarargs
    public final TagAppender<T> addTag(TagKey<T>... tags) {
      for (TagKey<T> tag : tags) {
        this.internalBuilder.addTag(tag.location());
      }
      return this;
    }

    /** Adds an optional tag to the tag */
    public TagAppender<T> addOptionalTag(Identifier... tags) {
      for (Identifier tag : tags) {
        this.internalBuilder.addOptionalTag(tag);
      }
      return this;
    }


    /* Forge methods */

    /** Sets the tag to replace */
    public TagAppender<T> replace() {
      return replace(true);
    }

    /** Sets the tag to replace */
    public TagAppender<T> replace(boolean value) {
      internalBuilder.setReplace(value);
      return this;
    }

    /**
     * Adds a registry entry to the tag json's remove list. Callable during datageneration.
     * @param entry The entry to remove
     * @return The builder for chaining
     */
    public TagAppender<T> remove(final T entry) {
      return remove(keyGetter.apply(entry));
    }

    /**
     * Adds multiple registry entries to the tag json's remove list. Callable during datageneration.
     * @param entries The entries to remove
     * @return The builder for chaining
     */
    @SafeVarargs
    public final TagAppender<T> remove(T first, T... entries) {
      this.remove(first);
      for (T entry : entries) {
        this.remove(entry);
      }
      return this;
    }

    /**
     * Adds a single element's ID to the tag json's remove list. Callable during datageneration.
     * @param location The ID of the element to remove
     * @return The builder for chaining
     */
    public TagAppender<T> remove(Identifier location) {
      internalBuilder.removeElement(location);
      return this;
    }

    /**
     * Adds multiple elements' IDs to the tag json's remove list. Callable during datageneration.
     * @param locations The IDs of the elements to remove
     * @return The builder for chaining
     */
    public TagAppender<T> remove(Identifier first, Identifier... locations) {
      this.remove(first);
      for (Identifier location : locations) {
        this.remove(location);
      }
      return this;
    }

    /**
     * Adds a tag to the tag json's remove list. Callable during datageneration.
     * @param tag The ID of the tag to remove
     * @return The builder for chaining
     */
    public TagAppender<T> remove(TagKey<T> tag) {
      internalBuilder.removeTag(tag.location());
      return this;
    }

    /**
     * Adds multiple tags to the tag json's remove list. Callable during datageneration.
     * @param tags The IDs of the tags to remove
     * @return The builder for chaining
     */
    @SafeVarargs
    public final TagAppender<T> remove(TagKey<T> first, TagKey<T>... tags) {
      this.remove(first);
      for (TagKey<T> tag : tags) {
        this.remove(tag);
      }
      return this;
    }
  }
}
