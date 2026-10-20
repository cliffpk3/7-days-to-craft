// Each cache supplies 128, 160, 192, 224 or 256 CGM rounds.
ItemEvents.rightClicked(event => {
  const id = event.item.id
  if (id !== 'kubejs:basic_ammo_box' && id !== 'kubejs:advanced_ammo_box') return
  const ammo = id === 'kubejs:basic_ammo_box' ? 'cgm:basic_bullet' : 'cgm:advanced_bullet'
  let remaining = 128 + 32 * Math.floor(Math.random() * 5)
  while (remaining > 0) {
    const count = Math.min(64, remaining)
    event.player.give(Item.of(ammo, count))
    remaining -= count
  }
  if (!event.player.creativeMode) event.item.shrink(1)
})
