// 7DTC (14/11/2026): retired night vision gear (see server_scripts/7dtc_darkzone_night_vision.js).
JEIEvents.hideItems(event => {
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
    "fracturepoint:nvg_battery",
    "zombie_extreme:night_vision_glasses_helmet",
    "zombie_extreme:pill_night_vision"
  ].forEach(item => event.hide(item))
})
