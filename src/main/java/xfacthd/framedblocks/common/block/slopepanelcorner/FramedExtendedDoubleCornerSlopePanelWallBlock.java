package xfacthd.framedblocks.common.block.slopepanelcorner;

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
import xfacthd.framedblocks.api.block.FramedProperties;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.block.AbstractFramedDoubleBlock;
import xfacthd.framedblocks.common.blockentity.doubled.FramedExtendedDoubleCornerSlopePanelWallBlockEntity;
import xfacthd.framedblocks.common.blockentity.doubled.FramedExtendedInnerDoubleCornerSlopePanelWallBlockEntity;
import xfacthd.framedblocks.common.data.BlockType;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.HorizontalRotation;
import xfacthd.framedblocks.common.util.DoubleBlockTopInteractionMode;

@SuppressWarnings("deprecation")
public class FramedExtendedDoubleCornerSlopePanelWallBlock extends AbstractFramedDoubleBlock
{
    public FramedExtendedDoubleCornerSlopePanelWallBlock(BlockType blockType)
    {
        super(blockType);
        registerDefaultState(defaultBlockState().setValue(FramedProperties.Y_SLOPE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(
                FramedProperties.FACING_HOR, PropertyHolder.ROTATION, FramedProperties.Y_SLOPE
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return FramedCornerSlopePanelWallBlock.getStateForPlacement(
                defaultBlockState(),
                ctx,
                getBlockType() == BlockType.FRAMED_EXT_DOUBLE_CORNER_SLOPE_PANEL_W
        );
    }

    @Override
    public boolean handleBlockLeftClick(BlockState state, Level level, BlockPos pos, Player player)
    {
        return IFramedBlock.toggleYSlope(state, level, pos, player);
    }

    @Override
    public BlockState rotate(BlockState state, Direction face, Rotation rot)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        if (face.getAxis() == dir.getAxis())
        {
            HorizontalRotation rotation = state.getValue(PropertyHolder.ROTATION);
            return state.setValue(PropertyHolder.ROTATION, rotation.rotate(rot));
        }
        else if (Utils.isY(face))
        {
            return state.setValue(FramedProperties.FACING_HOR, rot.rotate(dir));
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return rotate(state, state.getValue(FramedProperties.FACING_HOR), rot);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return FramedCornerSlopePanelWallBlock.mirrorCornerPanel(state, mirror);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return switch ((BlockType) getBlockType())
        {
            case FRAMED_EXT_DOUBLE_CORNER_SLOPE_PANEL_W -> new FramedExtendedDoubleCornerSlopePanelWallBlockEntity(pos, state);
            case FRAMED_EXT_INNER_DOUBLE_CORNER_SLOPE_PANEL_W -> new FramedExtendedInnerDoubleCornerSlopePanelWallBlockEntity(pos, state);
            default -> throw new IllegalStateException("Unexpected type: " + getBlockType());
        };
    }

    @Override
    public Tuple<BlockState, BlockState> calculateBlockPair(BlockState state)
    {
        Direction dir = state.getValue(FramedProperties.FACING_HOR);
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        HorizontalRotation backRot = rot.rotate(rot.isVertical() ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
        boolean ySlope = state.getValue(FramedProperties.Y_SLOPE);

        return switch ((BlockType) getBlockType())
        {
            case FRAMED_EXT_DOUBLE_CORNER_SLOPE_PANEL_W -> new Tuple<>(
                    FBContent.BLOCK_FRAMED_EXTENDED_CORNER_SLOPE_PANEL_WALL.get()
                            .defaultBlockState()
                            .setValue(FramedProperties.FACING_HOR, dir)
                            .setValue(PropertyHolder.ROTATION, rot)
                            .setValue(FramedProperties.Y_SLOPE, ySlope),
                    FBContent.BLOCK_FRAMED_LARGE_INNER_CORNER_SLOPE_PANEL_WALL.get()
                            .defaultBlockState()
                            .setValue(FramedProperties.FACING_HOR, dir.getOpposite())
                            .setValue(PropertyHolder.ROTATION, backRot)
                            .setValue(FramedProperties.Y_SLOPE, ySlope)
            );
            case FRAMED_EXT_INNER_DOUBLE_CORNER_SLOPE_PANEL_W -> new Tuple<>(
                    FBContent.BLOCK_FRAMED_EXTENDED_INNER_CORNER_SLOPE_PANEL_WALL.get()
                            .defaultBlockState()
                            .setValue(FramedProperties.FACING_HOR, dir)
                            .setValue(PropertyHolder.ROTATION, rot)
                            .setValue(FramedProperties.Y_SLOPE, ySlope),
                    FBContent.BLOCK_FRAMED_SMALL_CORNER_SLOPE_PANEL_WALL.get()
                            .defaultBlockState()
                            .setValue(FramedProperties.FACING_HOR, dir.getOpposite())
                            .setValue(PropertyHolder.ROTATION, backRot)
                            .setValue(FramedProperties.Y_SLOPE, ySlope)
            );
            default -> throw new IllegalArgumentException("Invalid type for this block: " + getBlockType());
        };
    }

    @Override
    public DoubleBlockTopInteractionMode calculateTopInteractionMode(BlockState state)
    {
        HorizontalRotation rot = state.getValue(PropertyHolder.ROTATION);
        if (rot == HorizontalRotation.UP || rot == HorizontalRotation.RIGHT)
        {
            return DoubleBlockTopInteractionMode.EITHER;
        }
        return DoubleBlockTopInteractionMode.SECOND;
    }
}
