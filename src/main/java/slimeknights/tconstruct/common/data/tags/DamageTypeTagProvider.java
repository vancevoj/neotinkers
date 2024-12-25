package slimeknights.tconstruct.common.data.tags;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.TConstruct;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.tags.DamageTypeTags.AVOIDS_GUARDIAN_THORNS;
import static net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR;
import static net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS;
import static net.minecraft.tags.DamageTypeTags.IS_EXPLOSION;
import static net.minecraft.tags.DamageTypeTags.IS_FIRE;
import static net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO;
import static slimeknights.tconstruct.common.TinkerDamageTypes.BLEEDING;
import static slimeknights.tconstruct.common.TinkerDamageTypes.PIERCING;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SELF_DESTRUCT;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_HEAT;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_MAGIC;

public class DamageTypeTagProvider extends DamageTypeTagsProvider {
  public DamageTypeTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookup, @Nullable ExistingFileHelper existingFileHelper) {
    super(packOutput, lookup, TConstruct.MOD_ID, existingFileHelper);
  }

  @Override
  protected void addTags(Provider pProvider) {
    tag(IS_FIRE).add(SMELTERY_HEAT);
    tag(IS_EXPLOSION).add(SELF_DESTRUCT);
    tag(WITCH_RESISTANT_TO).add(SMELTERY_MAGIC);
    tag(BYPASSES_ARMOR).add(PIERCING, SELF_DESTRUCT, BLEEDING);
    tag(BYPASSES_ENCHANTMENTS).add(BLEEDING);
    tag(AVOIDS_GUARDIAN_THORNS).add(BLEEDING);
  }
}
