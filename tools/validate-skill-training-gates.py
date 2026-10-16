"""Validate the one-skill-per-chapter FTB training layout and gating."""

import json
import re
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHAPTERS = ROOT / "config/ftbquests/quests/chapters"
DEFINITIONS = ROOT / "kubejs/data/seven_days_to_craft/puffish_skills/categories/survivor/definitions.json"
MAPPING = runpy.run_path(str(ROOT / "tools/split-skill-training-chapters.py"))
SKILLS, ARCHIVED = MAPPING["SKILLS"], MAPPING["ARCHIVED"]
BLOCK = re.compile(r"(?ms)^\t\t\{\r?\n.*?^\t\t\}")
ID = re.compile(r'(?m)^\t\t\tid: "([0-9A-F]{16})"')
DEP = re.compile(r'(?m)^\t\t\tdependencies: \["([0-9A-F]{16})"\]')


def main() -> None:
    for inactive in ("skill_training.snbt", "alexs_mobs_field_notes.snbt",
                     "alexs_mobs_field_notes_unlocks.snbt"):
        if (CHAPTERS / inactive).exists():
            raise SystemExit(f"Old combined chapter is still active: {inactive}")

    definitions = json.loads(DEFINITIONS.read_text(encoding="utf-8"))
    seen_ids = set()
    count = 0
    chapters = sorted(CHAPTERS.glob("training_*.snbt"))
    if len(chapters) != len(SKILLS) + len(ARCHIVED):
        raise SystemExit(f"Expected {len(SKILLS) + len(ARCHIVED)} skill chapters, got {len(chapters)}")
    for path in chapters:
        text = path.read_text(encoding="utf-8")
        chapter_id = re.search(r'(?m)^\tid: "(7D7C3A00000000[0-9A-F]{2})"', text)
        if not chapter_id:
            raise SystemExit(f"Missing chapter ID: {path.name}")
        number = int(chapter_id.group(1)[-2:], 16)
        key = SKILLS.get(number)
        expected_title = definitions[key]["title"] if key else ARCHIVED.get(number)
        if expected_title is None or f'\ttitle: "{expected_title}"' not in text:
            raise SystemExit(f"Chapter title differs from the skill: {path.name}")
        if f'\tfilename: "{path.stem}"' not in text:
            raise SystemExit(f"Filename mismatch: {path.name}")
        if "\thide_quest_until_deps_visible: true" not in text:
            raise SystemExit(f"Skill gate missing: {path.name}")
        if key is None and "\talways_invisible: true" not in text:
            raise SystemExit(f"Retired skill chapter is visible: {path.name}")
        if key and "\talways_invisible: true" in text:
            raise SystemExit(f"Current skill chapter is hidden: {path.name}")

        previous = f"7D7C3600000000{number:02X}"
        quests = BLOCK.findall(text)
        if not quests:
            raise SystemExit(f"Empty skill chapter: {path.name}")
        for quest in quests:
            quest_id = ID.search(quest)
            dep = DEP.search(quest)
            if not quest_id or not dep or dep.group(1) != previous:
                raise SystemExit(f"Broken training sequence: {path.name}")
            if quest_id.group(1) in seen_ids:
                raise SystemExit(f"Duplicate quest ID: {quest_id.group(1)}")
            seen_ids.add(quest_id.group(1))
            previous = quest_id.group(1)
            count += 1
    if count != 61:
        raise SystemExit(f"Expected 61 training quests, got {count}")
    print(f"Verified {len(chapters)} independent chapters, {count} preserved quests and skill gates")


if __name__ == "__main__":
    main()
