package nin.transferpipe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import nin.transferpipe.MixinTemp;
import nin.transferpipe.block.TileBeyondBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LevelChunk.class)
public abstract class HoldTileDataBeyondBlockState {

    @Shadow
    @Nullable
    public abstract BlockEntity getBlockEntity(BlockPos p_62912_);

    @Inject(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    public void storeNBT(BlockPos pos, BlockState p_62866_, boolean p_62867_, CallbackInfoReturnable<BlockState> cir) {
        if (getBlockEntity(pos) instanceof TileBeyondBlockState)
            MixinTemp.tileData.set(getBlockEntity(pos).saveWithFullMetadata());
    }

    //setBlockStateから呼ばれたTile追加処理だけに当てたかったけど、そこでtileを取得するためのcapture localの使い方が分からなかったのでこっちで代用
    @Inject(method = "addAndRegisterBlockEntity", at = @At("HEAD"))
    public void loadNBT(BlockEntity tile, CallbackInfo ci) {
        if (MixinTemp.tileData.get() != null) {
            tile.load(MixinTemp.tileData.get());
            MixinTemp.tileData.remove();
        }
    }
}
