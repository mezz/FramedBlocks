package xfacthd.framedblocks.api.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import xfacthd.framedblocks.api.FramedBlocksAPI;
import xfacthd.framedblocks.api.FramedBlocksClientAPI;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.util.FramedBlockData;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.block.FramedBlockEntity;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("deprecation")
public abstract class FramedBlockModel extends BakedModelProxy
{
    private final Table<BlockState, RenderType, Map<Direction, List<BakedQuad>>> quadCacheTable = HashBasedTable.create();
    private final Map<BlockState, BakedModel> modelCache = new HashMap<>();
    private final BlockState state;
    private final IBlockType type;

    public FramedBlockModel(BlockState state, BakedModel baseModel)
    {
        super(baseModel);
        this.state = state;
        this.type = ((IFramedBlock)state.getBlock()).getBlockType();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand, IModelData extraData)
    {
        RenderType layer = MinecraftForgeClient.getRenderType();
        BlockState camoState = Blocks.AIR.defaultBlockState();

        if (extraData instanceof FramedBlockData data && layer != null)
        {
            if (side != null && data.isSideHidden(side)) { return Collections.emptyList(); }

            camoState = data.getCamoState();
            if (camoState != null && !camoState.isAir())
            {
                boolean camoInLayer = canRenderInLayer(camoState, layer);
                if (camoInLayer || hasAdditionalQuadsInLayer(layer))
                {
                    return getCamoQuads(state, camoState, side, rand, extraData, layer, camoInLayer);
                }
            }
        }

        if (layer == null) { layer = RenderType.cutout(); }
        if (camoState == null || camoState.isAir())
        {
            boolean baseModelInLayer = canRenderBaseModelInLayer(layer);
            if (baseModelInLayer || hasAdditionalQuadsInLayer(layer))
            {
                return getCamoQuads(state, FramedBlocksAPI.getInstance().defaultModelState(), side, rand, extraData, layer, baseModelInLayer);
            }
        }

        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, Random rand)
    {
        if (state == null) { state = this.state; }
        return getCamoQuads(state, FramedBlocksAPI.getInstance().defaultModelState(), side, rand, EmptyModelData.INSTANCE, RenderType.cutout(), true);
    }

    private List<BakedQuad> getCamoQuads(BlockState state, BlockState camoState, Direction side, Random rand, IModelData extraData, RenderType layer, boolean camoInLayer)
    {
        if (type.getCtmPredicate().test(state, side))
        {
            boolean additionalQuads = hasAdditionalQuadsInLayer(layer);
            if (!camoInLayer && !additionalQuads) { return Collections.emptyList(); }

            List<BakedQuad> quads = new ArrayList<>();

            if (camoInLayer)
            {
                BakedModel model;
                synchronized (modelCache)
                {
                    if (!modelCache.containsKey(camoState))
                    {
                        modelCache.put(camoState, getCamoModel(camoState));
                    }
                    model = modelCache.get(camoState);
                }

                IModelData data = getCamoData(model, camoState, extraData);
                quads.addAll(model.getQuads(camoState, side, rand, data));
            }

            if (additionalQuads)
            {
                getAdditionalQuads(quads, side, state, rand, extraData, layer);
            }

            return quads;
        }
        else
        {
            synchronized (quadCacheTable)
            {
                if (!quadCacheTable.contains(camoState, layer))
                {
                    quadCacheTable.put(camoState, layer, makeQuads(state, camoState, rand, extraData, layer, camoInLayer));
                }
                return quadCacheTable.get(camoState, layer).get(side);
            }
        }
    }

