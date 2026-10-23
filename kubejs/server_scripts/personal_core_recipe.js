// The visual block is supplied by Functional Statues. The Core mod binds the
// first personally crafted copy to the crafter's UUID on the server.
ServerEvents.recipes(event => {
  event.remove({ id: 'golemstatue:shielded_knight_statue' })
  event.shaped('golemstatue:shielded_knight_statue', [
    ' S ',
    'ICI',
    ' R '
  ], {
    S: 'minecraft:iron_sword',
    I: 'minecraft:iron_ingot',
    C: 'minecraft:copper_block',
    R: 'minecraft:rotten_flesh'
  }).id('golemstatue:shielded_knight_statue')
})
