package nin.transferpipe.data;

import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.block.property.FlowStates;

import java.util.stream.Collectors;

import static nin.transferpipe.TransferPipe.NODES;
import static nin.transferpipe.TransferPipe.PIPES;
import static nin.transferpipe.block.property.ConnectionStates.MACHINE;
import static nin.transferpipe.block.property.ConnectionStates.PIPE;
import static nin.transferpipe.block.property.TPProperties.CONNECTIONS;
import static nin.transferpipe.block.property.TPProperties.FLOW;

public class TPBlockStateProvider extends net.minecraftforge.client.model.generators.BlockStateProvider {

    public TPBlockStateProvider(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
        super(gen, modid, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        PIPES.getEntries().forEach(this::pipe);
        NODES.getEntries().forEach(this::node);
    }

    private void pipe(RegistryObject<Block> ro) {
        var block = ro.get();
        var id = ro.getId().getPath();
        var center = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_center"));
        var limb = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb"));
        var joint = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_joint"));
        var overlayIgnoreCenter = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_center"));
        var overlayIgnoreLimb = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_limb"));
        var overlayOneway = new ModelFile.UncheckedModelFile(modLoc("block/overlay_oneway"));
        var mb = getMultipartBuilder(block);
        mb.part().modelFile(center).addModel().end();//??????
        mb.part().modelFile(overlayIgnoreCenter).addModel()//?????????????????????????????????
                .condition(FLOW, FlowStates.IGNORE).end();

        Direction.stream().forEach(d -> {
            rotate(mb.part().modelFile(limb), d).addModel()//???
                    .condition(CONNECTIONS.get(d), PIPE, MACHINE).end();//??????????????????????????????

            rotate(mb.part().modelFile(joint), d).addModel()//?????????
                    .condition(CONNECTIONS.get(d), MACHINE).end();//??????????????????

            rotate(mb.part().modelFile(overlayIgnoreLimb), d).addModel()//??????????????????????????????
                    .condition(FLOW, FlowStates.IGNORE)//?????????????????????
                    .condition(CONNECTIONS.get(d), PIPE).end();//?????????????????????

            var oneWayStates = Direction.stream().filter(f -> f != d).map(FlowStates::fromDirection).collect(Collectors.toSet());
            oneWayStates.add(FlowStates.NONE);
            rotate(mb.part().modelFile(overlayOneway), d).addModel()//??????????????????????????????
                    .condition(FLOW, oneWayStates.toArray(new FlowStates[]{}))//?????????????????????????????????????????????????????????
                    .condition(CONNECTIONS.get(d), PIPE).end();//?????????????????????
        });

        var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_inv"));
        simpleBlockItem(block, inv);
    }

    private void node(RegistryObject<Block> ro) {
        var block = ro.get();
        var id = ro.getId().getPath();
        var model = new ModelFile.UncheckedModelFile(modLoc("block/" + id));


        var mb = getMultipartBuilder(block);
        Direction.stream().forEach(d ->
                rotate(mb.part().modelFile(model), d).addModel()
                        .condition(TransferNodeBlock.FACING, d).end());

        var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_inv"));
        simpleBlockItem(block, inv);
    }

    public static ConfiguredModel.Builder<MultiPartBlockStateBuilder.PartBuilder> rotate(ConfiguredModel.Builder<MultiPartBlockStateBuilder.PartBuilder> m, Direction d) {
        return switch (d) {
            case DOWN -> m.rotationX(90);
            case UP -> m.rotationX(270);
            case NORTH -> m.rotationY(0);
            case SOUTH -> m.rotationY(180);
            case WEST -> m.rotationY(270);
            case EAST -> m.rotationY(90);
        };
    }
}
