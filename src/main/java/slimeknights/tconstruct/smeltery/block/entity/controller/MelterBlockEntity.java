package slimeknights.tconstruct.smeltery.block.entity.controller;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import slimeknights.mantle.block.entity.NameableBlockEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.fluid.FluidTankAnimated;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.utils.NBTTags;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.controller.MelterBlock;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity.ITankInventoryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.module.MeltingModuleInventory;
import slimeknights.tconstruct.smeltery.block.entity.module.SolidFuelModule;
import slimeknights.tconstruct.smeltery.menu.MelterContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MelterBlockEntity extends NameableBlockEntity implements ITankInventoryBlockEntity {

  /** Max capacity for the tank */
  private static final int TANK_CAPACITY = FluidValues.INGOT * 24;
  /* tags */
  private static final String TAG_INVENTORY = "inventory";
  /** Name of the GUI */
  private static final MutableComponent NAME = TConstruct.makeTranslation("gui", "melter");

  public static final BlockEntityTicker<MelterBlockEntity> SERVER_TICKER = (level, pos, state, self) -> self.tick(level, pos, state);

  /* Tank */
  /** Internal fluid tank output */
  @Getter
  protected final FluidTankAnimated tank = new FluidTankAnimated(TANK_CAPACITY, this);
  /** Last comparator strength to reduce block updates */
  @Getter @Setter
  private int lastStrength = -1;

  /** Internal tick counter */
  private int tick;

  /* Heating */
  /** Handles all the melting needs */
  private final MeltingModuleInventory meltingInventory = new MeltingModuleInventory(this, tank, Config.COMMON.melterOreRate, 3);

  /** Fuel handling logic */
  @Getter
  private final SolidFuelModule fuelModule;

  /** Main constructor */
  public MelterBlockEntity(BlockPos pos, BlockState state) {
    this(TinkerSmeltery.melter.get(), pos, state);
  }

  /** Extendable constructor */
  @SuppressWarnings("WeakerAccess")
  protected MelterBlockEntity(BlockEntityType<? extends MelterBlockEntity> type, BlockPos pos, BlockState state) {
    super(type, pos, state, NAME);
    this.fuelModule = new SolidFuelModule(this, pos.below());
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inv, Player playerEntity) {
    return new MelterContainerMenu(id, inv, this);
  }

  @Override
  public MeltingModuleInventory getItemHandler() {
    return meltingInventory;
  }

  /*
   * Tank methods
   */

  @Override
  public @NotNull ModelData getModelData() {
    return ModelData.builder()
                    .with(ModelProperties.FLUID_STACK, tank.getFluid())
                    .with(ModelProperties.TANK_CAPACITY, tank.getCapacity()).build();
  }

  /*
   * Melting
   */

  /** Checks if the tile entity is active */
  private boolean isFormed() {
    BlockState state = this.getBlockState();
    return state.hasProperty(MelterBlock.IN_STRUCTURE) && state.getValue(MelterBlock.IN_STRUCTURE);
  }

  /** Ticks the TE on the server */
  private void tick(Level level, BlockPos pos, BlockState state) {
    // are we fully formed?
    if (isFormed()) {
      switch (tick) {
        // tick 0: find fuel
        case 0 -> {
          if (!fuelModule.hasFuel() && meltingInventory.canHeat(fuelModule.findFuel(false))) {
            fuelModule.findFuel(true);
          }
        }
        // tick 2: heat items and consume fuel
        case 2 -> {
          boolean hasFuel = fuelModule.hasFuel();
          // update the active state
          if (state.getValue(ControllerBlock.ACTIVE) != hasFuel) {
            level.setBlockAndUpdate(pos, state.setValue(ControllerBlock.ACTIVE, hasFuel));
            // update the heater below
            BlockPos down = pos.below();
            BlockState downState = level.getBlockState(down);
            if (downState.is(TinkerTags.Blocks.FUEL_TANKS) && downState.hasProperty(ControllerBlock.ACTIVE) && downState.getValue(ControllerBlock.ACTIVE) != hasFuel) {
              level.setBlockAndUpdate(down, downState.setValue(ControllerBlock.ACTIVE, hasFuel));
            }
          }
          // heat items
          if (hasFuel) {
            meltingInventory.heatItems(fuelModule.getTemperature(), fuelModule.getRate());
            fuelModule.decreaseFuel(1);
          } else {
            meltingInventory.coolItems();
          }
        }
      }
    } else if (tick == 2) {
      // if we have fuel, lose fuel
      if (fuelModule.hasFuel()) {
        fuelModule.decreaseFuel(1);
      } else {
        // if we lack fuel, cool items
        meltingInventory.coolItems();
      }
    }
    tick = (tick + 1) % 4;
  }


  /*
   * NBT
   */

  @Override
  protected boolean shouldSyncOnUpdate() {
    return true;
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    tank.readFromNBT(input.childOrEmpty(NBTTags.TANK));
    fuelModule.readFromTag(input);
    input.child(TAG_INVENTORY).ifPresent(meltingInventory::readFromTag);
  }

  @Override
  public void saveSynced(ValueOutput output) {
    super.saveSynced(output);
    tank.writeToNBT(output.child(NBTTags.TANK));
    meltingInventory.writeToTag(output.child(TAG_INVENTORY));
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    fuelModule.writeToTag(output);
  }
}
