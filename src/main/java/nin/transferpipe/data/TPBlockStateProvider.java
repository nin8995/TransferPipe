package nin.transferpipe.data;

import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.TransferPipe;

import static nin.transferpipe.block.TransferPipeBlock.*;
import static nin.transferpipe.block.TransferPipeBlock.ConnectionStates.MACHINE;
import static nin.transferpipe.block.TransferPipeBlock.ConnectionStates.PIPE;

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
        var centerI = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_ignore"));
        var limb = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb"));
        var limbI = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb_ignore"));
        var limbOW = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb_oneway"));
        var joint = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_joint"));
        var mb = getMultipartBuilder(block);
        mb.part().modelFile(center).addModel()//通常中心
                .condition(FLOW, FlowStates.statesWithout(FlowStates.IGNORE)).end();//無視したくないとき

        mb.part().modelFile(centerI).addModel()//無視用中心
                .condition(FLOW, FlowStates.IGNORE).end();//無視したいとき

        Direction.stream().forEach(d -> {
            var theWay = FlowStates.fromDirection(d);
            var oneWayStates = new java.util.ArrayList<>(FlowStates.directions().filter(f -> f != theWay).toList());
            oneWayStates.add(FlowStates.NONE);
            rotate(mb.part().modelFile(limbOW), d).addModel()//一方通行用
                    .condition(FLOW, oneWayStates.toArray(new FlowStates[]{}))//向いてる方向以外または塞ぎ込んでるとき
                    .condition(CONNECTIONS.get(d), PIPE).end();//パイプに向けて

            rotate(mb.part().modelFile(limb), d).addModel()//通常
                    .condition(FLOW, theWay, FlowStates.ALL)//向いてる方向または開放的なとき
                    .condition(CONNECTIONS.get(d), PIPE).end();//パイプに向けて
            rotate(mb.part().modelFile(limb), d).addModel()//また
                    .condition(FLOW, FlowStates.statesWithout(FlowStates.IGNORE))//無視したくないとき
                    .condition(CONNECTIONS.get(d), MACHINE).end();//機械に向けて

            rotate(mb.part().modelFile(joint), d).addModel()//接合部
                    .condition(FLOW, FlowStates.statesWithout(FlowStates.IGNORE))//無視したくないとき
                    .condition(CONNECTIONS.get(d), MACHINE).end();//機械に向けて

            rotate(mb.part().modelFile(limbI), d).addModel()//無視用
                    .condition(FLOW, FlowStates.IGNORE)//無視したいとき
                    .condition(CONNECTIONS.get(d), PIPE).end();//パイプに向けて
        });

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
