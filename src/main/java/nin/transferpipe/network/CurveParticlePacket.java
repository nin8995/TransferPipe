package nin.transferpipe.network;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CurveParticlePacket extends BasePacket.arg3<Entity, Vec3, Vec3> {

    @Override
    public FriendlyByteBuf encode(FriendlyByteBuf buf) {
        buf.writeVector3f(startPos.toVector3f());
        buf.writeVector3f(startVel.toVector3f());
        buf.writeVector3f(endPos.toVector3f());
        buf.writeVector3f(endVel.toVector3f());
        return buf;
    }

    @Override
    public BasePacket decode(FriendlyByteBuf buf) {
        startPos = new Vec3(buf.readVector3f());
        startVel = new Vec3(buf.readVector3f());
        endPos = new Vec3(buf.readVector3f());
        endVel = new Vec3(buf.readVector3f());
        return this;
    }

    public Vec3 startPos;
    public Vec3 startVel;
    public Vec3 endPos;
    public Vec3 endVel;

    @Override
    public BasePacket init(Entity entity, Vec3 vec3, Vec3 vec32) {
        startPos = vec3;
        startVel = vec32.scale(10);
        endPos = entity.position();
        endVel = entity.getDeltaMovement().scale(10);
        return this;
    }

    @Override
    public void handleOnClient(Player p) {
        var rand = p.level.random.nextFloat() * 0.6F + 0.4F;
        var color = new Vector3f(rand, rand * 0.3F, rand * 0.9F);
        var particle = new DustParticleOptions(color, 1.0F);

        var relative = endPos.subtract(startPos);
        var coefs = new Vec3[]{
                relative.scale(-2).add(endVel.add(startVel)),
                relative.scale(3).subtract(endVel.add(startVel.scale(2))),
                startVel,
                startPos};
        var step = 0.15D / relative.length();
        for (double t = 0; t <= 1.0D; t += step) {
            var r = coefs[0].scale(t).add(coefs[1]).scale(t).add(coefs[2]).scale(t).add(coefs[3]);
            p.level.addParticle(particle, r.x, r.y, r.z, 0, 0, 0);
        }
    }
}
