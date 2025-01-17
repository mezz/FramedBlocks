package xfacthd.framedblocks.selftest;

import com.google.common.base.Stopwatch;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import xfacthd.framedblocks.FramedBlocks;
import xfacthd.framedblocks.api.block.IFramedBlock;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.selftest.tests.*;

import java.util.List;

public final class SelfTest
{
    public static void runSelfTest(@SuppressWarnings("unused") final FMLLoadCompleteEvent event)
    {
        FramedBlocks.LOGGER.info("=======================================");
        FramedBlocks.LOGGER.info("Running self-test");
        Stopwatch stopwatch = Stopwatch.createStarted();

        List<Block> blocks = FBContent.getRegisteredBlocks()
                .stream()
                .map(Holder::value)
                .filter(IFramedBlock.class::isInstance)
                .toList();

        OcclusionPropertyConsistency.checkOcclusionProperty(blocks);
        WaterloggingPropertyConsistency.checkWaterloggingProperty(blocks);
        LockingPropertyConsistency.checkLockingProperty(blocks);
        ClientBlockExtensionsPresence.checkClientExtensionsPresent(blocks);
        SpecialShapeRendererPresence.checkSpecialShapePresent(blocks);
        SkipPredicatePresenceConsistency.checkSkipPredicateConsistency();
        StateCacheValidity.checkStateCacheValid(blocks);
        DoubleBlockCamoConnectionConsistency.checkConnectionConsistency(blocks);
        DoubleBlockSolidSideConsistency.checkSolidSideConsistency(blocks);
        RotateMirrorErrors.checkRotateMirrorErrors(blocks);
        JadeRenderStateErrors.checkJadeRenderStateErrors(blocks);
        BlockEntityPresence.checkBlockEntityTypePresent(blocks);

        stopwatch.stop();
        FramedBlocks.LOGGER.info("Self test completed in {}", stopwatch);
        FramedBlocks.LOGGER.info("=======================================");
    }



    private SelfTest() { }
}
