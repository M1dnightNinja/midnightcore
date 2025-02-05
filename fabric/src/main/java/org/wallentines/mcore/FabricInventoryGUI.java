package org.wallentines.mcore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mcore.text.WrappedComponent;
import org.wallentines.mcore.util.ConversionUtil;

import java.util.ArrayList;
import java.util.List;

public class FabricInventoryGUI extends SingleInventoryGUI {

    private final List<Menu> open = new ArrayList<>();

    public FabricInventoryGUI(UnresolvedComponent title, int rows, PlaceholderContext ctx) {
        super(title, rows, ctx);
    }

    @Override
    public void update() {

        open.removeIf(m -> m.player.get() == null);
        for(Menu m : open) {
            m.update();
        }
    }

    @Override
    protected void doOpen(Player player) {

        ServerPlayer spl = ConversionUtil.validate(player);
        if(spl.hasDisconnected()) return;

        if(spl.containerMenu != spl.inventoryMenu) {
            spl.closeContainer();
        }
        spl.openMenu(new MenuProvider() {
            @Override
            public @NotNull net.minecraft.network.chat.Component getDisplayName() {
                return new WrappedComponent(FabricInventoryGUI.this.title.resolveFor(spl));
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, net.minecraft.world.entity.player.Player player) {

                if(!(player instanceof ServerPlayer spl)) return null;
                Menu out = new Menu(id, spl);
                open.add(out);

                return out;
            }
        });
    }

    @Override
    protected void doClose(Player player) {

        if(player == null) return;

        ServerPlayer pl = ConversionUtil.validate(player);
        if(pl.containerMenu != pl.inventoryMenu) {
            pl.closeContainer();
        }
    }

    @Override
    public void closeAll() {
        for(Menu m : open) {
            Player spl = m.player.get();
            if(spl != null) {
                OPEN_GUIS.remove(spl.getUUID());
                ConversionUtil.validate(spl).closeContainer();
            }
        }
        open.clear();
    }

    @Override
    public void moveViewers(InventoryGUI other) {

        for(Menu menu : open) {
            Player player = menu.player.get();
            if(player != null) {
                other.open(player);
            }
        }
    }

    private static ClickType getActionType(int action, net.minecraft.world.inventory.ClickType type) {
        return switch (type) {
            case PICKUP -> action == 0 ? ClickType.LEFT : ClickType.RIGHT;
            case QUICK_MOVE -> action == 0 ? ClickType.SHIFT_LEFT : ClickType.SHIFT_RIGHT;
            case SWAP -> ClickType.NUMBER_KEY;
            case CLONE -> ClickType.MIDDLE;
            case THROW -> action == 0 ? ClickType.THROW : ClickType.THROW_ALL;
            case PICKUP_ALL -> ClickType.DOUBLE;
            default -> null;
        };
    }

    private class Menu extends AbstractContainerMenu {

        private final WrappedPlayer player;
        private final PlaceholderContext ctx;

        Menu(int id, ServerPlayer spl) {
            super(getMenuType(FabricInventoryGUI.this.size / 9), id);


            this.player = new WrappedPlayer(spl);
            this.ctx = context.copy().withValue(spl);

            Container container = new SimpleContainer(FabricInventoryGUI.this.size);
            int rows = FabricInventoryGUI.this.size / 9;

            for(int row = 0; row < rows; row++) {
                for(int col = 0; col < 9; col++) {
                    addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }

            this.addStandardInventorySlots(spl.getInventory(), 8, 18 + rows * 2 + 13);
            update();
        }

        public void update() {

            Player spl = player.get();
            if(spl == null || !spl.isOnline()) {
                return;
            }

            int stateId = incrementStateId();

            for(int i = 0 ; i < FabricInventoryGUI.this.size ; i++) {
                Entry ent = FabricInventoryGUI.this.items[i];
                if(ent == null) continue;

                org.wallentines.mcore.ItemStack is = ent.getItem(ctx);
                if(is != null) {
                    setItem(i, stateId, ConversionUtil.validate(is));
                }
            }
        }

        @Override
        public @NotNull ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int i) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return true;
        }

        @Override
        public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType clickType, net.minecraft.world.entity.player.Player player) {

            if(player.level().isClientSide || slot < 0 || slot >= items.length) return;
            FabricInventoryGUI.this.onClick(slot, (ServerPlayer) player, getActionType(button, clickType));
        }

        @Override
        public void removed(net.minecraft.world.entity.player.Player player) {
            if(player == this.player.get()) {
                open.remove(this);
            }
        }

        private static MenuType<?> getMenuType(int rows) {
            return switch (rows) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                default -> MenuType.GENERIC_9x6;
            };
        }

    }
}
