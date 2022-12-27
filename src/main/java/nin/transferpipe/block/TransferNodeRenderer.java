package nin.transferpipe.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import nin.transferpipe.util.PipeStateUtil;

public class TransferNodeRenderer implements BlockEntityRenderer<TransferNodeBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public TransferNodeRenderer(BlockEntityRendererProvider.Context p_173623_) {
        this.blockRenderer = p_173623_.getBlockRenderDispatcher();
    }

    @Override
    public void render(TransferNodeBlockEntity be, float p_112308_, PoseStack pose, MultiBufferSource mbs, int p_112311_, int p_112312_) {
        var vc = mbs.getBuffer(RenderType.cutout());
        var bs = be.getPipeState();
        if (PipeStateUtil.hasNoConnection(bs))
            return;
        var bp = be.getBlockPos();
        blockRenderer.getModelRenderer().tesselateBlock(be.getLevel(), blockRenderer.getBlockModel(bs), bs, bp, pose, vc, true, RandomSource.create(), bs.getSeed(bp), 0);
    }
}
