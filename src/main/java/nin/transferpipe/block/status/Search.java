package nin.transferpipe.block.status;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;

import java.util.stream.Stream;

public class Search {

    /**
     * 基本情報
     */

    private BlockPos pos;
    private Direction previousPosDir;

    public static String POS = "Pos";
    public static String PREVIOUS_POS_DIR = "PreviousPosDir";
    public TransferNodeBlockEntity be;
    public Level level;//beから取れるけど簡略化
    private boolean initialized;//levelをフィールドに使うということは最初の動作時に初期化しないといけないことを意味する

    public Search(TransferNodeBlockEntity be, BlockPos pos, Direction facing) {
        this.be = be;

        this.pos = pos;
        this.previousPosDir = facing;
    }

    //dirに進む
    public void proceed(Direction dir) {
        pos = pos.relative(dir);
        previousPosDir = dir.getOpposite();
        be.setChanged();//タイルのSearchStateが更新された
    }

    public CompoundTag write() {
        var tag = new CompoundTag();
        tag.put(POS, NbtUtils.writeBlockPos(pos));
        tag.putString(PREVIOUS_POS_DIR, previousPosDir.toString());//nameだと大文字になる

        return tag;
    }

    public Search read(CompoundTag tag) {
        if (tag.contains(POS))
            pos = NbtUtils.readBlockPos(tag.getCompound(POS));
        if (tag.contains(PREVIOUS_POS_DIR)) {
            var str = tag.getString(PREVIOUS_POS_DIR);
            previousPosDir = Direction.byName(str);
        }
        return this;
    }

    /**
     * 機能
     */

    public void next() {
        if (!initialized) {
            this.level = be.getLevel();
            initialized = true;
        }

        //分かりやすさのための検索状況パーティクル
        if (level instanceof ServerLevel sl)
            TPUtils.addParticle(sl, ParticleTypes.ANGRY_VILLAGER, pos.getCenter(), Vec3.ZERO, 0);

        //仕事先があれば即出勤
        var workingDir = getWorkingDir();
        if (workingDir != null) {
            proceed(workingDir);
            be.terminal(pos, previousPosDir);
            be.resetSearchStatus();
            return;
        }

        //次を検索
        var nextDir = getNextDir();
        if (nextDir == null) {//見つからなかったらオワオワリ
            be.resetSearchStatus();
            return;
        }

        proceed(nextDir);
    }

    public Direction getNextDir() {
        return TPUtils.getRandomlyFrom(getValidDirs().toList(), level.random);
    }

    public Direction getWorkingDir() {
        var workingDirs = getValidDirs()
                .filter(d -> PipeUtils.currentConnection(level, pos, d) == Connection.MACHINE);
        return TPUtils.getRandomlyFrom(workingDirs.toList(), level.random);
    }

    public Stream<Direction> getValidDirs() {
        return Direction.stream()
                .filter(d -> d != previousPosDir)
                .filter(d -> PipeUtils.currentConnection(level, pos, d) != Connection.NONE)
                .filter(d -> PipeUtils.isFlowOpen(level, pos, d));
    }
}
