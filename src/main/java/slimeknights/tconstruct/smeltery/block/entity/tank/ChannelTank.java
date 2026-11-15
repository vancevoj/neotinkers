package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.FluidTank;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Tank for channel contents */
public class ChannelTank extends FluidTank {
	private static final String TAG_LOCKED = "locked";

	/**
	 * Amount of fluid that may not be extracted this tick
	 * Essentially, since we cannot guarantee tick order, this prevents us from having a net 0 fluid for the renderer
	 * if draining and filling at the same time
	 */
	private int locked;
	/** Journal to revert changes to {@link #locked} for aborted transactions */
	private final SnapshotJournal<Integer> lockedJournal = new SnapshotJournal<>() {
		@Override
		protected Integer createSnapshot() {
			return locked;
		}

		@Override
		protected void revertToSnapshot(Integer snapshot) {
			locked = snapshot;
		}
	};

	/** Tank owner */
	private final ChannelBlockEntity parent;

	public ChannelTank(int capacity, ChannelBlockEntity parent) {
		super(capacity);
		this.parent = parent;
	}

	/**
	 * Called on channel update to clear the lock, allowing this fluid to be drained
	 */
	public void freeFluid() {
		this.locked = 0;
	}

	/**
	 * Returns the maximum fluid that can be extracted from this tank
	 * @return  Max fluid that can be pulled
	 */
	public int getMaxUsable() {
		return Math.max(getFluidAmount() - locked, 0);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		int filled = super.insert(index, resource, amount, transaction);
		if (filled > 0) {
			lockedJournal.updateSnapshots(transaction);
			locked += filled;
		}
		return filled;
	}

	@Override
	protected void onContentsChanged(int index, FluidStack previousContents) {
		super.onContentsChanged(index, previousContents);
		// if we added something to an empty tank or removed everything, sync to client
		if (previousContents.isEmpty() != isEmpty()) {
			parent.sendFluidUpdate();
		}
	}

	@Override
	public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
		this.locked = nbt.getIntOr(TAG_LOCKED, 0);
		super.readFromNBT(lookupProvider, nbt);
		return this;
	}

	@Override
	public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
		nbt = super.writeToNBT(lookupProvider, nbt);
		nbt.putInt(TAG_LOCKED, locked);
		return nbt;
	}

	@Override
	public FluidTank readFromNBT(ValueInput input) {
		this.locked = input.getIntOr(TAG_LOCKED, 0);
		super.readFromNBT(input);
		return this;
	}

	@Override
	public void writeToNBT(ValueOutput output) {
		super.writeToNBT(output);
		output.putInt(TAG_LOCKED, locked);
	}
}
