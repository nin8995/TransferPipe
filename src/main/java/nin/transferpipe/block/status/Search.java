package nin.transferpipe.block.status;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nin.transferpipe.block.tile.TileTransferNode;
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;

import java.util.stream.Stream;

public class Search {

    /**
     * 基本情報
     */

    private BlockPos currentPos;
    private BlockPos nextPos;
    private Direction previousPosDir;

    public static String CURRENT_POS = "CurrentPos";
    public static String NEXT_POS = "NextPos";
    public static String PREVIOUS_POS_DIR = "PreviousPosDir";
    public TileTransferNode be;
    public Level level;//beから取れるけど簡略化
    private boolean initialized;//levelをフィールドに使うということは最初の動作時に初期化しないといけないことを意味する

    public Search(TileTransferNode be) {
        this.be = be;
        currentPos = be.POS;
        reset();
    }

    public Search reset() {
        nextPos = be.POS;
        previousPosDir = be.FACING;
        return this;
    }

    public BlockPos getCurrentPos() {
        return currentPos;
    }

    public BlockPos getNextPos() {
        return nextPos;
    }

    public void setPos(int x, int y, int z) {
        currentPos = new BlockPos(x, y, z);
    }

    public CompoundTag write() {
        var tag = new CompoundTag();
        tag.put(CURRENT_POS, NbtUtils.writeBlockPos(currentPos));
        tag.put(NEXT_POS, NbtUtils.writeBlockPos(nextPos));
        tag.putString(PREVIOUS_POS_DIR, previousPosDir.toString());//nameだと大文字になる

        return tag;
    }

    public Search read(CompoundTag tag) {
        if (tag.contains(CURRENT_POS))
            currentPos = NbtUtils.readBlockPos(tag.getCompound(CURRENT_POS));
        if (tag.contains(NEXT_POS))
            nextPos = NbtUtils.readBlockPos(tag.getCompound(NEXT_POS));
        if (tag.contains(PREVIOUS_POS_DIR))
            previousPosDir = Direction.byName(tag.getString(PREVIOUS_POS_DIR));

        return this;
    }

    /**
     * 機能
     */

    public Search proceed() {
        if (!initialized) {
            this.level = be.getLevel();
            initialized = true;
        }

        //進
        currentPos = nextPos;

        //分かりやすさのための検索状況パーティクル
        if (level instanceof ServerLevel sl)
            TPUtils.addParticle(sl, ParticleTypes.ANGRY_VILLAGER, currentPos.getCenter(), Vec3.ZERO, 0);

        //仕事先があれば即出勤
        var workableDir = getWorkableDir();
        if (workableDir != null) {
            be.terminal(currentPos.relative(workableDir), workableDir.getOpposite());
            return reset();
        }

        //次を検索しておく
        var nextDir = getNextDir();
        if (nextDir == null)
            return reset();
        nextPos = currentPos.relative(nextDir);
        previousPosDir = nextDir.getOpposite();
        return this;
    }

    public Direction getNextDir() {
        return TPUtils.getRandomlyFrom(getPipeDirs().toList(), level.random);
    }

    public Direction getWorkableDir() {
        var workingDirs = Direction.stream()
                .filter(d -> d != previousPosDir)
                .filter(d -> PipeUtils.currentConnection(level, currentPos, d) == Connection.MACHINE)
                .filter(d -> be.canWork(currentPos.relative(d), d.getOpposite()));
        return TPUtils.getRandomlyFrom(workingDirs.toList(), level.random);
    }

    public Stream<Direction> getPipeDirs() {
        return Direction.stream()
                .filter(d -> d != previousPosDir)
                .filter(d -> PipeUtils.canProceedPipe(level, currentPos, d));
    }
}
