"""Tests für specdata.min_multipliers (22.3, Kapitel 28): python -m unittest discover -s tools -p "test_*.py"."""
import math
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402


def recipe(rid, *ings):
    return {"id": rid, "type": "assembly", "ingredients": [{"item": i, "count": n} for i, n in ings]}


def spec_with(*recipes, lo=0.25):
    s = sd.Spec()
    s.recipes = list(recipes)
    s.balance = {"misc": {"config": {"recipe_amount_multiplier": {"default": 1.0, "min": lo, "max": 4.0}}}}
    return s


def scaled(r, f, mm):
    e = max(f, mm.get(r["id"], 0.0))
    return tuple(sorted((i["item"], max(1, math.ceil(i["count"] * e - 1e-9))) for i in r["ingredients"]))


class MinMultiplierTest(unittest.TestCase):
    def test_later_recipe_gets_minimum(self):
        a = recipe("x:a", ("p", 2), ("q", 1))
        b = recipe("x:b", ("p", 4), ("q", 1))
        mm = sd.min_multipliers(spec_with(a, b))
        self.assertNotIn("x:a", mm)
        self.assertGreater(mm["x:b"], 0.25)
        for k in range(25, 100):
            self.assertNotEqual(scaled(a, k / 100, mm), scaled(b, k / 100, mm), k)

    def test_distinct_recipes_need_nothing(self):
        mm = sd.min_multipliers(spec_with(recipe("x:a", ("p", 1)), recipe("x:b", ("q", 1))))
        self.assertEqual(mm, {})

    def test_real_data_has_no_collisions(self):
        spec = sd.load()
        mm = sd.min_multipliers(spec)
        asm = [r for r in spec.recipes if r.get("type") == "assembly"]
        stack1 = {i for i, e in spec.items.items() if e.get("max_stack") == 1}
        for k in range(250, 1000):
            f = k / 1000
            seen = {}
            for r in asm:
                e = max(f, mm.get(r["id"], 0.0))
                key = (tuple(sorted((str(sd.ingredient_ids(i)), i.get("count", 1) if sd.ingredient_ids(i)[:1] and
                                     sd.ingredient_ids(i)[0] in stack1 else max(1, math.ceil(i.get("count", 1) * e - 1e-9)))
                                    for i in r.get("ingredients") or [])), str(r.get("fluid")), r.get("mold"))
                self.assertNotIn(key, seen, f"{r['id']} = {seen.get(key)} bei Faktor {f}")
                seen[key] = r["id"]


if __name__ == "__main__":
    unittest.main()
