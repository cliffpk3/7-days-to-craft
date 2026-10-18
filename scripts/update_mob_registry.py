"""Review newly installed hostile mobs from /7dtc_mob_audit.

Run without --apply to generate a report. --apply adds missing entries only;
existing manual balance and spawn rules are never overwritten.
"""

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ATTRIBUTES = (
    "maxHealth", "attackDamage", "armor", "armorToughness", "movementSpeed",
    "followRange", "knockbackResistance",
)
CATEGORIES = {1: "common", 2: "dangerous", 3: "elite", 4: "boss"}
ABILITY_WORDS = ("spell", "summon", "explode", "teleport", "ranged", "projectile", "shoot")


def estimate(row):
    hp = float(row.get("maxHealth") or 0)
    damage = float(row.get("attackDamage") or 0)
    armor = float(row.get("armor") or 0)
    speed = float(row.get("movementSpeed") or 0)
    goals = [str(goal).lower() for goal in row.get("goalClasses", [])]
    special = sorted({word for word in ABILITY_WORDS if any(word in goal for goal in goals)})
    if hp >= 100 or damage >= 15:
        tier = 4
    elif hp >= 50 or damage >= 10:
        tier = 3
    elif hp >= 30 or damage >= 6 or armor >= 8:
        tier = 2
    else:
        tier = 1
    if special or speed >= 0.35:
        tier = min(4, tier + 1)
    return tier, special


def run(audit_path, registry_path, report_path, apply):
    audit = json.loads(audit_path.read_text(encoding="utf-8"))
    registry = json.loads(registry_path.read_text(encoding="utf-8"))
    if audit.get("schemaVersion") != 1 or not isinstance(audit.get("mobs"), dict):
        raise ValueError("Unsupported or incomplete runtime audit")
    mobs = registry["mobs"]
    report = {"schemaVersion": 1, "audited": len(audit["mobs"]), "missing": {},
              "existing": len(set(mobs) & set(audit["mobs"])), "failures": audit.get("failures", [])}
    for mob_id, row in sorted(audit["mobs"].items()):
        if mob_id in mobs:
            continue
        tier, abilities = estimate(row)
        report["missing"][mob_id] = {
            "suggestedTier": tier,
            "observed": {key: row.get(key) for key in ATTRIBUTES},
            "possibleAbilities": abilities,
            "needsReview": bool(abilities) or tier >= 3,
        }
        if apply:
            mobs[mob_id] = {
                "name": row.get("name", mob_id),
                "source": mob_id.split(":", 1)[0],
                "attributes": {key: None for key in ATTRIBUTES},
                "spawn": {"allow": True, "minY": None, "maxY": None,
                          "dimensions": [], "biomes": [], "chance": 1.0},
                "threat": {"tier": tier, "category": CATEGORIES[tier]},
                "_audit": {"estimateOnly": True, "needsReview": bool(abilities) or tier >= 3,
                           "possibleAbilities": abilities, "observed": report["missing"][mob_id]["observed"]},
            }
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if apply and report["missing"]:
        backup = registry_path.with_suffix(registry_path.suffix + ".pre-audit")
        if backup.exists():
            raise FileExistsError(f"Backup already exists: {backup}")
        backup.write_bytes(registry_path.read_bytes())
        registry_path.write_text(json.dumps(registry, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Audited {report['audited']} hostile mobs; {len(report['missing'])} missing; "
          f"{len(report['failures'])} inspection failures. Report: {report_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--audit", type=Path, default=ROOT / "staging/mob-audit-runtime.json")
    parser.add_argument("--registry", type=Path, default=ROOT / "kubejs/config/7dtc_mob_registry.json")
    parser.add_argument("--report", type=Path, default=ROOT / "staging/mob-audit-suggestions.json")
    parser.add_argument("--apply", action="store_true", help="Add missing entries after saving a backup")
    args = parser.parse_args()
    run(args.audit, args.registry, args.report, args.apply)


if __name__ == "__main__":
    main()
