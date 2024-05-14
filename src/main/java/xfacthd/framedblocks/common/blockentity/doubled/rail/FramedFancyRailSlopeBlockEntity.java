package xfacthd.framedblocks.common.blockentity.doubled.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import xfacthd.framedblocks.api.camo.CamoContainer;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.ISlopeBlock;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;

public class FramedFancyRailSlopeBlockEntity extends FramedDoubleBlockEntity
{
    public FramedFancyRailSlopeBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_FANCY_RAIL_SLOPE.value(), pos, state);
    }

    @Override
    protected boolean hitSecondary(BlockHitResult hit, Player player)
    {
        Direction side = hit.getDirection();
        BlockState state = getBlockState();
        return side == Direction.UP || side == ((ISlopeBlock) state.getBlock()).getFacing(state).getOpposite();
    }

    @Override
    public CamoContainer<?, ?> getCamo(BlockState state)
    {
        //The primary camo is the only camo needed in skip predicates
        return getCamo();
    }

    @Override
    protected boolean isCamoSolid()
    {
        CamoContainer<?, ?> camo = getCamo();
        return !camo.isEmpty() && camo.getContent().isSolid(level(), worldPosition);
    }
}
