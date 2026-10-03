// Central mob balance bridge for 7 Days to Craft.
// Editable values live in kubejs/config/7dtc_mob_registry.json.

const $ForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
const $Attributes = Java.loadClass('net.minecraft.world.entity.ai.attributes.Attributes')
const $LivingEntity = Java.loadClass('net.minecraft.world.entity.LivingEntity')

const MOB_REGISTRY_PATH = 'kubejs/config/7dtc_mob_registry.json'
const MOB_REGISTRY = JsonIO.read(MOB_REGISTRY_PATH)

function registryValue(map, key) {
  if (map == null) return null
  if (typeof map.get === 'function') {
    const value = map.get(key)
    return value === undefined ? null : value
  }
  const value = map[key]
  return value === undefined ? null : value
}

function mobId(entity) {
  const key = $ForgeRegistries.ENTITY_TYPES.getKey(entity.type)
  return key == null ? '' : String(key)
}

function resourceId(value) {
  if (value == null) return ''
  const text = String(value)
  const matches = text.match(/[a-z0-9_.-]+:[a-z0-9_./-]+/g)
  return matches == null || matches.length === 0 ? text : matches[matches.length - 1]
}

function listContains(list, value) {
  if (list == null) return false
  const size = typeof list.size === 'function' ? list.size() : list.length
  for (let index = 0; index < size; index++) {
    const entry = typeof list.get === 'function' ? list.get(index) : list[index]
    if (String(entry) === value) return true
  }
  return false
}

function listHasEntries(list) {
  if (list == null) return false
  return typeof list.size === 'function' ? list.size() > 0 : list.length > 0
}

function finiteNumber(value) {
  if (value == null || value === '') return null
  const parsed = Number(value)
  return isFinite(parsed) ? parsed : null
}

function mobProfile(entity) {
  if (MOB_REGISTRY == null || registryValue(registryValue(MOB_REGISTRY, 'settings'), 'enabled') === false) return null
  return registryValue(registryValue(MOB_REGISTRY, 'mobs'), mobId(entity))
}

function rejectSpawn(event, id, reason) {
  const settings = registryValue(MOB_REGISTRY, 'settings')
  if (registryValue(settings, 'logRejectedSpawns') === true) {
    console.info(`[7DTC Mob Registry] Spawn de ${id} bloqueado: ${reason}`)
  }
  event.cancel()
}

EntityEvents.checkSpawn(event => {
  const profile = mobProfile(event.entity)
  if (profile == null) return

  const spawn = registryValue(profile, 'spawn')
  if (spawn == null) return
  const id = mobId(event.entity)

  if (registryValue(spawn, 'allow') === false) {
    rejectSpawn(event, id, 'allow=false')
    return
  }

  const minY = registryValue(spawn, 'minY')
  const maxY = registryValue(spawn, 'maxY')
  const parsedMinY = finiteNumber(minY)
  const parsedMaxY = finiteNumber(maxY)
  if (parsedMinY != null && event.y < parsedMinY) {
    rejectSpawn(event, id, `Y ${event.y} abaixo de ${minY}`)
    return
  }
  if (parsedMaxY != null && event.y > parsedMaxY) {
    rejectSpawn(event, id, `Y ${event.y} acima de ${maxY}`)
    return
  }

  const dimensions = registryValue(spawn, 'dimensions')
  if (listHasEntries(dimensions)) {
    const dimension = resourceId(event.level.dimension)
    if (!listContains(dimensions, dimension)) {
      rejectSpawn(event, id, `dimensao ${dimension}`)
      return
    }
  }

  const biomes = registryValue(spawn, 'biomes')
  if (listHasEntries(biomes)) {
    const biome = resourceId(event.block.biomeId)
    if (!listContains(biomes, biome)) {
      rejectSpawn(event, id, `bioma ${biome}`)
      return
    }
  }

  const chanceValue = registryValue(spawn, 'chance')
  const parsedChance = finiteNumber(chanceValue)
  const chance = parsedChance == null ? 1.0 : Math.max(0.0, Math.min(1.0, parsedChance))
  if (chance < 1.0 && event.level.random.nextDouble() >= chance) {
    rejectSpawn(event, id, `chance ${chance}`)
  }
})

const ATTRIBUTE_KEYS = {
  maxHealth: $Attributes.MAX_HEALTH,
  attackDamage: $Attributes.ATTACK_DAMAGE,
  armor: $Attributes.ARMOR,
  armorToughness: $Attributes.ARMOR_TOUGHNESS,
  movementSpeed: $Attributes.MOVEMENT_SPEED,
  followRange: $Attributes.FOLLOW_RANGE,
  knockbackResistance: $Attributes.KNOCKBACK_RESISTANCE
}

EntityEvents.spawned(event => {
  const entity = event.entity
  if (!(entity instanceof $LivingEntity)) return

  const profile = mobProfile(entity)
  if (profile == null) return
  const configured = registryValue(profile, 'attributes')
  if (configured == null) return

  const oldMaxHealth = entity.maxHealth
  let maxHealthChanged = false

  Object.keys(ATTRIBUTE_KEYS).forEach(key => {
    const value = registryValue(configured, key)
    if (value == null) return
    const parsed = finiteNumber(value)
    if (parsed == null) return
    const instance = entity.getAttribute(ATTRIBUTE_KEYS[key])
    if (instance == null) return
    instance.setBaseValue(parsed)
    if (key === 'maxHealth') maxHealthChanged = true
  })

  if (maxHealthChanged && entity.health >= oldMaxHealth * 0.99) {
    entity.health = entity.maxHealth
  }
})

console.info(`[7DTC Mob Registry] Registro central carregado de ${MOB_REGISTRY_PATH}`)
