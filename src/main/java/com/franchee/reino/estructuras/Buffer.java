package com.franchee.reino.estructuras;

import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Una "maqueta" 3D de bloques que se arma una sola vez con codigo y despues se copia al mundo
 * pedazo por pedazo (un chunk a la vez). Las coordenadas son relativas al centro:
 * x, z en [-radio, radio] e y en [-profundidad, altura] (0 = el bloque de superficie).
 * Un casillero "sin tocar" (null) deja el terreno como esta.
 */
public final class Buffer {

    private final int radio;
    private final int prof;
    private final int altura;
    private final int ancho;
    private final int alto;
    private final short[] datos;
    private final List<BlockData> paleta = new ArrayList<>();
    private final Map<BlockData, Short> indice = new HashMap<>();
    private final Map<Long, Integer> cofres = new HashMap<>();

    public Buffer(int radio, int profundidad, int altura) {
        this.radio = radio;
        this.prof = profundidad;
        this.altura = altura;
        this.ancho = 2 * radio + 1;
        this.alto = profundidad + altura + 1;
        this.datos = new short[ancho * alto * ancho];
        this.paleta.add(null); // indice 0 = sin tocar
    }

    public int radio() {
        return radio;
    }

    private int idx(int x, int y, int z) {
        return ((y + prof) * ancho + (z + radio)) * ancho + (x + radio);
    }

    public boolean dentro(int x, int y, int z) {
        return x >= -radio && x <= radio && z >= -radio && z <= radio && y >= -prof && y <= altura;
    }

    public void set(int x, int y, int z, BlockData bloque) {
        if (!dentro(x, y, z)) return;
        Short i = indice.get(bloque);
        if (i == null) {
            paleta.add(bloque);
            i = (short) (paleta.size() - 1);
            indice.put(bloque, i);
        }
        datos[idx(x, y, z)] = i;
    }

    public BlockData get(int x, int y, int z) {
        if (!dentro(x, y, z)) return null;
        return paleta.get(datos[idx(x, y, z)]);
    }

    /** Rellena una caja (extremos incluidos). */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockData bloque) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, bloque);
                }
            }
        }
    }

    // ---------------- Cofres ----------------

    private static long clave(int x, int y, int z) {
        return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (long) (z + 512);
    }

    /** Pone un cofre mirando hacia "orientacion" (north/south/east/west) y lo anota con su nivel de loot. */
    public void cofre(int x, int y, int z, int tier, String orientacion) {
        set(x, y, z, Paleta.cofre(orientacion));
        cofres.put(clave(x, y, z), tier);
    }

    /** Nivel de loot del cofre en esa posicion relativa, o 0 si ahi no hay un cofre generado. */
    public int tierCofre(int x, int y, int z) {
        Integer t = cofres.get(clave(x, y, z));
        return t == null ? 0 : t;
    }

    // ---------------- Copia al mundo ----------------

    /**
     * Copia al mundo solo la parte de la maqueta que cae dentro del chunk
     * [minX..maxX] x [minZ..maxZ]. (baseX, baseY, baseZ) = donde cae el (0,0,0) de la maqueta.
     */
    public void pegar(LimitedRegion region, int baseX, int baseY, int baseZ,
                      int minX, int maxX, int minZ, int maxZ) {
        int x0 = Math.max(minX, baseX - radio);
        int x1 = Math.min(maxX, baseX + radio);
        int z0 = Math.max(minZ, baseZ - radio);
        int z1 = Math.min(maxZ, baseZ + radio);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int ry = -prof; ry <= altura; ry++) {
                    short i = datos[idx(x - baseX, ry, z - baseZ)];
                    if (i == 0) continue;
                    int y = baseY + ry;
                    if (!region.isInRegion(x, y, z)) continue;
                    region.setBlockData(x, y, z, paleta.get(i));
                }
            }
        }
    }
}
