"""Build the curated TaCZ recipes and blueprint assets for the CGM Workbench."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "tacz/tacz_default_gun/data/tacz/recipes"
RES = ROOT / "source/seven-days-locks/src/main/resources/assets/seven_days_locks"
GUN_TIERS = {
    1: ("glock_17", "m1911", "db_short"),
    2: ("p320", "hk_mp5a5", "uzi", "m870", "sks_tactical"),
    3: ("m4a1", "ak47", "scar_l", "m1014", "m700", "m249"),
    4: ("deagle", "hk416d", "rpk", "m107"),
}
ATTACHMENTS = (
    "scope_retro_2x", "scope_acog_ta31", "scope_standard_8x",
    "muzzle_silencer_wraith", "muzzle_compensator_trident", "muzzle_choke_sg",
    "grip_vertical_military", "extended_mag_1", "sniper_extended_mag_1",
    "shotgun_extended_mag_1", "laser_compact",
)
OLD_CGM_GUNS = (
    "pistol", "machine_pistol", "rifle", "shotgun", "assault_rifle",
    "heavy_rifle", "grenade_launcher", "mini_gun", "bazooka",
)
OLD_CGM_ATTACHMENTS = (
    "light_grip", "light_stock", "long_scope", "medium_scope", "short_scope",
    "silencer", "specialised_grip", "tactical_stock", "weighted_stock",
)


def gun_recipe(name):
    native = json.loads((PACK / "gun" / f"{name}.json").read_text(encoding="utf-8"))
    materials = [{"item": f"seven_days_locks:blueprint_tacz_{name}", "count": 1}]
    for entry in native["materials"]:
        ingredient = entry["item"]
        material = {"count": min(int(entry.get("count", 1)), 64)}
        if "tag" in ingredient:
            material["tag"] = ingredient["tag"]
        else:
            material["item"] = ingredient["item"]
        materials.append(material)
    # Preserve the original recipes but cap huge costs; the rare blueprint is
    # the main access gate. The final anti-materiel rifle still needs netherite.
    fire_mode = "AUTO" if name in ("uzi", "m249") else "SEMI"
    nbt = f'{{GunId:"tacz:{name}",GunFireMode:"{fire_mode}"}}'
    return {"type": "cgm:workbench", "materials": materials,
            "result": {"item": "tacz:modern_kinetic_gun", "nbt": nbt}}


def attachment_recipe(name):
    native = json.loads((PACK / "attachments" / f"{name}.json").read_text(encoding="utf-8"))
    materials = []
    for entry in native["materials"]:
        ingredient = entry["item"]
        material = {"count": min(int(entry.get("count", 1)), 64)}
        material["tag" if "tag" in ingredient else "item"] = ingredient.get("tag", ingredient.get("item"))
        materials.append(material)
    return {"type": "cgm:workbench", "materials": materials,
            "result": {"item": "tacz:attachment", "nbt": f'{{AttachmentId:"tacz:{name}"}}'}}


def main():
    names = [gun for tier in GUN_TIERS.values() for gun in tier]
    lines = ["// Curated TaCZ weapons and attachments; all guns require a matching blueprint.",
             "ServerEvents.recipes(event => {"]
    for old in OLD_CGM_GUNS + OLD_CGM_ATTACHMENTS:
        lines.append(f"  event.remove({{ id: 'cgm:{old}' }})")
    for gun in names:
        recipe = json.dumps(gun_recipe(gun), separators=(",", ":"))
        lines.append(f"  event.custom({recipe}).id('seven_days_to_craft:cgm/weapons/{gun}')")
    for attachment in ATTACHMENTS:
        recipe = json.dumps(attachment_recipe(attachment), separators=(",", ":"))
        lines.append(f"  event.custom({recipe}).id('seven_days_to_craft:cgm/attachments/{attachment}')")
    lines.append("})")
    (ROOT / "kubejs/server_scripts/gun_blueprints.js").write_text("\n".join(lines) + "\n", encoding="utf-8")

    model_dir = RES / "models/item"
    model_dir.mkdir(parents=True, exist_ok=True)
    for gun in names:
        (model_dir / f"blueprint_tacz_{gun}.json").write_text(
            '{"parent":"minecraft:item/generated","textures":{"layer0":"create:item/crafting_blueprint"}}\n',
            encoding="utf-8")
    for language in ("en_us", "pt_br"):
        path = RES / "lang" / f"{language}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for gun in names:
            label = gun.replace("_", " ").upper() if gun in ("m4a1", "m1911", "m1014", "m107", "m249", "m700", "m870", "p320", "rpk", "uzi", "ak47", "hk416d") else gun.replace("_", " ").title()
            data[f"item.seven_days_locks.blueprint_tacz_{gun}"] = (
                f"Blueprint: {label}" if language == "en_us" else f"Projeto: {label}")
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
