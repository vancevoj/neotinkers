package slimeknights.tconstruct.library.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.registration.object.IdAwareObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;

/** Class implementing a modifier hook, used as a key for {@link ModuleHookMap )} */
@RequiredArgsConstructor
public class ModuleHook<T> implements IdAwareObject {
  /** Unique name of this hook, used for serialization */
  @Getter
  private final ResourceLocation id;
  /** Filter to check if an object is valid for this hook */
  private final Class<T> filter;
  /** Logic to merge multiple instances into a single instance */
  @Nullable
  private final Function<Collection<T>,T> merger;
  /** Default instance for when a modifier does not implement this hook */
  @Getter
  private final T defaultInstance;

  public ModuleHook(ResourceLocation name, Class<T> filter, T defaultInstance) {
    this(name, filter, null, defaultInstance);
  }

  /** checks if the given module can be used for this hook */
  public boolean isValid(Object module) {
    return filter.isInstance(module);
  }

  /** Checks if the given class */
  public boolean supportsHook(Class<?> classType) {
    return classType.isAssignableFrom(filter);
  }

  /** Unchecked cast of the module to this hook type. Use only if certain the module type and hook type are the same */
  @SuppressWarnings("unchecked")
  public T cast(Object module) {
    return (T) module;
  }

  /** Returns true if this hook supports merging */
  public boolean canMerge() {
    return merger != null;
  }

  /** Merges the given modifiers into a single instance. Only supported if {@link #canMerge()} returns true */
  public T merge(Collection<T> modules) {
    if (modules.isEmpty()) {
      return defaultInstance;
    }
    if (modules.size() == 1) {
      return modules.iterator().next();
    }
    if (merger == null) {
      throw new IllegalStateException(id + " does not support merging");
    }
    return merger.apply(modules);
  }

  @Override
  public String toString() {
    return "ModifierHook{" + id + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleHook<?> that = (ModuleHook<?>)o;
    return this.id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
