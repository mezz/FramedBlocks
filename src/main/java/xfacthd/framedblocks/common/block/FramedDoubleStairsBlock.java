package xfacthd.framedblocks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import xfacthd.framedblocks.api.util.*;
import xfacthd.framedblocks.common.blockentity.FramedDoubleStairsBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;

public class FramedDoubleStairsBlock extends AbstractFramedDoubleBlock
{
    public static final CtmPredicate CTM_PREDICATE = (state, side) ->
    {
        if (side == state.getValue(FramedProperties.FACING_HOR))
        {
            return true;
        }

        boolean top = state.getValue(FramedProperties.TOP);
        return Utils.isY(side) && top == (side == Direction.UP);
    };

    public FramedDoubleStairsBlock()
    {
        super(BlockType.FRAMED_DOUBLE_STAIRS);
        registerDefaultState(defaultBlockState().setValue(FramedProperties.TOP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FramedProperties.FACING_HOR, FramedProperties.TOP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = defaultBlockState().setValue(FramedProperties.FACING_HOR, context.getHorizontalDirection());
        return withTop(state, context.getClickedFace(), context.getClickLocation());
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        if (Utils.isY(face))
        {
            Direction dir = state.getValue(FramedProperties.FACING_HOR);
            return state.setValue(FramedProperties.FACING_HOR, rot.rotate(dir));
        }
        else if (rot != Rotation.NONE)
        {
            return state.cycle(FramedProperties.TOP);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return rotate(state, Direction.UP, rot);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return Utils.mirrorFaceBlock(state, mirror);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleStairsBlockEntity(pos, state);
    }
}
