package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.FillOnlyFluidHandler;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Tank for each side connection, for the sake of rendering */
public class ChannelSideTank extends FillOnlyFluidHandler {
	private final ChannelBlockEntity channel;
	private final Direction side;

	public ChannelSideTank(ChannelBlockEntity channel, ChannelTank tank, Direction side) {
		super(tank);
		// only horizontals
		assert side.getAxis() != Axis.Y;
		this.channel = channel;
		this.side = side;
	}

	/** Marks the side as flowing once the transaction commits */
	private int markFlowing(int filled, TransactionContext transaction) {
		if (filled > 0) {
			new RootCommitJournal(() -> channel.setFlow(side, true)).updateSnapshots(transaction);
		}
		return filled;
	}

	@Override
	public int insert(FluidResource resource, int amount, TransactionContext transaction) {
		return markFlowing(super.insert(resource, amount, transaction), transaction);
	}

	@Override
	public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
		return markFlowing(super.insert(index, resource, amount, transaction), transaction);
	}
}
