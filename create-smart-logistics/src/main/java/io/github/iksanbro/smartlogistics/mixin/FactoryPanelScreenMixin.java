package io.github.iksanbro.smartlogistics.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import io.github.iksanbro.smartlogistics.client.BigGridScreen;
import io.github.iksanbro.smartlogistics.gauge.GridCodec;
import io.github.iksanbro.smartlogistics.network.GaugeGridPayload;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Crafting grids of any size in the factory gauge's menu.
 *
 * <p>A button opens {@link BigGridScreen}, where the grid (3 x 3 up to 100 x 100) is laid out with
 * the gauge's input items. A grid bigger than 3 x 3 is shown in the gauge's menu as the list of its
 * items, and sent to the server with {@link GaugeGridPayload}.
 */
@Mixin(value = FactoryPanelScreen.class, remap = false)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {
    @Shadow
    private FactoryPanelBehaviour behaviour;
    @Shadow
    private boolean restocker;
    @Shadow
    private boolean craftingActive;
    @Shadow
    private List<BigItemStack> craftingIngredients;
    @Shadow
    private List<BigItemStack> inputConfig;
    @Shadow
    private BigItemStack outputConfig;
    @Shadow
    private boolean sendReset;

    @Unique
    private boolean smartlogistics$gridEdited;

    protected FactoryPanelScreenMixin() {
        super();
    }

    @Unique
    private static List<BigItemStack> smartlogistics$toIngredients(List<ItemStack> grid) {
        List<BigItemStack> out = new ArrayList<>(grid.size());
        for (ItemStack s : grid) out.add(new BigItemStack(s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1), 1));
        return out;
    }

    @Unique
    private boolean smartlogistics$bigGridActive() {
        return craftingActive && craftingIngredients != null && craftingIngredients.size() > 9;
    }

    /** A saved big grid stays active even though no 3 x 3 recipe matches it. */
    @Inject(method = "updateConfigs", at = @At("TAIL"))
    private void smartlogistics$keepBigGrid(CallbackInfo ci) {
        if (!smartlogistics$gridEdited && GridCodec.isBig(behaviour.activeCraftingArrangement)) {
            craftingActive = true;
            craftingIngredients = smartlogistics$toIngredients(behaviour.activeCraftingArrangement);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void smartlogistics$gridButton(CallbackInfo ci) {
        if (restocker || behaviour.targetedBy.isEmpty()) return;
        IconButton button = new IconButton(guiLeft + windowWidth - 77, guiTop + windowHeight - 25, AllIcons.I_PLACEMENT_SETTINGS);
        button.withCallback(() -> {
            List<ItemStack> palette = new ArrayList<>();
            for (BigItemStack b : inputConfig) if (!b.stack.isEmpty()) palette.add(b.stack.copyWithCount(1));
            List<ItemStack> grid = new ArrayList<>();
            if (craftingActive && craftingIngredients != null) for (BigItemStack b : craftingIngredients) grid.add(b.stack);
            minecraft.setScreen(new BigGridScreen(this, palette, grid, outputConfig.stack, result -> {
                smartlogistics$gridEdited = true;
                boolean any = result.stream().anyMatch(s -> !s.isEmpty());
                craftingActive = any;
                craftingIngredients = smartlogistics$toIngredients(result);
            }));
        });
        button.setToolTip(Component.translatable("smartlogistics.gui.gauge.grid_button"));
        addRenderableWidget(button);
    }

    /** A big grid does not fit the 3 x 3 slots: its items are listed there with their counts. */
    @Inject(method = "renderInputItem", at = @At("HEAD"), cancellable = true)
    private void smartlogistics$bigGridOverview(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY, CallbackInfo ci) {
        if (!smartlogistics$bigGridActive()) return;
        ci.cancel();
        if (slot != 0) return;
        InventorySummary summary = new InventorySummary();
        for (BigItemStack b : craftingIngredients) if (!b.stack.isEmpty()) summary.add(b.stack, 1);
        List<BigItemStack> items = summary.getStacksByCount();
        for (int i = 0; i < Math.min(9, items.size()); i++) {
            int x = guiLeft + 68 + i % 3 * 20;
            int y = guiTop + 28 + i / 3 * 20;
            graphics.renderItem(items.get(i).stack, x, y);
            graphics.renderItemDecorations(font, items.get(i).stack, x, y, String.valueOf(items.get(i).count));
        }
        int side = GridCodec.side(craftingIngredients.size());
        String size = side + "×" + side;
        graphics.drawString(font, size, guiLeft + 149 - font.width(size) / 2, guiTop + 36, 0x555555, false);
    }

    @ModifyArg(method = "sendIt", index = 3, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelConfigurationPacket;<init>(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;Ljava/lang/String;Ljava/util/Map;Ljava/util/List;IILcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;ZZZ)V"))
    private List<ItemStack> smartlogistics$leaveBigGridOut(List<ItemStack> arrangement) {
        return GridCodec.isBig(arrangement) ? List.of() : arrangement;
    }

    @Inject(method = "sendIt", at = @At("TAIL"))
    private void smartlogistics$sendBigGrid(CallbackInfo ci) {
        if (sendReset || !smartlogistics$bigGridActive()) return;
        List<ItemStack> grid = new ArrayList<>(craftingIngredients.size());
        for (BigItemStack b : craftingIngredients) grid.add(b.stack);
        GridCodec.Encoded e = GridCodec.encode(grid);
        PacketDistributor.sendToServer(new GaugeGridPayload(behaviour.getPanelPosition(), e.palette(), e.cells()));
    }
}
