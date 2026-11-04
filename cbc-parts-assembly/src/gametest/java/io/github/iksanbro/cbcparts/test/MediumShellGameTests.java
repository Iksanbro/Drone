package io.github.iksanbro.cbcparts.test;

import io.github.iksanbro.cbcparts.CBCPartsAssembly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Medium cannon rounds (needs CBC Neo Warfare; the Mk rounds need CBC Enchanced Shells, run with
 * -PwithEnhancedShells). The checks are in {@link MediumShellChecks}; this class names no class of
 * those mods, so the tests load (and pass as skipped) without them.
 */
@GameTestHolder(CBCPartsAssembly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MediumShellGameTests {
    private MediumShellGameTests() {}

    private static boolean skip(GameTestHelper helper, boolean needsEnhancedShells) {
        if (ModList.get().isLoaded("cbcmodernwarfare") && (!needsEnhancedShells || ModList.get().isLoaded("cbc_enhanced_shells")))
            return false;
        helper.succeed();
        return true;
    }

    @GameTest(template = "empty")
    public static void everyRoundIsRegistered(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.everyRoundIsRegistered(helper);
    }

    @GameTest(template = "empty")
    public static void halfOfTheBigShell(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.halfOfTheBigShell(helper);
    }

    @GameTest(template = "empty")
    public static void shrapnelRoundBursts(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.shrapnelRoundBursts(helper);
    }

    @GameTest(template = "empty")
    public static void fluidRoundIsFilledAndSplashes(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.fluidRoundIsFilledAndSplashes(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void mortarStoneRoundExplodesOnImpact(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.mortarStoneRoundExplodesOnImpact(helper);
    }

    @GameTest(template = "empty")
    public static void roundsGoIntoCartridges(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.roundsGoIntoCartridges(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void grapeshotRoundBursts(GameTestHelper helper) throws Exception {
        if (!skip(helper, true)) MediumShellChecks.grapeshotRoundBursts(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void clusterRoundSplits(GameTestHelper helper) throws Exception {
        if (!skip(helper, true)) MediumShellChecks.clusterRoundSplits(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void bunkerBusterDrills(GameTestHelper helper) throws Exception {
        if (!skip(helper, true)) MediumShellChecks.bunkerBusterDrills(helper);
    }

    @GameTest(template = "empty")
    public static void mediumCannonFiresTheRound(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.mediumCannonFiresTheRound(helper);
    }

    @GameTest(template = "empty")
    public static void fliesLikeNeoWarfareRounds(GameTestHelper helper) throws Exception {
        if (!skip(helper, false)) MediumShellChecks.fliesLikeNeoWarfareRounds(helper);
    }
}
