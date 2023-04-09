package nin.transferpipe.particle;


import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.DustParticleOptionsBase;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.function.Function;

public class ColorSquare extends TextureSheetParticle {

    public static final float baseSize = 0.05F;

    public ColorSquare(Vector3f color, float a, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        float f4 = level.random.nextFloat() * 0.4F + 0.6F;
        setColor(forEachValue(color, f -> f * f4 * (level.random.nextFloat() * 0.2F + 0.8F)));
        setAlpha(a);
        calcSize(0);
        setParticleSpeed(xd, yd, zd);
        lifetime = (int) (10.0 / (Math.random() * 0.2 + 0.6));
        hasPhysics = false;
    }

    public Vector3f forEachValue(Vector3f v, Function<Float, Float> f) {
        return new Vector3f(f.apply(v.x), f.apply(v.y), f.apply(v.z));
    }

    public void setColor(Vector3f color) {
        setColor(color.x, color.y, color.z);
    }

    @Override
    public void render(VertexConsumer p_107678_, Camera p_107679_, float partialTick) {
        calcSize(partialTick);
        super.render(p_107678_, p_107679_, partialTick);
    }

    public void calcSize(float partialTick) {
        var ageScale = Mth.clamp(1.0F - (age + partialTick) / lifetime, 0, 1);
        quadSize = baseSize * (1.0F - ageScale * 0.5F) * Math.min(1.0F, 2.0F * ageScale);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static TextureSheetParticle createParticle(Option option, ClientLevel p_172502_, double x, double y, double z, double xd, double yd, double zd) {
        return new ColorSquare(option.color, option.alpha, p_172502_, x, y, z, xd, yd, zd);
    }

    public static class Option implements ParticleOptions {
        private final Vector3f color;
        private final float alpha;

        public Option(float r, float g, float b, float alpha) {
            this.color = new Vector3f(r, g, b);
            this.alpha = alpha;
        }

        public Option(Vector3f color, float alpha) {
            this.color = color;
            this.alpha = alpha;
        }

        @Override
        public ParticleType<?> getType() {
            return TPParticles.COLOR_SQUARE.get();
        }

        @Override
        public void writeToNetwork(FriendlyByteBuf buf) {
            buf.writeVector3f(color);
            buf.writeFloat(alpha);
        }

        @Override
        public String writeToString() {
            return String.format(Locale.ROOT, "%.2f %.2f %.2f %.2f", color.x, color.y, color.z, alpha);
        }

        public static final ParticleOptions.Deserializer<Option> DESERIALIZER = new ParticleOptions.Deserializer<Option>() {

            @Override
            public Option fromNetwork(ParticleType<Option> p_123735_, FriendlyByteBuf buf) {
                return new Option(buf.readVector3f(), buf.readFloat());
            }

            @Override
            public Option fromCommand(ParticleType<Option> p_123733_, StringReader reader) throws CommandSyntaxException {
                Vector3f vector3f = DustParticleOptionsBase.readVector3f(reader);
                reader.expect(' ');
                float f = reader.readFloat();
                return new Option(vector3f, f);
            }
        };

        public static final Codec<Option> CODEC = RecordCodecBuilder.create((builder) -> builder.group(
                ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(it -> it.color),
                Codec.FLOAT.fieldOf("a").forGetter(it -> it.alpha)).apply(builder, Option::new));
    }

    /*@Override
    public void render(VertexConsumer vc, Camera camera, float p_107680_) {
        IntStream.of(3).forEach(yRot -> IntStream.of(1).forEach(xRot ->
                renderWithRotation(vc, camera, p_107680_, new Quaternionf().rotationZ((float) (yRot / 2F * Math.PI)).rotationX((float) (xRot * Math.PI)))));
    }

    public void renderWithRotation(VertexConsumer vc, Camera camera, float p_107680_, Quaternionf q) {
        var cameraPos = camera.getPosition();

        var rotation = this.roll == 0.0F ? camera.rotation() : new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(p_107680_, this.oRoll, this.roll));
        var offsetX = (float) (Mth.lerp(p_107680_, xo, x) - cameraPos.x());
        var offsetY = (float) (Mth.lerp(p_107680_, yo, y) - cameraPos.y());
        var offsetZ = (float) (Mth.lerp(p_107680_, zo, z) - cameraPos.z());

        plusMinusOne(x -> plusMinusOne(y -> {
            var v = new Vector3f(x, y, 1).rotate(q).mul(quadSize).add(offsetX, offsetY, offsetZ);
            vc.vertex(v.x, v.y, v.z).uv(x == 1 ? getU0() : getU1(), y == 1 ? getV0() : getV1()).color(rCol, gCol, bCol, alpha).uv2(getLightColor(p_107680_)).endVertex();
        }));
    }

    public static void plusMinusOne(IntConsumer func) {
        func.accept(1);
        func.accept(-1);
    }

    public void setPos(Vector3d v) {
        setPos(v.x, v.y, v.z);
    }*/
}