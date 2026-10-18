# Zombie hearing v1

Ordinary loaded zombies, including the pack's Zombie Extreme and zombie derived cave mobs, investigate a sound's last known position. This does not assign the shooter as a target through walls. Existing combat targets take priority, and tagged horde mobs keep their horde behavior.

Server-side sound triggers:

| Sound | Default radius | Maximum responders |
| --- | ---: | ---: |
| TaCZ or CGM gunshot | 48 blocks | 16 |
| TaCZ gun with a silencer | 16 blocks | 16 |
| Militia killed by a hostile monster | 24 blocks | 8 |
| Zombie killed by a non-zombie living entity | 14 blocks | 4 |

Gunfire is processed at most once every 20 ticks per shooter. Zombie death cries are processed at most once every 60 ticks per chunk. All searches are limited to loaded entities inside the radius and at most 24 vertical blocks. Sound memory lasts 160 ticks. The goal yields to combat and expires when the zombie reaches the location or time runs out.

Tuning: `config/seven-days-zombie-hearing.toml`. Restart the game/server after editing. To roll back, restore the previous JAR from `backups/zombie-hearing-v1-20261018/`, remove the hearing config, and restart.

Validation: Gradle build and resource/config checks succeeded. In-game tests pending: unsuppressed TaCZ shot, silenced TaCZ shot, CGM shot, militia scream, zombie death cry, target behavior through walls, and horde focus. Watch TPS during a city firefight with multiple squads.
