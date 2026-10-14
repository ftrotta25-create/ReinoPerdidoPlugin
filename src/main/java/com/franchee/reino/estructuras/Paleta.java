package com.franchee.reino.estructuras;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.Random;

/**
 * Todos los BlockData que usan las estructuras. Se crean una sola vez (la primera vez que
 * se toca esta clase, que es en onEnable, en el hilo principal).
 */
public final class Paleta {

    private Paleta() {
    }

    private static BlockData d(String s) {
        return Bukkit.createBlockData(s);
    }

    private static BlockData d(Material m) {
        return Bukkit.createBlockData(m);
    }

    public static final BlockData AIRE = d(Material.AIR);

    // Muros y pisos
    public static final BlockData LADRILLO_PROF = d(Material.DEEPSLATE_BRICKS);
    public static final BlockData LADRILLO_PROF_ROTO = d(Material.CRACKED_DEEPSLATE_BRICKS);
    public static final BlockData TEJAS = d(Material.DEEPSLATE_TILES);
    public static final BlockData PIEDRA_MUSGO = d(Material.MOSSY_STONE_BRICKS);
    public static final BlockData PIEDRA_LADRILLO = d(Material.STONE_BRICKS);
    public static final BlockData PROF_PULIDA = d(Material.POLISHED_DEEPSLATE);
    public static final BlockData NEGRO_LADRILLO = d(Material.POLISHED_BLACKSTONE_BRICKS);

    // Decoracion "real"
    public static final BlockData LLORON = d(Material.CRYING_OBSIDIAN);
    public static final BlockData AMATISTA = d(Material.AMETHYST_BLOCK);
    public static final BlockData CLUSTER = d("minecraft:amethyst_cluster[facing=up]");
    public static final BlockData PURPUR = d(Material.PURPUR_BLOCK);
    public static final BlockData PURPUR_PILAR = d(Material.PURPUR_PILLAR);
    public static final BlockData ALFOMBRA = d(Material.PURPLE_CARPET);
    public static final BlockData VIDRIO = d(Material.PURPLE_STAINED_GLASS);
    public static final BlockData ESTANTERIA = d(Material.BOOKSHELF);
    public static final BlockData TELARANA = d(Material.COBWEB);
    public static final BlockData CADENA = d(Material.CHAIN);
    public static final BlockData FAROL = d("minecraft:soul_lantern[hanging=false]");
    public static final BlockData FAROL_COLGANTE = d("minecraft:soul_lantern[hanging=true]");
    public static final BlockData VELA = d("minecraft:purple_candle[candles=3,lit=true]");

    // Escaleras y escalerillas
    public static final BlockData ESCALERA_OESTE = d("minecraft:ladder[facing=west]");
    public static final BlockData PIEDRA_ESC_SUR = d("minecraft:deepslate_brick_stairs[facing=south,half=bottom]");
    public static final BlockData PIEDRA_ESC_NORTE = d("minecraft:deepslate_brick_stairs[facing=north,half=bottom]");
    public static final BlockData PURPUR_ESC_NORTE = d("minecraft:purpur_stairs[facing=north,half=bottom]");

    // Campamento
    public static final BlockData FOGATA = d("minecraft:campfire[lit=false]");
    public static final BlockData BARRIL = d("minecraft:barrel[facing=up]");
    public static final BlockData HENO = d(Material.HAY_BLOCK);
    public static final BlockData LANA = d(Material.WHITE_WOOL);
    public static final BlockData VALLA = d(Material.SPRUCE_FENCE);
    public static final BlockData TIERRA_GRUESA = d(Material.COARSE_DIRT);
    public static final BlockData PODZOL = d(Material.PODZOL);

    // Troncos (arboles muertos y bancos)
    public static final BlockData TRONCO_Y = d("minecraft:dark_oak_log[axis=y]");
    public static final BlockData TRONCO_X = d("minecraft:dark_oak_log[axis=x]");
    public static final BlockData TRONCO_Z = d("minecraft:dark_oak_log[axis=z]");
    public static final BlockData ABETO_X = d("minecraft:spruce_log[axis=x]");

    // Cimientos
    public static final BlockData CIMIENTO = d(Material.COBBLESTONE);

    public static BlockData cofre(String orientacion) {
        return d("minecraft:chest[facing=" + orientacion + "]");
    }

    public static BlockData muro(Random r) {
        double u = r.nextDouble();
        if (u < 0.50) return LADRILLO_PROF;
        if (u < 0.72) return LADRILLO_PROF_ROTO;
        if (u < 0.85) return TEJAS;
        if (u < 0.93) return PIEDRA_MUSGO;
        return PIEDRA_LADRILLO;
    }

    public static BlockData piso(Random r) {
        double u = r.nextDouble();
        if (u < 0.55) return PROF_PULIDA;
        if (u < 0.85) return TEJAS;
        return NEGRO_LADRILLO;
    }
}
