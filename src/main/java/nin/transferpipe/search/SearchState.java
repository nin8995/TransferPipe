package nin.transferpipe.search;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.property.ConnectionStates;
import nin.transferpipe.block.property.TPProperties;
import nin.transferpipe.util.ParticleUtil;
import nin.transferpipe.util.PipeStateUtil;

import java.util.Random;

public class SearchState {
    private final TransferNodeBlockEntity be;
    private final Random rand = new Random();
    private Direction previouslySearched;
    private BlockPos currentlySearching;
    private int cooltime;

    public SearchState(TransferNodeBlockEntity be, BlockPos currentlySearching) {
        this.be = be;
        this.currentlySearching = currentlySearching;
    }

    public void tick() {
        if (cooltime < 0) {
            searchNext();
            cooltime += 10;
        }
        cooltime--;
    }

    public void searchNext() {
        var myState = PipeStateUtil.currentPipeState(be.getLevel(), currentlySearching);
        if (myState == null) {
            be.resetSearchState();
            return;
        }

        var validDirections = Direction.stream()
                .filter(d -> PipeStateUtil.canGo(myState.getValue(TPProperties.FLOW), d))
                .filter(d -> myState.getValue(TPProperties.CONNECTIONS.get(d)) != ConnectionStates.NONE)
                .filter(d -> d != previouslySearched)
                .toList();
        if (validDirections.size() == 0) {
            be.resetSearchState();
            return;
        }

        var directionToSearch = validDirections.size() == 1 ? validDirections.get(0)
                : validDirections.get(rand.nextInt(validDirections.size()));
        currentlySearching = currentlySearching.relative(directionToSearch);
        previouslySearched = directionToSearch.getOpposite();

        if (be.getLevel() instanceof ServerLevel sl)
            ParticleUtil.add(sl, ParticleTypes.ANGRY_VILLAGER, currentlySearching.getCenter(), Vec3.ZERO, 0);
        be.setChanged();
    }

}
