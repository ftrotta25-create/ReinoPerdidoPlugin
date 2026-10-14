package com.franchee.reino.gen;

import com.franchee.reino.estructuras.Ciudadela;
import com.franchee.reino.estructuras.MiniEstructuras;
import com.franchee.reino.estructuras.Paleta;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Despues del terreno: pega la ciudadela (por pedazos), las mini estructuras, y decora
 * el resto con geodas de amatista, arboles muertos, plantas y pilares en ruinas.
 */
public class ReinoPopulator extends BlockPopulator {

    @Override
    public void populate(WorldInfo info, Random random, int cx, int cz, LimitedRegion region) {
        long semilla = info.getSeed();
        int minX = cx * 16;
        int maxX = minX + 15;
        int minZ = cz * 16;
        int maxZ = minZ + 15;

        // 1. Ciudadela
        if (Ciudadela.buffer() != null
                && maxX >= -Ciudadela.RADIO && minX <= Ciudadela.RADIO
                && maxZ >= -Ciudadela.RADIO && minZ <= Ciudadela.RADIO) {
            Ciudadela.buffer().pegar(region, 0, Ciudadela.Y0, 0, minX, maxX, minZ, maxZ);
            return;
        }

        Terreno terreno = Terreno.de(semilla);

        // 2. Mini estructura
        MiniEstructuras.Sitio sitio = MiniEstructuras.sitio(semilla, terreno, cx, cz);
        if (sitio != null) {
            cimentar(region, sitio, minX, minZ);
            sitio.buffer().pegar(region, sitio.centroX(), sitio.baseY(), sitio.centroZ(), minX, maxX, minZ, maxZ);
            return;
        }

        // 3. Decoracion
        decorar(semilla, terreno, cx, cz, region);
    }

    /** Rellena hacia abajo bajo la mini estructura para que no quede flotando sobre una pendiente o un lago. */
    private void cimentar(LimitedRegion region, MiniEstructuras.Sitio sitio, int minX, int minZ) {
        for (int x = sitio.centroX() - 6; x <= sitio.centroX() + 6; x++) {
            for (int z = sitio.centroZ() - 6; z <= sitio.centroZ() + 6; z++) {
                for (int y = sitio.baseY() - 1; y >= sitio.baseY() - 10; y--) {
                    if (!region.isInRegion(x, y, z)) break;
                    if (region.getType(x, y, z).isSolid()) break;
                    region.setBlockData(x, y, z, Paleta.CIMIENTO);
                }
            }
        }
    }

    private void decorar(long semilla, Terreno terreno, int cx, int cz, LimitedRegion region) {
        int baseX = cx * 16;
        int baseZ = cz * 16;
        Random r = new Random(Hash.h(semilla, cx, cz, 77));

        // Geodas de amatista bajo tierra (en todo el mundo)
        if (r.nextDouble() < 0.14) {
            int gx = baseX + 4 + r.nextInt(8);
            int gz = baseZ + 4 + r.nextInt(8);
            int gy = -45 + r.nextInt(60);
            if (gy + 8 < terreno.altura(gx, gz)) geoda(region, gx, gy, gz);
        }

        // Cerca de la meseta no se decora (queda el campo de batalla pelado)
        if (Math.max(Math.abs(baseX + 8), Math.abs(baseZ + 8)) <= 150) return;

        // Plantas sueltas
        for (int i = 0; i < 40; i++) {
            int x = baseX + r.nextInt(16);
            int z = baseZ + r.nextInt(16);
            int y = terreno.altura(x, z);
            if (y <= Terreno.NIVEL_AGUA) continue;
            Material suelo = region.getType(x, y, z);
            if (suelo == Material.GRASS_BLOCK || suelo == Material.PODZOL || suelo == Material.COARSE_DIRT) {
                if (!region.getType(x, y + 1, z).isAir()) continue;
                double u = r.nextDouble();
                Material planta = suelo == Material.COARSE_DIRT
                        ? Material.DEAD_BUSH
                        : (u < 0.6 ? Material.GRASS : (u < 0.9 ? Material.FERN : Material.DEAD_BUSH));
                if (planta == Material.DEAD_BUSH && suelo == Material.GRASS_BLOCK) planta = Material.FERN;
                region.setType(x, y + 1, z, planta);
            }
        }

        // Arboles muertos
        if (r.nextDouble() < 0.35) {
            int n = 1 + r.nextInt(3);
            for (int k = 0; k < n; k++) {
                int x = baseX + 4 + r.nextInt(8);
                int z = baseZ + 4 + r.nextInt(8);
                int y = terreno.altura(x, z);
                if (y <= Terreno.NIVEL_AGUA + 1) continue;
                arbolMuerto(region, r, x, y, z);
            }
        }

        // Pilares en ruinas
        if (r.nextDouble() < 0.18) {
            int x = baseX + 3 + r.nextInt(10);
            int z = baseZ + 3 + r.nextInt(10);
            int y = terreno.altura(x, z);
            if (y > Terreno.NIVEL_AGUA + 1) {
                int alto = 2 + r.nextInt(5);
                for (int i = 1; i <= alto; i++) {
                    region.setBlockData(x, y + i, z, r.nextDouble() < 0.3 ? Paleta.LADRILLO_PROF_ROTO : Paleta.LADRILLO_PROF);
                }
                if (r.nextDouble() < 0.4) region.setBlockData(x, y + alto + 1, z, Paleta.LLORON);
            }
        }
    }

