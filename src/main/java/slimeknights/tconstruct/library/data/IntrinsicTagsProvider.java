package slimeknights.tconstruct.library.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Tag provider for built in registries which restores the value based tag appender removed from vanilla.
 * Extends Mantle's {@link BuiltinRegistryTagProvider} with an appender that keeps returning itself after the NeoForge extension methods
 * ({@code addTags}, {@code addOptionalTags}, {@code replace()}, varargs {@code remove}) and accepts IDs for optional entries, so chains such as
 * {@code tag(x).addTags(a, b).add(block)} keep working.
 * <p>
 * The nested {@link IntrinsicTagAppender} intentionally hides Mantle's class of the same name for subclasses.
 * @param <T>  Registry type
 */
public abstract class IntrinsicTagsProvider<T> extends BuiltinRegistryTagProvider<T> {
  private final Function<T,ResourceKey<T>> keyExtractor;

  public IntrinsicTagsProvider(PackOutput packOutput, Registry<T> registry, CompletableFuture<Provider> lookupProvider, String modId) {
    super(packOutput, registry, lookupProvider, modId);
    this.keyExtractor = value -> registry.getResourceKey(value).orElseThrow(() -> new IllegalArgumentException("Value " + value + " is not registered in " + registry.key().identifier()));
  }

  /** Gets the key for the given value */
  protected ResourceKey<T> key(T value) {
    return keyExtractor.apply(value);
  }

  @Override
  protected IntrinsicTagAppender<T> tag(TagKey<T> tag) {
    return new IntrinsicTagAppender<>(TagAppender.forBuilder(this.getOrCreateRawBuilder(tag)), keyExtractor, registryKey);
  }

  @Override
  protected IntrinsicTagAppender<T> tag(TagKey<T> tag, boolean replace) {
    TagBuilder builder = this.getOrCreateRawBuilder(tag);
    builder.setReplace(replace);
    return new IntrinsicTagAppender<>(TagAppender.forBuilder(builder), keyExtractor, registryKey);
  }

  /** Tag appender supporting registry values, keys, and IDs, which returns itself from all builder methods */
  @SuppressWarnings({"UnusedReturnValue", "unused"})  // API
  public static class IntrinsicTagAppender<T> extends BuiltinRegistryTagProvider.IntrinsicTagAppender<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;

    protected IntrinsicTagAppender(TagAppender<T> parent, Function<T,ResourceKey<T>> keyExtractor, ResourceKey<? extends Registry<T>> registryKey) {
      super(parent, keyExtractor);
      this.registryKey = registryKey;
    }

    /** Creates a key in this registry */
    private ResourceKey<T> key(Identifier id) {
      return ResourceKey.create(registryKey, id);
    }

    /** Creates a tag key in this registry */
    private TagKey<T> tagKey(Identifier id) {
      return TagKey.create(registryKey, id);
    }

    /* Value methods */

    @Override
    public IntrinsicTagAppender<T> add(T value) {
      super.add(value);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> remove(T value) {
      super.remove(value);
      return this;
    }

    /* ID methods */

    /** Adds the given IDs to the tag as required elements */
    public IntrinsicTagAppender<T> addId(Identifier... ids) {
      for (Identifier id : ids) {
        add(key(id));
      }
      return this;
    }

    /** Adds the given IDs to the tag as optional elements */
    public IntrinsicTagAppender<T> addOptional(Identifier... ids) {
      for (Identifier id : ids) {
        addOptional(key(id));
      }
      return this;
    }

    /** Adds the given tag IDs to the tag as optional tags */
    public IntrinsicTagAppender<T> addOptionalTag(Identifier... tags) {
      for (Identifier tag : tags) {
        addOptionalTag(tagKey(tag));
      }
      return this;
    }

    /** Removes the given ID from the tag */
    public IntrinsicTagAppender<T> remove(Identifier id) {
      return remove(key(id));
    }

    /* Key methods, overridden for the return type */

    @Override
    public IntrinsicTagAppender<T> add(ResourceKey<T> element) {
      super.add(element);
      return this;
    }

    @SafeVarargs
    @Override
    public final IntrinsicTagAppender<T> add(ResourceKey<T>... elements) {
      for (ResourceKey<T> element : elements) {
        add(element);
      }
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> addOptional(ResourceKey<T> element) {
      super.addOptional(element);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> addTag(TagKey<T> tag) {
      super.addTag(tag);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> addOptionalTag(TagKey<T> tag) {
      super.addOptionalTag(tag);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> add(TagEntry entry) {
      super.add(entry);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> replace(boolean value) {
      super.replace(value);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> remove(ResourceKey<T> element) {
      super.remove(element);
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> remove(TagKey<T> tag) {
      super.remove(tag);
      return this;
    }

    /* NeoForge extension methods, overridden for the return type */

    @SafeVarargs
    @Override
    public final IntrinsicTagAppender<T> addTags(TagKey<T>... values) {
      for (TagKey<T> value : values) {
        addTag(value);
      }
      return this;
    }

    @SafeVarargs
    @Override
    public final IntrinsicTagAppender<T> addOptionalTags(TagKey<T>... values) {
      for (TagKey<T> value : values) {
        addOptionalTag(value);
      }
      return this;
    }

    @Override
    public IntrinsicTagAppender<T> replace() {
      return replace(true);
    }

    @SafeVarargs
    @Override
    public final IntrinsicTagAppender<T> remove(ResourceKey<T> first, ResourceKey<T>... others) {
      remove(first);
      for (ResourceKey<T> other : others) {
        remove(other);
      }
      return this;
    }

    @SafeVarargs
    @Override
    public final IntrinsicTagAppender<T> remove(TagKey<T> first, TagKey<T>... others) {
      remove(first);
      for (TagKey<T> other : others) {
        remove(other);
      }
      return this;
    }
  }
}
