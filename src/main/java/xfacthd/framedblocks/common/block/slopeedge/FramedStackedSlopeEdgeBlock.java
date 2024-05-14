package xfacthd.framedblocks.common.block.slopeedge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.*;
import xfacthd.framedblocks.common.blockentity.doubled.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.doubleblock.*;
import xfacthd.framedblocks.common.data.property.SlopeType;

public class FramedStackedSlopeEdgeBlock extends AbstractFramedDoubleBlock implements IComplexSlopeSource
{
    public FramedStackedSlopeEdgeBlock()
    {
        super(BlockType.FRAMED_STACKED_SLOPE_EDGE);
        registerDefaultState(defaultBlockState().setValue(FramedProperties.Y_SLOPE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(
                FramedProperties.FACING_HOR, PropertyHolder.SLOPE_TYPE,
                FramedProperties.Y_SLOPE, BlockStateProperties.WATERLOGGED
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return ExtPlacementStateBuilder.of(this, ctx)
                .withHorizontalFacingAndSlopeType()
                .withWater()
                .build();
    }

    @Override
    public boolean handleBlockLeftClick(BlockState state, Level level, BlockPos pos, Player player)
    {
        return IFramedBlock.toggleYSlope(state, level, pos, player);
    }

    @Override
    public BlockState rotate(BlockState state, BlockHitResult hit, Rotation rot)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        Direction face = hit.getDirection();
        if (hit.getDirection() == dir.getOpposite())
        {
            Direction coordDir = switch (state.getValue(PropertyHolder.SLOPE_TYPE))
            {
                case BOTTOM -> Direction.UP;
                case HORIZONTAL -> dir.getClockWise();
                case TOP -> Direction.DOWN;
            };
            if (Utils.fractionInDir(hit.getLocation(), coordDir) < .5)
            {
                face = dir;
            }
        }
        return rotate(state, face, rot);
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        if (Utils.isY(face) || (type != SlopeType.HORIZONTAL && face == dir.getOpposite()))
        {
            return state.setValue(FramedProperties.FACING_HOR, rot.rotate(dir));
        }
        else if (rot != Rotation.NONE && face == dir)
        {
            return state.cycle(PropertyHolder.SLOPE_TYPE);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return rotate(state, Direction.UP, rot);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        if (state.getValue(PropertyHolder.SLOPE_TYPE) == SlopeType.HORIZONTAL)
        {
            return Utils.mirrorCornerBlock(state, mirror);
        }
        else
        {
            return Utils.mirrorFaceBlock(state, mirror);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FramedDoubleBlockEntity(pos, state);
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        if (type == SlopeType.TOP)
        {
            return DoubleBlockTopInteractionMode.FIRST;
        }
        return DoubleBlockTopInteractionMode.EITHER;
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);

        BlockState edgeState = FBContent.BLOCK_FRAMED_SLOPE_EDGE.value()
                .defaultBlockState()
                .setValue(FramedProperties.FACING_HOR, dir)
                .setValue(PropertyHolder.SLOPE_TYPE, type)
                .setValue(PropertyHolder.ALT_TYPE, true)
                .setValue(FramedProperties.Y_SLOPE, ySlope);

        if (type == SlopeType.HORIZONTAL)
        {
            return new Tuple<>(
                    FBContent.BLOCK_FRAMED_VERTICAL_STAIRS.value()
                            .defaultBlockState()
                            .setValue(FramedProperties.FACING_HOR, dir),
                    edgeState
            );
        }
        else
        {
            Half half = type == SlopeType.TOP ? Half.TOP : Half.BOTTOM;
            return new Tuple<>(
                    FBContent.BLOCK_FRAMED_STAIRS.value()
                            .defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, dir)
                            .setValue(BlockStateProperties.HALF, half),
                    edgeState
            );
        }
    }

    @Override
    public SolidityCheck calculateSolidityCheck(BlockState state, Direction side)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        if (side == dir)
        {
            return SolidityCheck.FIRST;
        }

        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        Direction dirTwo = switch (type)
        {
            case BOTTOM -> Direction.DOWN;
            case HORIZONTAL -> dir.getCounterClockWise();
            case TOP -> Direction.UP;
        };
        if (side == dirTwo)
        {
            return SolidityCheck.FIRST;
        }

        return SolidityCheck.NONE;
    }

    @Override
    public CamoGetter calculateCamoGetter(BlockState state, Direction side, @Nullable Direction edge)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        if (side == dir)
        {
            return CamoGetter.FIRST;
        }

        SlopeType type = state.getValue(PropertyHolder.SLOPE_TYPE);
        Direction dirTwo = switch (type)
        {
            case BOTTOM -> Direction.DOWN;
            case HORIZONTAL -> dir.getCounterClockWise();
            case TOP -> Direction.UP;
        };
        if (side == dirTwo)
        {
            return CamoGetter.FIRST;
        }
        else if (side == dirTwo.getOpposite())
        {
            if (edge == dir)
            {
                return CamoGetter.FIRST;
            }
            return CamoGetter.NONE;
        }
        else if (side == dir.getOpposite())
        {
            if (edge == dirTwo)
            {
                return CamoGetter.FIRST;
            }
            return CamoGetter.NONE;
        }
        else // Triangle faces
        {
            if (edge == dir || edge == dirTwo)
            {
                return CamoGetter.FIRST;
            }
            return CamoGetter.NONE;
        }
    }

    @Override
    public BlockState getItemModelSource()
    {
        return defaultBlockState().setValue(FramedProperties.FACING_HOR, Direction.SOUTH);
    }

    @Override
    public BlockState getJadeRenderState(BlockState state)
    {
        return getItemModelSource();
    }

    @Override
    public boolean isHorizontalSlope(BlockState state)
    {
        return state.getValue(PropertyHolder.SLOPE_TYPE) == SlopeType.HORIZONTAL;
    }
}
