// 7DTC (14/11/2026): night vision comes only from the Night Vision Goggles (artifact) found in the Dark Zone,
// and from the Potion of Night Vision brewed with Cat's Eye Moss. Other night vision gear loses its recipes;
// seven_days_darkzone also blocks equipment-style night vision (NightVisionGuard). Hunter helmets were made from
// the Survival Instinct night vision helmet, so they go too. Kept: scguns:night_vision_module (Viventrum package
// ingredient); its exosuit night vision is blocked by the guard.
ServerEvents.recipes(event => {
  [
    "survival_instinct:night_vision_goggles_helmet",
    "survival_instinct:black_hunter_helmet",
    "survival_instinct:desert_hunter_helmet",
    "survival_instinct:green_hunter_helmet",
    "fracturepoint:nvg_goggles",
    "fracturepoint:simple_nvg_goggles",
    "fracturepoint:digital_goggles",
    "fracturepoint:thermal_goggles",
    "fracturepoint:thermal_black_goggles",
    "fracturepoint:thermal_white_goggles",
    "fracturepoint:nvg_battery"
  ].forEach(item => event.remove({ output: item }))
})
