package nin.transferpipe.util;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ParticleUtil {


    public static void add(ServerLevel sl, SimpleParticleType particle, Vec3 pos, Vec3 vel, int speed) {
        sl.players().forEach(sp -> sl.sendParticles(sp, particle, true, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, speed));
    }

    public static void addWithServerEntity(LivingEntity e, SimpleParticleType particle, Vec3 r, Vec3 v) {
        var scale = e.getScale();
        add((ServerLevel) e.getLevel(), particle, e.position().add(r.scale(scale)), v.scale(scale), (int) scale);
    }
}