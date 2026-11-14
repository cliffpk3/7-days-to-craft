# 7DTC (12/11/2026): after Chunky finishes, put the world back to day 1 morning with normal rules.
gamerule doDaylightCycle true
gamerule doWeatherCycle true
gamerule doMobSpawning true
gamerule doPatrolSpawning true
gamerule doTraderSpawning true
gamerule doInsomnia true
gamerule randomTickSpeed 3
time set 0
weather clear
gamemode survival @s
tellraw @s {"text":"7DTC: regras restauradas, dia 1 de manha. Pode salvar o mundo modelo.","color":"green"}
