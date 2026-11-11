// 9/11/2026 (docs/7DTC-ANALISE-LOOT-20261109.md, lote 2): 6 Blueprint Scraps -> 1 blueprint.
// The table 7dtc:gameplay/blueprint_cache picks level 3 groups once any workbench reached level 3.
const FRAGMENT_COST = 6
ItemEvents.rightClicked('scguns:blueprint_scrap', event => {
  const player = event.player
  const stack = event.item
  if (stack.count < FRAGMENT_COST && !player.creativeMode) {
    player.setStatusMessage(Text.yellow(`Need ${FRAGMENT_COST} Blueprint Scraps to assemble a blueprint`))
    return
  }
  if (!player.creativeMode) stack.shrink(FRAGMENT_COST)
  event.server.runCommandSilent(`execute as ${player.username} at @s run loot give @s loot 7dtc:gameplay/blueprint_cache`)
  player.setStatusMessage(Text.green('Blueprint assembled from scraps'))
  event.cancel()
})
