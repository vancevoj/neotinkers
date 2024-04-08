package slimeknights.tconstruct.plugin.jei.entity;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.plugin.jei.TConstructJEIConstants;

import javax.annotation.Nullable;

/** Handler for working with entity types as ingredients */
@SuppressWarnings("rawtypes")
public class EntityIngredientHelper implements IIngredientHelper<EntityInput> {
  @Override
  public IIngredientType<EntityInput> getIngredientType() {
    return TConstructJEIConstants.ENTITY_TYPE;
  }

  @Override
  public String getDisplayName(EntityInput type) {
    return type.type().getDescription().getString();
  }

  @Override
  public String getUniqueId(EntityInput type, UidContext context) {
    return getResourceLocation(type).toString();
  }

  @Override
  public ResourceLocation getResourceLocation(EntityInput type) {
    return Registry.ENTITY_TYPE.getKey(type.type());
  }

  @Override
  public EntityInput copyIngredient(EntityInput type) {
    return type;
  }

  @Override
  public String getErrorInfo(@Nullable EntityInput type) {
    if (type == null) {
      return "null";
    }
    return getResourceLocation(type).toString();
  }
}
