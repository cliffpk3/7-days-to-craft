// Native durability * 1.5 for the Divine Weaponry families used by 7DTC.
// ItemEvents.modification runs before item properties are frozen.
ItemEvents.modification(event => {
  const types = ['battleaxe', 'cutlass', 'dagger', 'glaive', 'hammer', 'katana',
    'longsword', 'rapier', 'scythe', 'shuriken', 'sickle', 'spear']
  for (const material of ['stone', 'iron', 'diamond', 'netherite']) {
    for (const type of types) {
      event.modify(`divine_weaponry:${material}_${type}`, item => {
        item.maxDamage = Math.round(item.maxDamage * 1.5)
      })
    }
  }
  for (const id of ['amaterasu_shuriken', 'apophis_katana', 'cernunnos_sickle',
    'chaos_sword', 'erebus_scythe', 'fujindagger', 'hyperion_longsword',
    'jotunn_battleaxe', 'mjollnir_hammer', 'om_spear', 'sobek_rapier',
    'tunupa_glaive', 'zeus_cutlass']) {
    event.modify(`divine_weaponry:${id}`, item => {
      item.maxDamage = Math.round(item.maxDamage * 1.5)
    })
  }
})
