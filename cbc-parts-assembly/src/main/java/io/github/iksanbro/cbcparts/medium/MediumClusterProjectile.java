package io.github.iksanbro.cbcparts.medium;

import javax.annotation.Nonnull;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;

/**
 * Splits into medium HE rounds, like the cluster shell of CBC Enchanced Shells: at the top of its
 * flight, when it nearly stops, when the ground is closer than {@value #AIRBURST_GROUND_DISTANCE}
 * blocks below it on the way down, or when its fuze goes off. The sub-rounds fly on in a cone and
 * carry a copy of its fuze (an impact fuze if it has none).
 */
public class MediumClusterProjectile extends MediumcannonFuzedProjectile {
    static final double AIRBURST_GROUND_DISTANCE = 30;
    /** It does not split for the first ticks, still close to the cannon. */
    static final int ARMING_TICKS = 10;

    private boolean split;
    private int flightTicks;
    private double lastVy = Double.NaN;

    public MediumClusterProjectile(EntityType<? extends MediumClusterProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (split || isRemoved() || level().isClientSide) return;
        Vec3 velocity = getDeltaMovement();
        if (++flightTicks <= ARMING_TICKS) {
            lastVy = velocity.y;
            return;
        }
        boolean apogee = !Double.isNaN(lastVy) && lastVy > 0 && velocity.y <= 0;
        boolean stalled = velocity.lengthSqr() < 0.05;
        boolean groundClose = velocity.y < 0 && level().clip(new ClipContext(position(),
                position().add(0, -AIRBURST_GROUND_DISTANCE, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this))
                .getType() != HitResult.Type.MISS;
        lastVy = velocity.y;
        if (apogee || stalled || groundClose) splitNow();
    }

    @Override
    protected void detonate(Position position) {
        if (!split && level() instanceof ServerLevel) splitNow();
    }

    /** Sub-rounds released by this round: {@code cluster_projectile}, {@code cluster_count} of them. */
    public EntityType<?> subRoundType() {
        ResourceLocation id = ResourceLocation.tryParse(properties().subRound());
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    public boolean hasSplit() {
        return split;
    }

    void splitNow() {
        split = true;
        if (!(level() instanceof ServerLevel level)) return;
        MediumRoundProperties properties = properties();
        Vec3 pos = position();
        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        boolean still = speed < 0.01;
        if (still) speed = 0.5;

        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 15, 0.6, 0.4, 0.6, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0f, 1.3f);

        // aimed forward and down, as the cluster shell does
        Vec3 forward = still ? new Vec3(0, 1, 0) : motion.normalize();
        Vec3 down = still ? new Vec3(0, 1, 0) : new Vec3(0, -1, 0);
        Vec3 axis = forward.scale(0.4).add(down).normalize();
        Vec3 right = axis.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = axis.cross(new Vec3(1, 0, 0));
        right = right.normalize();
        Vec3 up = axis.cross(right).normalize();

        EntityType<?> subType = subRoundType();
        int count = properties.burst().burstProjectileCount();
        double cone = Math.toRadians(properties.burst().burstSpread());
        RandomSource random = level.getRandom();
        ItemStack subFuze = fuze.isEmpty() ? impactFuze() : fuze;
        for (int i = 0; subType != null && i < count; i++) {
            if (!(subType.create(level) instanceof MediumcannonFuzedProjectile sub)) break;
            double phi = Math.PI * 2 * i / count + random.nextDouble() * 0.3;
            double r = Math.sqrt(random.nextDouble()) * Math.sin(cone);
            double along = Math.sqrt(Math.max(0, 1 - r * r));
            Vec3 dir = axis.scale(along).add(right.scale(Math.cos(phi) * r)).add(up.scale(Math.sin(phi) * r)).normalize();
            Vec3 velocity = dir.scale(speed * 0.85 * (0.9 + random.nextDouble() * 0.2));
            sub.setPos(pos.x, pos.y, pos.z);
            sub.setDeltaMovement(velocity);
            sub.setOrientation(velocity.normalize());
            sub.setOwner(getOwner());
            if (!subFuze.isEmpty()) sub.setFuze(subFuze.copy());
            level.addFreshEntity(sub);
        }
        discard();
    }

    private static ItemStack impactFuze() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("createbigcannons", "impact_fuze"))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    protected MediumRoundProperties properties() {
        return MediumShellKind.CLUSTER_PROPERTIES.getPropertiesOf(this);
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
        tag.putBoolean("Split", split);
        tag.putInt("FlightTicks", flightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        split = tag.getBoolean("Split");
        flightTicks = tag.getInt("FlightTicks");
    }
}
