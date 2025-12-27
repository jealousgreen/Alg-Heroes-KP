package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.*;

/**
 * Отбор доступных целей для атаки.
 *
 * unitsByRow 3 списка юнитов армии-цели.
 * Подход: в каждом ряду доступны только передние юниты, кто ближе всего к атакующей стороне.
 *
 * Если isLeftArmyTarget == true: цель слева, атакующий справа => берём юнита с МАКСИМАЛЬНЫМ x в каждом ряду.
 * Если isLeftArmyTarget == false: цель справа, атакующий слева => берём юнита с МИНИМАЛЬНЫМ x в каждом ряду.
 *
 * Сложность: O(N), где N  - количество юнитов в 3 рядах (кол-во рядов фиксировано).
 */
public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        if (unitsByRow == null || unitsByRow.isEmpty()) return Collections.emptyList();

        List<Unit> suitable = new ArrayList<>();

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) continue;

            Unit best = null;
            for (Unit u : row) {
                if (u == null || !u.isAlive()) continue;
                if (best == null) {
                    best = u;
                    continue;
                }
                if (isLeftArmyTarget) {
                    // атакуем слева -  выбираем самого правого по x
                    if (u.getxCoordinate() > best.getxCoordinate()) best = u;
                } else {
                    // атакуем справа - выбираем самого левого по x
                    if (u.getxCoordinate() < best.getxCoordinate()) best = u;
                }
            }
            if (best != null) suitable.add(best);
        }

        return suitable;
    }
}
