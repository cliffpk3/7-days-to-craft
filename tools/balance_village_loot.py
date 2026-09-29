"""Generate the 7DTC village loot balance datapack overrides.

Towns and Towers and Better Villages often place several loot containers in a
single structure template.  Vanilla loot tables are balanced around one chest
per building, so reducing stack sizes alone does not solve the multiplication.

Run this script again after updating either mod.  It keeps the physical
containers but removes LootTable from the excess block entities.
"""

from __future__ import annotations

import gzip
import io
import json
from collections import Counter
from pathlib import Path
import zipfile

import nbtlib


ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "kubejs" / "data"
TOWNS_JAR = ROOT / "mods" / "Towns-and-Towers-1.12-Fabric+Forge.jar"
BETTER_JAR = ROOT / "mods" / "bettervillage-forge-1.20.1-3.2.0.jar"


def scalar(value):
    try:
        return value.unpack(json=False)
    except AttributeError:
        return value


def loot_compounds(value, found=None):
    if found is None:
        found = []
    if isinstance(value, (dict, nbtlib.Compound)):
        if "LootTable" in value:
            found.append(value)
        for child in value.values():
            loot_compounds(child, found)
    elif isinstance(value, (list, nbtlib.List)):
        for child in value:
            loot_compounds(child, found)
    return found


def structure_cap(name: str) -> int | None:
    # Better Villages replaces vanilla village templates in the minecraft
    # namespace. Houses normally get one loot container; larger town centres
    # may keep three different rewards.
    if name.startswith("data/minecraft/structures/village/"):
        return 3 if "/town_centers/" in name else 1

    # Towns and Towers village templates use the kaisyn namespace.
    if name.startswith("data/kaisyn/structures/village/"):
        if "/town_centers/" in name:
            return 3
        if "/side/" in name:
            return 2
        return 1

    # Its ocean villages are assembled from unusually loot-dense ships.
    if name.startswith("data/kaisyn/structures/ships/village_ocean/"):
        if "/mothership/" in name:
            return 4
        return 2
    return None


def choose_kept(nodes, cap: int):
    """Prefer table variety, then preserve original structure order."""
    kept = []
    seen = set()
    for node in nodes:
        table = str(scalar(node["LootTable"]))
        if table not in seen and len(kept) < cap:
            kept.append(node)
            seen.add(table)
    for node in nodes:
        if len(kept) >= cap:
            break
        if all(node is not existing for existing in kept):
            kept.append(node)
    return kept


def balance_structures(jar_path: Path):
    changed = []
    removed_by_table = Counter()
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            cap = structure_cap(name)
            if cap is None or not name.endswith(".nbt"):
                continue
            source = gzip.decompress(jar.read(name))
            root = nbtlib.File.parse(io.BytesIO(source))
            nodes = loot_compounds(root)
            if len(nodes) <= cap:
                continue
            kept = choose_kept(nodes, cap)
            for node in nodes:
                if all(node is not existing for existing in kept):
                    removed_by_table[str(scalar(node["LootTable"]))] += 1
                    del node["LootTable"]
                    node.pop("LootTableSeed", None)
            relative = Path(name).relative_to("data")
            target = DATA / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            root.save(target, gzipped=True)
            changed.append((name, len(nodes), cap))
    return changed, removed_by_table


def clamp_number_provider(value, maximum):
    if isinstance(value, dict):
        if "max" in value:
            value["max"] = min(value["max"], maximum)
        if "min" in value:
            value["min"] = min(value["min"], maximum)
    elif isinstance(value, (int, float)):
        value = min(value, maximum)
    return value


def limit_rolls(value):
    """Village containers get between one and three entry rolls."""
    if isinstance(value, dict):
        value["min"] = 1
        value["max"] = min(value.get("max", 3), 3)
    else:
        value = min(value, 3)
    return value


def balance_loot_tables():
    changed = []
    with zipfile.ZipFile(TOWNS_JAR) as jar:
        for name in jar.namelist():
            if "/loot_tables/" not in name or not name.endswith(".json"):
                continue
            # These tables occur inside villages. Armory and unrelated outpost
            # tables keep their danger-based rewards.
            village_table = "/loot_tables/village/" in name
            shared_village_table = name.endswith(
                (
                    "/outpost/common/food.json",
                    "/outpost/exclusives/outpost_beach_barrel.json",
                    "/outpost/exclusives/outpost_mediterranean_barrel.json",
                )
            )
            if not (village_table or shared_village_table):
                continue
            table = json.loads(jar.read(name))
            for pool in table.get("pools", []):
                pool["rolls"] = limit_rolls(pool.get("rolls", 1))
                for entry in pool.get("entries", []):
                    item = entry.get("name", "")
                    for function in entry.get("functions", []):
                        if function.get("function", "").split(":")[-1] != "set_count":
                            continue
                        maximum = 3
                        if item in ("minecraft:cod", "minecraft:salmon"):
                            maximum = 2
                        if item in ("minecraft:gold_block", "minecraft:iron_block"):
                            maximum = 1
                        function["count"] = clamp_number_provider(function.get("count", 1), maximum)
            relative = Path(name).relative_to("data")
            target = DATA / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(table, indent=2) + "\n", encoding="utf-8")
            changed.append(name)
    return changed


def main():
    structure_changes = []
    removed = Counter()
    for jar in (BETTER_JAR, TOWNS_JAR):
        changes, counts = balance_structures(jar)
        structure_changes.extend(changes)
        removed.update(counts)
    tables = balance_loot_tables()
    print(f"Balanced {len(structure_changes)} structure templates and {len(tables)} loot tables.")
    print(f"Removed loot assignments from {sum(removed.values())} excess containers.")
    for name, before, after in structure_changes:
        print(f"  {before:2} -> {after}: {name}")


if __name__ == "__main__":
    main()
