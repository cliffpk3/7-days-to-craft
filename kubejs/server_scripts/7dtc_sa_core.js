// 7DTC sa_core (10/11/2026, docs/7DTC-SA-CORE-PROPOSTA.md). O pack ja tem a recicladora do Corail Recycler
// (receitas proprias em config/corail_recycler/user_defined_recipes.json); a do sa_core sai para nao repetir o papel.
ServerEvents.recipes(event => {
  event.remove({ id: 'sa_core:blocks/recycler' })
  // Parte 3 (10/11): multiferramentas so na bancada de utilidades (N2 e N3), sem receita de mesa de trabalho.
  event.remove({ id: 'sa_core:tools/scrap_multitool' })
  event.remove({ id: 'sa_core:tools/ashen_steel_multitool' })
})
