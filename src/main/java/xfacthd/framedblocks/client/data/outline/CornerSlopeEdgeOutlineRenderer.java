package xfacthd.framedblocks.client.data.outline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.block.state.BlockState;
import xfacthd.framedblocks.api.render.OutlineRenderer;
import xfacthd.framedblocks.api.render.Quaternions;
import xfacthd.framedblocks.common.data.PropertyHolder;
import xfacthd.framedblocks.common.data.property.CornerType;

public final class CornerSlopeEdgeOutlineRenderer implements OutlineRenderer
{
    @Override
    public void draw(BlockState state, PoseStack poseStack, VertexConsumer builder)
    {
        if (state.getValue(PropertyHolder.ALT_TYPE))
        {
            //Back edges
            OutlineRenderer.drawLine(builder, poseStack, .5F, .5F, .5F, .5F, 1, .5F);
            OutlineRenderer.drawLine(builder, poseStack,   1, .5F, .5F,   1, 1, .5F);
            OutlineRenderer.drawLine(builder, poseStack, .5F, .5F,   1, .5F, 1,   1);
            OutlineRenderer.drawLine(builder, poseStack, .5F, .5F, .5F, .5F, .5F,   1);
            OutlineRenderer.drawLine(builder, poseStack, .5F, .5F, .5F,   1, .5F, .5F);
            OutlineRenderer.drawLine(builder, poseStack, .5F,   1, .5F, .5F,   1,   1);
            OutlineRenderer.drawLine(builder, poseStack, .5F,   1, .5F,   1,   1, .5F);

            //Bottom face
            OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 0,   0, .5F,   1);
            OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 0,   1, .5F,   0);
            OutlineRenderer.drawLine(builder, poseStack, 1, .5F, 0,   1, .5F, .5F);
            OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 1, .5F, .5F,   1);

            //Slope
            OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 0, .5F, 1, .5F);
            OutlineRenderer.drawLine(builder, poseStack, 1, .5F, 0,   1, 1, .5F);
            OutlineRenderer.drawLine(builder, poseStack, 0, .5F, 1, .5F, 1,   1);
        }
        else
        {
            //Back edge
            OutlineRenderer.drawLine(builder, poseStack, 1, 0, 1, 1, .5F, 1);

            //Bottom face
            OutlineRenderer.drawLine(builder, poseStack, .5F, 0, .5F, .5F, 0,   1);
            OutlineRenderer.drawLine(builder, poseStack, .5F, 0, .5F,   1, 0, .5F);
            OutlineRenderer.drawLine(builder, poseStack,   1, 0, .5F,   1, 0,   1);
            OutlineRenderer.drawLine(builder, poseStack, .5F, 0,   1,   1, 0,   1);

            //Slope
            OutlineRenderer.drawLine(builder, poseStack, .5F, 0, .5F, 1, .5F, 1);
            OutlineRenderer.drawLine(builder, poseStack,   1, 0, .5F, 1, .5F, 1);
            OutlineRenderer.drawLine(builder, poseStack, .5F, 0,   1, 1, .5F, 1);
        }
    }

    @Override
    public void rotateMatrix(PoseStack poseStack, BlockState state)
    {
        OutlineRenderer.super.rotateMatrix(poseStack, state);

        CornerType type = state.getValue(PropertyHolder.CORNER_TYPE);
        if (!type.isHorizontal())
        {
            if (type.isTop())
            {
                OutlineRenderer.mirrorHorizontally(poseStack, true);
            }
        }
        else
        {
            poseStack.mulPose(Quaternions.XN_90);
            switch (type)
            {
                case HORIZONTAL_TOP_RIGHT -> poseStack.mulPose(Quaternions.YN_90);
                case HORIZONTAL_BOTTOM_LEFT -> poseStack.mulPose(Quaternions.YP_90);
                case HORIZONTAL_BOTTOM_RIGHT -> poseStack.mulPose(Quaternions.YP_180);
            }
        }
    }
}
