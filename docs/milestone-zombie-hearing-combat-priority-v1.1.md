# Zombie hearing combat priority v1.1

The sound investigation is available only while a zombie is out of combat. A zombie with an active target never receives a new sound destination. Acquiring or losing a target, or being hurt by a living attacker, clears a pending sound and starts a 100-tick combat cooldown. The sound goal also checks this state every tick and yields immediately to normal combat AI.

The original hearing radii and per-sound responder limits from v1 are unchanged. Build: `source/seven-days-locks/gradlew.bat --offline clean build` with Java 17. In-game validation remains necessary: draw a zombie's aggro, fire a second gun elsewhere, and confirm it keeps pursuing its target; repeat after losing the target to check the five-second grace period.

Rollback: restore the prior JAR from `backups/zombie-hearing-combat-priority-v1.1-20261018/` and restart Minecraft.
