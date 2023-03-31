package nin.transferpipe.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.block.status.Search;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.Nullable;

//搬送する種類に依らない、「ノード」のタイルエンティティとしての機能
public abstract class TransferNodeBlockEntity extends BlockEntity  {

    /**
     * 基本情報
     */

    //タイルのフィールドは変更を知らせないといけないので普通setterを持つ
    private BlockState pipeState = PipeUtils.defaultState();
    private Search search;
    private int cooltime;
    private boolean initialized;//でもこれは内部的な値なのでsetterとかない

    public static final String PIPE_STATE = "PipeState";
    public static final String SEARCH = "Search";
    public static String COOLTIME = "Cooltime";
    public static final String INITIALIZED = "Initialized";

    public TransferNodeBlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        search = new Search(this, this.worldPosition);
    }

    /**
     * フィールドの取り扱い
     */

    public BlockState getPipeState() {
        return pipeState;
    }

    public void setPipeStateAndUpdate(BlockState state) {
        if (pipeState != state) {
            pipeState = state;
            setChanged();//タイルエンティティ更新時の処理
            level.markAndNotifyBlock(getBlockPos(), level.getChunkAt(getBlockPos()), getBlockState(), getBlockState(), 3, 512);//ブロック更新時の処理
        }
    }

    public Search getSearchState() {
        return search;
    }

    public void setSearchState(Search ss) {
        search = ss;
        setChanged();
    }

    public void resetSearchState() {
        setSearchState(new Search(this, this.worldPosition));
    }

    /**
     * NBT変換
     */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        tag.put(SEARCH, search.write());
        tag.putInt(COOLTIME, cooltime);
        tag.putBoolean(INITIALIZED, initialized);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(PIPE_STATE))
            pipeState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(PIPE_STATE));
        if (tag.contains(SEARCH))
            search = search.read(tag.getCompound(SEARCH));
        if (tag.contains(COOLTIME))
            cooltime = tag.getInt(COOLTIME);
        if (tag.contains(INITIALIZED))
            initialized = tag.getBoolean(INITIALIZED);
    }

    //見た目を変化させる変更をクライアントに伝えるためのタグ
    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.put(PIPE_STATE, NbtUtils.writeBlockState(pipeState));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 一般の機能
     */

    //pipeState(特定のパイプの特定の状況)を表示
    public static class Renderer implements BlockEntityRenderer<TransferNodeBlockEntity> {

        private final BlockRenderDispatcher blockRenderer;

        public Renderer(BlockEntityRendererProvider.Context p_173623_) {
            this.blockRenderer = p_173623_.getBlockRenderDispatcher();
        }

        @Override
        public void render(TransferNodeBlockEntity be, float p_112308_, PoseStack pose, MultiBufferSource mbs, int p_112311_, int p_112312_) {
            var pipeState = be.getPipeState();

            if (be.shouldRenderPipe())
                TPUtils.renderBlockState(pipeState, be.getLevel(), be.getBlockPos(),
                        blockRenderer, pose, mbs.getBuffer(RenderType.cutout()));
        }
    }

    public boolean shouldRenderPipe() {
        return !PipeUtils.centerOnly(pipeState);
    }

    public void tick(Level level, BlockPos pos) {
        //インスタンス生成時はthis.levelがnullでpipeState分らんからここで
        if (!initialized) {
            this.setPipeStateAndUpdate(PipeUtils.recalcConnections(level, pos));
            initialized = true;
        }

        decreaseCooltime();

        while (cooltime <= 0) {
            search.next();
            cooltime += 10;
        }
    }

    public void decreaseCooltime() {
        cooltime--;
    }

    /**
     * 搬送種毎の機能
     */

    public abstract void terminal(BlockPos pos);

    public static class Item extends TransferNodeBlockEntity {

        public Item(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ITEM.type(), p_155229_, p_155230_);
        }

        @Override
        public void terminal(BlockPos pos) {

        }
    }
}
