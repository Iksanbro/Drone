package io.github.iksanbro.cbcparts.medium;

import javax.annotation.Nonnull;
import net.minecraft.core.Position;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.big_cannon.mortar_stone.MortarStoneExplosion;
import rbasamoyai.createbigcannons.munitions.big_cannon.shrapnel.ShrapnelExplosion;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.fragment_burst.CBCProjectileBurst;
import riftyboi.cbcmodernwarfare.index.CBCModernWarfareMunitionPropertiesHandlers;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.AbstractMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.ap.APMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.aphe.APHEMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.he.HEMediumcannonProjectile;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.he.HEMediumcannonProjectileProperties;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.smoke.SmokeMediumcannonProjectile;

/**
 * Projectiles of the medium rounds. Those Neo Warfare already has a projectile for (HE, APHE, AP,
 * smoke) reuse it; they only show their own round in flight.
 */
public final class MediumProjectiles {
    private MediumProjectiles() {}

    /** How much explosions may break blocks, the server setting of Create Big Cannons. */
    static Explosion.BlockInteraction blockInteraction() {
        return CBCConfigs.server().munitions.damageRestriction.get().explosiveInteraction();
    }

    public static class HE extends HEMediumcannonProjectile {
        public HE(EntityType<? extends HE> type, Level level) {
            super(type, level);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }
    }

    public static class APHE extends APHEMediumcannonProjectile {
        public APHE(EntityType<? extends APHE> type, Level level) {
            super(type, level);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }
    }

    public static class Shot extends APMediumcannonProjectile {
        public Shot(EntityType<? extends Shot> type, Level level) {
            super(type, level);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }
    }

    public static class Smoke extends SmokeMediumcannonProjectile {
        public Smoke(EntityType<? extends Smoke> type, Level level) {
            super(type, level);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }
    }

    /** Small explosion and a cone of shrapnel, like the big shrapnel shell. */
    public static class Shrapnel extends MediumcannonFuzedProjectile {
        public Shrapnel(EntityType<? extends Shrapnel> type, Level level) {
            super(type, level);
        }

        @Override
        protected void detonate(Position position) {
            MediumRoundProperties properties = properties();
            Vec3 velocity = getDeltaMovement();
            CreateBigCannons.handleCustomExplosion(level(), new ShrapnelExplosion(level(), null, indirectArtilleryFire(false),
                    position.x(), position.y(), position.z(),
                    properties.explosion().blockDamagePower(), properties.explosion().entityDamagePower(), blockInteraction()));
            CBCProjectileBurst.spawnConeBurst(level(), MediumShells.SHRAPNEL_BURST.get(),
                    new Vec3(position.x(), position.y(), position.z()), velocity,
                    properties.burst().burstProjectileCount(), properties.burst().burstSpread());
        }

        protected MediumRoundProperties properties() {
            return MediumShellKind.SHRAPNEL_PROPERTIES.getPropertiesOf(this);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }

        @Nonnull
        @Override
        public EntityDamagePropertiesComponent getDamageProperties() {
            return properties().damage();
        }

        @Nonnull
        @Override
        protected BallisticPropertiesComponent getBallisticProperties() {
            return properties().ballistics();
        }
    }

    /** Bursts into grapeshot as it leaves the barrel, like the big bag of grapeshot. */
    public static class Grapeshot extends AbstractMediumcannonProjectile {
        public Grapeshot(EntityType<? extends Grapeshot> type, Level level) {
            super(type, level);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide || isRemoved()) return;
            MediumRoundProperties properties = properties();
            CBCProjectileBurst.spawnConeBurst(level(), MediumShells.GRAPESHOT_BURST.get(), position(), getDeltaMovement(),
                    properties.burst().burstProjectileCount(), properties.burst().burstSpread());
            discard();
        }

        protected MediumRoundProperties properties() {
            return MediumShellKind.GRAPESHOT_PROPERTIES.getPropertiesOf(this);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }

        @Nonnull
        @Override
        public EntityDamagePropertiesComponent getDamageProperties() {
            return properties().damage();
        }

        @Nonnull
        @Override
        protected BallisticPropertiesComponent getBallisticProperties() {
            return properties().ballistics();
        }
    }

    /** Explodes where it hits a block, like the big mortar stone. */
    public static class MortarStone extends AbstractMediumcannonProjectile {
        public MortarStone(EntityType<? extends MortarStone> type, Level level) {
            super(type, level);
        }

        @Override
        protected boolean onImpact(HitResult hit, ImpactResult impact, ProjectileContext context) {
            super.onImpact(hit, impact, context);
            if (hit.getType() != HitResult.Type.BLOCK) return false;
            if (!level().isClientSide) {
                Vec3 at = hit.getLocation();
                HEMediumcannonProjectileProperties properties = properties();
                CreateBigCannons.handleCustomExplosion(level(), new MortarStoneExplosion(level(), null, indirectArtilleryFire(false),
                        at.x, at.y, at.z, properties.explosion().blockDamagePower(), properties.explosion().entityDamagePower(),
                        blockInteraction()));
            }
            return true;
        }

        protected HEMediumcannonProjectileProperties properties() {
            return CBCModernWarfareMunitionPropertiesHandlers.HE_MEDIUMCANNON.getPropertiesOf(this);
        }

        @Override
        public ItemStack getItem() {
            return MediumShells.roundOf(getType());
        }

        @Nonnull
        @Override
        public EntityDamagePropertiesComponent getDamageProperties() {
            return properties().damage();
        }

        @Nonnull
        @Override
        protected BallisticPropertiesComponent getBallisticProperties() {
            return properties().ballistics();
        }
    }
}
