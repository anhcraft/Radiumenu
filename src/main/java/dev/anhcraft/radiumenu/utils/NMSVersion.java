package dev.anhcraft.radiumenu.utils;

import org.bukkit.Bukkit;

public enum NMSVersion {
    v1_9_R1(0),
    v1_9_R2(1),
    v1_10_R1(2),
    v1_11_R1(3),
    v1_12_R1(4),
    v1_13_R1(5),
    v1_13_R2(6),
    v1_14_R1(7),
    v1_15_R1(8),
    v1_16_R1(9),
    v1_16_R2(10),
    v1_16_R3(11),
    v1_17_R1(12),
    v1_18_R1(13),
    v1_18_R2(14),
    v1_19_R1(15),
    v1_19_R2(16),
    v1_19_R3(17),
    v1_20_R1(18);

    private static final NMSVersion version;
    private final int id;

    private NMSVersion(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static NMSVersion getVersion() {
        return version;
    }

    public static boolean is1_9Above() {
        return v1_9_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_10Above() {
        return v1_10_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_11Above() {
        return v1_11_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_12Above() {
        return v1_12_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_13Above() {
        return v1_13_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_14Above() {
        return v1_14_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_15Above() {
        return v1_15_R1.getId() <= NMSVersion.getVersion().getId();
    }

    public static boolean is1_16Above() {
        return v1_16_R1.getId() <= NMSVersion.getVersion().getId();
    }

    static {
        version = NMSVersion.valueOf(Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3]);
    }
}
