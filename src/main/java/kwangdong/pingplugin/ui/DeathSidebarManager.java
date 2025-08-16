package kwangdong.pingplugin.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Criteria; // <-- add this import (Paper/Bukkit 1.20+)

import java.util.*;

public class DeathSidebarManager {

    private static final String OBJECTIVE_NAME = "wave_deaths";

    // 사이드바에 쓸 스코어보드 (웨이브 동안 공유)
    private Scoreboard waveBoard;
    private Objective objective;

    // 웨이브 시작 직전의 플레이어별 기존 보드 (종료 시 복원)
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<>();

    // 현재 사이드바에 표시 중인 플레이어 집합
    private final Set<UUID> showing = new HashSet<>();

    public boolean isActive() {
        return waveBoard != null && objective != null;
    }

    /**
     * 웨이브 시작 시 사이드바 생성 + 참가자에게 세팅
     * @param participants 라운드 참가자
     * @param deathCounts  참가자별 남은 데스카운트 (UUID -> left)
     */
    public void start(List<Player> participants, Map<UUID, Integer> deathCounts) {
        if (participants == null || participants.isEmpty()) return;

        // 새 보드 생성
        ScoreboardManager m = Bukkit.getScoreboardManager();
        this.waveBoard = m.getNewScoreboard();

        // ✅ NEW: use Criteria.DUMMY and set render type separately
        this.objective = waveBoard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                Component.text("데스카운트", NamedTextColor.GOLD)
        );
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.setRenderType(RenderType.INTEGER);

        // 초기 값 세팅 + 보드 적용
        for (Player p : participants) {
            previousBoards.put(p.getUniqueId(), p.getScoreboard()); // 기존 보드 백업
            setScore(p.getName(), deathCounts.getOrDefault(p.getUniqueId(), 0));
            p.setScoreboard(waveBoard);
            showing.add(p.getUniqueId());
        }
    }

    /**
     * 특정 플레이어의 남은 데스카운트 갱신 (사망 시 호출)
     */
    public void update(Player player, int left) {
        if (!isActive() || player == null) return;
        setScore(player.getName(), left);
    }

    /**
     * 전체 갱신(필요할 때)
     */
    public void updateAll(Map<UUID, Integer> deathCounts) {
        if (!isActive()) return;
        for (UUID id : showing) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                setScore(p.getName(), deathCounts.getOrDefault(id, 0));
            }
        }
    }

    /**
     * 웨이브 종료 시, 기존 보드로 복원
     */
    public void stop() {
        if (!isActive()) {
            clearCaches();
            return;
        }
        for (UUID id : showing) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Scoreboard prev = previousBoards.get(id);
            if (prev != null) p.setScoreboard(prev);
        }
        clearCaches();
    }

    private void clearCaches() {
        previousBoards.clear();
        showing.clear();
        waveBoard = null;
        objective = null;
    }

    private void setScore(String entry, int value) {
        // ✅ 단순화: 엔트리 존재 여부 상관없이 바로 셋업 가능
        objective.getScore(entry).setScore(value);
    }
}
