# Horde roles v2 — 2026-10-18

- Grappler assignment falls from 10% to 5% of eligible horde zombies. The live config `config/seven-days-horde-roles.toml` was set to `fisherChance = 0.05`; the Java default also matches. The 5% applies to newly assigned roles, not mobs already spawned.
- Pull horizontal cap rises from 0.55 to 0.62 and the distance factor from 0.08 to 0.09, about 12% stronger. Vertical impulse remains capped at 0.18.
- Special-role labels are always enabled for Grappler, Miner (former Breaker), Tanker (former Tank), Disruptor and Summoner. The existing nameplate visibility rule now also covers role-tagged mobs: maximum 24 blocks and line of sight, preventing labels through walls or deep underground. Ordinary Assault mobs keep no label. Existing loaded Breaker/Tank names migrate to Miner/Tanker on entity join.
- Build passed. The combined JAR, also containing the pending city-entry and announcement corrections, is staged at `staging/horde-roles-v2-20261018/seven-days-locks-1.0.1.jar`. The installed JAR is backed up at `backups/horde-roles-v2-20261018/` and is not replaced while the user plays.
- In-game checks: new horde sample with role distribution over several waves; compare one Grappler pull; confirm five labels at 5/24/25 blocks and behind a solid wall, including Infernal elites; verify ordinary zombies have no persistent label.
