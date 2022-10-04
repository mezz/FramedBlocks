package xfacthd.framedblocks.client.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import xfacthd.framedblocks.api.util.FramedProperties;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.FramedDoubleBlockEntity;
import xfacthd.framedblocks.common.blockentity.FramedFlatElevatedDoubleSlopeSlabCornerBlockEntity;

public class FramedFlatElevatedDoubleSlopeSlabCornerModel extends FramedDoubleBlockModel
{
    private final Direction facing;
    private final boolean top;

    public FramedFlatElevatedDoubleSlopeSlabCornerModel(BlockState state, BakedModel baseModel)
    {
        super(baseModel, true);
        this.facing = state.getValue(FramedProperties.FACING_HOR);
        this.top = state.getValue(FramedProperties.TOP);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data)
    {
        return getSpriteOrDefault(
                data,
                top ? FramedDoubleBlockEntity.DATA_LEFT : FramedDoubleBlockEntity.DATA_RIGHT,
                top ? getModels().getA() : getModels().getB()
        );
    }

    @Override
    protected Tuple<BlockState, BlockState> getDummyStates()
    {
        return FramedFlatElevatedDoubleSlopeSlabCornerBlockEntity.getBlockPair(facing, top);
    }



    public static BlockState itemSource()
    {
        return FBContent.blockFramedFlatElevatedDoubleSlopeSlabCorner.get()
                .defaultBlockState()
                .setValue(FramedProperties.FACING_HOR, Direction.SOUTH);
    }
}