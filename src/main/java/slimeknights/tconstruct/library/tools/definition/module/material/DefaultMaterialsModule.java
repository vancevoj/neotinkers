package slimeknights.tconstruct.library.tools.definition.module.material;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.RandomSource;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;

import java.util.List;

/** Module to fill missing materials on a tool */
public record DefaultMaterialsModule(List<RandomMaterial> materials) implements MissingMaterialsToolHook, ToolModule {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.defaultHooks(ToolHooks.MISSING_MATERIALS);
  /** Loader instance */
  public static final RecordLoadable<DefaultMaterialsModule> LOADER = RecordLoadable.create(RandomMaterial.LOADER.list(1).requiredField("materials", m -> m.materials), DefaultMaterialsModule::new);

  @Override
  public RecordLoadable<DefaultMaterialsModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public MaterialNBT fillMaterials(ToolDefinition definition, RandomSource random) {
    return RandomMaterial.build(ToolMaterialHook.stats(definition), materials, random);
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final ImmutableList.Builder<RandomMaterial> materials = ImmutableList.builder();

    private Builder() {}

    /** Adds a material to the builder */
    public Builder material(RandomMaterial material) {
      this.materials.add(material);
      return this;
    }

    /** Adds a material to the builder */
    public Builder material(RandomMaterial... materials) {
      for (RandomMaterial material : materials) {
        material(material);
      }
      return this;
    }

    /** Adds a material to the builder */
    public Builder material(MaterialId material) {
      return material(RandomMaterial.fixed(material));
    }

    /** Adds a material to the builder */
    public Builder firstWithStat() {
      return material(RandomMaterial.firstWithStat());
    }

    /** Builds the final module */
    public DefaultMaterialsModule build() {
      List<RandomMaterial> materials = this.materials.build();
      if (materials.isEmpty()) {
        throw new IllegalArgumentException("Must have at least 1 material");
      }
      return new DefaultMaterialsModule(materials);
    }
  }
}