    private void arbolMuerto(LimitedRegion region, Random r, int x, int y, int z) {
        int altura = 5 + r.nextInt(5);
        for (int i = 1; i <= altura; i++) region.setBlockData(x, y + i, z, Paleta.TRONCO_Y);

        int ramas = 2 + r.nextInt(2);
        for (int k = 0; k < ramas; k++) {
            int ry = 3 + r.nextInt(Math.max(1, altura - 3));
            int dir = r.nextInt(4);
            int dx = dir == 0 ? 1 : (dir == 1 ? -1 : 0);
            int dz = dir == 2 ? 1 : (dir == 3 ? -1 : 0);
            int largo = 1 + r.nextInt(3);
            for (int l = 1; l <= largo; l++) {
                int bx = x + dx * l;
                int bz = z + dz * l;
                int by = y + ry + (l >= 2 ? 1 : 0);
                if (!region.isInRegion(bx, by, bz)) break;
                region.setBlockData(bx, by, bz, dx != 0 ? Paleta.TRONCO_X : Paleta.TRONCO_Z);
            }
        }
    }

    /** Geoda de amatista: basalto, calcita, amatista (con algo de budding) y hueco con clusters. */
    private void geoda(LimitedRegion region, int cx, int cy, int cz) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    if (!region.isInRegion(x, y, z)) continue;

                    if (d <= 2.6) {
                        region.setType(x, y, z, Material.AIR);
                    } else if (d <= 3.4) {
                        double u = Hash.unit(1L, x, y, z, 6);
                        region.setType(x, y, z, u < 0.08 ? Material.BUDDING_AMETHYST : Material.AMETHYST_BLOCK);
                    } else if (d <= 4.2) {
                        region.setType(x, y, z, Material.CALCITE);
                    } else if (d <= 5.0) {
                        region.setType(x, y, z, Material.SMOOTH_BASALT);
                    }
                }
            }
        }
        // Clusters sobre la amatista del fondo
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 2; dy++) {
                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;
                    if (!region.isInRegion(x, y, z) || !region.isInRegion(x, y - 1, z)) continue;
                    Material debajo = region.getType(x, y - 1, z);
                    boolean sobreAmatista = debajo == Material.AMETHYST_BLOCK || debajo == Material.BUDDING_AMETHYST;
                    if (sobreAmatista && region.getType(x, y, z).isAir() && Hash.unit(2L, x, y, z, 7) < 0.35) {
                        region.setBlockData(x, y, z, Paleta.CLUSTER);
                    }
                }
            }
        }
    }
}
