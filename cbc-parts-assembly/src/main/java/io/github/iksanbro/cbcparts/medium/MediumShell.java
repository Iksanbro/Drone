package io.github.iksanbro.cbcparts.medium;

import java.util.ArrayList;
import java.util.List;

/**
 * A medium cannon round made after a big cannon shell that CBC Neo Warfare has no medium version of.
 *
 * <p>Ids: the projectile entity is {@code cbcparts:medium_<name>}, the round
 * {@code cbcparts:medium_<name>_round} and the cartridge {@code cbcparts:medium_<name>_cartridge}.
 * Its munition properties are in {@code data/cbcparts/munition_properties/projectiles/medium_<name>.json},
 * written by {@code tools/generate_medium_shells.py} as half of the big shell's.
 *
 * @param name     name of the big shell, {@code he_shell_mk1} for a shell of CBC Enchanced Shells
 * @param kind     how the round flies and goes off
 * @param bigShell item id of the big shell it is made from
 */
public record MediumShell(String name, MediumShellKind kind, String bigShell) {
    public static final String CBC = "createbigcannons";
    public static final String ENHANCED_SHELLS = "cbc_enhanced_shells";

    public String entityName() {
        return "medium_" + name;
    }

    public String roundName() {
        return entityName() + "_round";
    }

    public String cartridgeName() {
        return entityName() + "_cartridge";
    }

    public boolean fromEnhancedShells() {
        return bigShell.startsWith(ENHANCED_SHELLS + ":");
    }

    /** Big shells of Create Big Cannons that Neo Warfare has no medium round for. */
    public static List<MediumShell> createBigCannons() {
        return List.of(
                new MediumShell("shrapnel_shell", MediumShellKind.SHRAPNEL, CBC + ":shrapnel_shell"),
                new MediumShell("fluid_shell", MediumShellKind.FLUID, CBC + ":fluid_shell"),
                new MediumShell("solid_shot", MediumShellKind.SHOT, CBC + ":solid_shot"),
                new MediumShell("drop_mortar_shell", MediumShellKind.HE, CBC + ":drop_mortar_shell"),
                new MediumShell("mortar_stone", MediumShellKind.MORTAR_STONE, CBC + ":mortar_stone"));
    }

    /**
     * Mk.I-V shells of CBC Enchanced Shells. The nuclear, hydrogen, gravitational, singularity,
     * plasma, disintegration and vortex shells and the shell holder are left out on purpose.
     */
    public static List<MediumShell> enhancedShells() {
        String[][] types = {
                {"he_shell", "HE"}, {"ap_shell", "AP_SHELL"}, {"ap_shot", "SHOT"}, {"solid_shot", "SHOT"},
                {"shrapnel_shell", "SHRAPNEL"}, {"smoke_shell", "SMOKE"}, {"drop_mortar_shell", "HE"},
                {"bag_of_grapeshot", "GRAPESHOT"}, {"mortar_stone", "MORTAR_STONE"},
                {"cluster_shell", "CLUSTER"}, {"bunker_buster_shell", "BUNKER_BUSTER"},
        };
        List<MediumShell> list = new ArrayList<>();
        for (String[] t : types) {
            for (int mk = 1; mk <= 5; mk++) {
                String name = t[0] + "_mk" + mk;
                list.add(new MediumShell(name, MediumShellKind.valueOf(t[1]), ENHANCED_SHELLS + ":" + name));
            }
        }
        return list;
    }
}
