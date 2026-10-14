package com.franchee.reino.gen;

/** Hash determinista (SplitMix64) para decidir cosas por coordenada sin guardar nada. */
public final class Hash {

    private Hash() {
    }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    public static long h(long semilla, int a, int b, int c, int sal) {
        long v = semilla ^ (sal * 0x9E3779B97F4A7C15L);
        v = mix(v + a * 0x632BE59BD9B4E019L);
        v = mix(v + b * 0x8CB92BA72F3D8DD7L);
        v = mix(v + c * 0xC2B2AE3D27D4EB4FL);
        return v;
    }

    /** Numero uniforme en [0,1). */
    public static double unit(long semilla, int a, int b, int c, int sal) {
        return (h(semilla, a, b, c, sal) >>> 11) * (1.0 / (1L << 53));
    }
}
