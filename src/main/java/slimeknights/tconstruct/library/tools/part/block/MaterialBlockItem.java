package slimeknights.tconstruct.library.tools.part.block;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.library.tools.part.MaterialItem;

import java.util.function.Consumer;

/** Implementation of {@link MaterialItem} on a {@link BlockItem}. */
public class MaterialBlockItem extends BlockItem implements IMaterialItem {
  public MaterialBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public MaterialVariantId getMaterial(ItemStack stack) {
    // 1.21: read the material string from the CUSTOM_DATA component compound instead of item NBT
    // 26.3: also resolves material redirects, as items no longer have a load hook to update them
    return MaterialItem.getMaterialFromCustomData(stack);
  }

  @Override
  public Component getName(ItemStack stack) {
    return MaterialItem.withRarity(MaterialItem.getName(this, stack), getRarity(stack));
  }

  /** Gets the rarity of the material on this stack */
  public Rarity getRarity(ItemStack stack) {
    return MaterialRegistry.getMaterial(getMaterial(stack).getId()).getRarity();
  }

  @Override
  public boolean isFoil(ItemStack stack) {
    return MaterialRegistry.getInstance().isInTag(getMaterial(stack).getId(), TinkerTags.Materials.SHINY);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    MaterialItem.appendHoverText(this, stack, tooltip, flag);
    super.appendHoverText(stack, context, display, tooltip, flag);
  }

  @Override
  public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
    return MaterialItem.getCreatorModId(this, stack);
  }

  // 26.3: Item#verifyComponentsAfterLoad is gone, renamed materials are remapped when read in getMaterial
}
