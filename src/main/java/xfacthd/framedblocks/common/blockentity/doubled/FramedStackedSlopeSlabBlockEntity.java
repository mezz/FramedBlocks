package xfacthd.framedblocks.common.blockentity.doubled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.util.DoubleBlockTopInteractionMode;

public class FramedStackedSlopeSlabBlockEntity extends FramedDoubleBlockEntity
{
    private final boolean corner;
    private final boolean innerCorner;

    public FramedStackedSlopeSlabBlockEntity(BlockPos pos, BlockState state)
    {
        super(FBContent.BE_TYPE_FRAMED_STACKED_SLOPE_SLAB.get(), pos, state);
        BlockType type = (BlockType) getBlockType();
        this.corner = type == BlockType.FRAMED_FLAT_STACKED_SLOPE_SLAB_CORNER;
        this.innerCorner = type == BlockType.FRAMED_FLAT_STACKED_INNER_SLOPE_SLAB_CORNER;
    }

    @Override
    protected boolean hitSecondary(BlockHitResult hit)
    {
        boolean top = getBlockState().getValue(FramedProperties.TOP);
        if (hit.getDirection() == Direction.DOWN)
        {
            return top;
        }

        boolean upper = hit.getDirection() == Direction.UP || Mth.frac(hit.getLocation().y()) >= .5F;
        return upper != top;
    }

    @Override
    protected DoubleBlockTopInteractionMode calculateTopInteractionMode()
    {
        boolean top = getBlockState().getValue(FramedProperties.TOP);
        return top ? DoubleBlockTopInteractionMode.FIRST : DoubleBlockTopInteractionMode.SECOND;
    }

    @Override
    protected CamoGetter getCamoGetter(Direction side, @Nullable Direction edge)
    {
        boolean top = getBlockState().getValue(FramedProperties.TOP);
        Direction dirTwo = top ? Direction.UP : Direction.DOWN;

        if (side == dirTwo || (!Utils.isY(side) && edge == dirTwo))
        {
            return this::getCamo;
        }
        else if (!corner && edge == dirTwo.getOpposite())
        {
            Direction facing = getBlockState().getValue(FramedProperties.FACING_HOR);
            if (side == facing || (innerCorner && side == facing.getCounterClockWise()))
            {
                return this::getCamoTwo;
            }
        }

        return EMPTY_GETTER;
    }

    @Override
    protected SolidityCheck getSolidityCheck(Direction side)
    {
        boolean top = getBlockState().getValue(FramedProperties.TOP);
        if ((!top && side == Direction.DOWN) || (top && side == Direction.UP))
        {
            return SolidityCheck.FIRST;
        }

        if (!corner)
        {
            Direction facing = getBlockState().getValue(FramedProperties.FACING_HOR);
            if (side == facing || (innerCorner && side == facing.getCounterClockWise()))
            {
                return SolidityCheck.BOTH;
            }
        }
        return SolidityCheck.NONE;
    }
}