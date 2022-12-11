package nin.transferpipe.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.block.TransferPipeBlock;

public class TPBlockStateProvider extends net.minecraftforge.client.model.generators.BlockStateProvider {

    public TPBlockStateProvider(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
        super(gen, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        TransferPipe.BLOCKS.getEntries().forEach(this::pipeBlockAndItem);
    }

    private void pipeBlockAndItem(RegistryObject<Block> ro) {
        var block = ro.get();
        var id = ro.getId().getPath();
        var center = new ModelFile.UncheckedModelFile(modLoc("block/" + id));
        var limb = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb"));
        var attachment = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_joint"));
        var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_inv"));

        getMultipartBuilder(block)
                .part().modelFile(center).addModel().end()
                .part().modelFile(limb).addModel().condition(BlockStateProperties.NORTH, true).end()
                .part().modelFile(attachment).addModel().condition(TransferPipeBlock.NORTH_ATTACHED, true).end()
                .part().modelFile(limb).rotationY(90).addModel().condition(BlockStateProperties.EAST, true).end()
                .part().modelFile(attachment).rotationY(90).addModel().condition(TransferPipeBlock.EAST_ATTACHED, true).end()
                .part().modelFile(limb).rotationY(180).addModel().condition(BlockStateProperties.SOUTH, true).end()
                .part().modelFile(attachment).rotationY(180).addModel().condition(TransferPipeBlock.SOUTH_ATTACHED, true).end()
                .part().modelFile(limb).rotationY(270).addModel().condition(BlockStateProperties.WEST, true).end()
                .part().modelFile(attachment).rotationY(270).addModel().condition(TransferPipeBlock.WEST_ATTACHED, true).end()
                .part().modelFile(limb).rotationX(90).addModel().condition(BlockStateProperties.DOWN, true).end()
                .part().modelFile(attachment).rotationX(90).addModel().condition(TransferPipeBlock.DOWN_ATTACHED, true).end()
                .part().modelFile(limb).rotationX(270).addModel().condition(BlockStateProperties.UP, true).end()
                .part().modelFile(attachment).rotationX(270).addModel().condition(TransferPipeBlock.UP_ATTACHED, true).end();
        simpleBlockItem(block, inv);
    }
}
