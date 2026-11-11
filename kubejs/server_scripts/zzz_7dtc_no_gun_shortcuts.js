// 7DTC (8/11/2026): guns come only from the workbenches, so blueprints and levels cannot be bypassed with Create.
ServerEvents.recipes(event => { event.remove({ id: /^scguns:create\/mechanical_crafting\// }) })
