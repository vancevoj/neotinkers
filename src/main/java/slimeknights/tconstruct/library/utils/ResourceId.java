package slimeknights.tconstruct.library.utils;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Helper for use with our extensions of resource location for some type safety in IDs.
 * Note we left {@link Identifier#withPath(String)} and alike as returning {@link Identifier} as there is not much use extending an ID.
 * @see IdParser
 */
public abstract class ResourceId extends Identifier {
  /**
   * Marker type replacing the vanilla {@code Identifier.Dummy} that backed the validation-skipping constructor in
   * 1.20. In 1.21 {@link Identifier} is a final class with a single private {@code (String, String)} constructor;
   * that constructor is widened to public via the access transformer and {@link Identifier} is de-finalized so we
   * may still subclass it. Subclasses inherit this marker and keep their {@code (String, String, Dummy)} constructors
   * unchanged.
   */
  protected static final class Dummy {
    private Dummy() {}
  }

  protected ResourceId(String namespace, String path, @Nullable Dummy pDummy) {
    super(namespace, path);
  }

  public ResourceId(Identifier location) {
    super(location.getNamespace(), location.getPath());
  }

  @SuppressWarnings("removal")
  public ResourceId(String namespace, String path) {
    super(namespace, path);
  }

  @SuppressWarnings("removal")
  public ResourceId(String location) {
    this(parse(location));
  }


  /* Helpers for static constructors */

  /**
   * Creates a new ID from the given string
   * @param string  String
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends Identifier> T tryParse(String string, BiFunction<String,String,T> constructor) {
    String[] parts = IdParser.decompose(DEFAULT_NAMESPACE, string, ':');
    return tryBuild(parts[0], parts[1], constructor);
  }

  /**
   * Creates a new ID from the given namespace and path
   * @param namespace  Namespace
   * @param path       Path
   * @return  ID, or null if invalid
   */
  @Nullable
  protected static <T extends Identifier> T tryBuild(String namespace, String path, BiFunction<String,String,T> constructor) {
    if (isValidNamespace(namespace) && isValidPath(path)) {
      return constructor.apply(namespace, path);
    }
    return null;
  }
}
