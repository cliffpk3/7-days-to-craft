"""One-time migration of the old Milestone Training chapter into skill chapters.

The original is kept as a .backup file so FTB Quests will not load it.
Quest/task/reward IDs are deliberately preserved for existing worlds.
"""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHAPTERS = ROOT / "config/ftbquests/quests/chapters"
SOURCE = CHAPTERS / "skill_training.snbt"
BACKUP = CHAPTERS / "skill_training.snbt.backup-split-20261015"
DEFINITIONS = ROOT / "kubejs/data/seven_days_to_craft/puffish_skills/categories/survivor/definitions.json"
BLOCK = re.compile(r"(?ms)^\t\t\{\r?\n.*?^\t\t\}")
ROOT_DEP = re.compile(r'dependencies: \["(7D7C3600000000[0-9A-F]{2})"\]')

# IDs are from the unlock chapter. None of the four archived skills are in
# the current Puffish tree; their quests remain loadable but invisible.
SKILLS = {
    0x01: "mobility_2_advancedparkour",
    0x02: "mobility_2_grapplerlicense",
    0x03: "scavenging_2_backpacktraining",
    0x04: "scavenging_2_cartographyminimap",
    0x05: "scavenging_2_oresense",
    0x06: "scavenging_2_lumberjack",
    0x07: "scavenging_2_keyhunter",
    0x08: "scavenging_2_salvager",
    0x0C: "engineering_2_mechanicalfoundations",
    0x0E: "engineering_2_advancedmanufacturing",
    0x0F: "engineering_2_sentryengineering",
    0x10: "engineering_2_turretengineering",
    0x11: "mobility_3_emergencymobility",
    0x12: "mobility_3_breakfallreflex",
    0x13: "mobility_3_endertraversal",
    0x14: "combat_3_paintolerance",
    0x15: "combat_3_secondwind",
    0x16: "combat_3_enduringvitality",
    0x17: "scavenging_3_keyconservation",
    0x18: "scavenging_3_darkadaptation",
    0x19: "scavenging_3_mastersalvager",
    0x1A: "engineering_3_createautomation",
    0x1B: "engineering_3_fortressconstruction",
    0x1C: "engineering_3_automateddefense",
    0x1F: "mobility_2_lowprofile",
    0x20: "mobility_2_reservejump",
    0x21: "mobility_2_beastrider",
    0x23: "combat_2_threatsense",
    0x24: "combat_2_ranger",
    0x25: "mobility_2_evasiverush",
    0x26: "combat_2_extendedreach",
}
ARCHIVED = {0x09: "Gunsmithing", 0x0A: "Ammunition I", 0x0B: "Ammunition II", 0x0D: "Mechanical Processing"}


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit("Already split: the combined chapter is no longer active")
    if not BACKUP.exists():
        BACKUP.write_bytes(SOURCE.read_bytes())
    source = BACKUP.read_text(encoding="utf-8")
    newline = "\r\n" if "\r\n" in source else "\n"
    definitions = json.loads(DEFINITIONS.read_text(encoding="utf-8"))
    groups = {}
    current = None
    for match in BLOCK.finditer(source):
        quest = match.group()
        dep = ROOT_DEP.search(quest)
        if dep:
            current = int(dep.group(1)[-2:], 16)
            if current in groups:
                raise SystemExit(f"Duplicate skill group: {current:02X}")
            groups[current] = []
        if current is None:
            raise SystemExit("Quest before first skill group")
        # Coordinates are local to each new chapter; retain every other field.
        quest = re.sub(r'(?m)^\t\t\tx: [^\r\n]+',
                       f'\t\t\tx: {len(groups[current]) * 1.8:.1f}d', quest)
        quest = re.sub(r'(?m)^\t\t\ty: [^\r\n]+', '\t\t\ty: 0.0d', quest)
        groups[current].append(quest)

    if set(groups) != set(SKILLS) | set(ARCHIVED) or sum(map(len, groups.values())) != 61:
        raise SystemExit("Skill group or quest count differs from the reviewed source")

    for number, quests in groups.items():
        key = SKILLS.get(number)
        legacy = key is None
        title = ARCHIVED[number] if legacy else definitions[key]["title"]
        if not isinstance(title, str):
            raise SystemExit(f"Missing skill title for {key}")
        slug = key.split("_", 2)[-1] if key else f"legacy_{number:02x}"
        filename = f"training_{slug}"
        icon_data = definitions[key].get("icon", {}) if key else {}
        item = icon_data.get("data", {}).get("item") if icon_data.get("type") == "item" else None
        fields = [
            "{",
            f'\tfilename: "{filename}"',
            '\tgroup: "7D7C000000000001"',
            f'\tid: "7D7C3A00000000{number:02X}"',
            f"\torder_index: {10 + number}",
            "\tdefault_hide_dependency_lines: false",
            "\tdefault_quest_shape: \"\"",
            "\thide_quest_until_deps_visible: true",
        ]
        if legacy:
            fields.append("\talways_invisible: true")
        if item:
            fields.append(f'\ticon: "{item}"')
        fields.extend(["\tquest_links: [ ]", "\tquests: [", *quests, "\t]", f'\ttitle: "{title}"', "}"])
        (CHAPTERS / f"{filename}.snbt").write_bytes(newline.join(fields).encode("utf-8"))

    SOURCE.unlink(missing_ok=True)
    print(f"Created {len(groups)} separate chapters with {sum(map(len, groups.values()))} unchanged quest IDs")


if __name__ == "__main__":
    main()
