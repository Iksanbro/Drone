package io.github.iksanbro.cbcparts.medium;

import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;

/**
 * Drills through blocks, then explodes, like the bunker buster shell of CBC Enchanced Shells: when
 * a block is right ahead it stops and bores on in a straight line, {@value #BLOCKS_PER_TICK} blocks
 * a tick, until it has broken {@code drill_depth} blocks or meets an unbreakable one, then explodes
 * with its {@code explosive_power}. When explosions may not break blocks (Create Big Cannons server
 * setting) it explodes where it hits.
 */
public class MediumBunkerBusterProjectile extends MediumcannonFuzedProjectile {
    static final int BLOCKS_PER_TICK = 2;

    private boolean drilling;
    private int drilled;
    private Vec3 direction = Vec3.ZERO;

    public MediumBunkerBusterProjectile(EntityType<? extends MediumBunkerBusterProjectile> type, Level level) {
        super(type, level);
    }

    public boolean isDrilling() {
        return drilling;
    }

    public int drilledBlocks() {
        return drilled;
    }

    @Override
    public void tick() {
        if (drilling) {
            tickDrill();
            return;
        }
        if (!level().isClientSide) {
            Vec3 velocity = getDeltaMovement();
            if (velocity.lengthSqr() > 1e-4) {
                Vec3 dir = velocity.normalize();
                double reach = velocity.length() + 0.5;
                for (double d = 0.5; d <= reach; d += 0.5) {
                    BlockPos pos = BlockPos.containing(position().add(dir.scale(d)));
                    if (drillable(level().getBlockState(pos), pos)) {
                        Vec3 contact = position().add(dir.scale(Math.max(0, d - 0.5)));
                        setPos(contact.x, contact.y, contact.z);
                        if (MediumProjectiles.blockInteraction() == Explosion.BlockInteraction.KEEP) {
                            explode();
                        } else {
                            drilling = true;
                            direction = dir;
                            setDeltaMovement(Vec3.ZERO);
                            level().playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 3.0f, 0.6f);
                        }
                        return;
                    }
                }
            }
        }
        super.tick();
    }

    private boolean drillable(BlockState state, BlockPos pos) {
        if (state.isAir() || state.getDestroySpeed(level(), pos) < 0 || state.is(BlockTags.FEATURES_CANNOT_REPLACE)) return false;
        return state.canOcclude() || state.getFluidState().isEmpty();
    }

    private void tickDrill() {
        if (!(level() instanceof ServerLevel level)) return;
        int depth = properties().drillDepth();
        for (int step = 0; step < BLOCKS_PER_TICK && drilled < depth; step++) {
            Vec3 next = position().add(direction);
            setPos(next.x, next.y, next.z);
            BlockPos pos = BlockPos.containing(next);
            BlockState state = level.getBlockState(pos);
            if (state.getDestroySpeed(level, pos) < 0) {
                explode();
                return;
            }
            if (state.isAir()) continue;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), next.x, next.y, next.z, 12, 0.3, 0.3, 0.3, 0.05);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            drilled++;
        }
        if (drilled >= depth) explode();
    }

    @Override
    protected void detonate(Position position) {
        explode();
    }

    private void explode() {
        if (level() instanceof ServerLevel && !isRemoved()) {
            MediumRoundProperties properties = properties();
            CreateBigCannons.handleCustomExplosion(level(), new ShellExplosion(level(), this, indirectArtilleryFire(false),
                    getX(), getY(), getZ(), properties.explosion().blockDamagePower(), properties.explosion().entityDamagePower(),
                    false, MediumProjectiles.blockInteraction()));
        }
        discard();
    }

    protected MediumRoundProperties properties() {
        return MediumShellKind.BUNKER_BUSTER_PROPERTIES.getPropertiesOf(this);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Drilling", drilling);
        tag.putInt("Drilled", drilled);
        tag.putDouble("DirX", direction.x);
        tag.putDouble("DirY", direction.y);
        tag.putDouble("DirZ", direction.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        drilling = tag.getBoolean("Drilling");
        drilled = tag.getInt("Drilled");
        direction = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
    }
}
