package io.github.iksanbro.cbcparts.test;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Neo Warfare tags and CBC:AT recipes that the addons ship at paths or names 1.21.1 does not read. */
@GameTestHolder(CBCPartsAssembly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AddonDataFixGameTests {
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @GameTest(template = "empty")
    public static void neoWarfareTagsLoaded(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cbcmodernwarfare")) {
            helper.succeed();
            return;
        }
        String[][] itemTags = {
                {"createbigcannons:autocannon_rounds", "cbcmodernwarfare:apds_autocannon_round"},
                {"createbigcannons:autocannon_ammo_containers", "cbcmodernwarfare:rotarycannon_ammo_drum"},
                {"cbcmodernwarfare:mediumcannon_rounds", "cbcmodernwarfare:ap_mediumcannon_round"},
                {"cbcmodernwarfare:mediumcannon_cartridges", "cbcmodernwarfare:ap_mediumcannon_cartridge"},
                {"c:ingots/carbon_steel", "cbcmodernwarfare:carbon_steel_ingot"},
        };
        for (String[] c : itemTags) {
            helper.assertTrue(stack(c[1]).is(ItemTags.create(ResourceLocation.parse(c[0]))), c[1] + " is not in #" + c[0]);
        }
        TagKey<EntityType<?>> precise = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("ritchiesprojectilelib:precise_motion"));
        EntityType<?> shell = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("cbcmodernwarfare:he_mediumshell"));
        helper.assertTrue(shell.is(precise), "medium cannon shells are not in #ritchiesprojectilelib:precise_motion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rocketFuzingRecipeLoaded(GameTestHelper helper) {
        if (!ModList.get().isLoaded("cbc_at")) {
            helper.succeed();
            return;
        }
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("cbcparts:fixes/cbc_at/rocket_fuzing"));
        helper.assertTrue(recipe.isPresent(), "fixed rocket fuzing recipe not loaded");
        ResourceLocation serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.get().value().getSerializer());
        helper.assertTrue("cbc_at:rocket_fuzing".equals(String.valueOf(serializer)), "serializer is " + serializer);
        helper.succeed();
    }
}
