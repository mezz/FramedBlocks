package xfacthd.framedblocks.common.blockentity.doubled;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.util.DoubleBlockTopInteractionMode;

public class FramedDoublePanelBlockEntity extends FramedDoubleBlockEntity
{
    public FramedDoublePanelBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_DOUBLE_FRAMED_PANEL.get(), pos, state);
    }

    @Override
    protected boolean hitSecondary(BlockHitResult hit)
    {
        Direction facing = getBlockState().getValue(FramedProperties.FACING_NE);
        Direction side = hit.getDirection();
        Vec3 vec = Utils.fraction(hit.getLocation());

        if (side == facing)
        {
            return false;
        }
        if (side == facing.getOpposite())
        {
            return true;
        }

        if (facing == Direction.NORTH)
        {
            return vec.z() > .5F;
        }
        else
        {
            return vec.x() <= .5F;
        }
    }

    @Override
    protected DoubleBlockTopInteractionMode calculateTopInteractionMode()
    {
        return DoubleBlockTopInteractionMode.EITHER;
    }

    @Override
    protected CamoGetter getCamoGetter(Direction side, @Nullable Direction edge)
    {
        Direction facing = getBlockState().getValue(FramedProperties.FACING_NE);
        boolean notFacingAxis = side.getAxis() != facing.getAxis();
        if (side == facing || (notFacingAxis && edge == facing))
        {
            return this::getCamo;
        }
        if (side == facing.getOpposite() || (notFacingAxis && edge == facing.getOpposite()))
        {
            return this::getCamoTwo;
        }
        return EMPTY_GETTER;
    }

    @Override
    protected SolidityCheck getSolidityCheck(Direction side)
    {
        Direction facing = getBlockState().getValue(FramedProperties.FACING_NE);
        if (side == facing)
        {
            return SolidityCheck.FIRST;
        }
        else if (side == facing.getOpposite())
        {
            return SolidityCheck.SECOND;
        }
        return SolidityCheck.BOTH;
    }
}