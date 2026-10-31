package io.github.iksanbro.smartlogistics.test;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import io.github.iksanbro.smartlogistics.Config;
import io.github.iksanbro.smartlogistics.SmartLogistics;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A crafting order the way the stock ticker or a factory gauge sends it: one box with the
 * ingredients and the recipe pattern, through the Re-Packager logic, into a packager in front of a
 * real grid of powered mechanical crafters. The result must come out of the crafters.
 */
@GameTestHolder(SmartLogistics.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrafterGameTests {
    private static final int TIMEOUT = 500;

    @GameTest(template = "tall", timeoutTicks = TIMEOUT)
    public static void crushingWheel5x5(GameTestHelper helper) {
        craft(helper, "create:mechanical_crafting/crushing_wheel", 5, 5, true);
    }

    @GameTest(template = "tall", timeoutTicks = TIMEOUT)
    public static void potatoCannon5x2(GameTestHelper helper) {
        craft(helper, "create:mechanical_crafting/potato_cannon", 5, 2, true);
    }

    @GameTest(template = "tall", timeoutTicks = TIMEOUT)
    public static void chest3x3(GameTestHelper helper) {
        craft(helper, "minecraft:chest", 3, 3, true);
    }

    /** Create alone: the 5x5 recipe is cut after 9 cells and fails. Starts after the tests above. */
    @GameTest(template = "tall", setupTicks = TIMEOUT + 20, timeoutTicks = TIMEOUT)
    public static void crushingWheelWithoutFix(GameTestHelper helper) {
        craft(helper, "create:mechanical_crafting/crushing_wheel", 5, 5, false);
    }

    /** The pattern list the stock keeper and the factory gauge build (FactoryPanelScreen.convertRecipeToPackageOrderContext). */
    static List<BigItemStack> pattern(Recipe<?> recipe) {
        List<BigItemStack> out = new ArrayList<>();
        BigItemStack empty = new BigItemStack(ItemStack.EMPTY, 1);
        var ingredients = recipe.getIngredients();
        int width = Math.min(3, ingredients.size()), height = Math.min(3, ingredients.size() / 3 + 1);
        if (recipe instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        if (height == 1) for (int i = 0; i < 3; i++) out.add(empty);
        if (width == 1) out.add(empty);
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            out.add(ing.isEmpty() ? empty : new BigItemStack(ing.getItems()[0].copyWithCount(1), 1));
            if (width < 3 && (i + 1) % width == 0)
                for (int j = 0; j < 3 - width; j++) if (out.size() < 9) out.add(empty);
        }
        while (out.size() < 9) out.add(empty);
        return out;
    }

    private static void craft(GameTestHelper helper, String recipeId, int w, int h, boolean fix) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(recipeId)).orElseThrow().value();
        ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
        boolean oldFix = Config.CRAFTER_FIX.get();
        Config.CRAFTER_FIX.set(fix);

        // a w x h grid of crafters facing north, columns along x, rows along y
        int x0 = 3, y0 = 1, z = 8;
        List<BlockPos> crafters = new ArrayList<>();
        for (int y = y0; y < y0 + h; y++)
            for (int x = x0; x < x0 + w; x++) {
                BlockPos p = new BlockPos(x, y, z);
                // every crafter passes west; the west column passes down; the bottom west one out to the chest
                Direction want = x > x0 ? Direction.WEST : y > y0 ? Direction.DOWN : Direction.WEST;
                helper.setBlock(p, crafterPointing(want));
                crafters.add(p);
            }
        BlockPos output = new BlockPos(x0 - 1, y0, z);
        helper.setBlock(output, Blocks.CHEST);
        // crafters turn like cogwheels: a cogwheel next to the grid, driven by a motor behind it
        BlockPos cog = new BlockPos(x0 + w, y0, z);
        BlockPos motor = cog.south();
        helper.setBlock(cog, AllBlocks.COGWHEEL.get().defaultBlockState()
                .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS, net.minecraft.core.Direction.Axis.Z));
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        BlockPos packager = new BlockPos(x0 + w - 1, y0 + h, z);
        helper.setBlock(packager, AllBlocks.PACKAGER.get().defaultBlockState().setValue(PackagerBlock.FACING, Direction.UP));

        List<ItemStack> boxes = new ArrayList<>();
        int[] tick = {0};
        int[] fed = {0};
        helper.onEachTick(() -> {
            tick[0]++;
            if (tick[0] == 2) {
                ((CreativeMotorBlockEntity) helper.getBlockEntity(motor)).generatedSpeed.setValue(128);
                BlockPos first = helper.absolutePos(crafters.get(0));
                for (int i = 1; i < crafters.size(); i++)
                    ConnectedInputHandler.toggleConnection(helper.getLevel(), first, helper.absolutePos(crafters.get(i)));
                boxes.addAll(repackedBoxes(helper, recipe));
                SmartLogistics.LOGGER.info("[test] {}: {} boxes from the Re-Packager", recipeId, boxes.size());
            }
            // the packager takes the next box when it is free
            if (tick[0] > 20 && fed[0] < boxes.size()) {
                PackagerBlockEntity pbe = (PackagerBlockEntity) helper.getBlockEntity(packager);
                if (pbe.inventory.insertItem(0, boxes.get(fed[0]).copy(), false).isEmpty()) fed[0]++;
            }
        });

        helper.runAfterDelay(TIMEOUT - 20, () -> {
            Config.CRAFTER_FIX.set(oldFix);
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(output);
            int made = 0;
            for (int i = 0; i < chest.getContainerSize(); i++)
                if (ItemStack.isSameItem(chest.getItem(i), result)) made += chest.getItem(i).getCount();
            List<String> left = new ArrayList<>();
            for (BlockPos p : crafters) {
                ItemStack in = ((MechanicalCrafterBlockEntity) helper.getBlockEntity(p)).getInventory().getStackInSlot(0);
                if (!in.isEmpty()) left.add(in.getItem().toString());
            }
            float speed = ((MechanicalCrafterBlockEntity) helper.getBlockEntity(crafters.get(0))).getSpeed();
            SmartLogistics.LOGGER.info("[test] {} (fix {}): {} x {} crafted, boxes fed {}/{}, crafter speed {}, items left in crafters {}",
                    recipeId, fix ? "on" : "off", made, result.getItem(), fed[0], boxes.size(), speed, left);
            if (fix) helper.assertTrue(made == result.getCount(), "crafted " + made + " of " + result.getCount());
            else helper.assertTrue(made == 0, "Create alone should fail here, crafted " + made);
            helper.succeed();
        });
    }

    private static BlockState crafterPointing(Direction want) {
        BlockState base = AllBlocks.MECHANICAL_CRAFTER.get().defaultBlockState().setValue(MechanicalCrafterBlock.HORIZONTAL_FACING, Direction.NORTH);
        for (Pointing p : Pointing.values()) {
            BlockState s = base.setValue(MechanicalCrafterBlock.POINTING, p);
            if (MechanicalCrafterBlock.getTargetDirection(s) == want) return s;
        }
        throw new IllegalStateException("no pointing for " + want);
    }

    /** One order box with all ingredients and the recipe, as the stock ticker sends it, repacked by Create's Re-Packager logic. */
    private static List<ItemStack> repackedBoxes(GameTestHelper helper, Recipe<?> recipe) {
        List<BigItemStack> pattern = pattern(recipe);
        InventorySummary needed = new InventorySummary();
        for (BigItemStack cell : pattern) if (!cell.stack.isEmpty()) needed.add(cell.stack, 1);
        ItemStackHandler contents = new ItemStackHandler(PackageItem.SLOTS);
        int slot = 0;
        for (BigItemStack s : needed.getStacks()) contents.setStackInSlot(slot++, s.stack.copyWithCount(s.count));
        ItemStack box = PackageItem.containing(contents);
        PackageItem.addAddress(box, "Crafters");
        PackageOrderWithCrafts context = new PackageOrderWithCrafts(new PackageOrder(needed.getStacks()),
                List.of(new CraftingEntry(new PackageOrder(pattern), 1)));
        PackageItem.setOrder(box, 1234, 0, true, 0, true, context);

        PackageRepackageHelper repackager = new PackageRepackageHelper();
        int order = repackager.addPackageFragment(box);
        helper.assertTrue(order == 1234, "order complete");
        List<ItemStack> out = new ArrayList<>();
        for (BigItemStack b : repackager.repack(order, helper.getLevel().getRandom()))
            for (int i = 0; i < b.count; i++) out.add(b.stack.copy());
        return out;
    }
}
