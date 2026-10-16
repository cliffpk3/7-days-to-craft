# Padrão visual dos ícones de itens do 7DTC

Padrão aprovado em 16/10/2026 a partir dos ícones NVG Battery, Basic/Advanced Turret Ammo Box e Grenade Canister.

- Desenhar o arquivo final diretamente em **16×16 pixels**, com transparência binária e sem suavização.
- Manter uma silhueta que identifique o objeto na escala do inventário. Mostrar só os detalhes essenciais, como alça, fecho, compartimentos ou carga.
- Iluminar pelo canto superior esquerdo: plano superior e borda superior mais claros; lateral direita e base mais escuras.
- Usar uma paleta coesa. A borda normalmente precisa de apenas dois ou três tons da mesma família, sem variações aleatórias.
- Para variantes do mesmo item, reutilizar a mesma matriz de pixels e alterar somente a paleta, salvo quando a função do item exigir outra forma.
- Ampliar com nearest-neighbor apenas para revisão visual. A textura instalada permanece em 16×16.
- Preferir substituições em `kubejs/assets/<modid>/` e validar o modelo que aponta para cada textura.

Fontes de referência e scripts editáveis: `C:\Users\carli\Documents\Codex\2026-09-20\e\outputs\item-icons`.
