package io.github.iksanbro.cbcparts.medium;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.index.CBCEntityTypes;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.EndFluidStack;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidBlobBurst;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidExplosion;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.fragment_burst.CBCProjectileBurst;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.MediumcannonFuzedProjectile;

/** Splashes its fluid in blobs where it goes off, like the big fluid shell. */
public class MediumFluidProjectile extends MediumcannonFuzedProjectile {
    private static Method setBlobSize;

    private FluidStack fluid = FluidStack.EMPTY;

    public MediumFluidProjectile(EntityType<? extends MediumFluidProjectile> type, Level level) {
        super(type, level);
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid.copy();
    }

    public FluidStack getFluid() {
        return fluid.copy();
    }

    @Override
    protected void detonate(Position position) {
        MediumRoundProperties properties = properties();
        Vec3 velocity = getDeltaMovement();
        CreateBigCannons.handleCustomExplosion(level(), new FluidExplosion(level(), null, indirectArtilleryFire(false),
                position.x(), position.y(), position.z(),
                properties.explosion().blockDamagePower(), properties.explosion().entityDamagePower(),
                MediumProjectiles.blockInteraction(), fluid.getFluid()));
        if (fluid.isEmpty()) return;
        int perBlob = properties.millibucketsPerBlob();
        int count = (int) Math.ceil((double) fluid.getAmount() / perBlob);
        FluidBlobBurst burst = CBCProjectileBurst.spawnConeBurst(level(), CBCEntityTypes.FLUID_BLOB_BURST.get(),
                new Vec3(position.x(), position.y(), position.z()), velocity, count, properties.fluidBlobSpread());
        if (burst == null) return;
        burst.setFluidStack(new EndFluidStack(fluid.getFluid(), perBlob, fluid.getComponentsPatch()));
        setBlobSize(burst, (byte) (perBlob / properties.millibucketsPerAoeRadius()));
    }

    /** The blob size setter is not public; Create Big Cannons calls it from the same package. */
    private static void setBlobSize(FluidBlobBurst burst, byte size) {
        try {
            if (setBlobSize == null) {
                setBlobSize = FluidBlobBurst.class.getDeclaredMethod("setBlobSize", byte.class);
                setBlobSize.setAccessible(true);
            }
            setBlobSize.invoke(burst, size);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // blobs keep their default size
        }
    }

    protected MediumRoundProperties properties() {
        return MediumShellKind.FLUID_PROPERTIES.getPropertiesOf(this);
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
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.save(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fluid = FluidStack.parseOptional(registryAccess(), tag.getCompound("Fluid"));
    }
}
