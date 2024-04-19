package slimeknights.tconstruct.library.tools.definition;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.mantle.registration.object.IdAwareObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;

/**
 * This class serves primarily as a container where the datapack tool data will be injected on datapack load
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ToolDefinition {
  /** Empty tool definition instance to prevent the need for null for a fallback */
  public static final ToolDefinition EMPTY = new ToolDefinition(TConstruct.getResource("empty"), 0);

  @Getter
  private final ResourceLocation id;
  /** Max tier to pull materials from if uninitialized */
  @Getter
  private final int defaultMaxTier;
  /** Base data loaded from JSON, contains stats, traits, and starting slots */
  @Getter
  protected ToolDefinitionData data = ToolDefinitionData.EMPTY;

  /**
   * Creates an tool definition builder
   * @param id  Tool definition ID
   * @return Definition builder
   */
  public static ToolDefinition.Builder builder(ResourceLocation id) {
    return new Builder(id);
  }

  /**
   * Creates an tool definition builder
   * @param item  Tool item
   * @return Definition builder
   */
  public static ToolDefinition.Builder builder(RegistryObject<? extends ItemLike> item) {
    return builder(item.getId());
  }

  /**
   * Creates an tool definition builder
   * @param item  Tool item
   * @return Definition builder
   */
  public static ToolDefinition.Builder builder(IdAwareObject item) {
    return builder(item.getId());
  }

  /** Gets the given module from the tool */
  public <T> T getHook(ModifierHook<T> hook) {
    return data.getHook(hook);
  }

  /** Checks if the tool uses multipart stats */
  public boolean hasMaterials() {
    return !data.getHook(ToolHooks.TOOL_MATERIALS).getStatTypes(this).isEmpty();
  }


  /* Loader methods */

  /** Updates the data in this tool definition from the JSON loader, should not be called directly other than by the loader */
  @VisibleForTesting
  public void setData(ToolDefinitionData data) {
    this.data = data;
  }

  /** Sets the data back to empty */
  protected void clearData() {
    setData(ToolDefinitionData.EMPTY);
  }

  /** If true, the definition data is loaded from the datapack, so we can expect it to be reliable. False typically means datapacks are not yet loaded (e.g. menu startup) */
  public boolean isDataLoaded() {
    return data != ToolDefinitionData.EMPTY;
  }

	/** Builder to easily create a tool definition */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Builder {
    /** ID for loading the tool definition data from datapacks */
    private final ResourceLocation id;
    /** If true, registers the material with the tool definition data loader */
    private boolean register = true;
    /** Max tier to choose from for initializing tools with no materials, unused for non-multipart tools */
    @Setter @Accessors(chain = true)
    private int defaultMaxTier = 1;

    /** Tells the definition to not be registered with the loader, used internally for testing. In general mods wont need this */
    public Builder skipRegister() {
      register = false;
      return this;
    }

    /**
     * Builds the final tool definition
     * @return  Tool definition
     */
    public ToolDefinition build() {
      ToolDefinition definition = new ToolDefinition(id, defaultMaxTier);
      if (register) {
        ToolDefinitionLoader.getInstance().registerToolDefinition(definition);
      }
      return definition;
    }
  }
}
