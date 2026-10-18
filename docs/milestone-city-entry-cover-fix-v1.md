# City entry cover fix v1 — 2026-10-18

## Scope

- The 2–4 zombie city entry ambush now searches 26–42 blocks from the entering player and requires solid cover from every nearby player for each spawn position. If no concealed, loaded street is available, the event stays pending and retries; it does not charge the player for a failed ambush.
- Removed the separate spawn placement override for the periodic Hordes event. Its original spawning behavior remains owned by The Hordes.
- Other city encounters and periodic city patrols are unchanged.

## Status and rollback

Source build passed (`source/seven-days-locks`: `gradlew --offline build`). The new JAR is staged in `staging/city-entry-cover-fix-v1-20261018/` and is **not installed** while the user tests the current game. The installed JAR was copied to `backups/city-entry-cover-fix-v1-20261018/` for rollback.

## In-game validation after installation

1. In a new city, approach from an open street and verify no entry zombie materializes in direct view; the first group includes 2–4 zombies and a Runner.
2. Repeat with two players at different sight lines. A spawn point must be covered from both.
3. Re-enter and reconnect in that city; the ambush should not repeat for the same player. Enter a different city to trigger a fresh one.
4. Test the periodic horde separately; it should no longer skip a wave due to the removed override.

The cover rule can delay an entry ambush in a street with no usable covered position. Record coordinates if it never fires in a city with adjacent buildings.
