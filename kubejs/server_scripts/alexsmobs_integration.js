ServerEvents.recipes(event => {
  ;['alexsmobs:blood_sprayer','alexsmobs:hemolymph_blaster','alexsmobs:stink_ray','alexsmobs:unsettling_kimono','alexsmobs:sculk_boomer'].forEach(item => event.remove({ output: item }))
})
