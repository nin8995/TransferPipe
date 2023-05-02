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
import nin.transferpipe.block.node.BlockTransferNode;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.TransferPipe;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

import static nin.transferpipe.block.pipe.TransferPipe.CONNECTIONS;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;

public class TPUtils {

    /**
     * パラメーター取得
     */
    public static BlockState currentState(Level level, BlockPos pos) {
        var bs = level.getBlockState(pos);
        return bs.getBlock() instanceof TransferPipe
               ? bs
               : level.getBlockEntity(pos) instanceof TileBaseTransferNode be
                 ? be.pipeState
                 : null;//PipeStateを得得ないときにnull
    }

    @Nullable
    public static Direction currentNodeDir(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BlockTransferNode.FacingNode<?> node ? node.facing(level, pos) : null;
    }

    public static Connection connection(BlockState state, Direction dir) {
        return state.getValue(CONNECTIONS.get(dir));
    }

    public static Flow flow(BlockState state) {
        return state.getValue(FLOW);
    }

    @Nullable
    public static Connection currentConnection(Level level, BlockPos pos, Direction dir) {
        return PipeInstance.of(level, pos).map(it -> it.connections.get(dir)).orElse(null);
    }

    @Nullable
    public static Flow currentFlow(Level level, BlockPos pos) {
        return PipeInstance.of(level, pos).map(it -> it.flow).orElse(null);
    }

    /**
     * 見た目がキューブ
     */
    public static Set<BlockState> centers = TPBlocks.PIPES.stream().map(RegistryObject::get)
            .map(Block::defaultBlockState)
            .flatMap(state -> Flow.stream().map(flow -> state.setValue(FLOW, flow)))
            .collect(Collectors.toSet());

    public static boolean centerOnly(BlockState bs) {
        return centers.contains(bs);
    }

    /**
     * レンチ判定
     */
    public static final TagKey<Item> WRENCH_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    public static boolean usingWrench(Player pl, InteractionHand hand) {
        var item = pl.getItemInHand(hand);
        return item.is(Items.STICK) || item.is(WRENCH_TAG);
    }

    /**
     * TileHolderを考慮したlevel#getBlockEntity
     */
    public static BlockEntity getTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileHolderEntity tileHolder
               ? tileHolder.holdingTile
               : level.getBlockEntity(pos);
    }
}
