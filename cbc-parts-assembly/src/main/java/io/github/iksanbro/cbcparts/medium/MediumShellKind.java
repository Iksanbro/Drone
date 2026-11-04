package io.github.iksanbro.cbcparts.medium;

import io.github.iksanbro.cbcparts.medium.MediumRoundProperties.Extra;
import net.minecraft.world.entity.EntityType;
import rbasamoyai.createbigcannons.munitions.config.PropertiesTypeHandler;
import riftyboi.cbcmodernwarfare.index.CBCModernWarfareMunitionPropertiesHandlers;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.aphe.APHEMediumcannonProjectileProperties;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.config.InertMediumcannonProjectileProperties;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.config.MediumcannonProjectilePropertiesComponent;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.he.HEMediumcannonProjectileProperties;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.smoke.SmokeMediumcannonProjectileProperties;

/** How a medium round flies and goes off, and which munition properties it reads. */
public enum MediumShellKind {
    /** HE and drop mortar shells: Neo Warfare's HE round. */
    HE(true),
    /** AP shell: Neo Warfare's APHE round (explodes after it went through armor). */
    AP_SHELL(true),
    /** Solid shot and AP shot: Neo Warfare's inert AP round. */
    SHOT(false),
    SMOKE(true),
    SHRAPNEL(true),
    /** Bursts into grapeshot right at the muzzle. */
    GRAPESHOT(false),
    /** Explodes when it hits a block, like the big mortar stone. */
    MORTAR_STONE(false),
    /** Filled with a fluid by a spout; splashes it where it goes off. */
    FLUID(true),
    /** Splits into medium HE rounds at the top of its flight or close to the ground. */
    CLUSTER(true),
    /** Drills through blocks, then explodes. */
    BUNKER_BUSTER(true);

    /** The round takes a fuze (crafting table, deployer). */
    public final boolean fuzed;

    MediumShellKind(boolean fuzed) {
        this.fuzed = fuzed;
    }

    public static final MediumRoundProperties.Handler SHRAPNEL_PROPERTIES =
            new MediumRoundProperties.Handler("shrapnel_", Extra.EXPLOSION, Extra.BURST);
    public static final MediumRoundProperties.Handler GRAPESHOT_PROPERTIES =
            new MediumRoundProperties.Handler("grapeshot_", Extra.BURST);
    public static final MediumRoundProperties.Handler FLUID_PROPERTIES =
            new MediumRoundProperties.Handler("", Extra.EXPLOSION, Extra.FLUID);
    public static final MediumRoundProperties.Handler CLUSTER_PROPERTIES =
            new MediumRoundProperties.Handler("cluster_", Extra.EXPLOSION, Extra.BURST, Extra.SUB_ROUND);
    public static final MediumRoundProperties.Handler BUNKER_BUSTER_PROPERTIES =
            new MediumRoundProperties.Handler("", Extra.EXPLOSION, Extra.DRILL);

    public PropertiesTypeHandler<EntityType<?>, ?> handler() {
        return switch (this) {
            case HE, MORTAR_STONE -> CBCModernWarfareMunitionPropertiesHandlers.HE_MEDIUMCANNON;
            case AP_SHELL -> CBCModernWarfareMunitionPropertiesHandlers.APHE_MEDIUMCANNON;
            case SHOT -> CBCModernWarfareMunitionPropertiesHandlers.INERT_MEDIUMCANNON_PROJECTILE;
            case SMOKE -> CBCModernWarfareMunitionPropertiesHandlers.SMOKE_MEDIUMCANNON;
            case SHRAPNEL -> SHRAPNEL_PROPERTIES;
            case GRAPESHOT -> GRAPESHOT_PROPERTIES;
            case FLUID -> FLUID_PROPERTIES;
            case CLUSTER -> CLUSTER_PROPERTIES;
            case BUNKER_BUSTER -> BUNKER_BUSTER_PROPERTIES;
        };
    }

    /** Recoil and squib properties of the round, read by the medium cannon when it fires. */
    public MediumcannonProjectilePropertiesComponent mediumcannonProperties(EntityType<?> type) {
        Object p = handler().getPropertiesOf(type);
        return switch (p) {
            case HEMediumcannonProjectileProperties he -> he.mediumcannonProperties();
            case APHEMediumcannonProjectileProperties aphe -> aphe.mediumcannonProperties();
            case InertMediumcannonProjectileProperties inert -> inert.mediumcannonProperties();
            case SmokeMediumcannonProjectileProperties smoke -> smoke.mediumcannonProperties();
            case MediumRoundProperties own -> own.mediumcannonProperties();
            default -> MediumcannonProjectilePropertiesComponent.DEFAULT;
        };
    }

    public EntityType.EntityFactory<? extends AbstractMediumcannonProjectile> factory() {
        return switch (this) {
            case HE -> (EntityType.EntityFactory<MediumProjectiles.HE>) MediumProjectiles.HE::new;
            case AP_SHELL -> (EntityType.EntityFactory<MediumProjectiles.APHE>) MediumProjectiles.APHE::new;
            case SHOT -> (EntityType.EntityFactory<MediumProjectiles.Shot>) MediumProjectiles.Shot::new;
            case SMOKE -> (EntityType.EntityFactory<MediumProjectiles.Smoke>) MediumProjectiles.Smoke::new;
            case SHRAPNEL -> (EntityType.EntityFactory<MediumProjectiles.Shrapnel>) MediumProjectiles.Shrapnel::new;
            case GRAPESHOT -> (EntityType.EntityFactory<MediumProjectiles.Grapeshot>) MediumProjectiles.Grapeshot::new;
            case MORTAR_STONE -> (EntityType.EntityFactory<MediumProjectiles.MortarStone>) MediumProjectiles.MortarStone::new;
            case FLUID -> (EntityType.EntityFactory<MediumFluidProjectile>) MediumFluidProjectile::new;
            case CLUSTER -> (EntityType.EntityFactory<MediumClusterProjectile>) MediumClusterProjectile::new;
            case BUNKER_BUSTER -> (EntityType.EntityFactory<MediumBunkerBusterProjectile>) MediumBunkerBusterProjectile::new;
        };
    }
}
