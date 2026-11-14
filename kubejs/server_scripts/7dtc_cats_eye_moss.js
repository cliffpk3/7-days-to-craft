// 13/11/2026 (user): Night Vision comes from the Dark Zone. 6 Cat's Eye Moss + an Awkward Potion = Potion of Night Vision.
// Golden carrots no longer brew it (seven_days_darkzone PotionBrewingMixin).
ServerEvents.recipes(event => {
  event.shapeless(Item.of('minecraft:potion', '{Potion:"minecraft:night_vision"}'), [
    '6x seven_days_darkzone:7dtc_cats_eye_moss',
    Item.of('minecraft:potion', '{Potion:"minecraft:awkward"}').strongNBT()
  ]).id('7dtc:night_vision_from_cats_eye_moss')
})
