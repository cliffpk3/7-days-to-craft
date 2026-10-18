# Ore Sense brightness v3 — 2026-10-18

- Client ore light emission changed from level 6 to 8. Light levels are integers, so this is the nearest value to the requested +30% (6 × 1.30 = 7.8; actual numeric increase 33.3%).
- The 20-block range, skill gate and position-aware light logic are unchanged.
- Offline Gradle build passed. The new JAR is staged at `staging/ore-sense-brightness-v3-20261018/seven-days-skill-bridge-1.0.1.jar`; it has not been installed while the user is playing.
- The currently installed JAR is backed up at `backups/ore-sense-brightness-v3-20261018/seven-days-skill-bridge-1.0.1.jar`.
- After installation, compare the same nearby ores with the previous visual reference, including shaders, and check that ore farther than 20 blocks stays dark. No server-side block light or mob spawning change is intended.
