package nin.transferpipe.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.IntConsumer;

import static nin.transferpipe.TPMod.MODID;

public interface TPParticles {

    DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    RegistryObject<ParticleType<ColorSquare.Option>> COLOR_SQUARE = register("color_square", false, ColorSquare.Option.DESERIALIZER, ColorSquare.Option.CODEC);

    static <T extends ParticleOptions> RegistryObject<ParticleType<T>> register(String name, boolean bool, ParticleOptions.Deserializer<T> deserializer, Codec<T> codec) {
        return PARTICLES.register(name, () -> new ParticleType<>(bool, deserializer) {
            @Override
            public Codec<T> codec() {
                return codec;
            }
        });
    }

    static void init(IEventBus bus) {
        PARTICLES.register(bus);
    }

    static void clientInit(RegisterParticleProvidersEvent e) {
        e.registerSprite(COLOR_SQUARE.get(), ColorSquare::createParticle);
    }

    static void addSearch(Level level, Vec3 pos, ColorSquare.Option option) {
        addSquares(level, pos, 0.125F, 0.4F, 0.005F, option);
    }

    static void addTerminal(Level level, Vec3 pos, ColorSquare.Option option) {
        addSquares(level, pos, 0.5F, 0.4F, 0.005F, option);
    }

    static void addSquares(Level level, Vec3 pos, float relativeScale, float velocityRandomness, float velocityScale, ColorSquare.Option option) {
        if (level instanceof ServerLevel sl)
            plusMinusOne(x -> plusMinusOne(y -> plusMinusOne(z ->
                    addParticle(sl, option, pos.add(new Vec3(x, y, z).scale(relativeScale)),
                            new Vec3(x + level.random.nextGaussian() * velocityRandomness,
                                    y + level.random.nextGaussian() * velocityRandomness,
                                    z + level.random.nextGaussian() * velocityRandomness).scale(velocityScale)))));
    }

    static void plusMinusOne(IntConsumer func) {
        func.accept(1);
        func.accept(-1);
    }

    static void addParticle(ServerLevel sl, ParticleOptions particle, Vec3 pos, Vec3 vel) {
        sl.players().forEach(sp -> sl.sendParticles(sp, particle, true, pos.x, pos.y, pos.z, 0, vel.x, vel.y, vel.z, 1));
    }
}
