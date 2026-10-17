package dev.sevendaystocraft.locks;

/** Shared player-ammo mapping for reloads and gun tooltips. */
public final class TaczAmmoTypes {
    private TaczAmmoTypes() {}

    public static String ammoId(String gunId) {
        return switch (gunId) {
            case "tacz:glock_17", "tacz:m1911", "tacz:p320", "tacz:hk_mp5a5", "tacz:uzi"
                -> "cgm:basic_bullet";
            case "tacz:db_short", "tacz:m870", "tacz:m1014" -> "cgm:shell";
            case "tacz:sks_tactical", "tacz:m4a1", "tacz:ak47", "tacz:scar_l", "tacz:m700", "tacz:m249",
                "tacz:deagle", "tacz:hk416d", "tacz:rpk", "tacz:m107" -> "cgm:advanced_bullet";
            default -> "";
        };
    }
}
