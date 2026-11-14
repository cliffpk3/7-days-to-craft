# 7DTC (12/11/2026): pre-generate the template world with time and spawns frozen. Run as the player: /function 7dtc:chunky/pregen
# Chunky commands cannot live in a function (Chunky fails while the function is parsed, so the whole file is dropped).
# They are offered as clickable chat buttons instead.
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule doPatrolSpawning false
gamerule doTraderSpawning false
gamerule doInsomnia false
gamerule randomTickSpeed 0
time set 0
weather clear
gamemode spectator @s
tellraw @s {"text":"7DTC: tempo e spawns pausados. Clique na ordem:","color":"yellow"}
tellraw @s ["",{"text":"[1. Centro no spawn] ","color":"aqua","clickEvent":{"action":"run_command","value":"/chunky spawn"}},{"text":"[2. Raio 1000] ","color":"aqua","clickEvent":{"action":"run_command","value":"/chunky radius 1000"}},{"text":"[3. Iniciar]","color":"green","clickEvent":{"action":"run_command","value":"/chunky start"}}]
tellraw @s {"text":"Ao terminar: /function 7dtc:chunky/done","color":"yellow"}
