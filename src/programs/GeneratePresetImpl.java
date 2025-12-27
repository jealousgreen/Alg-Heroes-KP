package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

/**
 * Генерация армии компьютера.
 *
 * Идея: жадный выбор по эффективности (атака/стоимость), при равенстве — здоровье/стоимость.
 * Ограничения:
 *  - суммарная стоимость <= maxPoints
 *  - каждого типа <= 11
 *
 * Сложность: O(n log n + m), где n — число типов, m — число юнитов в результате.
 */
public class GeneratePresetImpl implements GeneratePreset {

    private static final int MAX_PER_TYPE = 11;
    private static final int FIELD_LEFT_X_MIN = 0;
    private static final int FIELD_LEFT_X_MAX = 2;
    private static final int FIELD_HEIGHT = 21;

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        if (unitList == null || unitList.isEmpty() || maxPoints <= 0) {
            return new Army(Collections.emptyList());
        }

        // Считаем "эффективность" для каждого типа.
        List<UnitTypeScore> types = new ArrayList<>();
        for (Unit u : unitList) {
            if (u == null) continue;
            if (u.getCost() <= 0) continue;
            double atkScore = (double) u.getBaseAttack() / (double) u.getCost();
            double hpScore = (double) u.getHealth() / (double) u.getCost();
            types.add(new UnitTypeScore(u, atkScore, hpScore));
        }

        // Сортировка типов по убыванию (атака/стоимость), затем (здоровье/стоимость)
        types.sort((a, b) -> {
            int c1 = Double.compare(b.atkScore, a.atkScore);
            if (c1 != 0) return c1;
            int c2 = Double.compare(b.hpScore, a.hpScore);
            if (c2 != 0) return c2;
            // стабильный "тай-брейк" — по стоимости (дешевле раньше)
            return Integer.compare(a.template.getCost(), b.template.getCost());
        });

        // Для добивки по выживаемости — заранее найдём лучший по hp/стоимость
        UnitTypeScore bestHp = null;
        for (UnitTypeScore t : types) {
            if (bestHp == null || t.hpScore > bestHp.hpScore) bestHp = t;
        }

        List<Unit> result = new ArrayList<>();
        Map<String, Integer> countByType = new HashMap<>();
        int pointsLeft = maxPoints;

        // Основной жадный проход: набираем максимально эффективных по атаке
        for (UnitTypeScore t : types) {
            if (pointsLeft <= 0) break;
            String type = safeType(t.template);
            int already = countByType.getOrDefault(type, 0);
            int canTake = MAX_PER_TYPE - already;
            if (canTake <= 0) continue;

            int cost = t.template.getCost();
            int maxByBudget = pointsLeft / cost;
            int take = Math.min(canTake, maxByBudget);

            for (int i = 0; i < take; i++) {
                result.add(cloneUnit(t.template, type, 0, 0));
            }
            if (take > 0) {
                countByType.put(type, already + take);
                pointsLeft -= take * cost;
            }
        }

        // Добивка оставшихся очков по лучшему hp/стоимость (не нарушая лимитов)
        if (bestHp != null) {
            String type = safeType(bestHp.template);
            int cost = bestHp.template.getCost();
            while (pointsLeft >= cost) {
                int already = countByType.getOrDefault(type, 0);
                if (already >= MAX_PER_TYPE) break;
                result.add(cloneUnit(bestHp.template, type, 0, 0));
                countByType.put(type, already + 1);
                pointsLeft -= cost;
            }
        }

        // Расставляем координаты в зоне компьютера (левые 3 столбца x=0..2), без пересечений.
        placeUnitsLeft(result);

        Army army = new Army();
        army.setUnits(result);
        army.setPoints(maxPoints - pointsLeft);
        return army;
    }

    private static String safeType(Unit u) {
        String t = (u.getUnitType() == null) ? "" : u.getUnitType().trim();
        return t.isEmpty() ? (u.getName() == null ? "UNKNOWN" : u.getName()) : t;
    }

    private static Unit cloneUnit(Unit template, String typeName, int x, int y) {
        // Конструктор Unit: (name, unitType, health, baseAttack, cost, attackType, attackBonuses, defenceBonuses, x, y)
        return new Unit(
                template.getName(),
                typeName,
                template.getHealth(),
                template.getBaseAttack(),
                template.getCost(),
                template.getAttackType(),
                template.getAttackBonuses(),
                template.getDefenceBonuses(),
                x,
                y
        );
    }

    private static void placeUnitsLeft(List<Unit> units) {
        boolean[][] occupied = new boolean[FIELD_LEFT_X_MAX - FIELD_LEFT_X_MIN + 1][FIELD_HEIGHT];
        int x = FIELD_LEFT_X_MIN;
        int y = 0;

        for (Unit u : units) {
            // ищем следующую свободную клетку
            while (x <= FIELD_LEFT_X_MAX && occupied[x - FIELD_LEFT_X_MIN][y]) {
                y++;
                if (y >= FIELD_HEIGHT) {
                    y = 0;
                    x++;
                }
            }
            if (x > FIELD_LEFT_X_MAX) {
                // места не хватило (теоретически), оставим как есть
                break;
            }
            occupied[x - FIELD_LEFT_X_MIN][y] = true;
            u.setxCoordinate(x);
            u.setyCoordinate(y);

            // следующий слот
            y++;
            if (y >= FIELD_HEIGHT) {
                y = 0;
                x++;
            }
        }
    }

    private static class UnitTypeScore {
        final Unit template;
        final double atkScore;
        final double hpScore;

        UnitTypeScore(Unit template, double atkScore, double hpScore) {
            this.template = template;
            this.atkScore = atkScore;
            this.hpScore = hpScore;
        }
    }
}
