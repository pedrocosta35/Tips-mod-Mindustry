package tipsMod;

public class DefenseCalculator {

    public  DefenseCalculator(JavaWavesTipsMod.DefenseStats playerStats, JavaWavesTipsMod.WaveStats enemyStats) {
        this.playerStats = playerStats;
        this.enemyStats = enemyStats;
    }

    public enum Result {RESIST, AMMO_WARNING, NO_AIR_DEFENSE_WARNING, DESTRUCTION}

    public static Result calculateDefenseStats(boolean noGroundAmmo, boolean noAirAmmo){
        float timeToDie = 0;
        if (enemyStats.dps() > 0) timeToDie = playerStats.health() / enemyStats.dps();
        float timeToKill = 0;
        if (playerStats.airDps() == 0 && enemyStats.airHealth() > 0) { //Player has no air targeting defense and air enemies spawn in this wave
            if (noAirAmmo) {
                return Result.AMMO_WARNING;
            } else {
                return Result.NO_AIR_DEFENSE_WARNING;
            }
        } else { //Player has air targeting defense or no air enemies spawn in this wave
            if (playerStats.groundDps() > 0) {
                if (playerStats.airDps() > 0)
                    timeToKill = (enemyStats.health() / playerStats.groundDps()) + (enemyStats.airHealth() / playerStats.airDps());
                else
                    timeToKill = enemyStats.health() / playerStats.groundDps();
            }
            if (timeToDie <= timeToKill) { //The player defense turrets will be destroyed before defeating the enemies
                if (noGroundAmmo) {
                    return Result.AMMO_WARNING;
                }
                return Result.DESTRUCTION;
            } else { //The player defense turrets will defeat the enemies before being destroyed
                return Result.RESIST;
            }
        }
    }
}