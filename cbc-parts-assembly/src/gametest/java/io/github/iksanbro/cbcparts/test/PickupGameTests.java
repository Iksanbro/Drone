package io.github.iksanbro.cbcparts.test;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import io.github.iksanbro.cbcparts.CannonPartWrench;
import io.github.iksanbro.cbcparts.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CBCPartsAssembly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickupGameTests {
    private static final Set<String> CANNON_MODS = Set.of("createbigcannons", "cbc_at", "cbcmodernwarfare");

    /**
     * Every block of the cannon mods that is meant to drop something must have a loaded loot table,
     * and a block that needs the right tool must be in a mineable tag; otherwise it can be neither
     * broken nor taken with the wrench.
     */
    @GameTest(template = "empty")
    public static void everyCannonModBlockCanBePickedUp(GameTestHelper helper) {
        var registries = helper.getLevel().getServer().reloadableRegistries();
        List<String> noLoot = new ArrayList<>();
        List<String> noTool = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!CANNON_MODS.contains(id.getNamespace())) continue;
            if (block.getLootTable() == BuiltInLootTables.EMPTY) continue; // block opted out of drops (fluids, technical)
            if (block.asItem() == net.minecraft.world.item.Items.AIR) continue;
            checked++;
            if (registries.getLootTable(block.getLootTable()) == LootTable.EMPTY) noLoot.add(id.toString());
            BlockState state = block.defaultBlockState();
            if (state.requiresCorrectToolForDrops() && !(state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)
                    || state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(BlockTags.MINEABLE_WITH_HOE))) noTool.add(id.toString());
        }
        CBCPartsAssembly.LOGGER.info("[test] cannon mod blocks checked: {}; without loot table: {} {}; needing an untagged tool: {} {}",
                checked, noLoot.size(), noLoot, noTool.size(), noTool);
        if (!noLoot.isEmpty() || !noTool.isEmpty()) helper.fail(noLoot.size() + " blocks without loot table, " + noTool.size() + " without tool tag");
        helper.succeed();
    }

    /** The HEAT shell of Neo Warfare: breaking it with a pickaxe and taking it with the Create wrench both give it back. */
    @GameTest(template = "empty")
    public static void heatShellCanBeBrokenAndWrenched(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cbcmodernwarfare")) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        Block shell = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("cbcmodernwarfare", "heap_shell"));
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.setBlock(rel, shell.defaultBlockState());
        // put an impact fuze in, the way a player does (right click the shell with the fuze)
        Player fuzer = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack fuze = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "impact_fuze")));
        fuzer.setItemInHand(InteractionHand.MAIN_HAND, fuze);
        BlockHitResult face = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        level.getBlockState(pos).useItemOn(fuzer.getMainHandItem(), level, fuzer, InteractionHand.MAIN_HAND, face);
        var fuzeComponent = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "fuze"));
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.stream().anyMatch(s -> s.is(shell.asItem())), "breaking the HEAT shell drops " + drops);
        boolean fuzed = drops.stream().anyMatch(s -> s.is(shell.asItem()) && fuzeComponent != null && s.has(fuzeComponent));
        CBCPartsAssembly.LOGGER.info("[test] HEAT shell drop keeps its fuze: {}", fuzed);
        helper.assertTrue(fuzed, "the fuze was lost: " + drops.get(0).getComponents());

        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench());
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        helper.assertTrue(level.getBlockState(pos).isAir(), "the wrench did not take the HEAT shell");
        boolean inInventory = false;
        for (ItemStack stack : player.getInventory().items) inInventory |= stack.is(shell.asItem()) && stack.has(fuzeComponent);
        helper.assertTrue(inInventory, "the fuzed HEAT shell is not in the inventory");
        helper.succeed();
    }

    /** Sneak + wrench takes every part with a recipe of this mod into the inventory as the part itself. */
    @GameTest(template = "empty")
    public static void wrenchTakesEveryPart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        List<String> errors = new ArrayList<>();
        for (Block part : CannonPartWrench.parts()) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench());
            helper.setBlock(rel, part.defaultBlockState());
            var event = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            String id = BuiltInRegistries.BLOCK.getKey(part).toString();
            if (!event.isCanceled()) errors.add(id + ": wrench ignored");
            if (!level.getBlockState(pos).isAir()) errors.add(id + ": still in the world");
            if (!player.getInventory().contains(new ItemStack(part.asItem()))) errors.add(id + ": not in the inventory");
            helper.setBlock(rel, Blocks.AIR.defaultBlockState());
        }
        CBCPartsAssembly.LOGGER.info("[test] wrench pickup checked for {} parts, problems: {}", CannonPartWrench.parts().size(), errors);
        if (!errors.isEmpty()) helper.fail(String.join("\n", errors));
        helper.succeed();
    }

    /** Taking a part out of an assembled line disconnects its neighbours, as breaking it does. */
    @GameTest(template = "empty")
    public static void wrenchDisconnectsNeighbours(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(180f);
        BlockPos[] line = {new BlockPos(2, 1, 2), new BlockPos(2, 1, 3), new BlockPos(2, 1, 4)};
        String[] parts = {"steel_autocannon_breech", "steel_autocannon_recoil_spring", "steel_autocannon_barrel"};
        for (int i = 0; i < line.length; i++) {
            BlockPos abs = helper.absolutePos(line[i]);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("createbigcannons", parts[i]))));
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        }
        BlockPos breech = helper.absolutePos(line[0]);
        var before = ((rbasamoyai.createbigcannons.cannons.ICannonBlockEntity<?>) level.getBlockEntity(breech)).cannonBehavior().isConnectedTo(Direction.SOUTH);
        helper.assertTrue(before, "parts did not connect");
        BlockPos middle = helper.absolutePos(line[1]);
        player.setShiftKeyDown(true);
        CannonPartWrench.pickUp(level, middle, level.getBlockState(middle), player);
        var after = ((rbasamoyai.createbigcannons.cannons.ICannonBlockEntity<?>) level.getBlockEntity(breech)).cannonBehavior().isConnectedTo(Direction.SOUTH);
        helper.assertTrue(!after, "breech still connected to the removed spring");
        helper.succeed();
    }

    /** With the option off the wrench leaves parts alone. */
    @GameTest(template = "empty")
    public static void wrenchPickupCanBeTurnedOff(GameTestHelper helper) {
        Config.WRENCH_PICKUP.set(false);
        try {
            BlockPos rel = new BlockPos(2, 1, 2);
            BlockPos pos = helper.absolutePos(rel);
            Block part = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("createbigcannons", "steel_autocannon_barrel"));
            helper.setBlock(rel, part.defaultBlockState());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, wrench());
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            helper.assertTrue(!helper.getLevel().getBlockState(pos).isAir(), "part was taken although the option is off");
        } finally {
            Config.WRENCH_PICKUP.set(true);
        }
        helper.succeed();
    }

    private static ItemStack wrench() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
    }
}
