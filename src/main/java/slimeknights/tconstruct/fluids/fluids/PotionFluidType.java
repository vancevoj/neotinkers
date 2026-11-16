package slimeknights.tconstruct.fluids.fluids;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  /** Gets the potion contents stored on the fluid stack, or empty if none */
  private static PotionContents getContents(FluidStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    return contents != null ? contents : PotionContents.EMPTY;
  }

  /**
   * Gets the translation key for the given potion contents, matching {@link PotionContents#getName(String)}.
   * Replaces the removed {@code Potion#getName(Optional, String)}.
   * @param contents  Potion contents
   * @param prefix    Translation key prefix
   * @return  Translation key
   */
  public static String getPotionKey(PotionContents contents, String prefix) {
    return prefix + contents.customName().or(() -> contents.potion().map(potion -> potion.value().name())).orElse("empty");
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    return getPotionKey(getContents(stack), "item.minecraft.potion.effect.");
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucket());
    PotionContents contents = fluidStack.get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      itemStack.set(DataComponents.POTION_CONTENTS, contents);
    }
    return itemStack;
  }

  // client: the potion tint and client extension are registered in FluidClientEvents, as initializeClient no longer exists

  /** Creates the legacy potion tag, used for datagen serialization of potion fluid outputs */
  private static CompoundTag potionTag(Identifier location) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Potion", location.toString());
    return tag;
  }

  /** Creates a component patch holding the given potion contents */
  private static DataComponentPatch potionPatch(Holder<Potion> potion) {
    return DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(potion)).build();
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    return potionFluid(BuiltInRegistries.POTION.getOrThrow(potion), size);
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(Holder<Potion> potion, int size) {
    return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(TinkerFluids.potion.get()), size, potionPatch(potion));
  }

  /** Creates a fluid output for the given potion */
  public static FluidOutput potionResult(Holder<Potion> potion, int size) {
    CompoundTag tag = potionTag(potion.unwrapKey().map(ResourceKey::identifier).orElseThrow());
    return FluidOutput.fromTag(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, tag);
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    return potionBucket(BuiltInRegistries.POTION.getOrThrow(potion));
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(Holder<Potion> potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
    return stack;
  }

  /** Reads the potion contents stored on the fluid stack, or null if none. Used by potion casting recipes. */
  @Nullable
  public static Holder<Potion> getPotion(FluidStack stack) {
    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
    if (contents != null) {
      Optional<Holder<Potion>> potion = contents.potion();
      if (potion.isPresent()) {
        return potion.get();
      }
    }
    return null;
  }
}
