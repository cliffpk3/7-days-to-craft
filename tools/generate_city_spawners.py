"""Rebuild the 7DTC Lost City template overrides from the installed mod JAR.

Requires nbtlib. Only affects newly generated city pieces after a server restart.
"""

from __future__ import annotations

import gzip
import io
import json
import zipfile
from copy import deepcopy
from pathlib import Path

import nbtlib
from nbtlib import tag


ROOT = Path(__file__).resolve().parents[1]
MOD = ROOT / "mods" / "the_lost_city-1.5.0-forge-1.20.1.jar"
DATA = ROOT / "kubejs" / "data"
COMMON = (
    ("infected", 7),
    ("crawler", 5),
    ("runner", 4),
    ("clicker", 3),
    ("revived", 3),
    ("infected_police", 2),
)
SPECIAL = (
    ("boomer", 2),
    ("infected_military", 2),
    ("infected_hazmat", 2),
    ("faceless", 1),
    ("divided", 1),
)
BOSS = (("infected_juggernaut", 1), ("demolisher", 1), ("night_hunter", 1))


def source(jar: zipfile.ZipFile, name: str) -> nbtlib.File:
    raw = jar.read(f"data/the_lost_city/structures/{name}.nbt")
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(raw)))


def save_template(name: str, template: nbtlib.File, namespace: str = "seven_days_locks") -> None:
    target = DATA / namespace / "structures" / f"{name}.nbt"
    target.parent.mkdir(parents=True, exist_ok=True)
    if target.exists():
        content = io.BytesIO()
        template.write(content)
        if gzip.decompress(target.read_bytes()) == content.getvalue():
            return
    template.save(target, gzipped=True)


def spawner(template: nbtlib.File, entity: str, pool: str) -> None:
    block = template["blocks"][0]
    block_nbt = block["nbt"]
    block_nbt["SpawnData"]["entity"]["id"] = tag.String(entity)
    block_nbt["SpawnCount"] = tag.Short(2)
    block_nbt["MaxNearbyEntities"] = tag.Short(8)
    block_nbt["RequiredPlayerRange"] = tag.Short(20)
    block_nbt["MinSpawnDelay"] = tag.Short(500)
    block_nbt["MaxSpawnDelay"] = tag.Short(1100)
    template["blocks"][1]["nbt"]["pool"] = tag.String(pool)


def pool(name: str, choices: list[tuple[str, int]]) -> None:
    target = DATA / "the_lost_city" / "worldgen" / "template_pool" / f"{name}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    value = {
        "name": f"the_lost_city:{name}",
        "fallback": "minecraft:empty",
        "elements": [
            {
                "weight": weight,
                "element": {
                    "element_type": "minecraft:single_pool_element",
                    "location": f"seven_days_locks:city_spawner_{entity}",
                    "processors": {"processors": []},
                    "projection": "rigid",
                },
            }
            for entity, weight in choices
        ],
    }
    target.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    with zipfile.ZipFile(MOD) as jar:
        # The Lost City uses these five pools for its built-in hostile spawners.
        # Redirect all of them to infected variants. No mystical or pillager
        # mobs are introduced by the city templates.
        zombie_pools = (
            "spawners_zombie", "spawners_zombie_moss", "spawners_husk",
            "spawners_pillager", "spawners_vindicator",
        )
        for pool_name in zombie_pools:
            for mob, _ in COMMON + SPECIAL + BOSS:
                variant = nbtlib.File.parse(io.BytesIO(gzip.decompress(
                    jar.read("data/the_lost_city/structures/spawner_zombie_moss.nbt" if pool_name.endswith("moss")
                             else "data/the_lost_city/structures/spawner_zombie.nbt")
                )))
                spawner(variant, f"zombie_extreme:{mob}", f"the_lost_city:{pool_name}")
                save_template(f"city_spawner_{mob}_{pool_name}", variant)
            # Give common enemies most rolls and make the boss spawner rare.
            choices = [(f"{mob}_{pool_name}", weight) for mob, weight in COMMON + SPECIAL + BOSS]
            pool(pool_name, choices)

        # The ruined police station is the militia-held building. Existing
        # jigsaw spawners and the built-in spawner become Simple Enemy units.
        police = source(jar, "m_police_station")
        spawner_state = None
        police_spawner_nbt = None
        for block in police["blocks"]:
            state = police["palette"][int(block["state"])]["Name"]
            if state == "minecraft:jigsaw" and "spawners_zombie" in str(block.get("nbt", {}).get("pool", "")):
                block["nbt"]["pool"] = tag.String("the_lost_city:spawners_7dtc_militia")
            if state == "minecraft:spawner":
                spawner_state = int(block["state"])
                block["nbt"]["SpawnData"]["entity"]["id"] = tag.String("simpleenemymod:usunit")
                block["nbt"]["SpawnCount"] = tag.Short(2)
                block["nbt"]["MaxNearbyEntities"] = tag.Short(8)
                block["nbt"]["MinSpawnDelay"] = tag.Short(500)
                block["nbt"]["MaxSpawnDelay"] = tag.Short(1000)
                police_spawner_nbt = deepcopy(block["nbt"])
            if state in ("minecraft:chest", "minecraft:barrel"):
                block["nbt"]["LootTable"] = tag.String("seven_days_locks:chests/city_militia")
        if spawner_state is None or police_spawner_nbt is None:
            raise ValueError("Police station no longer contains its expected spawner")
        # Raised posts sit one block inside opposite roof edges so guards can
        # shoot down to the street without the flat roof blocking their view.
        roof_posts = ((7, 9), (16, 18))
        quartz_state = next(i for i, state in enumerate(police["palette"])
                            if state["Name"] == "minecraft:quartz_block" and "Properties" not in state)
        roof_platform = {(x + dx, 33, z + dz) for x, z in roof_posts
                         for dx in (-1, 0, 1) for dz in (-1, 0, 1)}
        roof_positions = {(x, 34, z) for x, z in roof_posts}
        for block in police["blocks"]:
            pos = tuple(map(int, block["pos"]))
            if pos in roof_platform:
                if police["palette"][int(block["state"])]["Name"] != "minecraft:air":
                    raise ValueError(f"Rooftop platform position is occupied: {block['pos']}")
                block["state"] = tag.Int(quartz_state)
                continue
            if pos not in roof_positions:
                continue
            if police["palette"][int(block["state"])]["Name"] != "minecraft:air":
                raise ValueError(f"Rooftop spawn position is occupied: {block['pos']}")
            block["state"] = tag.Int(spawner_state)
            block["nbt"] = deepcopy(police_spawner_nbt)
            block["nbt"]["SpawnCount"] = tag.Short(1)
            block["nbt"]["MaxNearbyEntities"] = tag.Short(4)
            block["nbt"]["RequiredPlayerRange"] = tag.Short(88)
            block["nbt"]["SpawnRange"] = tag.Short(1)
            block["nbt"]["MinSpawnDelay"] = tag.Short(700)
            block["nbt"]["MaxSpawnDelay"] = tag.Short(1200)
        save_template("m_police_station", police, namespace="the_lost_city")

        for unit in ("usunit",):
            variant = source(jar, "spawner_zombie")
            spawner(variant, f"simpleenemymod:{unit}", "the_lost_city:spawners_7dtc_militia")
            save_template(f"city_spawner_militia_{unit}", variant)
        pool("spawners_7dtc_militia", [("militia_usunit", 1)])


if __name__ == "__main__":
    main()
