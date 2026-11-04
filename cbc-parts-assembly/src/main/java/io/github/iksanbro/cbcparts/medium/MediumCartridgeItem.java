package io.github.iksanbro.cbcparts.medium;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonCartridgeItem;

/**
 * A cartridge with its round, what the medium cannon fires. Neo Warfare's cartridge does the work
 * (it fires whatever round is inside); each round has its own cartridge item for its name and model.
 */
public class MediumCartridgeItem extends MediumcannonCartridgeItem {
    private final DeferredItem<MediumRoundItem> round;

    public MediumCartridgeItem(Properties properties, DeferredItem<MediumRoundItem> round) {
        super(properties);
        this.round = round;
    }

    /** This cartridge with an unfuzed round inside. */
    public ItemStack filled() {
        return MediumShells.filledCartridge(this, round.toStack());
    }
}
