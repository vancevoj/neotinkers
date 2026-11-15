package slimeknights.tconstruct.common.data.tags;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.world.TinkerWorld;

import java.util.concurrent.CompletableFuture;

import static slimeknights.mantle.Mantle.commonResource;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.COLLECTABLES;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.DISCARDABLE_COLLECTABLES;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.NECROTIC_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.REFLECTING_BLACKLIST;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.REFLECTING_PRESERVE_OWNER;
import static slimeknights.tconstruct.common.TinkerTags.EntityTypes.TRIDENTS;

@SuppressWarnings("unchecked")
public class EntityTypeTagProvider extends EntityTypeTagsProvider {

  public EntityTypeTagProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
    super(packOutput, lookupProvider, TConstruct.MOD_ID, existingFileHelper);
  }

  @SuppressWarnings("removal")
  @Override
  protected void addTags(Provider provider) {
    // mob classes
    this.tag(TinkerTags.EntityTypes.SLIMES).add(
      EntityTypes.SLIME, EntityTypes.MAGMA_CUBE,
      TinkerWorld.skySlimeEntity.get(), TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get()
    );
    // hostile
    this.tag(TinkerTags.EntityTypes.CREEPERS).add(EntityTypes.CREEPER);
    this.tag(TinkerTags.EntityTypes.SPIDERS).add(EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER);
    this.tag(TinkerTags.EntityTypes.GUARDIANS).add(EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN);
    this.tag(TinkerTags.EntityTypes.SILVERFISH).add(EntityTypes.SILVERFISH);
    this.tag(TinkerTags.EntityTypes.BLAZES).add(EntityTypes.BLAZE);
    this.tag(TinkerTags.EntityTypes.GHASTS).add(EntityTypes.GHAST);
    this.tag(TinkerTags.EntityTypes.PHANTOMS).add(EntityTypes.PHANTOM);
    this.tag(TinkerTags.EntityTypes.SHULKERS).add(EntityTypes.SHULKER);
    // passive
    this.tag(TinkerTags.EntityTypes.AXOLOTLS).add(EntityTypes.AXOLOTL);
    this.tag(TinkerTags.EntityTypes.BEES).add(EntityTypes.BEE);
    this.tag(TinkerTags.EntityTypes.FROGS).add(EntityTypes.FROG);
    this.tag(TinkerTags.EntityTypes.SQUIDS).add(EntityTypes.SQUID);
    this.tag(TinkerTags.EntityTypes.STRIDERS).add(EntityTypes.STRIDER);
    this.tag(TinkerTags.EntityTypes.TURTLES).add(EntityTypes.TURTLE);
    // villager
    this.tag(TinkerTags.EntityTypes.VILLAGERS).add(EntityTypes.VILLAGER, EntityTypes.WANDERING_TRADER, EntityTypes.ZOMBIE_VILLAGER);
    this.tag(TinkerTags.EntityTypes.ILLAGERS).add(EntityTypes.EVOKER, EntityTypes.ILLUSIONER, EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.WITCH);
    this.tag(TinkerTags.EntityTypes.PIGLINS).add(EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.ZOMBIFIED_PIGLIN);

    // melting
    this.tag(TinkerTags.EntityTypes.MELTING_SHOW).add(EntityTypes.IRON_GOLEM, EntityTypes.SNOW_GOLEM, EntityTypes.VILLAGER, EntityTypes.PLAYER);
    this.tag(TinkerTags.EntityTypes.MELTING_HIDE)
      .add(EntityTypes.GIANT)
      .addTag(TinkerTags.EntityTypes.MELTING_BLACKLIST)
      .addOptionalTag(TinkerTags.HIDDEN_FROM_RECIPE_VIEWERS);
    this.tag(TinkerTags.EntityTypes.MELTING_BLACKLIST);

    // meltable
    this.tag(TinkerTags.EntityTypes.MELTABLE_FARM_ANIMALS).add(
      EntityTypes.CHICKEN, EntityTypes.RABBIT,
      EntityTypes.COW, EntityTypes.MOOSHROOM,
      EntityTypes.PIG, EntityTypes.HOGLIN,
      EntityTypes.SHEEP, EntityTypes.GOAT,
      EntityTypes.COD, EntityTypes.SALMON, EntityTypes.TROPICAL_FISH);
    this.tag(TinkerTags.EntityTypes.MELTABLE_ZOMBIE).add(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.ZOMBIE_HORSE);
    this.tag(TinkerTags.EntityTypes.MELTABLE_DROWNED).add(EntityTypes.DROWNED);
    this.tag(TinkerTags.EntityTypes.MELTABLE_SKELETON).addTag(EntityTypeTags.SKELETONS).add(EntityTypes.SKELETON_HORSE);
    this.tag(TinkerTags.EntityTypes.MELTABLE_ENDER).add(EntityTypes.ENDERMAN, EntityTypes.ENDERMITE, EntityTypes.ENDER_DRAGON);
    this.tag(TinkerTags.EntityTypes.MELTABLE_SLIME).add(EntityTypes.SLIME);
    this.tag(TinkerTags.EntityTypes.MELTABLE_MAGMA).add(EntityTypes.MAGMA_CUBE);

    // ancient tools
    this.tag(TinkerTags.EntityTypes.SPAWNS_WITH_WAR_PICK).add(EntityTypes.ZOMBIE_VILLAGER, EntityTypes.VINDICATOR);
    this.tag(TinkerTags.EntityTypes.SPAWNS_WITH_MELTING_PAN).add(EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HUSK);
    this.tag(TinkerTags.EntityTypes.SPAWNS_WITH_SWASHER_DROWNED).add(EntityTypes.DROWNED);
    this.tag(TinkerTags.EntityTypes.SPAWNS_WITH_SWASHER_WITHER_SKELETON).add(EntityTypes.WITHER_SKELETON);
    this.tag(TinkerTags.EntityTypes.SPAWNS_WITH_BATTLE_SIGN).addTag(TinkerTags.EntityTypes.PIGLINS);

    // behavior
    this.tag(EntityTypeTags.FROG_FOOD).add(TinkerWorld.skySlimeEntity.get(), TinkerWorld.enderSlimeEntity.get(), TinkerWorld.terracubeEntity.get());
    this.tag(EntityTypeTags.ARROWS).add(TinkerTools.materialArrow.get());

    // compatability
    this.tag(TinkerTags.EntityTypes.BOBBERS).add(TinkerTools.fishingHook.get());

    // tool logic
    // players use tool daamge util
    this.tag(TinkerTags.EntityTypes.DAMAGE_MODIFIER_BLACKLIST).add(EntityTypes.PLAYER);
    this.tag(TinkerTags.EntityTypes.PIGGYBACKPACK_BLACKLIST);
    this.tag(TinkerTags.EntityTypes.SMALL_ARMOR).addTag(TinkerTags.EntityTypes.SLIMES);

    // projectile logic
    this.tag(TRIDENTS).add(EntityTypes.TRIDENT, TinkerTools.thrownTool.get());
    this.tag(TinkerTags.EntityTypes.REUSABLE_AMMO).addTag(TRIDENTS);

    // modifiers
    this.tag(TinkerTags.EntityTypes.KILLAGERS).addTags(TinkerTags.EntityTypes.VILLAGERS, TinkerTags.EntityTypes.ILLAGERS).add(EntityTypes.IRON_GOLEM, EntityTypes.RAVAGER);
    this.tag(TinkerTags.EntityTypes.BACON_PRODUCER).add(EntityTypes.PIG, EntityTypes.PIGLIN, EntityTypes.HOGLIN);
    // in theory this could just be reusable ammo, but it seems better to keep separate
    this.tag(TinkerTags.EntityTypes.ENDERFERENCE_ARROW_BLACKLIST).addTag(TRIDENTS);
    // prevent dummy from healing you with necrotic
    this.tag(NECROTIC_BLACKLIST)
      .addOptional(Identifier.fromNamespaceAndPath("dummmmmmy", "target_dummy"))
      .addOptionalTag(commonResource(NECROTIC_BLACKLIST.location().getPath()));

    // collecting - TODO 1.21: remove legacy tags
    this.tag(COLLECTABLES).add(
        EntityTypes.ITEM, TinkerTools.indestructibleItem.get(),
        EntityTypes.EXPERIENCE_ORB
      ).addTags(TRIDENTS, DISCARDABLE_COLLECTABLES)
      .addOptionalTag(commonResource(COLLECTABLES.location().getPath()));
    this.tag(DISCARDABLE_COLLECTABLES).add(EntityTypes.ARROW, EntityTypes.SPECTRAL_ARROW, TinkerTools.materialArrow.get())
      .addOptionalTag(commonResource(DISCARDABLE_COLLECTABLES.location().getPath()));

    // reflecting - TODO 1.21: remove legacy tags
    this.tag(REFLECTING_BLACKLIST).addOptionalTag(commonResource(REFLECTING_BLACKLIST.location().getPath()));
    this.tag(REFLECTING_PRESERVE_OWNER).add(EntityTypes.FISHING_BOBBER, TinkerTools.fishingHook.get())
      .addOptionalTag(commonResource(REFLECTING_PRESERVE_OWNER.location().getPath()));
  }

  @Override
  public String getName() {
    return "Neo Tinkers Entity Type TinkerTags";
  }
}
