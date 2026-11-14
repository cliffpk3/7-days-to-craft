// 7DTC (12/11/2026): one empty bottle in the pack (sa_core:empty_glass_bottle); the SI bottle converts in the inventory.
JEIEvents.hideItems(event => event.hide('survival_instinct:empty_bottle'))
// 12/11/2026: one steel (Steellium); the Zombie Extreme steel ingot converts to raw steel.
JEIEvents.hideItems(event => event.hide('zombie_extreme:ignotsteel'))
