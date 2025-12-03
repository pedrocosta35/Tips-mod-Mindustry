
import org.junit.Test;
import tipsMod.DefenseCalculator;
import tipsMod.DefenseCalculator.*;
import tipsMod.JavaWavesTipsMod;

import static org.junit.Assert.assertEquals;

public class DefenseCalculatorTest {

    @Test
    public void testNormalGroundVictory() { //Player has ground targeting defense and ground enemies that are weaker spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 100, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(1000, 0, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.RESIST, result);
    }

    @Test
    public void testNormalAirVictory() { //Player has air targeting defense and air enemies that are weaker spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 0, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(0, 1000, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.RESIST, result);
    }

    @Test
    public void testNormalGroundLoss() { //Player has ground targeting defense and ground enemies that are stronger spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(500, 100, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(1000, 0, 200);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.DESTRUCTION, result);
    }

    @Test
    public void testNormalAirLoss() { //Player has air targeting defense and air enemies that are stronger spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(500, 0, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(0, 1000, 200);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.DESTRUCTION, result);
    }

    @Test
    public void testNoGroundAmmo() { //Player has ground targeting defense and ground enemies spawn in this wave but has no ammo
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 100, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(1000, 0, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(true, false);
        assertEquals(Result.AMMO_WARNING, result);
    }

    @Test
    public void testNoAirAmmo() { //Player has air targeting defense and air enemies spawn in this wave but has no ammo
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 0, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(0, 1000, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, true);
        assertEquals(Result.DESTRUCTION, result);
    }

    @Test
    public void testNoGroundDefense() { //Player has no ground targeting defense and ground enemies spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 0, 100);
        var enemyStats = new JavaWavesTipsMod.WaveStats(1000, 0, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.DESTRUCTION, result);
    }

    @Test
    public void testNoAirDefense() { //Player has no air targeting defense and air enemies spawn in this wave
        var playerDefenses = new JavaWavesTipsMod.DefenseStats(2000, 100, 0);
        var enemyStats = new JavaWavesTipsMod.WaveStats(0, 1000, 100);
        DefenseCalculator calculator = new DefenseCalculator(playerDefenses, enemyStats);
        Result result = calculator.calculateDefenseStats(false, false);
        assertEquals(Result.NO_AIR_DEFENSE_WARNING, result);
    }
}