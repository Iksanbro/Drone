package io.github.iksanbro.smartlogistics.registry;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SmartLogistics.MOD_ID);

    /** Create's frogport menu (18 package slots), opened with the smart frogport screen. */
    public static final DeferredHolder<MenuType<?>, MenuType<PackagePortMenu>> SMART_FROGPORT = MENUS.register("smart_frogport",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new PackagePortMenu(ModMenus.SMART_FROGPORT.get(), id, inv, buf)));
}
