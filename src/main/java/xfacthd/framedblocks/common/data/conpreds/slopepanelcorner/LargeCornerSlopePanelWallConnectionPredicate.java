package xfacthd.framedblocks.common.data.conpreds.slopepanelcorner;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.contex.ConnectionPredicate;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;

public final class LargeCornerSlopePanelWallConnectionPredicate implements ConnectionPredicate
{
    @Override
    public boolean canConnectFullEdge(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction rotDir = rot.withFacing(dir);
        Direction perpRotDir = rot.rotate(Rotation.COUNTERCLOCKWISE_90).withFacing(dir);

        return side == dir && (edge == rotDir.getOpposite() || edge == perpRotDir.getOpposite());
    }

    @Override
    public boolean canConnectDetailed(BlockState state, Direction side, Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        Direction rotDir = rot.withFacing(dir);
        Direction perpRotDir = rot.rotate(Rotation.COUNTERCLOCKWISE_90).withFacing(dir);

        if (side == dir)
        {
            return edge == rotDir || edge == perpRotDir;
        }
        else if (side == rotDir)
        {
            return edge == perpRotDir || edge.getAxis() == dir.getAxis();
        }
        else if (side == perpRotDir)
        {
            return edge == rotDir || edge.getAxis() == dir.getAxis();
        }
        else if (side == dir.getOpposite() || side == rotDir.getOpposite() || side == perpRotDir.getOpposite())
        {
            return edge == rotDir || edge == perpRotDir;
        }
        return false;
    }
}
