package nin.transferpipe.block.status;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.TransferPipeBlock;
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;

public class Search {

    /**
     * 基本情報
     */

    private final TransferNodeBlockEntity be;
    private BlockPos currentPos;
    private Direction previousPosDir;

    public static String CURRENT_POS = "CurrentPos";
    public static String PREVIOUS_POS_DIR = "PreviousPosDir";

    public Search(TransferNodeBlockEntity be, BlockPos currentPos) {
        this.be = be;
        this.currentPos = currentPos;
    }

    public CompoundTag write() {
        var tag = new CompoundTag();
        tag.put(CURRENT_POS, NbtUtils.writeBlockPos(currentPos));
        if (previousPosDir != null)
            tag.putString(PREVIOUS_POS_DIR, previousPosDir.name());

        return tag;
    }

    public Search read(CompoundTag tag) {
        if (tag.contains(CURRENT_POS))
            currentPos = NbtUtils.readBlockPos(tag.getCompound(CURRENT_POS));
        if (tag.contains(PREVIOUS_POS_DIR))
            previousPosDir = Direction.byName(tag.getString(PREVIOUS_POS_DIR));

        return this;
    }


    /**
     * 機能
     */


    public void next() {
        var myState = PipeUtils.currentState(be.getLevel(), currentPos);
        if (myState == null) {//パイプから外れてるのに検索されたのなら
            be.terminal(currentPos);
            be.resetSearchState();
            return;//目的地に着いたということで終了
        }

        var validDirections = Direction.stream()
                .filter(d -> d != previousPosDir)
                .filter(d -> PipeUtils.canGoThroughPipe(myState.getValue(TransferPipeBlock.FLOW), d))
                .filter(d -> myState.getValue(TransferPipeBlock.CONNECTIONS.get(d)) != Connection.NONE).toList();
        if (validDirections.size() == 0) {
            be.resetSearchState();
            return;//詰まったので終了
        }
        var directionToSearch = TPUtils.getRandomlyFrom(validDirections, be.getLevel().random);

        currentPos = currentPos.relative(directionToSearch);
        previousPosDir = directionToSearch.getOpposite();
        be.setChanged();//タイルのSearchStateが更新された

        if (be.getLevel() instanceof ServerLevel sl)
            TPUtils.addParticle(sl, ParticleTypes.ANGRY_VILLAGER, currentPos.getCenter(), Vec3.ZERO, 0);
    }
}
