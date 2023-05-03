package nin.transferpipe.gui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

//CreativeModeInventoryScreen.SlotWrapperをpickupとplaceだけ殺した版
public class LockedSlot extends Slot implements SwapRestricted {

    @Override
    public boolean mayPickup(Player p_40228_) {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack p_40231_) {
        return false;
    }

    final Slot target;

    public LockedSlot(Slot p_98657_) {
        super(p_98657_.container, p_98657_.index, p_98657_.x, p_98657_.y);
        this.target = p_98657_;
    }

    public void onTake(Player p_169754_, ItemStack p_169755_) {
        this.target.onTake(p_169754_, p_169755_);
    }

    public ItemStack getItem() {
        return this.target.getItem();
    }

    public boolean hasItem() {
        return this.target.hasItem();
    }

    public void setByPlayer(ItemStack p_271008_) {
        this.target.setByPlayer(p_271008_);
    }

    public void set(ItemStack p_98679_) {
        this.target.set(p_98679_);
    }

    public void setChanged() {
        this.target.setChanged();
    }

    public int getMaxStackSize() {
        return this.target.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack p_98675_) {
        return this.target.getMaxStackSize(p_98675_);
    }

    @Nullable
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return this.target.getNoItemIcon();
    }

    public ItemStack remove(int p_98663_) {
        return this.target.remove(p_98663_);
    }

    public boolean isActive() {
        return this.target.isActive();
    }

    @Override
    public int getSlotIndex() {
        return this.target.getSlotIndex();
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return this.target.isSameInventory(other);
    }

    @Override
    public Slot setBackground(ResourceLocation atlas, ResourceLocation sprite) {
        this.target.setBackground(atlas, sprite);
        return this;
    }
}
