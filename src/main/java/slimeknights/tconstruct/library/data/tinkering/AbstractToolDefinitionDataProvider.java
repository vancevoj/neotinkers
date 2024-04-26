package slimeknights.tconstruct.library.data.tinkering;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.GenericDataProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionData;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionDataBuilder;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionLoader;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.item.ArmorSlotType.ArmorBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Base datagenerator to generate tool definition data */
public abstract class AbstractToolDefinitionDataProvider extends GenericDataProvider {
  private final Map<ResourceLocation,ToolDefinitionDataBuilder> allTools = new HashMap<>();
  /** Mod ID to filter definitions we care about */
  private final String modId;

  public AbstractToolDefinitionDataProvider(DataGenerator generator, String modId) {
    super(generator, PackType.SERVER_DATA, ToolDefinitionLoader.FOLDER);
    this.modId = modId;
  }

  /**
   * Function to add all relevant tool definitions
   */
  protected abstract void addToolDefinitions();

  /** Defines the given ID as a tool definition */
  protected ToolDefinitionDataBuilder define(ResourceLocation id) {
    return allTools.computeIfAbsent(id, i -> ToolDefinitionDataBuilder.builder());
  }

  /** Defines the given ID as a tool definition */
  protected ToolDefinitionDataBuilder define(ItemLike item) {
    return define(Registry.ITEM.getKey(item.asItem()));
  }

  /** Defines the given ID as a tool definition */
  protected ToolDefinitionDataBuilder define(ToolDefinition definition) {
    return define(definition.getId());
  }

  /** Defines an armor data builder */
  protected ArmorDataBuilder defineArmor(ModifiableArmorMaterial armorMaterial) {
    return new ArmorDataBuilder(armorMaterial);
  }

  @Override
  public void run(CachedOutput cache) throws IOException {
    addToolDefinitions();
    Map<ResourceLocation,ToolDefinition> relevantDefinitions = ToolDefinitionLoader.getInstance().getRegisteredToolDefinitions().stream()
                                                                                   .filter(def -> def.getId().getNamespace().equals(modId))
                                                                                   .collect(Collectors.toMap(ToolDefinition::getId, Function.identity()));
    // ensure all required definitions are included
    for (ToolDefinition definition : relevantDefinitions.values()) {
      ResourceLocation name = definition.getId();
      if (!allTools.containsKey(name)) {
        throw new IllegalStateException(String.format("Missing tool definition for '%s'", name));
      }
    }
    // ensure all included ones are required, and the built ones are valid
    for (Entry<ResourceLocation,ToolDefinitionDataBuilder> entry : allTools.entrySet()) {
      ResourceLocation id = entry.getKey();
      ToolDefinition definition = relevantDefinitions.get(id);
      if (definition == null) {
        throw new IllegalStateException("Unknown tool definition with ID " + id);
      }
      saveJson(cache, id, ToolDefinitionData.LOADABLE.serialize(entry.getValue().build()));
    }
  }

  /** Builder for an armor material to batch certain hooks */
  @SuppressWarnings("UnusedReturnValue")
  protected class ArmorDataBuilder {
    private final ResourceLocation name;
    private final ToolDefinitionDataBuilder[] builders;
    private final List<ArmorSlotType> slotTypes;
    private ArmorDataBuilder(ModifiableArmorMaterial armorMaterial) {
      this.name = armorMaterial.getId();
      this.builders = new ToolDefinitionDataBuilder[4];
      ImmutableList.Builder<ArmorSlotType> slotTypes = ImmutableList.builder();
      for (ArmorSlotType slotType : ArmorSlotType.values()) {
        ToolDefinition definition = armorMaterial.getArmorDefinition(slotType);
        if (definition != null) {
          this.builders[slotType.getIndex()] = define(definition);
          slotTypes.add(slotType);
        }
      }
      this.slotTypes = slotTypes.build();
    }

    /** Gets the builder for the given slot */
    protected ToolDefinitionDataBuilder getBuilder(ArmorSlotType slotType) {
      ToolDefinitionDataBuilder builder = builders[slotType.getIndex()];
      if (builder == null) {
        throw new IllegalArgumentException("Unsupported slot type " + slotType + " for material " + name);
      }
      return builder;
    }


    /* Modules */

    /** Adds a module to the definition with the given hooks */
    @SafeVarargs
    public final <T extends ToolModule> ArmorDataBuilder module(ArmorSlotType slotType, T module, ModuleHook<? super T>... hooks) {
      getBuilder(slotType).module(module, hooks);
      return this;
    }

    /** Adds a module to the definition */
    public ArmorDataBuilder module(ArmorSlotType slotType, ToolModule module) {
      getBuilder(slotType).module(module);
      return this;
    }

    /** Adds a module to the definition */
    public ArmorDataBuilder module(ArmorSlotType slotType, ToolModule... modules) {
      getBuilder(slotType).module(modules);
      return this;
    }


    /** Adds a module to the definition with the given hooks */
    @SafeVarargs
    public final <T extends ToolModule> ArmorDataBuilder module(T module, ModuleHook<? super T>... hooks) {
      for (ArmorSlotType armorSlot : slotTypes) {
        module(armorSlot, module, hooks);
      }
      return this;
    }

    /** Adds a module to the definition */
    public ArmorDataBuilder module(ToolModule module) {
      for (ArmorSlotType armorSlot : slotTypes) {
        module(armorSlot, module);
      }
      return this;
    }

    /** Adds a module to the definition */
    public ArmorDataBuilder module(ToolModule... modules) {
      for (ArmorSlotType armorSlot : slotTypes) {
        module(armorSlot, modules);
      }
      return this;
    }

    /** Adds modules to the definition using the passed builder */
    @SafeVarargs
    public final <T extends ToolModule> ArmorDataBuilder module(ArmorBuilder<T> builder, ModuleHook<? super T>... hooks) {
      for (ArmorSlotType armorSlot : slotTypes) {
        module(armorSlot, builder.build(armorSlot), hooks);
      }
      return this;
    }

    /** Adds modules to the definition using the passed builder */
    public ArmorDataBuilder module(ArmorBuilder<? extends ToolModule> builder) {
      for (ArmorSlotType armorSlot : slotTypes) {
        module(armorSlot, builder.build(armorSlot));
      }
      return this;
    }

    /** Adds modules to the definition using the passed builder */
    @SafeVarargs
    public final <T extends ToolModule> ArmorDataBuilder modules(Function<List<ArmorSlotType>,ArmorBuilder<T>> constructor, ModuleHook<? super T>... hooks) {
      return module(constructor.apply(slotTypes), hooks);
    }

    /** Adds modules to the definition using the passed builder */
    public ArmorDataBuilder modules(Function<List<ArmorSlotType>,ArmorBuilder<? extends ToolModule>> constructor) {
      return module(constructor.apply(slotTypes));
    }
  }
}
