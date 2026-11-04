package io.github.iksanbro.cbcparts.medium;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponentType;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.big_cannon.grapeshot.GrapeshotBurst;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelBurst;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonCartridgeItem;

/**
 * Medium cannon rounds and cartridges for the big shells that CBC Neo Warfare has no medium version
 * of. Only loaded when Neo Warfare is installed; the Mk shells of CBC Enchanced Shells only when that
 * mod is installed too.
 */
public final class MediumShells {
    private MediumShells() {}

    public static final String NEO_WARFARE = "cbcmodernwarfare";

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CBCPartsAssembly.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CBCPartsAssembly.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CBCPartsAssembly.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CBCPartsAssembly.MOD_ID);

    /** Fluid in a medium fluid round. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid_content",
                    b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<EntityType<?>, EntityType<ShrapnelBurst>> SHRAPNEL_BURST =
            ENTITIES.register("medium_shrapnel_burst", () -> burst(ShrapnelBurst::new, "medium_shrapnel_burst"));
    public static final DeferredHolder<EntityType<?>, EntityType<GrapeshotBurst>> GRAPESHOT_BURST =
            ENTITIES.register("medium_grapeshot_burst", () -> burst(GrapeshotBurst::new, "medium_grapeshot_burst"));

    /** One registered medium shell. */
    public record Entry(MediumShell shell,
                        DeferredHolder<EntityType<?>, EntityType<?>> entity,
                        DeferredItem<MediumRoundItem> round,
                        DeferredItem<MediumCartridgeItem> cartridge) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<ResourceLocation, Entry> BY_ENTITY = new HashMap<>();

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static void register(IEventBus modBus) {
        List<MediumShell> shells = new ArrayList<>(MediumShell.createBigCannons());
        if (ModList.get().isLoaded(MediumShell.ENHANCED_SHELLS)) shells.addAll(MediumShell.enhancedShells());
        for (MediumShell shell : shells) add(shell);

        TABS.register("medium_shells", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cbcparts.medium_shells"))
                .icon(() -> ENTRIES.getFirst().round().toStack())
                .displayItems((params, out) -> {
                    for (Entry e : ENTRIES) {
                        out.accept(e.round().get());
                        out.accept(e.cartridge().get().filled());
                    }
                })
                .build());

        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(MediumShells::onCommonSetup);
        modBus.addListener(MediumShells::onRegisterCapabilities);
        if (FMLEnvironment.dist.isClient()) MediumShellsClient.init(modBus);
        CBCPartsAssembly.LOGGER.info("Registered {} medium cannon shells", ENTRIES.size());
    }

    private static void add(MediumShell shell) {
        MediumShellKind kind = shell.kind();
        DeferredHolder<EntityType<?>, EntityType<?>> entity =
                ENTITIES.register(shell.entityName(), () -> (EntityType<?>) projectile(kind.factory(), shell.entityName()));
        // the round and the cartridge refer to each other; holders resolve once both are registered
        DeferredItem<MediumCartridgeItem> cartridge = DeferredItem.createItem(
                ResourceLocation.fromNamespaceAndPath(CBCPartsAssembly.MOD_ID, shell.cartridgeName()));
        DeferredItem<MediumRoundItem> round = ITEMS.register(shell.roundName(), () -> switch (kind) {
            case FLUID -> new FluidMediumRoundItem(new Item.Properties().stacksTo(16), shell, entity, cartridge);
            default -> kind.fuzed
                    ? new FuzedMediumRoundItem(new Item.Properties(), shell, entity, cartridge)
                    : new MediumRoundItem(new Item.Properties(), shell, entity, cartridge);
        });
        ITEMS.register(shell.cartridgeName(), () -> new MediumCartridgeItem(new Item.Properties(), round));
        Entry e = new Entry(shell, entity, round, cartridge);
        ENTRIES.add(e);
        BY_ENTITY.put(entity.getId(), e);
    }

    private static <T extends Entity> EntityType<T> projectile(EntityType.EntityFactory<T> factory, String name) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.2f, 0.2f)
                .fireImmune()
                .clientTrackingRange(16)
                .updateInterval(1)
                .setShouldReceiveVelocityUpdates(false)
                .build(CBCPartsAssembly.MOD_ID + ":" + name);
    }

    private static <T extends Entity> EntityType<T> burst(EntityType.EntityFactory<T> factory, String name) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.8f, 0.8f)
                .fireImmune()
                .clientTrackingRange(16)
                .updateInterval(1)
                .setShouldReceiveVelocityUpdates(true)
                .build(CBCPartsAssembly.MOD_ID + ":" + name);
    }

    /** The round a projectile of this type was fired from (what the projectile looks like). */
    public static ItemStack roundOf(EntityType<?> type) {
        Entry e = BY_ENTITY.get(EntityType.getKey(type));
        return e == null ? ItemStack.EMPTY : e.round().toStack();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MunitionPropertiesHandler.registerProjectileHandler(SHRAPNEL_BURST.get(), CBCMunitionPropertiesHandlers.PROJECTILE_BURST);
            MunitionPropertiesHandler.registerProjectileHandler(GRAPESHOT_BURST.get(), CBCMunitionPropertiesHandlers.PROJECTILE_BURST);
            for (Entry e : ENTRIES)
                MunitionPropertiesHandler.registerProjectileHandler(e.entity().get(), e.shell().kind().handler());
        });
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (Entry e : ENTRIES) {
            if (e.round().get() instanceof FluidMediumRoundItem fluidRound)
                event.registerItem(Capabilities.FluidHandler.ITEM,
                        (stack, ctx) -> new RoundFluidHandler(stack, fluidRound.capacity()), fluidRound);
        }
    }

    /** A filled cartridge of the type, with its round. */
    static ItemStack filledCartridge(Item cartridge, ItemStack round) {
        ItemStack stack = new ItemStack(cartridge);
        MediumcannonCartridgeItem.writeProjectile(round, stack);
        return stack;
    }
}
