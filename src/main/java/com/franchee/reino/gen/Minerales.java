package com.franchee.reino.gen;

import org.bukkit.Material;

/**
 * Minerales del Reino Perdido: MUCHO mas abundantes que en el overworld (esa es parte de
 * la recompensa de venir). Las menas salen en "grumos" de 2x2x2 y hay ancient debris suelto.
 */
public final class Minerales {

    private record Mena(Material normal, Material profunda, int yMin, int yMax, double prob, int sal) {
    }

    private static final Mena[] MENAS = {
            new Mena(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, -64, 16, 0.0012, 11),
            new Mena(Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, -16, 120, 0.0006, 12),
            new Mena(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, -64, 32, 0.0007, 13),
            new Mena(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, -64, 16, 0.0015, 14),
            new Mena(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, -64, 64, 0.0008, 15),
            new Mena(Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, -20, 90, 0.0025, 16),
            new Mena(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, -50, 90, 0.003, 17),
            new Mena(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, 0, 130, 0.004, 18)
    };

    private Minerales() {
    }

    /** Devuelve la mena que va en ese bloque, o null si es roca normal. */
    public static Material mineral(long semilla, int x, int y, int z, boolean profundo) {
        if (y >= -60 && y <= -10 && Hash.unit(semilla, x, y, z, 10) < 0.00008) {
            return Material.ANCIENT_DEBRIS;
        }
        for (Mena m : MENAS) {
            if (y < m.yMin() || y > m.yMax()) continue;
            if (Hash.unit(semilla, x >> 1, y >> 1, z >> 1, m.sal()) < m.prob()
                    && Hash.unit(semilla, x, y, z, m.sal() + 100) < 0.85) {
                return profundo ? m.profunda() : m.normal();
            }
        }
        return null;
    }
}
