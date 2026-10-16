"""Generate the 36 blueprint models, labels and CGM recipes from Divine's recipes."""
from collections import Counter
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source/seven-days-locks/src/main/resources/assets/seven_days_locks"
JAR = ROOT / "mods/Divine_Weaponry_v1.7_1.20.1.jar"
TYPES = ("battleaxe", "cutlass", "dagger", "glaive", "hammer", "katana",
         "longsword", "rapier", "scythe", "shuriken", "sickle", "spear")
MATERIALS = ("iron", "diamond", "netherite")

lines = ["// Blueprint-gated Divine Weaponry recipes, based on the mod's native material counts.",
         "ServerEvents.recipes(event => {", "  event.remove({ mod: 'divine_weaponry' })"]
language = SOURCE / "lang/en_us.json"
labels = json.loads(language.read_text(encoding="utf-8"))
with ZipFile(JAR) as archive:
    for material in MATERIALS:
        for kind in TYPES:
            name = f"{material}_{kind}"
            target = f"divine_weaponry:{name}"
            blueprint = f"seven_days_locks:blueprint_{name}"
            label = f"{material} {kind}".replace("_", " ").title()
            labels[f"item.seven_days_locks.blueprint_{name}"] = f"{label} Blueprint"
            model = SOURCE / f"models/item/blueprint_{name}.json"
            model.parent.mkdir(parents=True, exist_ok=True)
            model.write_text(json.dumps({"parent": "minecraft:item/generated", "textures":
                {"layer0": "create:item/crafting_blueprint"}}, separators=(",", ":")) + "\n", encoding="utf-8")

            path = f"data/divine_weaponry/recipes/{name}_craft.json"
            native = json.loads(archive.read(path))
            materials = [{"item": blueprint, "count": 1}]
            if native["type"] == "minecraft:smithing_transform":
                materials += [{"item": native["base"]["item"], "count": 1},
                              {"item": native["addition"]["item"], "count": 1}]
            else:
                counts = Counter()
                for row in native["pattern"]:
                    for symbol in row:
                        if symbol != " ":
                            ingredient = native["key"][symbol]
                            counts[("item" if "item" in ingredient else "tag",
                                    ingredient.get("item", ingredient.get("tag")))] += 1
                materials += [{field: value, "count": count} for (field, value), count in counts.items()]
            recipe = {"type": "cgm:workbench", "materials": materials, "result": {"item": target}}
            lines.append(f"  event.custom({json.dumps(recipe, separators=(',', ':'))}).id('seven_days_to_craft:cgm/divine/{name}')")
lines.append("})")
(ROOT / "kubejs/server_scripts/divine_weaponry_workbench.js").write_text("\n".join(lines) + "\n", encoding="utf-8")
labels["tooltip.seven_days_locks.blueprint_use"] = "Allows the crafting of %s %s time."
labels["tooltip.seven_days_locks.blueprint_uses"] = "Allows the crafting of %s %s times."
language.write_text(json.dumps(labels, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
