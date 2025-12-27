package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.*;

/**
 * Симуляция боя по раундам.
 *
 * Правила:
 *  - в начале каждого раунда формируется очередь из живых юнитов обеих армий
 *  - очередь сортируется по убыванию baseAttack
 *  - каждый живой юнит делает попытку атаки (attack()), после чего пишется лог
 *  - если юнит/цель умерли, они "исключаются" из дальнейших ходов через проверку isAlive (ленивая фильтрация)
 *  - бой заканчивается, когда у одной из армий нет живых юнитов, способных сделать ход (т.е. в раунде ни одна атака не произошла)
 *
 * Сложность:
 *  - на раунд: O(N log N) на сортировку + O(N) на проход
 *  - в худшем случае раундов O(N) => O(N^2 log N)
 */
public class SimulateBattleImpl implements SimulateBattle {
    private PrintBattleLog printBattleLog; // логировать после каждой атаки

    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        if (playerArmy == null || computerArmy == null) return;

        List<Unit> playerUnits = playerArmy.getUnits() == null ? Collections.emptyList() : playerArmy.getUnits();
        List<Unit> computerUnits = computerArmy.getUnits() == null ? Collections.emptyList() : computerArmy.getUnits();

        // Identity-set чтобы быстро понимать принадлежность юнита армии (equals может быть не переопределён)
        Set<Unit> playerSet = Collections.newSetFromMap(new IdentityHashMap<>());
        playerSet.addAll(playerUnits);

        while (true) {
            List<Unit> turnOrder = new ArrayList<>();
            for (Unit u : playerUnits) if (u != null && u.isAlive()) turnOrder.add(u);
            for (Unit u : computerUnits) if (u != null && u.isAlive()) turnOrder.add(u);

            if (turnOrder.isEmpty()) break;

            // сортировка по силе атаки (убывание)
            turnOrder.sort((a, b) -> Integer.compare(b.getBaseAttack(), a.getBaseAttack()));

            boolean playerDidAttack = false;
            boolean computerDidAttack = false;

            for (Unit attacker : turnOrder) {
                if (attacker == null || !attacker.isAlive()) continue;

                Unit target = attacker.getProgram() == null ? null : attacker.getProgram().attack();

                // лог всегда после попытки атаки (если цель null — лог не печатаем, чтобы не спамить)
                if (target != null) {
                    if (printBattleLog != null) {
                        printBattleLog.printBattleLog(attacker, target);
                    }
                    if (playerSet.contains(attacker)) playerDidAttack = true;
                    else computerDidAttack = true;
                }
            }

            // если одна из сторон в раунде не смогла атаковать — она "не способна сделать ход"
            if (!playerDidAttack || !computerDidAttack) break;

            // дополнительная страховка: если у одной армии нет живых — конец
            if (!hasAlive(playerUnits) || !hasAlive(computerUnits)) break;
        }
    }

    private boolean hasAlive(List<Unit> units) {
        for (Unit u : units) {
            if (u != null && u.isAlive()) return true;
        }
        return false;
    }
}
