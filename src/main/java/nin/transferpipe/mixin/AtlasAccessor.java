package nin.transferpipe.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlas.class)
public interface AtlasAccessor {

    @Accessor
    int getWidth();

    @Accessor
    int getHeight();
}
