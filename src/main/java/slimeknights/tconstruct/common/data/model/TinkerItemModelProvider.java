package slimeknights.tconstruct.common.data.model;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.gadgets.TinkerGadgets;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.world.TinkerWorld;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static slimeknights.tconstruct.TConstruct.getResource;

/**
 * Generates item model files under {@code models/item}. Since 1.21.4 NeoForge's item model provider is gone, so this uses the vanilla model templates.
 * Client item definitions ({@code items/*.json}) are not generated here, those come from the client item provider.
 */
@SuppressWarnings("UnusedReturnValue")
public class TinkerItemModelProvider extends ModelProvider {
  /** Template for generated items */
  private static final ModelTemplate GENERATED = ModelTemplates.FLAT_ITEM;
  /** Texture slot for material parts */
  private static final TextureSlot TEXTURE = TextureSlot.TEXTURE;

  /** Model output, set while generating */
  private BiConsumer<Identifier, ModelInstance> modelOutput;

  public TinkerItemModelProvider(PackOutput output) {
    super(output, TConstruct.MOD_ID);
  }

  @Override
  public String getName() {
    return "Neo Tinkers Item Models";
  }

  /* Client items come from another provider, so skip the vanilla validation */

  @Override
  protected Stream<? extends Holder<Block>> getKnownBlocks() {
    return Stream.empty();
  }

  @Override
  protected Stream<? extends Holder<Item>> getKnownItems() {
    return Stream.empty();
  }

