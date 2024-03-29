package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.node.BaseNodeBlock;
import nin.transferpipe.block.node.BaseTileNode;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.Pipe;
import nin.transferpipe.item.filter.PatternSlot;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.TileHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nin.transferpipe.block.pipe.Pipe.CONNECTIONS;
import static nin.transferpipe.block.pipe.Pipe.FLOW;

public interface TPUtils {

    /**
     * パラメーター取得
     */
    static BlockState currentState(Level level, BlockPos pos) {
        var bs = level.getBlockState(pos);
        return bs.getBlock() instanceof Pipe
               ? bs
               : level.getBlockEntity(pos) instanceof BaseTileNode<?> be
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
        return level.getBlockState(pos).getBlock() instanceof BaseNodeBlock.Facing<?> node ? node.facing(level, pos) : null;
    }

    static Connection connection(BlockState state, Direction dir) {
        return state.getValue(CONNECTIONS.get(dir));
    }

    static Flow flow(BlockState state) {
        return hasFlow(state) ? state.getValue(FLOW) : Flow.ALL;
    }

    static BlockState withFlow(BlockState state, Flow flow) {
        return hasFlow(state) ? state.setValue(FLOW, flow) : state;
    }

    static BlockState withFlow(BlockState state, BlockState flowReference) {
        return hasFlow(state) && hasFlow(flowReference)
               ? state.setValue(FLOW, flowReference.getValue(FLOW))
               : state;
    }

    @Nullable
    static Connection currentConnection(Level level, BlockPos pos, Direction dir) {
        return PipeInstance.of(level, pos).map(it -> it.connections.get(dir)).orElse(null);
    }

    @Nullable
    static Flow currentFlow(Level level, BlockPos pos) {
        return PipeInstance.of(level, pos).map(it -> it.flow).orElse(null);
    }

    static boolean hasFlow(BlockState state) {
        return state.hasProperty(FLOW);
    }

    static boolean hasFlow(Block block) {
        return hasFlow(block.defaultBlockState());
    }

    /**
     * 見た目がキューブ
     */
    Set<BlockState> centers = TPBlocks.PIPES.stream().map(RegistryObject::get)
            .map(Block::defaultBlockState)
            .flatMap(state -> TPUtils.hasFlow(state)
                              ? Flow.stream().map(flow -> state.setValue(FLOW, flow))
                              : Stream.of(state))
            .collect(Collectors.toSet());

    static boolean centerOnly(BlockState bs) {
        return centers.contains(bs);
    }

    /**
     * レンチ判定
     */
    TagKey<Item> WRENCH_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    static boolean usingWrench(Player player, InteractionHand hand) {
        return isWrench(player.getItemInHand(hand));
    }

    static boolean isWrench(ItemStack item) {
        return item.is(Items.STICK) || item.is(WRENCH_TAG);
    }

    /**
     * TileHolderを考慮したlevel#getBlockEntity
     */
    static BlockEntity getInnerTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileHolder tileHolder
               ? tileHolder.holdingTile
               : level.getBlockEntity(pos);
    }

    static BlockEntity getOuterTile(Level level, BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    /**
     * 同じ種類のパターン一つだけを登録
     */
    static boolean trySetPattern(List<? extends PatternSlot> patterns, ItemStack toSet) {
        var availableSlots = JavaUtils.filter(patterns, s -> !s.hasPattern() && s.shouldSet(toSet));
        if (!availableSlots.isEmpty() && patterns.stream().noneMatch(s -> s.isSamePattern(toSet))) {
            availableSlots.get(0).setPattern(toSet);
            return true;
        }
        return false;
    }
}
