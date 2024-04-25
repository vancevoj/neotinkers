package slimeknights.tconstruct.library.tools.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.registration.object.IdAwareObject;

/** Armor material that returns 0 except for name, since we bypass all the usages */
@RequiredArgsConstructor
@Getter
public class DummyArmorMaterial implements ArmorMaterial, IdAwareObject {
  private final ResourceLocation id;
  private final SoundEvent equipSound;

  @Override
  public String getName() {
    return id.toString();
  }


  /* Required dummy methods */

  @Override
  @Deprecated
  public int getDurabilityForSlot(EquipmentSlot pSlot) {
    return 0;
  }

  @Override
  @Deprecated
  public int getDefenseForSlot(EquipmentSlot pSlot) {
    return 0;
  }

  @Override
  public int getEnchantmentValue() {
    return 0;
  }

  @Override
  @Deprecated
  public Ingredient getRepairIngredient() {
    return Ingredient.EMPTY;
  }

  @Override
  @Deprecated
  public float getToughness() {
    return 0;
  }

  @Override
  @Deprecated
  public float getKnockbackResistance() {
    return 0;
  }
}
