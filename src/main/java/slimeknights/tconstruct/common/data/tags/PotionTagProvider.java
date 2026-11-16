package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.Potion;
import slimeknights.mantle.data.BuiltinRegistryTagProvider;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;

import java.util.concurrent.CompletableFuture;

public class PotionTagProvider extends BuiltinRegistryTagProvider<Potion> {
  public PotionTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider) {
    super(packOutput, BuiltInRegistries.POTION, lookupProvider, TConstruct.MOD_ID);
  }

  @Override
  protected void addTags(Provider provider) {
    tag(TinkerTags.Potions.HIDDEN_FLUID).addOptional(ResourceKey.create(Registries.POTION, TinkerTags.HIDDEN_FROM_RECIPE_VIEWERS));
  }

  @Override
  public String getName() {
    return "Neo Tinkers Potion Tags";
  }
}
