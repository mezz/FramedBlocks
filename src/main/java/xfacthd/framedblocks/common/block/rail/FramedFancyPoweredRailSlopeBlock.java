package xfacthd.framedblocks.common.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedBlockEntity;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.predicate.cull.SideSkipPredicate;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.*;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.SlopeType;
import xfacthd.framedblocks.common.util.DoubleBlockTopInteractionMode;
import xfacthd.framedblocks.common.util.FramedUtils;

import java.util.function.BiFunction;
import java.util.function.Consumer;

class FramedFancyPoweredRailSlopeBlock extends FramedPoweredRailSlopeBlock implements IFramedDoubleBlock
{
    FramedFancyPoweredRailSlopeBlock(
            BlockType type, boolean isPoweredRail, BiFunction<BlockPos, BlockState, FramedBlockEntity> beFactory
    )
    {
        super(type, isPoweredRail, beFactory);
    }

    @Override
    @Nullable
    public BlockState runOcclusionTestAndGetLookupState(
            SideSkipPredicate pred, BlockGetter level, BlockPos pos, BlockState state, BlockState adjState, Direction side
    )
    {
        Tuple<BlockState, BlockState> statePair = getBlockPair(adjState);
        return super.runOcclusionTestAndGetLookupState(pred, level, pos, state, statePair.getA(), side);
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        return DoubleBlockTopInteractionMode.FIRST;
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        RailShape shape = state.getValue(PropertyHolder.ASCENDING_RAIL_SHAPE);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);

        BlockState railState = (switch ((BlockType) getBlockType())
        {
            case FRAMED_FANCY_POWERED_RAIL_SLOPE -> FBContent.BLOCK_FRAMED_FANCY_POWERED_RAIL;
            case FRAMED_FANCY_ACTIVATOR_RAIL_SLOPE -> FBContent.BLOCK_FRAMED_FANCY_ACTIVATOR_RAIL;
            default -> throw new IllegalArgumentException("Invalid block type");
        }).get().defaultBlockState();
        Direction facing = FramedUtils.getDirectionFromAscendingRailShape(shape);

        return new Tuple<>(
                FBContent.BLOCK_FRAMED_SLOPE.get().defaultBlockState()
                        .setValue(PropertyHolder.SLOPE_TYPE, SlopeType.BOTTOM)
                        .setValue(FramedProperties.FACING_HOR, facing)
                        .setValue(FramedProperties.Y_SLOPE, ySlope),
                railState.setValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT, shape)
        );
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer)
    {
        consumer.accept(FramedDoubleBlockRenderProperties.INSTANCE);
    }
}
