package com.franchee.reino.gen;

import org.bukkit.util.noise.SimplexNoiseGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forma del terreno del Reino Perdido. Todo es una funcion pura de (semilla, x, z):
 * el generador, los populators y los listeners llegan siempre a la misma altura.
 */
public final class Terreno {

    public static final int NIVEL_AGUA = 62;
    public static final int MESETA_Y = 80;
    public static final int MESETA_RADIO = 110;
    public static final int MESETA_BORDE = 70;
    public static final int SIN_CUEVAS_HASTA = 140;

    private static final Map<Long, Terreno> CACHE = new ConcurrentHashMap<>();

    public static Terreno de(long semilla) {
        return CACHE.computeIfAbsent(semilla, Terreno::new);
    }

    private final SimplexNoiseGenerator colinas;
    private final SimplexNoiseGenerator montanas;
    private final SimplexNoiseGenerator detalle;
    private final SimplexNoiseGenerator cuevaA;
    private final SimplexNoiseGenerator cuevaB;
    private final SimplexNoiseGenerator cuevaC;

    private Terreno(long semilla) {
        colinas = new SimplexNoiseGenerator(semilla);
        montanas = new SimplexNoiseGenerator(semilla * 31 + 7);
        detalle = new SimplexNoiseGenerator(semilla * 17 + 3);
        cuevaA = new SimplexNoiseGenerator(semilla * 13 + 5);
        cuevaB = new SimplexNoiseGenerator(semilla * 19 + 11);
        cuevaC = new SimplexNoiseGenerator(semilla * 23 + 13);
    }

    private static double fbm(SimplexNoiseGenerator n, double x, double z, int octavas) {
        double suma = 0;
        double amplitud = 1;
        double frecuencia = 1;
        double norma = 0;
        for (int i = 0; i < octavas; i++) {
            suma += n.noise(x * frecuencia, z * frecuencia) * amplitud;
            norma += amplitud;
            amplitud *= 0.5;
            frecuencia *= 2;
        }
        return suma / norma;
    }

    /** Altura del bloque de superficie en (x, z). */
    public int altura(int x, int z) {
        double base = 72 + fbm(colinas, x * 0.006, z * 0.006, 5) * 16.0;
        double m = fbm(montanas, x * 0.0035, z * 0.0035, 4);
        if (m > 0.12) {
            double k = (m - 0.12) / 0.88;
            base += k * k * 60.0;
        }
        base += fbm(detalle, x * 0.03, z * 0.03, 2) * 2.5;

        double d = Math.max(Math.abs(x), Math.abs(z));
        double t = Math.min(1.0, Math.max(0.0, (d - MESETA_RADIO) / MESETA_BORDE));
        t = t * t * (3 - 2 * t);
        return (int) Math.round(MESETA_Y * (1 - t) + base * t);
    }

    /** True si en ese punto hay una cueva (ya se chequeo que no es la superficie). */
    public boolean cueva(int x, int y, int z) {
        double a = cuevaA.noise(x * 0.018, y * 0.03, z * 0.018);
        if (a > 0.58) return true;
        double b = cuevaB.noise(x * 0.03, y * 0.045, z * 0.03);
        if (Math.abs(b) >= 0.05) return false;
        double c = cuevaC.noise(x * 0.03 + 100, y * 0.045, z * 0.03 + 100);
        return Math.abs(c) < 0.05;
    }
}
