---
title: Weltraumnahrung
title_en: Space Food
order: 70
milestone: 5
requires: ["infinite_horizon:food_packer"]
icon: infinite_horizon:space_ration
items: ["infinite_horizon:food_packer", "infinite_horizon:space_ration"]
summary: Nahrungspacker und Weltraumration
---
Auf langen Reisen ist Platz im Inventar knapp. Der <item:infinite_horizon:food_packer> presst {{value:agriculture:infinite_horizon:space_ration.inputs}} beliebige Nahrungsmittel zu einer <item:infinite_horizon:space_ration>. Die Ration sättigt so viel wie alle Zutaten zusammen, höchstens {{value:agriculture:infinite_horizon:space_ration.nutrition_max}} Hungerpunkte und {{value:agriculture:infinite_horizon:space_ration.saturation_max}} Sättigung. Rationen stapeln sich bis {{value:agriculture:infinite_horizon:space_ration.max_stack}}.

Den Nahrungspacker schaltet der Forschungsknoten „Raumfahrtnahrung“ frei, siehe [[forschung|Forschung]].

@machine infinite_horizon:food_packer

## So geht's

1. Stell den Nahrungspacker auf und schließ ihn an Strom an.
2. Leg in jeden Eingabeslot ein Nahrungsmittel. Es dürfen verschiedene sein.
3. Nach einem Vorgang liegt die Ration im Ausgabeslot. Der Tooltip zeigt, wie viel sie sättigt.

@recipes_for infinite_horizon:food_packer

@tip Nimm sättigende Zutaten wie Brot, gebackene Kartoffeln oder Steak. Daraus wird eine Ration, die den Hunger fast ganz stillt.
