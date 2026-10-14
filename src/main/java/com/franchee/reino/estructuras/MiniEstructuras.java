package com.franchee.reino.estructuras;

import com.franchee.reino.gen.Hash;
import com.franchee.reino.gen.Terreno;

import java.util.Random;

/**
 * Mini estructuras repartidas por el mundo: una por chunk "elegido" (alrededor de 1 cada 40),
 * centrada en el chunk, asi nunca se corta entre chunks. La decision es determinista
 * (depende solo de semilla + chunk), por eso el listener de cofres puede reconstruirla.
 *
 * Tipos: 0 torre en ruinas, 1 santuario de amatista, 2 campamento abandonado, 3 cripta.
 */
public final class MiniEstructuras {

    public record Sitio(int tipo, int cx, int cz, int centroX, int centroZ, int baseY, Buffer buffer) {
    }

    private static Buffer[] prefabs;

    private MiniEstructuras() {
    }

    public static synchronized void inicializar() {
        if (prefabs != null) return;
        prefabs = new Buffer[]{torre(), santuario(), campamento(), cripta()};
    }

    public static boolean hay(long semilla, int cx, int cz) {
        int mx = cx * 16 + 8;
        int mz = cz * 16 + 8;
        if (Math.max(Math.abs(mx), Math.abs(mz)) <= 160) return false;
        return Hash.unit(semilla, cx, cz, 0, 901) < 1.0 / 40.0;
    }

    /** El sitio de ese chunk, o null si ahi no hay mini estructura. */
    public static Sitio sitio(long semilla, Terreno terreno, int cx, int cz) {
        if (prefabs == null || !hay(semilla, cx, cz)) return null;
        int tipo = (int) (Hash.unit(semilla, cx, cz, 0, 902) * prefabs.length);
        int centroX = cx * 16 + 8;
        int centroZ = cz * 16 + 8;
        int baseY = Math.max(terreno.altura(centroX, centroZ), Terreno.NIVEL_AGUA + 1);
        return new Sitio(tipo, cx, cz, centroX, centroZ, baseY, prefabs[tipo]);
    }

