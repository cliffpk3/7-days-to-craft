ServerEvents.recipes(event => {
  ;['alexsmobs:blood_sprayer','alexsmobs:hemolymph_blaster','alexsmobs:stink_ray','alexsmobs:unsettling_kimono','alexsmobs:sculk_boomer'].forEach(item => event.remove({ output: item }))
  const material = (item, count) => ({ item: item, count: count })
  const tagged = (tag, count) => ({ tag: tag, count: count })
  const addGun = (id, result, materials) => event.custom({ type: 'cgm:workbench', materials: materials, result: { item: result } }).id('seven_days_to_craft:cgm/weapons/' + id)
  addGun('blood_sprayer','alexsmobs:blood_sprayer',[material('alexsmobs:blood_sac',4),material('alexsmobs:mosquito_proboscis',1),material('minecraft:nether_brick',8),material('minecraft:iron_ingot',4)])
  addGun('hemolymph_blaster','alexsmobs:hemolymph_blaster',[material('alexsmobs:hemolymph_sac',6),material('alexsmobs:mosquito_proboscis',1),material('alexsmobs:warped_muscle',1),material('alexsmobs:mimicream',2),material('alexsmobs:blood_sprayer',1),tagged('forge:ingots/iron',8)])
  addGun('stink_ray','alexsmobs:stink_ray',[material('alexsmobs:stink_bottle',4),material('minecraft:hopper',1),tagged('forge:ingots/iron',12),material('minecraft:redstone',4)])
})