    private Map<Direction, List<BakedQuad>> makeQuads(BlockState state, BlockState camoState, Random rand, IModelData data, RenderType layer, boolean camoInLayer)
    {
        Map<Direction, List<BakedQuad>> quadMap = new Object2ObjectArrayMap<>(7);
        quadMap.put(null, new ArrayList<>());
        for (Direction dir : Direction.values()) { quadMap.put(dir, new ArrayList<>()); }

        if (camoInLayer)
        {
            BakedModel camoModel = getCamoModel(camoState);
            List<BakedQuad> quads =
                    getAllQuads(camoModel, camoState, rand)
                            .stream()
                            .filter(q -> !type.getCtmPredicate().test(state, q.getDirection()))
                            .toList();

            for (BakedQuad quad : quads)
            {
                transformQuad(quadMap, quad);
            }
            postProcessQuads(quadMap);
        }

        if (hasAdditionalQuadsInLayer(layer))
        {
            getAdditionalQuads(quadMap, state, rand, data, layer);
        }

        return quadMap;
    }

    protected abstract void transformQuad(Map<Direction, List<BakedQuad>> quadMap, BakedQuad quad);

    protected void postProcessQuads(Map<Direction, List<BakedQuad>> quadMap) {}

    protected boolean hasAdditionalQuadsInLayer(RenderType layer) { return false; }

    /**
     * Add additional quads to faces that return {@code true} from {@code xfacthd.framedblocks.api.util.CtmPredicate#test(BlockState, Direction)}<br>
     * The result of this method will NOT be cached, execution should therefore be as fast as possible
     */
    protected void getAdditionalQuads(List<BakedQuad> quads, Direction side, BlockState state, Random rand, IModelData data, RenderType layer) {}

    /**
     * Add additional quads to faces that return {@code false} from {@code xfacthd.framedblocks.api.util.CtmPredicate#test(BlockState, Direction)}<br>
     * The result of this method will be cached, processing time is therefore not critical
     */
    protected void getAdditionalQuads(Map<Direction, List<BakedQuad>> quadMap, BlockState state, Random rand, IModelData data, RenderType layer) {}

    protected boolean canRenderBaseModelInLayer(RenderType layer) { return layer == RenderType.cutout(); }

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull BlockAndTintGetter world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData)
    {
        boolean ghostData = tileData instanceof FramedBlockData data && data.isGhostData();
        if (!ghostData && world.getBlockEntity(pos) instanceof FramedBlockEntity be)
        {
            tileData = be.getModelData();
        }
        tileData.setData(FramedBlockData.LEVEL, world);
        tileData.setData(FramedBlockData.POS, pos);
        return tileData;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(IModelData data)
    {
        if (data instanceof FramedBlockData)
        {
            BlockState camoState = data.getData(FramedBlockData.CAMO);
            if (camoState != null && !camoState.isAir())
            {
                synchronized (modelCache)
                {
                    return modelCache.computeIfAbsent(camoState, state ->
                            getCamoModel(camoState)
                    ).getParticleIcon();
                }
            }
        }
        return baseModel.getParticleIcon();
    }



    private final Map<BlockState, BakedModel> fluidModels = new HashMap<>();
    protected BakedModel getCamoModel(BlockState camoState)
    {
        if (camoState.getBlock() instanceof LiquidBlock fluid)
        {
            return fluidModels.computeIfAbsent(camoState, state -> FramedBlocksClientAPI.getInstance().createFluidModel(fluid.getFluid()));
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(camoState);
    }

    private static IModelData getCamoData(BakedModel model, BlockState state, IModelData data)
    {
        BlockAndTintGetter level = data.getData(FramedBlockData.LEVEL);
        BlockPos pos = data.getData(FramedBlockData.POS);

        if (level == null || pos == null || pos.equals(BlockPos.ZERO)) { return data; }

        return model.getModelData(level, pos, state, data);
    }

    protected static List<BakedQuad> getAllQuads(BakedModel model, BlockState state, Random rand)
    {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values())
        {
            quads.addAll(model.getQuads(state, dir, rand, EmptyModelData.INSTANCE));
        }
        return quads;
    }

    private static boolean canRenderInLayer(BlockState camoState, RenderType layer)
    {
        if (camoState == null) { return false; }

        if (camoState.getBlock() instanceof LiquidBlock)
        {
            return ItemBlockRenderTypes.canRenderInLayer(camoState.getFluidState(), layer);
        }
        return ItemBlockRenderTypes.canRenderInLayer(camoState, layer);
    }
}