  @Override
  protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
    this.modelOutput = itemModels.modelOutput;
    registerModels();
    savePendingPart();
  }

  /** Registers all item models */
  protected void registerModels() {
    // tool parts //
    // rock
    part(TinkerToolParts.pickHead, "pickaxe/head").offset(-2, 1);
    part(TinkerToolParts.hammerHead, "sledge_hammer/head").offset(-3, 3);
    // axe
    part(TinkerToolParts.smallAxeHead, "hand_axe/head").offset(-2, 3);
    part(TinkerToolParts.broadAxeHead, "broad_axe/blade").offset(0, 3);
    // blades
    part(TinkerToolParts.smallBlade);
    part(TinkerToolParts.broadBlade, "cleaver/head").offset(-1, 1);
    // plates
    part(TinkerToolParts.adzeHead, "pickadze/adze").offset(-5, 1);
    part(TinkerToolParts.largePlate);
    // bows
    part(TinkerToolParts.bowLimb, "longbow/limb_bottom").offset(5, -2);
    part(TinkerToolParts.bowGrip, "crossbow/body").offset(-2, -2);
    part(TinkerToolParts.bowstring);
    part(TinkerToolParts.arrowHead, "ammo/arrow_head").offset(-4, 3);
    part(TinkerToolParts.arrowShaft, "ammo/arrow_shaft").offset(1, -1);;
    part(TinkerToolParts.fletching, "ammo/arrow_feather").offset(4, -5);;
    // other
    part(TinkerToolParts.toolBinding);
    part(TinkerToolParts.toolHandle);
    part(TinkerToolParts.toughHandle);
    part(TinkerToolParts.toughBinding);
    part(TinkerToolParts.repairKit);
    part(TinkerToolParts.fakeIngot, "parts/ingot");
    // armor
    TinkerToolParts.plating.forEach((slot, item) -> {
      PartModel b = this.part(item, "armor/plate/" + slot.getName() + "/plating");
      if (slot == ArmorType.HELMET) {
        b.offset(0, 2);
      } else if (slot == ArmorType.LEGGINGS) {
        b.offset(0, 1);
      }
    });
    part(TinkerToolParts.maille);
    part(TinkerToolParts.shieldCore, "armor/plate/shield/core");
    // slimesuit
    part(TinkerToolParts.ribcage, "armor/slime/chestplate/ribcage");
    part(TinkerToolParts.shell, "armor/slime/leggings/shell").offset(0, -1);
    part(TinkerToolParts.laces, "armor/slime/boots/laces").offset(0, -1);

    // gauges
    generated(TinkerSmeltery.copperGauge, "block/smeltery/io/gauge");
    generated(TinkerSmeltery.obsidianGauge, "block/foundry/io/gauge");

    // casts //
    // basic
    basicItem(TinkerSmeltery.blankSandCast, "sand_cast/blank");
    basicItem(TinkerSmeltery.blankRedSandCast, "red_sand_cast/blank");
    cast(TinkerSmeltery.ingotCast);
    cast(TinkerSmeltery.nuggetCast);
    cast(TinkerSmeltery.gemCast);
    cast(TinkerSmeltery.rodCast);
    cast(TinkerSmeltery.repairKitCast);
    // compat
    cast(TinkerSmeltery.plateCast);
    cast(TinkerSmeltery.gearCast);
    cast(TinkerSmeltery.coinCast);
    cast(TinkerSmeltery.wireCast);
    // small heads
    cast(TinkerSmeltery.pickHeadCast);
    cast(TinkerSmeltery.smallAxeHeadCast);
    cast(TinkerSmeltery.smallBladeCast);
    cast(TinkerSmeltery.adzeHeadCast);
    // large heads
    cast(TinkerSmeltery.hammerHeadCast);
    cast(TinkerSmeltery.broadBladeCast);
    cast(TinkerSmeltery.broadAxeHeadCast);
    cast(TinkerSmeltery.largePlateCast);
    // bindings
    cast(TinkerSmeltery.toolBindingCast);
    cast(TinkerSmeltery.toughBindingCast);
    // tool rods
    cast(TinkerSmeltery.toolHandleCast);
    cast(TinkerSmeltery.toughHandleCast);
    // bow
    cast(TinkerSmeltery.bowLimbCast);
    cast(TinkerSmeltery.bowGripCast);
    basicItem(TinkerSmeltery.arrowCast.getId(), "cast/arrow");
    // armor
    cast(TinkerSmeltery.helmetPlatingCast);
    cast(TinkerSmeltery.chestplatePlatingCast);
    cast(TinkerSmeltery.leggingsPlatingCast);
    cast(TinkerSmeltery.bootsPlatingCast);
    cast(TinkerSmeltery.mailleCast);
    // dummy parts
    TinkerSmeltery.dummyPlating.forEach((type, item) -> basicItem(item, "tool/parts/plating_" + type.getName()));

    // world //
    // shards
    basicItem(TinkerWorld.steelShard, "materials/steel_shard");
    basicItem(TinkerWorld.cobaltShard, "materials/cobalt_shard");
    basicItem(TinkerWorld.knightmetalShard, "materials/knightmetal_shard");
    generated(TinkerWorld.steelCluster, "block/geode/steel_cluster");
    generated(TinkerWorld.cobaltCluster, "block/geode/cobalt_cluster");
    generated(TinkerWorld.knightmetalCluster, "block/geode/knightmetal_cluster");
    // heads
    TinkerWorld.headItems.forEach(head -> new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("item/template_skull")), Optional.empty()).create(id(head).withPrefix("item/"), new TextureMapping(), modelOutput));
    // armor stands
    TinkerGadgets.armorStand.forEach((type, item) -> basicItem(item, "gadgets/" + type.name().toLowerCase(Locale.ROOT) + "_armor_stand"));
  }

  @SuppressWarnings("deprecation") // no its not
  private Identifier id(ItemLike item) {
    return BuiltInRegistries.ITEM.getKey(item.asItem());
  }

  /** Generated item with a texture */
  private Identifier generated(Identifier item, Identifier texture) {
    return GENERATED.create(item.withPrefix("item/"), TextureMapping.layer0(new Material(texture)), modelOutput);
  }

  /** Generated item with a texture */
  private Identifier generated(Identifier item, String texture) {
    return generated(item, Identifier.fromNamespaceAndPath(item.getNamespace(), texture));
  }

  /** Generated item with a texture */
  private Identifier generated(ItemLike item, String texture) {
    return generated(id(item), texture);
  }

  /** Generated item with a texture */
  private Identifier basicItem(Identifier item, String texture) {
    return generated(item, "item/" + texture);
  }

  /** Generated item with a texture */
  private Identifier basicItem(ItemLike item, String texture) {
    return basicItem(id(item), texture);
  }


  /* Parts */

  /**
   * Material part model, written when the next part starts or when generation finishes so the offset can still be set after creation.
   * Replacement for the 1.21.1 model builder return value.
   */
  private class PartModel {
    private final Identifier part;
    private final Identifier texture;
    private int offsetX = 0;
    private int offsetY = 0;
    private boolean hasOffset = false;

    private PartModel(Identifier part, Identifier texture) {
      this.part = part;
      this.texture = texture;
    }

    /** Sets the offset */
    public PartModel offset(int x, int y) {
      this.offsetX = x;
      this.offsetY = y;
      this.hasOffset = true;
      return this;
    }

    /** Writes the model */
    private void save() {
      Consumer<MaterialModelBuilder> loader = hasOffset ? builder -> builder.offset(offsetX, offsetY) : builder -> {};
      ExtendedModelTemplateBuilder.builder()
        .parent(Identifier.fromNamespaceAndPath("neoforge", "item/default"))
        .requiredTextureSlot(TEXTURE)
        .customLoader(MaterialModelBuilder::new, loader)
        .build()
        .create(part.withPrefix("item/"), TextureMapping.defaultTexture(new Material(texture)), modelOutput);
    }
  }

  /** Part model waiting to be saved */
  @Nullable
  private PartModel pendingPart = null;

  /** Saves the pending part model, if any */
  private void savePendingPart() {
    if (pendingPart != null) {
      pendingPart.save();
      pendingPart = null;
    }
  }

  /** Creates a part model with the given texture */
  private PartModel part(Identifier part, String texture) {
    savePendingPart();
    pendingPart = new PartModel(part, getResource("item/tool/" + texture));
    return pendingPart;
  }

  /** Creates a part model in the parts folder */
  private PartModel part(Item item, String texture) {
    return part(id(item), texture);
  }

  /** Creates a part model with the given texture */
  private PartModel part(ItemObject<? extends MaterialItem> part, String texture) {
    return part(part.getId(), texture);
  }

  /** Creates a part model in the parts folder */
  private void part(ItemObject<? extends MaterialItem> part) {
    part(part, "parts/" + part.getId().getPath());
  }


  /** Creates models for the given cast object */
  private void cast(CastItemObject cast) {
    String name = cast.getName().getPath();
    basicItem(cast.getId(), "cast/" + name);
    basicItem(cast.getSand(), "sand_cast/" + name);
    basicItem(cast.getRedSand(), "red_sand_cast/" + name);
  }
}
