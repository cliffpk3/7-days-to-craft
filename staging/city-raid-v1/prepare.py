"""Generate inactive 7DTC city-raid datapack candidates from inspected mod JARs.

The output stays below this staging directory. Nothing is copied into kubejs or mods.
"""

import argparse
import json
import pathlib
import zipfile


HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE / "candidate-data"


def write_json(relative, value):
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("zombie_extreme_jar", type=pathlib.Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.zombie_extreme_jar) as jar:
        modifiers = sorted(
            name for name in jar.namelist()
            if name.startswith("data/zombie_extreme/forge/biome_modifier/") and name.endswith(".json")
        )
        structures = sorted(
            name for name in jar.namelist()
            if name.startswith("data/zombie_extreme/worldgen/structure/") and name.endswith(".json")
        )
        recipes = sorted(
            name for name in jar.namelist()
            if name.startswith("data/zombie_extreme/recipes/") and name.endswith(".json")
        )
        if (len(modifiers), len(structures), len(recipes)) != (42, 10, 74):
            raise SystemExit("Unexpected Zombie Extreme version or data layout; review before staging")
        for name in modifiers:
            write_json(name.removeprefix("data/"), {"type": "forge:none"})
        for name in structures:
            structure = json.loads(jar.read(name))
            structure["biomes"] = "#seven_days_to_craft:never_generate_zombie_extreme_structures"
            write_json(name.removeprefix("data/"), structure)
        write_json(
            "seven_days_to_craft/tags/worldgen/biome/never_generate_zombie_extreme_structures.json",
            {"replace": False, "values": []},
        )
        write_json("../audit.json", {
            "source_jar": args.zombie_extreme_jar.name,
            "disabled_biome_modifiers": [pathlib.PurePosixPath(p).stem for p in modifiers],
            "disabled_structures": [pathlib.PurePosixPath(p).stem for p in structures],
            "recipes_to_remove_by_namespace": [pathlib.PurePosixPath(p).stem for p in recipes],
        })
    print(f"Staged {len(modifiers)} biome modifiers, {len(structures)} structures, {len(recipes)} recipe IDs")


if __name__ == "__main__":
    main()
