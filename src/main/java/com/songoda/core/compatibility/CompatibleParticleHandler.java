package com.songoda.core.compatibility;

import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class CompatibleParticleHandler {

    public static void spawnParticles(ParticleType type, Location location, int count) {
        if (ServerVersion.isServerVersionAtOrBelow(ServerVersion.V1_8)) {
            for (int i = 0; i < count; ++i) {
                float xx = (float) (1 * (Math.random() - Math.random()));
                float yy = (float) (1 * (Math.random() - Math.random()));
                float zz = (float) (1 * (Math.random() - Math.random()));

                Location at = location.clone().add(xx, yy, zz);
                LegacyParticleEffects.createParticle(at, type.compatibleEffect);
            }
        } else {
            location.getWorld().spawnParticle((Particle) type.particle, location, count);
        }
    }

    public enum ParticleType {
        EXPLOSION_NORMAL,
        EXPLOSION_LARGE,
        EXPLOSION_HUGE,
        FIREWORKS_SPARK,
        WATER_BUBBLE,
        WATER_SPLASH,
        WATER_WAKE,
        SUSPENDED,
        SUSPENDED_DEPTH,
        CRIT,
        CRIT_MAGIC,
        SMOKE_NORMAL,
        SMOKE_LARGE,
        SPELL,
        SPELL_INSTANT,
        SPELL_MOB,
        SPELL_MOB_AMBIENT,
        SPELL_WITCH,
        DRIP_WATER,
        DRIP_LAVA,
        VILLAGER_ANGRY,
        VILLAGER_HAPPY,
        TOWN_AURA,
        NOTE,
        PORTAL,
        ENCHANTMENT_TABLE,
        FLAME,
        LAVA,
        CLOUD,
        REDSTONE(), //DustOptions
        SNOWBALL,
        SNOW_SHOVEL,
        SLIME,
        HEART,
        BARRIER,
        ITEM_CRACK(), // ItemStack
        BLOCK_CRACK(), // BlockData
        BLOCK_DUST(), // BlockData
        WATER_DROP,
        // 1.8-1.12 included ITEM_TAKE
        MOB_APPEARANCE,
        /// End 1.8 particles ///
        DRAGON_BREATH(ServerVersion.V1_9, "SPELL_MOB_AMBIENT"),
        END_ROD(ServerVersion.V1_9, "ENCHANTMENT_TABLE"),
        DAMAGE_INDICATOR(ServerVersion.V1_9, "VILLAGER_ANGRY"),
        SWEEP_ATTACK(ServerVersion.V1_9, "CRIT"),
        /// End 1.9 particles ///
        FALLING_DUST(ServerVersion.V1_10, "BLOCK_DUST"), // BlockData
        /// End 1.10 ///
        TOTEM(ServerVersion.V1_11, "VILLAGER_HAPPY"),
        SPIT(ServerVersion.V1_11, "REDSTONE"),
        /// End 1.11-1.12 ///
        SQUID_INK(ServerVersion.V1_13, "CRIT"),
        BUBBLE_POP(ServerVersion.V1_13, "CRIT"),
        CURRENT_DOWN(ServerVersion.V1_13, "CRIT"),
        BUBBLE_COLUMN_UP(ServerVersion.V1_13, "CRIT"),
        NAUTILUS(ServerVersion.V1_13, "ENCHANTMENT_TABLE"),
        DOLPHIN(ServerVersion.V1_13, "TOWN_AURA"),
        /// End 1.13 ///
        SNEEZE(ServerVersion.V1_14, "REDSTONE"),
        CAMPFIRE_COSY_SMOKE(ServerVersion.V1_14, "SMOKE_NORMAL"),
        CAMPFIRE_SIGNAL_SMOKE(ServerVersion.V1_14, "SMOKE_LARGE"),
        COMPOSTER(ServerVersion.V1_14, "CRIT"),
        FLASH(ServerVersion.V1_14, "EXPLOSION_NORMAL"), // idk
        FALLING_LAVA(ServerVersion.V1_14, "DRIP_LAVA"),
        LANDING_LAVA(ServerVersion.V1_14, "LAVA"),
        FALLING_WATER(ServerVersion.V1_14, "DRIP_WATER"),
        /// End 1.14 ///
        DRIPPING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        FALLING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        FALLING_NECTAR(ServerVersion.V1_15, "DRIP_WATER"),
        LANDING_HONEY(ServerVersion.V1_15, "DRIP_WATER"),
        /// End 1.15 ///
        // ToDo: Someone needs to make better compatible fall backs.
        SOUL_FIRE_FLAME(ServerVersion.V1_16, "DRIP_WATER"),
        ASH(ServerVersion.V1_16, "DRIP_WATER"),
        CRIMSON_SPORE(ServerVersion.V1_16, "DRIP_WATER"),
        WARPED_SPORE(ServerVersion.V1_16, "DRIP_WATER"),
        SOUL(ServerVersion.V1_16, "DRIP_WATER"),
        DRIPPING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        FALLING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        LANDING_OBSIDIAN_TEAR(ServerVersion.V1_16, "DRIP_WATER"),
        REVERSE_PORTAL(ServerVersion.V1_16, "DRIP_WATER"),
        WHITE_ASH(ServerVersion.V1_16, "DRIP_WATER"),
        /// End 1.16 ///
        // ToDo: Someone needs to make better compatible fall backs.
        LIGHT(ServerVersion.V1_17, "DRIP_WATER"),
        DUST_COLOR_TRANSITION(ServerVersion.V1_17, "DRIP_WATER"),
        VIBRATION(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_SPORE_BLOSSOM(ServerVersion.V1_17, "DRIP_WATER"),
        SPORE_BLOSSOM_AIR(ServerVersion.V1_17, "DRIP_WATER"),
        SMALL_FLAME(ServerVersion.V1_17, "DRIP_WATER"),
        SNOWFLAKE(ServerVersion.V1_17, "DRIP_WATER"),
        DRIPPING_DRIPSTONE_LAVA(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_DRIPSTONE_LAVA(ServerVersion.V1_17, "DRIP_WATER"),
        DRIPPING_DRIPSTONE_WATER(ServerVersion.V1_17, "DRIP_WATER"),
        FALLING_DRIPSTONE_WATER(ServerVersion.V1_17, "DRIP_WATER"),
        GLOW_SQUID_INK(ServerVersion.V1_17, "DRIP_WATER"),
        GLOW(ServerVersion.V1_17, "DRIP_WATER"),
        WAX_ON(ServerVersion.V1_17, "DRIP_WATER"),
        WAX_OFF(ServerVersion.V1_17, "DRIP_WATER"),
        ELECTRIC_SPARK(ServerVersion.V1_17, "DRIP_WATER"),
        SCRAPE(ServerVersion.V1_17, "DRIP_WATER"),
        /// End 1.17 ///
        ;

        final static Map<String, ParticleType> map = new HashMap<>();

        static {
            for (ParticleType t : values()) {
                map.put(t.name(), t);
            }
        }

        final LegacyParticleEffects.Type compatibleEffect;
        final Object particle;

        ParticleType() {
            if (ServerVersion.isServerVersionAtOrBelow(ServerVersion.V1_8)) {
                this.particle = null;
                this.compatibleEffect = LegacyParticleEffects.Type.valueOf(name());
            } else {
                this.compatibleEffect = null;
                // does this particle exist in our version?
                Particle check = Stream.of(Particle.values()).filter(p -> p.name().equals(name())).findFirst().orElse(null);
                if (check != null) {
                    this.particle = check;
                } else {
                    // this shouldn't happen, really
                    this.particle = Particle.END_ROD;
                }
            }
        }

        ParticleType(ServerVersion minVersion, String compatible) {
            // Particle class doesn't exist in 1.8
            if (ServerVersion.isServerVersionAtOrBelow(ServerVersion.V1_8)) {
                this.compatibleEffect = LegacyParticleEffects.Type.valueOf(compatible);
                this.particle = null;
            } else if (ServerVersion.isServerVersionBelow(minVersion)) {
                this.compatibleEffect = null;
                this.particle = Particle.valueOf(compatible);
            } else {
                this.compatibleEffect = null;
                // does this particle exist in our version?
                Particle check = Stream.of(Particle.values()).filter(p -> p.name().equals(name())).findFirst().orElse(null);
                if (check != null) {
                    this.particle = check;
                } else {
                    // this shouldn't happen, really
                    this.particle = Particle.END_ROD;
                }
            }
        }

        public static ParticleType getParticle(String name) {
            return map.get(name);
        }
    }
}
