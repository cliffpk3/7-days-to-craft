ItemEvents.tooltip(event => {
  event.add('kubejs:basic_ammo_box', Text.gray('Right-click: 128-256 CGM Basic Bullets'))
  event.add('kubejs:advanced_ammo_box', Text.gray('Right-click: 128-256 CGM Advanced Bullets'))

  // Match the actual reload bridge, not the original TaCZ caliber label.
  event.addAdvanced('tacz:modern_kinetic_gun', (item, advanced, text) => {
    const gunId = String(item.nbt?.GunId || '')
    const basic = ['glock_17', 'm1911', 'p320', 'hk_mp5a5', 'uzi']
    const shells = ['db_short', 'm870', 'm1014']
    const name = gunId.split(':').pop()
    if (basic.includes(name)) text.add(1, Text.green('Uses CGM Basic Bullets'))
    else if (shells.includes(name)) text.add(1, Text.aqua('Uses CGM Shells'))
    else if (['sks_tactical', 'm4a1', 'ak47', 'scar_l', 'm700', 'm249',
      'deagle', 'hk416d', 'rpk', 'm107'].includes(name))
      text.add(1, Text.gold('Uses CGM Advanced Bullets'))
  })
})