    /** Nivel de loot del cofre en esa posicion ABSOLUTA, o 0 si no es un cofre de mini estructura. */
    public static int tierCofre(long semilla, Terreno terreno, int x, int y, int z) {
        Sitio s = sitio(semilla, terreno, Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (s == null) return 0;
        return s.buffer().tierCofre(x - s.centroX(), y - s.baseY(), z - s.centroZ());
    }

    // ------------------------------------------------------------ prefabs

    private static Buffer nuevo() {
        Buffer b = new Buffer(8, 12, 20);
        b.fill(-7, 1, -7, 7, 19, 7, Paleta.AIRE); // despeja arboles y plantas
        return b;
    }

    private static Buffer torre() {
        Buffer b = nuevo();
        Random r = new Random(101);

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) b.set(x, 0, z, Paleta.piso(r));
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) != 4 && Math.abs(z) != 4) continue;
                for (int ry = 1; ry <= 13; ry++) {
                    if (ry >= 7 && r.nextDouble() < (ry - 6) * 0.11) continue;
                    b.set(x, ry, z, Paleta.muro(r));
                }
            }
        }
        b.fill(-1, 1, -4, 1, 3, -4, Paleta.AIRE); // puerta sur

        // Piso superior (la mitad norte) y escalera de mano
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 3 && z == 0) continue;
                if (z >= -1) b.set(x, 6, z, Paleta.piso(r));
            }
        }
        for (int ry = 1; ry <= 6; ry++) b.set(3, ry, 0, Paleta.ESCALERA_OESTE);

        b.cofre(0, 1, 3, 1, "south");
        b.cofre(0, 7, 3, 1, "south");
        b.set(-3, 1, -3, Paleta.TELARANA);
        b.set(3, 1, -3, Paleta.TELARANA);
        b.set(-3, 5, 3, Paleta.TELARANA);
        b.set(-3, 1, 3, Paleta.FAROL);
        return b;
    }

    private static Buffer santuario() {
        Buffer b = nuevo();

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean borde = Math.abs(x) == 5 || Math.abs(z) == 5;
                b.set(x, 0, z, borde ? Paleta.PURPUR : Paleta.NEGRO_LADRILLO);
            }
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                for (int ry = 1; ry <= 5; ry++) b.set(sx * 4, ry, sz * 4, Paleta.PURPUR_PILAR);
                b.set(sx * 4, 6, sz * 4, Paleta.LLORON);
            }
        }
        b.set(0, 1, 0, Paleta.AMATISTA);
        b.set(0, 2, 0, Paleta.CLUSTER);
        b.set(2, 1, 0, Paleta.VELA);
        b.set(-2, 1, 0, Paleta.VELA);
        b.set(0, 1, 2, Paleta.VELA);
        b.set(0, 1, -2, Paleta.VELA);
        b.cofre(0, 1, -3, 1, "south");
        b.cofre(0, 1, 3, 1, "north");
        return b;
    }

    private static Buffer campamento() {
        Buffer b = nuevo();
        Random r = new Random(303);

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x * x + z * z <= 42) {
                    b.set(x, 0, z, r.nextDouble() < 0.7 ? Paleta.TIERRA_GRUESA : Paleta.PODZOL);
                }
            }
        }
        b.set(0, 1, 0, Paleta.FOGATA);
        b.set(2, 1, 1, Paleta.ABETO_X);
        b.set(-2, 1, -1, Paleta.ABETO_X);

        // Tienda ruinosa
        for (int[] p : new int[][]{{-5, 3}, {-1, 3}, {-5, 5}, {-1, 5}}) {
            b.set(p[0], 1, p[1], Paleta.VALLA);
            b.set(p[0], 2, p[1], Paleta.VALLA);
        }
        for (int x = -5; x <= -1; x++) {
            for (int z = 3; z <= 5; z++) {
                if (r.nextDouble() < 0.8) b.set(x, 3, z, Paleta.LANA);
            }
        }
        b.cofre(-3, 1, 4, 1, "south");
        b.set(3, 1, -2, Paleta.BARRIL);
        b.set(4, 1, -2, Paleta.HENO);
        b.cofre(4, 1, 3, 1, "west");
        b.set(1, 1, 3, Paleta.TELARANA);
        b.set(-4, 1, -3, Paleta.TELARANA);
        return b;
    }

    private static Buffer cripta() {
        Buffer b = nuevo();
        Random r = new Random(404);

        // Cenador en la superficie
        for (int x = -3; x <= 3; x++) {
            for (int z = -6; z <= 0; z++) b.set(x, 0, z, Paleta.piso(r));
        }
        for (int[] p : new int[][]{{-3, -6}, {3, -6}, {-3, 0}, {3, 0}}) {
            for (int ry = 1; ry <= 4; ry++) b.set(p[0], ry, p[1], Paleta.muro(r));
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -6; z <= 0; z++) b.set(x, 5, z, Paleta.muro(r));
        }

        // Sala subterranea (interior x -3..3, z 3..7, ry -6..-3)
        for (int x = -4; x <= 4; x++) {
            for (int z = 2; z <= 8; z++) {
                for (int ry = -7; ry <= -2; ry++) {
                    b.set(x, ry, z, ry == -7 ? Paleta.piso(r) : Paleta.muro(r));
                }
            }
        }
        b.fill(-3, -6, 3, 3, -3, 7, Paleta.AIRE);

        // Escalera hacia abajo (hacia +z)
        for (int i = 0; i <= 6; i++) {
            int z = -4 + i;
            b.fill(0, -i + 1, z, 0, -i + 3, z, Paleta.AIRE);
            b.set(0, -i, z, Paleta.PIEDRA_ESC_NORTE);
        }

        b.cofre(-2, -6, 7, 2, "south");
        b.cofre(2, -6, 7, 2, "south");
        b.set(3, -4, 3, Paleta.TELARANA);
        b.set(-3, -4, 7, Paleta.TELARANA);
        b.set(0, -3, 5, Paleta.FAROL_COLGANTE);
        return b;
    }
}
