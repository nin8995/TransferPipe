package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TileHolderEntity;
import nin.transferpipe.block.node.BaseBlockNode;
import nin.transferpipe.block.node.BaseTileNode;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.TransferPipe;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

import static nin.transferpipe.block.pipe.TransferPipe.CONNECTIONS;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;

public interface TPUtils {

    /**
     * パラメーター取得
     */
    static BlockState currentState(Level level, BlockPos pos) {
        var bs = level.getBlockState(pos);
        return bs.getBlock() instanceof TransferPipe
               ? bs
               : level.getBlockEntity(pos) instanceof BaseTileNode be
                 ? be.pipeState
                 : null;//PipeStateを得得ないときにnull
    }

    @Nullable
    static Block currentPipeBlock(Level level, BlockPos pos) {
        var state = currentState(level, pos);
        return state != null ? state.getBlock() : null;
    }

    @Nullable
    static Direction currentNodeDir(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BaseBlockNode.Facing<?> node ? node.facing(level, pos) : null;
    }

    static Connection connection(BlockState state, Direction dir) {
        return state.getValue(CONNECTIONS.get(dir));
    }

    static Flow flow(BlockState state) {
        return state.getValue(FLOW);
    }

    @Nullable
    static Connection currentConnection(Level level, BlockPos pos, Direction dir) {
        return PipeInstance.of(level, pos).map(it -> it.connections.get(dir)).orElse(null);
    }

    @Nullable
    static Flow currentFlow(Level level, BlockPos pos) {
        return PipeInstance.of(level, pos).map(it -> it.flow).orElse(null);
    }

    /**
     * 見た目がキューブ
     */
    Set<BlockState> centers = TPBlocks.PIPES.stream().map(RegistryObject::get)
            .map(Block::defaultBlockState)
            .flatMap(state -> Flow.stream().map(flow -> state.setValue(FLOW, flow)))
            .collect(Collectors.toSet());

    static boolean centerOnly(BlockState bs) {
        return centers.contains(bs);
    }

    /**
     * レンチ判定
     */
    TagKey<Item> WRENCH_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    static boolean usingWrench(Player pl, InteractionHand hand) {
        var item = pl.getItemInHand(hand);
        return item.is(Items.STICK) || item.is(WRENCH_TAG);
    }

    /**
     * TileHolderを考慮したlevel#getBlockEntity
     */
    static BlockEntity getTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileHolderEntity tileHolder
               ? tileHolder.holdingTile
               : level.getBlockEntity(pos);
    }
}
