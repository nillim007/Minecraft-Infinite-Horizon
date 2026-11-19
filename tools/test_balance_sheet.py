"""Tests der Regeln von tools/balance_sheet.py (Kapitel 27, Abnahme M9): python -m unittest discover -s tools -p "test_*.py"."""
import sys
import unittest
from fractions import Fraction
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import balance_sheet as bs  # noqa: E402


def recipe(rid, doc):
    ins, outs = bs.parse_recipe(doc)
    return rid, ins, outs


def assembly(result, count, *ingredients, mold=None, fluid=None, byproducts=()):
    doc = {"type": "infinite_horizon:assembly",
           "ingredients": [{"ingredient": i, "count": n} for i, n in ingredients],
           "results": [{"id": result, "count": count, "chance": 1.0}] + [{"id": b, "count": 1} for b in byproducts]}
    if mold:
        doc["mold"] = mold
    if fluid:
        doc["fluid"] = {"fluid": fluid[0], "amount": fluid[1]}
    return doc


class BalanceSheetTest(unittest.TestCase):

    def test_divides_by_output_and_ignores_byproducts_and_molds(self):
        recipes = [recipe("ih:crushing_dust", assembly("ih:dust", 2, ("ih:raw", 1), byproducts=("ih:gravel",))),
                   recipe("ih:pressing_plate", assembly("ih:plate", 1, ("ih:dust", 1), mold="ih:mold_plate"))]
        sheet = bs.Sheet(recipes, {})
        bill = sheet.bill([(bs.ITEM, "ih:plate", 4)])
        self.assertEqual({(bs.ITEM, "ih:raw"): Fraction(2)}, bill)

    def test_first_recipe_alphabetically_wins(self):
        recipes = sorted([recipe("ih:b_from_iron", assembly("ih:gear", 1, ("ih:iron", 3))),
                          recipe("ih:a_from_copper", assembly("ih:gear", 1, ("ih:copper", 2)))], key=lambda r: r[0])
        bill = bs.Sheet(recipes, {}).bill([(bs.ITEM, "ih:gear", 1)])
        self.assertEqual({(bs.ITEM, "ih:copper"): Fraction(2)}, bill)

    def test_fluids_sum_in_mb(self):
        electrolysis = {"type": "infinite_horizon:electrolysis", "ingredients": [], "results": [],
                        "fluid": {"fluid": "minecraft:water", "amount": 200},
                        "fluid_results": [{"fluid": "ih:hydrogen", "amount": 200}, {"fluid": "ih:oxygen", "amount": 100}]}
        sheet = bs.Sheet([recipe("ih:electrolysis", electrolysis),
                          recipe("ih:tank", assembly("ih:tank", 1, ("ih:plate", 1), fluid=("ih:oxygen", 500)))], {})
        bill = sheet.bill([(bs.ITEM, "ih:tank", 2), (bs.FLUID, "ih:hydrogen", 100)])
        self.assertEqual(Fraction(2100), bill[(bs.FLUID, "minecraft:water")])
        self.assertEqual(Fraction(2), bill[(bs.ITEM, "ih:plate")])

    def test_tag_counts_first_element_and_shaped_crafting(self):
        shaped = {"type": "minecraft:crafting_shaped", "pattern": ["##", "#X"],
                  "key": {"#": "#ih:planks", "X": "ih:frame"}, "result": {"id": "ih:box", "count": 2}}
        sheet = bs.Sheet([recipe("ih:box", shaped)], {"#ih:planks": ["minecraft:oak_planks", "minecraft:birch_planks"]})
        bill = sheet.bill([(bs.ITEM, "ih:box", 2)])
        self.assertEqual(Fraction(3), bill[(bs.ITEM, "minecraft:oak_planks")])
        self.assertEqual(Fraction(1), bill[(bs.ITEM, "ih:frame")])

    def test_cycle_stops(self):
        recipes = [recipe("ih:block", assembly("ih:block", 1, ("ih:ingot", 9))),
                   recipe("ih:ingot", assembly("ih:ingot", 9, ("ih:block", 1)))]
        bill = bs.Sheet(recipes, {}).bill([(bs.ITEM, "ih:block", 1)])
        self.assertEqual({(bs.ITEM, "ih:block"): Fraction(1)}, bill)

    def test_real_data_runs(self):
        if not any(bs.DATA.glob("*/recipe")):
            self.skipTest("keine erzeugten Rezepte")
        rep = bs.report(bs.Sheet(bs.load_recipes(), bs.load_tags()), {**bs.rockets(), **bs.sections()})
        self.assertIn("Rakete T1", rep)
        self.assertTrue(any(k.startswith("Werftabschnitt 8") for k in rep))
        self.assertTrue(all(v > 0 for bill in rep.values() for v in bill.values()))


if __name__ == "__main__":
    unittest.main()
