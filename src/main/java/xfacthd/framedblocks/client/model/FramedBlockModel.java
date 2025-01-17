package xfacthd.framedblocks.client.model;

import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;
import xfacthd.framedblocks.api.block.cache.StateCache;
import xfacthd.framedblocks.api.camo.CamoContainerHelper;
import xfacthd.framedblocks.api.camo.CamoContent;
import xfacthd.framedblocks.api.camo.block.BlockCamoContent;
import xfacthd.framedblocks.api.camo.empty.EmptyCamoContent;
import xfacthd.framedblocks.api.model.AbstractFramedBlockModel;
import xfacthd.framedblocks.api.model.cache.QuadCacheKey;
import xfacthd.framedblocks.api.model.data.*;
import xfacthd.framedblocks.api.model.geometry.Geometry;
import xfacthd.framedblocks.api.model.wrapping.GeometryFactory;
import xfacthd.framedblocks.api.model.util.ModelUtils;
import xfacthd.framedblocks.api.predicate.contex.ConTexMode;
import xfacthd.framedblocks.api.type.IBlockType;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.data.ConTexDataHandler;
import xfacthd.framedblocks.common.config.ClientConfig;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.data.PropertyHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class FramedBlockModel extends AbstractFramedBlockModel
{
    private static final FramedBlockData DEFAULT_DATA = new FramedBlockData(EmptyCamoContent.EMPTY, false);
    private static final ChunkRenderTypeSet BASE_MODEL_RENDER_TYPES = ModelUtils.CUTOUT;
    private static final int FLAG_NO_CAMO_ATL_MODEL = 0b001;
    private static final int FLAG_NO_CAMO_REINFORCED = 0b010;
    private static final int FLAG_NO_CAMO_SOLID_BG = 0b100;
    private static final BlockCamoContent[] DEFAULT_NO_CAMO_CONTENTS = makeNoCamoContents(FBContent.BLOCK_FRAMED_CUBE.value().defaultBlockState());
    private static final UnaryOperator<BakedQuad> EMISSIVE_PROCESSOR = QuadTransformers.settingMaxEmissivity()::process;

    private final Map<QuadCacheKey, QuadTable> quadCache = new ConcurrentHashMap<>();
    private final Map<QuadCacheKey, CachedRenderTypes> renderTypeCache = new ConcurrentHashMap<>();
    private final Geometry geometry;
    private final IBlockType type;
    private final boolean isBaseCube;
    private final boolean forceUngeneratedBaseModel;
    private final boolean useBaseModel;
    private final boolean useSolidBase;
    private final boolean uncachedPostProcess;
    private final StateCache stateCache;
    private final Predicate<Direction> xformDirFilter;
    private final BlockCamoContent[] noCamoContents;

    public FramedBlockModel(GeometryFactory.Context ctx, Geometry geometry)
    {
        super(ctx.baseModel(), ctx.state(), geometry.getItemModelInfo());
        BlockState state = ctx.state();
        this.geometry = geometry;
        this.type = ((IFramedBlock) state.getBlock()).getBlockType();
        this.isBaseCube = state.getBlock() == FBContent.BLOCK_FRAMED_CUBE.value();
        this.forceUngeneratedBaseModel = geometry.forceUngeneratedBaseModel();
        this.useBaseModel = geometry.useBaseModel();
        this.useSolidBase = geometry.useSolidNoCamoModel();
        this.uncachedPostProcess = geometry.hasUncachedPostProcessing();
        this.stateCache = ((IFramedBlock) state.getBlock()).getCache(state);
        this.xformDirFilter = geometry.transformAllQuads() ? d -> true : d -> !stateCache.isFullFace(d);
        this.noCamoContents = isBaseCube ? makeNoCamoContents(state) : DEFAULT_NO_CAMO_CONTENTS;

        Preconditions.checkState(
                this.useBaseModel || !this.forceUngeneratedBaseModel,
                "Geometry::useBaseModel() must return true when Geometry::forceUngeneratedBaseModel() returns true"
        );
        Preconditions.checkState(
                !this.useSolidBase || !this.useBaseModel,
                "Geometry#useSolidNoCamoModel() and Geometry#useBaseModel() cannot both return true"
        );
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType)
    {
        CamoContent<?> camoState = EmptyCamoContent.EMPTY;
        FramedBlockData data = extraData.get(FramedBlockData.PROPERTY);
        if (data != null)
        {
            if (side != null && data.isSideHidden(side))
            {
                return List.of();
            }

            camoState = data.getCamoContent();
            if (camoState != null && !camoState.isEmpty())
            {
                return getCamoQuads(camoState, side, rand, extraData, data, renderType);
            }
        }

        if (data == null)
        {
            data = DEFAULT_DATA;
        }
        if (camoState == null || camoState.isEmpty())
        {
            return getCamoQuads(null, side, rand, extraData, data, renderType);
        }

        return List.of();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return getCamoQuads(null, side, rand, ModelData.EMPTY, DEFAULT_DATA, RenderType.cutout());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data)
    {
        FramedBlockData fbData = data.get(FramedBlockData.PROPERTY);
        if (isBaseCube && (fbData == null || fbData.getCamoContent().isEmpty()))
        {
            return originalModel.getRenderTypes(state, rand, data);
        }
        if (fbData == null)
        {
            fbData = DEFAULT_DATA;
        }

        CamoContent<?> camoContent = fbData.getCamoContent();
        CamoContent<?> keyContent = camoContent;
        if (camoContent == null || camoContent.isEmpty())
        {
            camoContent = EmptyCamoContent.EMPTY;
            keyContent = getNoCamoModelSourceContent(fbData);
        }
        return getCachedRenderTypes(keyContent, camoContent, rand, data).allTypes;
    }

    private CachedRenderTypes getCachedRenderTypes(
            CamoContent<?> keyContent, CamoContent<?> camoContent, RandomSource rand, ModelData data
    )
    {
        QuadCacheKey key = geometry.makeCacheKey(keyContent, null, data);
        CachedRenderTypes cachedTypes = renderTypeCache.get(key);
        if (cachedTypes == null)
        {
            cachedTypes = buildRenderTypeCache(camoContent, rand, data);
            renderTypeCache.put(key, cachedTypes);
        }
        return cachedTypes;
    }

    private CachedRenderTypes buildRenderTypeCache(CamoContent<?> camoState, RandomSource rand, ModelData data)
    {
        ChunkRenderTypeSet camoTypes = BASE_MODEL_RENDER_TYPES;
        if (!camoState.isEmpty())
        {
            camoTypes = CamoContainerHelper.Client.getRenderTypes(camoState, rand, data);
        }
        return new CachedRenderTypes(
                camoTypes,
                geometry.getAdditionalRenderTypes(rand, data),
                geometry.getOverlayRenderTypes(rand, data)
        );
    }

    private List<BakedQuad> getCamoQuads(
            @Nullable CamoContent<?> camoContent,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            FramedBlockData fbData,
            @Nullable RenderType renderType
    )
    {
        boolean nullLayer = renderType == null;
        ModelData camoData;
        BakedModel camoModel;
        boolean noProcessing = stateCache.isFullFace(side);
        boolean needCtCtx;
        CachedRenderTypes renderTypes;
        boolean reinforce;

        if (camoContent == null)
        {
            needCtCtx = false;
            camoContent = getNoCamoModelSourceContent(fbData);
            reinforce = useBaseModel && fbData.isReinforced() && renderType == RenderType.cutout() && side != null;
            noProcessing |= forceUngeneratedBaseModel && (nullLayer || BASE_MODEL_RENDER_TYPES.contains(renderType));
            camoModel = getCamoModel(camoContent, useBaseModel, fbData.useAltModel());
            camoData = ModelData.EMPTY;
            renderTypes = getCachedRenderTypes(EmptyCamoContent.EMPTY, camoContent, rand, extraData);
        }
        else
        {
            needCtCtx = type.supportsConnectedTextures() && needCtContext(noProcessing, type.getMinimumConTexMode());
            camoModel = getCamoModel(camoContent, false, false);
            camoData = needCtCtx ? ModelUtils.getCamoModelData(extraData) : ModelData.EMPTY;
            renderTypes = getCachedRenderTypes(camoContent, camoContent, rand, extraData);
            reinforce = false;
        }

        if (noProcessing)
        {
            boolean camoInRenderType = nullLayer || renderTypes.camoTypes.contains(renderType);
            boolean additionalQuads = !nullLayer && renderTypes.additionalTypes.contains(renderType);
            if (!camoInRenderType && !additionalQuads && !reinforce)
            {
                return List.of();
            }

            boolean needCopy = additionalQuads || reinforce || uncachedPostProcess;
            List<BakedQuad> quads = List.of();
            if (camoInRenderType)
            {
                quads = camoModel.getQuads(camoContent.getAppearanceState(), side, rand, camoData, renderType);
                if (quads.isEmpty())
                {
                    // Try extracting useful quads from the list of (supposedly) uncullable quads if querying cullable
                    // ones returned nothing due to the dev forgetting to specify cull-faces in the model
                    quads = ModelUtils.getFilteredNullQuads(camoModel, camoContent.getAppearanceState(), rand, camoData, renderType, side);
                }
                if (camoContent.isEmissive())
                {
                    quads = Utils.copyAllWithModifier(quads, new ArrayList<>(quads.size()), EMISSIVE_PROCESSOR);
                    needCopy = false;
                }
            }
            if (needCopy)
            {
                quads = Utils.copyAll(quads, new ArrayList<>(quads.size()));
            }
            if (additionalQuads)
            {
                // List of quads is guaranteed to be an ArrayList by this point
                geometry.getAdditionalQuads((ArrayList<BakedQuad>) quads, side, rand, extraData, renderType);
            }
            if (reinforce)
            {
                quads.add(ReinforcementModel.getQuad(side));
            }
            if (uncachedPostProcess)
            {
                geometry.postProcessUncachedQuads(quads);
            }
            return quads;
        }
        else
        {
            Object ctCtx = needCtCtx ? ConTexDataHandler.extractConTexData(camoData) : null;
            QuadCacheKey key = geometry.makeCacheKey(camoContent, ctCtx, extraData);
            QuadTable quadTable = quadCache.get(key);
            if (quadTable == null)
            {
                ModelData ctData = ctCtx != null ? camoData : ModelData.EMPTY;
                quadTable = buildQuadCache(key.camo(), camoModel, rand, extraData, ctData, renderTypes, reinforce);
                quadCache.put(key, quadTable);
            }
            return nullLayer ? quadTable.getAllQuads(side) : quadTable.getQuads(renderType, side);
        }
    }

    private static boolean needCtContext(boolean noProcessing, ConTexMode minMode)
    {
        ConTexMode mode = ClientConfig.VIEW.getConTexMode();
        if (mode == ConTexMode.NONE)
        {
            return false;
        }
        return noProcessing || (mode.atleast(ConTexMode.FULL_EDGE) && mode.atleast(minMode));
    }

    /**
     * Builds a {@link RenderType} -> {@link Direction} -> {@link List<BakedQuad>} table with all render types used by this model
     */
    private QuadTable buildQuadCache(
            CamoContent<?> camoContent,
            BakedModel camoModel,
            RandomSource rand,
            ModelData data,
            ModelData camoData,
            CachedRenderTypes renderTypes,
            boolean reinforce
    )
    {
        QuadTable quadTable = new QuadTable();
        UnaryOperator<BakedQuad> preprocessor = camoContent.isEmissive() ? EMISSIVE_PROCESSOR : UnaryOperator.identity();

        for (RenderType renderType : renderTypes.camoTypes)
        {
            quadTable.initializeForLayer(renderType);

            ArrayList<BakedQuad> quads = ModelUtils.getCullableQuads(camoModel, camoContent.getAppearanceState(), rand, camoData, renderType, xformDirFilter);
            if (reinforce && renderType == RenderType.cutout())
            {
                ReinforcementModel.getFiltered(quads, xformDirFilter);
            }
            for (BakedQuad quad : quads)
            {
                quad = preprocessor.apply(quad);
                geometry.transformQuad(quadTable, quad, data);
            }
        }
        for (RenderType renderType : renderTypes.additionalTypes)
        {
            quadTable.initializeForLayer(renderType);
            geometry.getAdditionalQuads(quadTable, rand, data, renderType);
        }
        for (RenderType renderType : renderTypes.overlayTypes)
        {
            quadTable.initializeForLayer(renderType);
            geometry.getGeneratedOverlayQuads(quadTable, rand, data, renderType);
        }
        quadTable.bindRenderType(null);
        quadTable.trim();

        return quadTable;
    }

    private CamoContent<?> getNoCamoModelSourceContent(FramedBlockData fbData)
    {
        int idx = 0;
        if (fbData.useAltModel()) idx |= FLAG_NO_CAMO_ATL_MODEL;
        if (fbData.isReinforced()) idx |= FLAG_NO_CAMO_REINFORCED;
        if (ClientConfig.VIEW.getSolidFrameMode().useSolidFrame(useSolidBase)) idx |= FLAG_NO_CAMO_SOLID_BG;
        return noCamoContents[idx];
    }

    private static BlockCamoContent[] makeNoCamoContents(BlockState state)
    {
        BlockCamoContent[] contents = new BlockCamoContent[1 << 3];
        for (int i = 0; i < contents.length; i++)
        {
            BlockState stateOut = state;
            if ((i & FLAG_NO_CAMO_ATL_MODEL) != 0) stateOut = stateOut.setValue(PropertyHolder.ALT, true);
            if ((i & FLAG_NO_CAMO_REINFORCED) != 0) stateOut = stateOut.setValue(PropertyHolder.REINFORCED, true);
            if ((i & FLAG_NO_CAMO_SOLID_BG) != 0) stateOut = stateOut.setValue(PropertyHolder.SOLID_BG, true);
            contents[i] = new BlockCamoContent(stateOut);
        }
        return contents;
    }

    /**
     * Return the {@link BakedModel} to use as the camo model for the given camoState
     *
     * @param camoContent The {@link CamoContent} used as camo
     * @param useBaseModel If true, the {@link BakedModelWrapper#originalModel} is requested instead of the model of the given state
     * @param useAltModel Whether an alternative base model for the second component of a double model should be used
     *                    (only has an effect if {@code useBaseModel} is true)
     *
     * @apiNote Most models shouldn't need to override this. If the model loaded from JSON should be used when no camo
     * is applied, return true from {@link Geometry#useBaseModel()}. If the model loaded from JSON should be
     * used without applying any quad modifications when no camo is applied, return true from
     * {@link Geometry#forceUngeneratedBaseModel()} as well
     */
    private BakedModel getCamoModel(CamoContent<?> camoContent, boolean useBaseModel, boolean useAltModel)
    {
        if (useBaseModel)
        {
            return geometry.getBaseModel(originalModel, useAltModel);
        }
        return CamoContainerHelper.Client.getOrCreateModel(camoContent);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData tileData)
    {
        if (!type.supportsConnectedTextures())
        {
            return tileData;
        }

        FramedBlockData data = tileData.get(FramedBlockData.PROPERTY);
        if (data == null)
        {
            return tileData;
        }

        CamoContent<?> camoContent = data.getCamoContent();
        ModelData camoData = ModelData.EMPTY;
        if (!camoContent.isEmpty() && needCtContext(stateCache.hasAnyFullFace(), type.getMinimumConTexMode()))
        {
            BakedModel model = CamoContainerHelper.Client.getOrCreateModel(camoContent);
            try
            {
                // Try getting camo data with the enclosing state, some mods may not like that
                // This option provides better CT behaviour
                camoData = model.getModelData(level, pos, state, ModelData.EMPTY);
            }
            catch (Throwable t)
            {
                // Fall back to getting camo data with the camo state if a mod didn't like it
                // This option may cause some CT weirdness
                camoData = model.getModelData(level, pos, camoContent.getAppearanceState(), ModelData.EMPTY);
            }
        }
        ModelData auxData = geometry.getAuxModelData(level, pos, state, tileData);
        // Avoid copying the entire data if the camo model and geometry didn't produce any additional model data
        if (camoData != ModelData.EMPTY || auxData != ModelData.EMPTY)
        {
            ModelData.Builder builder = tileData.derive();
            if (camoData != ModelData.EMPTY)
            {
                builder.with(FramedBlockData.CAMO_DATA, camoData);
            }
            if (auxData != ModelData.EMPTY)
            {
                builder.with(FramedBlockData.AUX_DATA, auxData);
            }
            tileData = builder.build();
        }
        return tileData;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon(ModelData data)
    {
        FramedBlockData fbdata = data.get(FramedBlockData.PROPERTY);
        if (fbdata != null)
        {
            CamoContent<?> camoState = fbdata.getCamoContent();
            if (!camoState.isEmpty())
            {
                return getCamoModel(camoState, false, false).getParticleIcon();
            }
        }
        return originalModel.getParticleIcon();
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType)
    {
        return geometry.useAmbientOcclusion(state, data, renderType);
    }

    public BakedModel getBaseModel()
    {
        return originalModel;
    }

    @Override
    public void clearCache()
    {
        super.clearCache();
        quadCache.clear();
        renderTypeCache.clear();
    }



    private record CachedRenderTypes(
            ChunkRenderTypeSet camoTypes,
            ChunkRenderTypeSet additionalTypes,
            ChunkRenderTypeSet overlayTypes,
            ChunkRenderTypeSet allTypes
    )
    {
        public CachedRenderTypes(ChunkRenderTypeSet camoTypes, ChunkRenderTypeSet additionalTypes, ChunkRenderTypeSet overlayTypes)
        {
            this(camoTypes, additionalTypes, overlayTypes, ChunkRenderTypeSet.union(camoTypes, additionalTypes, overlayTypes));
        }
    }
}
