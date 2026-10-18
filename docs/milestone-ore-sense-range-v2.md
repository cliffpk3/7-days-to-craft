# Ore Sense: luz posicional em chunks (v2)

Em 18/10/2026, o teste da v1 revelou que minerios proximos tambem deixaram de brilhar. Forge usa uma consulta posicional em `BlockState` durante a varredura de emissores nos chunks. A v2 faz essa consulta usar o mesmo raio de 20 blocos do motor de luz. A varredura inicial da paleta recebe `BlockPos.ZERO`; nessa etapa o minerio e marcado como possivel emissor, e cada posicao real e filtrada depois.

- JAR instalado: `mods/seven-days-skill-bridge-1.0.1.jar`.
- Backup da v1: `backups/ore-sense-range-v2-20261018/seven-days-skill-bridge-1.0.1.jar`.
- Backup anterior a v1: `backups/ore-sense-range-v1-20261018/seven-days-skill-bridge-1.0.1.jar`.
- Compilacao offline com JDK 17: passou; hash SHA-256 do JAR instalado `22095119CEFB90453FBCFD404EB1A7ABE8483304F1EB7BE760D57DB24DF5B8E1`.

Pendente: repetir no jogo o teste visual com minérios a 19/20/21 blocos, skill ligada e desligada, movimento para dentro e fora do raio e shaders ativos. Nao considerar concluido antes dessa confirmacao.
