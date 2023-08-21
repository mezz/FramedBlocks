package xfacthd.framedblocks.common.data.facepreds.slopepanelcorner;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.fullface.FullFacePredicate;

public final class ExtendedInnerDoubleCornerSlopePanelFullFacePredicate implements FullFacePredicate
{
    @Override
    public boolean test(BlockState state, Direction side)
    {
        Direction facing = state.getValue(FramedProperties.FACING_HOR);
        return side == facing.getOpposite() || side == facing.getClockWise();
    }
}
