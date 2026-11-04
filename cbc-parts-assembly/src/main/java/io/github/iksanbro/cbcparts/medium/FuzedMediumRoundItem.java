package io.github.iksanbro.cbcparts.medium;

import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.FuzedMediumItemMunition;

/** A round that takes a fuze (Neo Warfare's fuzing recipes look for {@link FuzedMediumItemMunition}). */
public class FuzedMediumRoundItem extends MediumRoundItem implements FuzedMediumItemMunition {
    public FuzedMediumRoundItem(Properties properties, MediumShell shell,
                                DeferredHolder<EntityType<?>, EntityType<?>> entity,
                                DeferredItem<MediumCartridgeItem> cartridge) {
        super(properties, shell, entity, cartridge);
    }
}
