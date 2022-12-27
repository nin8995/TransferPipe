package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.search.SearchState;
import nin.transferpipe.util.NBTUtil;
import nin.transferpipe.util.PipeStateUtil;
import org.jetbrains.annotations.Nullable;

public class TransferNodeBlockEntity extends BlockEntity {

    private BlockState pipeState;
    private static final String BLOCK_STATE_KEY = "BlockState";
    private boolean init;
    private SearchState searchState;

    public TransferNodeBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(TransferPipe.TRANSFER_NODE_ITEM_BE.get(), p_155229_, p_155230_);
        this.pipeState = PipeStateUtil.defaultPipeState();
    }

    public static <T> void tick(Level l, BlockPos p, BlockState bs, T t) {
        if (t instanceof TransferNodeBlockEntity be) {
            if (!be.init && be.getPipeState() == PipeStateUtil.defaultPipeState()) {
                be.setPipeState(PipeStateUtil.recalcPipeState(l, p));
                be.init = true;
            }

            if (be.searchState == null)
                be.setSearchState(new SearchState(be, p));
            be.searchState.tick();
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(BLOCK_STATE_KEY, NBTUtil.writeBlockState(pipeState));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        pipeState = NBTUtil.readBlockState(TransferPipe.TRANSFER_PIPE.get(), tag.getCompound(BLOCK_STATE_KEY));
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.put(BLOCK_STATE_KEY, NBTUtil.writeBlockState(pipeState));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public BlockState getPipeState() {
        return pipeState;
    }

    public void setPipeState(BlockState bs) {
        if (pipeState != bs) {
            pipeState = bs;
            setChanged();
            level.markAndNotifyBlock(getBlockPos(), level.getChunkAt(getBlockPos()), getBlockState(), getBlockState(), 3, 512);
        }
    }

    public SearchState getSearchState() {
        return searchState;
    }

    public void setSearchState(SearchState ss) {
        searchState = ss;
        setChanged();
    }

    public void resetSearchState() {
        setSearchState(null);
    }
}
