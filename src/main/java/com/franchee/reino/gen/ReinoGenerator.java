package com.franchee.reino.gen;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/** Generador del Reino Perdido: terreno, cuevas, minerales ricos y lagos. Todo con bloques vanilla. */
public class ReinoGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData data) {
        long semilla = info.getSeed();
        Terreno terreno = Terreno.de(semilla);
        int minY = info.getMinHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkX * 16 + lx;
                int z = chunkZ * 16 + lz;
                int h = terreno.altura(x, z);
                boolean cuevasOk = Math.max(Math.abs(x), Math.abs(z)) > Terreno.SIN_CUEVAS_HASTA;

                for (int y = minY; y <= h; y++) {
                    Material m = bloque(terreno, semilla, x, y, z, h, minY, cuevasOk);
                    if (m != Material.AIR) {
                        data.setBlock(lx, y, lz, m);
                    }
                }
                for (int y = h + 1; y <= Terreno.NIVEL_AGUA; y++) {
                    data.setBlock(lx, y, lz, Material.WATER);
                }
            }
        }
    }

    private Material bloque(Terreno t, long s, int x, int y, int z, int h, int minY, boolean cuevasOk) {
        if (y == minY) return Material.BEDROCK;
        if (y <= minY + 4 && Hash.unit(s, x, y, z, 1) < (minY + 5 - y) * 0.2) return Material.BEDROCK;

        int prof = h - y;

        if (prof >= 7 && cuevasOk && y > minY + 5 && t.cueva(x, y, z)) {
            return y < -52 ? Material.LAVA : Material.AIR;
        }
        if (prof == 0) return superficie(s, x, z, h);
        if (prof <= 3) return h <= Terreno.NIVEL_AGUA ? Material.GRAVEL : Material.DIRT;

        boolean profundo = y < 0 || (y < 8 && Hash.unit(s, x, y, z, 2) < (8 - y) / 8.0);
        Material mena = Minerales.mineral(s, x, y, z, profundo);
        if (mena != null) return mena;
        if (Hash.unit(s, x >> 2, y >> 2, z >> 2, 3) < 0.10) return Material.TUFF;
        return profundo ? Material.DEEPSLATE : Material.STONE;
    }

    private Material superficie(long s, int x, int z, int h) {
        if (h <= Terreno.NIVEL_AGUA) {
            return h < Terreno.NIVEL_AGUA - 2 ? Material.GRAVEL : Material.MUD;
        }
        if (Math.max(Math.abs(x), Math.abs(z)) <= Terreno.MESETA_RADIO) {
            double u = Hash.unit(s, x, 0, z, 4);
            if (u < 0.40) return Material.COARSE_DIRT;
            if (u < 0.75) return Material.GRAVEL;
            return Material.PODZOL;
        }
        double u = Hash.unit(s, x >> 2, 0, z >> 2, 5);
        if (u < 0.10) return Material.COARSE_DIRT;
        if (u < 0.18) return Material.PODZOL;
        if (u < 0.24) return Material.MOSS_BLOCK;
        return Material.GRASS_BLOCK;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo info) {
        return new Biomas();
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return List.of(new ReinoPopulator());
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, Terreno.MESETA_Y + 1, -100.5);
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    /** Dos biomas: bosque oscuro por defecto y pantano en las zonas bajas con agua. */
    public static class Biomas extends BiomeProvider {

        @Override
        public Biome getBiome(WorldInfo info, int x, int y, int z) {
            return Terreno.de(info.getSeed()).altura(x, z) <= Terreno.NIVEL_AGUA + 2
                    ? Biome.SWAMP : Biome.DARK_FOREST;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo info) {
            return List.of(Biome.DARK_FOREST, Biome.SWAMP);
        }
    }
}
