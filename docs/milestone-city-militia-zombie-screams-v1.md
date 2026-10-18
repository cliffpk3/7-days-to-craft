# City militia zombie and screams v1

- A city `simpleenemymod:usunit` killed directly by a configured city zombie becomes an ordinary `minecraft:zombie` at the death location.
- The Hordes infection conversion table also maps US units to an ordinary zombie. Its conversion event applies the same equipment treatment.
- The zombie retains the soldier's held TaCZ gun as a visual item with zero loaded and dummy ammunition. Existing armor is copied; empty armor slots receive olive leather uniform pieces. All copied equipment has zero drop chance.
- A hostile monster killing a US unit triggers one of seven `militia_infected_death` sounds provided by the pack owner. The unit's native death sound is silenced in this case. A player kill retains the native sound.
- The direct conversion keeps the prior city density caps: at most four reanimated militia and thirty city zombies within the local search area.

Build: `source/seven-days-locks/gradlew.bat --offline clean build` succeeded on 2026-10-18. The installed JAR matches the built JAR by SHA-256. In-game sound, visual equipment, and conversion behavior still require a live check.

Rollback files: `backups/city-militia-zombie-screams-v1-20261018/`. Restore the saved JAR to `mods/seven-days-locks-1.0.1.jar` and the saved JSON to `config/hordes/data/hordes/infection/infection_conversions.json`, then restart Minecraft.
