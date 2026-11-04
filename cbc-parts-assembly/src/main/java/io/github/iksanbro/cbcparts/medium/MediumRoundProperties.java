package io.github.iksanbro.cbcparts.medium;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import rbasamoyai.createbigcannons.munitions.config.EntityPropertiesTypeHandler;
import rbasamoyai.createbigcannons.munitions.config.PropertiesTypeHandler;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.ExplosionPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.fragment_burst.ProjectileBurstParentPropertiesComponent;
import riftyboi.cbcmodernwarfare.munitions.medium_cannon.config.MediumcannonProjectilePropertiesComponent;

/**
 * Munition properties of the medium rounds that CBC Neo Warfare has no properties type for:
 * shrapnel, grapeshot, fluid, cluster and bunker buster rounds. Each kind reads only the
 * {@link Extra extras} it uses; the others keep their defaults.
 *
 * @param burst       fragments ({@code shrapnel_}/{@code grapeshot_}) or cluster sub-rounds
 *                    ({@code cluster_count}, {@code cluster_spread} = cone half angle in degrees)
 * @param subRound    entity type of the cluster sub-rounds ({@code cluster_projectile})
 * @param drillDepth  blocks a bunker buster drills through before it explodes ({@code drill_depth})
 */
public record MediumRoundProperties(
        BallisticPropertiesComponent ballistics,
        EntityDamagePropertiesComponent damage,
        MediumcannonProjectilePropertiesComponent mediumcannonProperties,
        ExplosionPropertiesComponent explosion,
        ProjectileBurstParentPropertiesComponent burst,
        String subRound,
        int fluidCapacity,
        int millibucketsPerBlob,
        int millibucketsPerAoeRadius,
        float fluidBlobSpread,
        int drillDepth) {

    public static final MediumRoundProperties DEFAULT = new MediumRoundProperties(
            BallisticPropertiesComponent.DEFAULT, EntityDamagePropertiesComponent.DEFAULT,
            MediumcannonProjectilePropertiesComponent.DEFAULT, ExplosionPropertiesComponent.DEFAULT,
            ProjectileBurstParentPropertiesComponent.DEFAULT, "", 0, 0, 0, 0, 0);

    public enum Extra { EXPLOSION, BURST, SUB_ROUND, FLUID, DRILL }

    public static final class Handler extends EntityPropertiesTypeHandler<MediumRoundProperties> {
        private final Set<Extra> extras;
        private final String burstPrefix;

        public Handler(String burstPrefix, Extra first, Extra... rest) {
            this.extras = EnumSet.of(first, rest);
            this.burstPrefix = burstPrefix;
        }

        @Override
        protected MediumRoundProperties parseJson(ResourceLocation location, JsonObject obj) throws JsonParseException {
            String id = location.toString();
            ExplosionPropertiesComponent explosion = extras.contains(Extra.EXPLOSION)
                    ? ExplosionPropertiesComponent.fromJson(id, obj) : ExplosionPropertiesComponent.DEFAULT;
            ProjectileBurstParentPropertiesComponent burst = extras.contains(Extra.BURST)
                    ? ProjectileBurstParentPropertiesComponent.fromJson(id, burstPrefix, obj)
                    : ProjectileBurstParentPropertiesComponent.DEFAULT;
            String subRound = extras.contains(Extra.SUB_ROUND)
                    ? PropertiesTypeHandler.getOrWarn(obj, "cluster_projectile", id, "", JsonElement::getAsString) : "";
            int capacity = 0, perBlob = 0, perRadius = 0;
            float blobSpread = 0;
            if (extras.contains(Extra.FLUID)) {
                capacity = Math.max(1, PropertiesTypeHandler.getOrWarn(obj, "fluid_shell_capacity", id, 1000, JsonElement::getAsInt));
                perBlob = Math.max(25, PropertiesTypeHandler.getOrWarn(obj, "millibuckets_per_fluid_blob", id, 250, JsonElement::getAsInt));
                perRadius = Math.max(25, PropertiesTypeHandler.getOrWarn(obj, "millibuckets_per_area_of_effect_radius", id, 50, JsonElement::getAsInt));
                blobSpread = Math.max(0.01f, PropertiesTypeHandler.getOrWarn(obj, "fluid_blob_spread", id, 1.0f, JsonElement::getAsFloat));
            }
            int drill = extras.contains(Extra.DRILL)
                    ? Math.max(0, PropertiesTypeHandler.getOrWarn(obj, "drill_depth", id, 5, JsonElement::getAsInt)) : 0;
            return new MediumRoundProperties(
                    BallisticPropertiesComponent.fromJson(id, obj),
                    EntityDamagePropertiesComponent.fromJson(id, obj),
                    MediumcannonProjectilePropertiesComponent.fromJson(id, obj),
                    explosion, burst, subRound, capacity, perBlob, perRadius, blobSpread, drill);
        }

        @Override
        protected MediumRoundProperties readPropertiesFromNetwork(EntityType<?> type, FriendlyByteBuf buf) {
            return new MediumRoundProperties(
                    BallisticPropertiesComponent.fromNetwork(buf),
                    EntityDamagePropertiesComponent.fromNetwork(buf),
                    MediumcannonProjectilePropertiesComponent.fromNetwork(buf),
                    ExplosionPropertiesComponent.fromNetwork(buf),
                    ProjectileBurstParentPropertiesComponent.fromNetwork(buf),
                    buf.readUtf(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
                    buf.readVarInt());
        }

        @Override
        protected void writePropertiesToNetwork(MediumRoundProperties p, FriendlyByteBuf buf) {
            p.ballistics().toNetwork(buf);
            p.damage().toNetwork(buf);
            p.mediumcannonProperties().toNetwork(buf);
            p.explosion().toNetwork(buf);
            p.burst().toNetwork(buf);
            buf.writeUtf(p.subRound());
            buf.writeVarInt(p.fluidCapacity()).writeVarInt(p.millibucketsPerBlob()).writeVarInt(p.millibucketsPerAoeRadius());
            buf.writeFloat(p.fluidBlobSpread());
            buf.writeVarInt(p.drillDepth());
        }

        @Override
        protected MediumRoundProperties getNoPropertiesValue() {
            return DEFAULT;
        }
    }
}
