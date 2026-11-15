package slimeknights.tconstruct.library.tools.part;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.utils.DomainDisplayName;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an item that has a Material associated with it. The NBT of the itemstack identifies which material the
 * itemstack of this item has.
 */
public class MaterialItem extends Item implements IMaterialItem {
  private static final String ADDED_BY = TConstruct.makeTranslationKey("tooltip", "part.added_by");

  public MaterialItem(Properties properties) {
    super(properties);
  }

  /** Gets the material ID for the given NBT compound */
  public static MaterialVariantId getMaterialId(@Nullable CompoundTag nbt) {
    if (nbt != null) {
      String str = nbt.getStringOr(MATERIAL_TAG, "");
      if (!str.isEmpty()) {
        MaterialVariantId id = MaterialVariantId.tryParse(str);
        if (id != null) {
          return id;
        }
      }
    }
    return MaterialId.UNKNOWN;
  }

  /**
   * Resolves material redirects on the given material.
   * Items used to update their stored material when loaded, but 26.3 has no load hook for item components, so redirects are resolved when read instead.
   */
  public static MaterialVariantId resolveRedirects(MaterialVariantId material) {
    if (!material.equals(MaterialId.UNKNOWN) && MaterialRegistry.isFullyLoaded()) {
      MaterialId original = material.getId();
      MaterialId resolved = MaterialRegistry.getInstance().resolve(original);
      if (original != resolved) {
        return MaterialVariantId.create(resolved, material.getVariant());
      }
    }
    return material;
  }

  /** Gets the material stored in the custom data of the given stack, with redirects resolved. Used by {@link #getMaterial(ItemStack)} and {@link slimeknights.tconstruct.library.tools.part.block.MaterialBlockItem} */
  public static MaterialVariantId getMaterialFromCustomData(ItemStack stack) {
    // 1.21: read the material string from the CUSTOM_DATA component compound instead of item NBT
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return resolveRedirects(getMaterialId(data == null ? null : data.copyTag()));
  }

  @Override
  public MaterialVariantId getMaterial(ItemStack stack) {
    return getMaterialFromCustomData(stack);
  }

  @Nullable
  private static Component getName(String baseKey, MaterialVariantId material) {
    // if there is a specific name, use that
    Identifier location = material.getLocation('.');
    String fullKey = String.format("%s.%s.%s", baseKey, location.getNamespace(), location.getPath());
    if (Util.canTranslate(fullKey)) {
      return Component.translatable(fullKey);
    }
    // try material name prefix next
    String materialKey = MaterialTooltipCache.getKey(material);
    String materialPrefix = materialKey + ".format";
    if (Util.canTranslate(materialPrefix)) {
      return Component.translatable(materialPrefix, Component.translatable(baseKey));
    }
    // format as "<material> <item name>"
    if (Util.canTranslate(materialKey)) {
      return Component.translatable(TooltipUtil.KEY_FORMAT, Component.translatable(materialKey), Component.translatable(baseKey));
    }
    return null;
  }

  /** Static helper to get part name, used also by {@link slimeknights.tconstruct.library.tools.part.block.MaterialBlockItem} */
  public static Component getName(IMaterialItem self, ItemStack stack) {
    // if no material, return part name directly
    MaterialVariantId material = self.getMaterial(stack);
    // 26.3: the stack sensitive description ID is gone, the item's description ID is final
    String key = self.asItem().getDescriptionId();
    if (material.equals(MaterialId.UNKNOWN)) {
      return Component.translatable(key);
    }
    // try variant first
    if (material.hasVariant()) {
      Component component = getName(key, material);
      if (component != null) {
        return component;
      }
    }
    // if variant did not work, do base material
    Component component = getName(key, material.getId());
    if (component != null) {
      return component;
    }
    // if neither worked, format directly
    return Component.translatable(key);
  }

  @Override
  public Component getName(ItemStack stack) {
    return withRarity(getName(this, stack), getRarity(stack));
  }

  /** Gets the rarity of the material on this stack */
  public Rarity getRarity(ItemStack stack) {
    return MaterialRegistry.getMaterial(getMaterial(stack).getId()).getRarity();
  }

  /** 1.21: Item#getRarity(ItemStack) was removed in favor of the static rarity component, so style the name with the material rarity directly */
  public static Component withRarity(Component name, Rarity rarity) {
    if (rarity != Rarity.COMMON) {
      return name.copy().withStyle(rarity.getStyleModifier());
    }
    return name;
  }

  @Override
  public boolean isFoil(ItemStack stack) {
    return MaterialRegistry.getInstance().isInTag(getMaterial(stack).getId(), TinkerTags.Materials.SHINY);
  }

  /** Helper to append the material ID to the tool part. */
  public static void appendHoverText(IMaterialItem self, ItemStack stack, Consumer<Component> tooltip, TooltipFlag flag) {
    if (flag.isAdvanced() && !TooltipUtil.isDisplay(stack)) {
      MaterialVariantId materialVariant = self.getMaterial(stack);
      if (!materialVariant.equals(MaterialId.UNKNOWN)) {
        tooltip.accept((Component.translatable(ToolPartItem.MATERIAL_KEY, materialVariant.toString())).withStyle(ChatFormatting.DARK_GRAY));
      }
    }
  }

  /** Helper to append the material ID to the tool part. */
  public static void appendHoverText(IMaterialItem self, ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
    appendHoverText(self, stack, (Consumer<Component>) tooltip::add, flag);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    appendHoverText(this, stack, tooltip, flag);
  }

  /** Gets the creator mod ID based on the material. */
  @SuppressWarnings("deprecation")  // deprecation? more like not deprecation
  public static String getCreatorModId(IMaterialItem self, ItemStack stack) {
    MaterialVariantId material = self.getMaterial(stack);
    if (!MaterialId.UNKNOWN.equals(material)) {
      String namespace = material.getId().getNamespace();
      // skip if it's a tinkers material; we want addon tool parts to prefer showing their mod ID as end users mistake those for us
      if (!TConstruct.MOD_ID.equals(namespace)) {
        return namespace;
      }
    }
    return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
  }

  @Override
  public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
    return getCreatorModId(this, stack);
  }


  /**
   * Adds the mod that added the material to the tooltip
   * @param tooltip   Tooltip list
   * @param material  Material to add
   */
  @Deprecated(forRemoval = true)
  protected static void addModTooltip(IMaterial material, List<Component> tooltip) {
    if (material != IMaterial.UNKNOWN) {
      tooltip.add(Component.empty());
      tooltip.add(Component.translatable(ADDED_BY, DomainDisplayName.nameFor(material.getIdentifier().getNamespace())));
    }
  }

  /** Updates material redirects in the given custom data compound */
  public static void verifyTag(CompoundTag nbt) {
    // if the material exists and was changed, update it
    MaterialVariantId id = getMaterialId(nbt);
    if (!id.equals(MaterialId.UNKNOWN)) {
      MaterialId original = id.getId();
      MaterialId resolved = MaterialRegistry.getInstance().resolve(original);
      if (original != resolved) {
        nbt.putString(MATERIAL_TAG, MaterialVariantId.create(resolved, id.getVariant()).toString());
      }
    }
  }

  // 26.3: Item#verifyComponentsAfterLoad is gone, so renamed materials are remapped when read in getMaterial, see resolveRedirects
}
