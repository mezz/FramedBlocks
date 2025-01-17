package xfacthd.framedblocks.api.block.render;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.block.blockentity.IFramedDoubleBlockEntity;
import xfacthd.framedblocks.api.model.util.ModelUtils;
import xfacthd.framedblocks.api.util.CamoList;
import xfacthd.framedblocks.api.util.Utils;

import javax.annotation.Nullable;

// TODO 1.21.2: store full CamoContainer in FramedBlockData and replace use of BEs with ModelData lookups
public class FramedBlockColor implements BlockColor, ItemColor
{
    public static final FramedBlockColor INSTANCE = new FramedBlockColor();

    @Override
    public int getColor(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex)
    {
        if (level != null && pos != null)
        {
            BlockEntity be = level.getBlockEntity(pos);
            if (tintIndex < -1 && be instanceof IFramedDoubleBlockEntity dbe)
            {
                tintIndex = ModelUtils.decodeSecondaryTintIndex(tintIndex);
                return dbe.getCamoTwo().getTintColor(level, pos, tintIndex);
            }
            else if (tintIndex >= 0 && be instanceof FramedBlockEntity fbe)
            {
                return fbe.getCamo().getTintColor(level, pos, tintIndex);
            }
        }
        return -1;
    }

    @Override
    public int getColor(ItemStack stack, int tintIndex)
    {
        CamoList camos = stack.getOrDefault(Utils.DC_TYPE_CAMO_LIST, CamoList.EMPTY);
        if (tintIndex < -1 && stack.getItem() instanceof BlockItem item && isDoubleBlock(item.getBlock()))
        {
            tintIndex = ModelUtils.decodeSecondaryTintIndex(tintIndex);
            return camos.getCamo(1).getTintColor(stack, tintIndex);
        }
        else if (tintIndex >= 0)
        {
            return camos.getCamo(0).getTintColor(stack, tintIndex);
        }
        return -1;
    }

    protected static boolean isDoubleBlock(Block block)
    {
        return block instanceof IFramedBlock framedBlock && framedBlock.getBlockType().isDoubleBlock();
    }
}
