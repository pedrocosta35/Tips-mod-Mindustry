package tipsMod;

import arc.*;
import arc.util.*;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType.*;
import mindustry.game.SpawnGroup;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import java.util.concurrent.atomic.AtomicBoolean;
import static mindustry.Vars.*;


public class JavaWavesTipsMod extends Mod {
    private String event;
    private boolean isWaveAlive = true;
    private float nrEnemies = getNrEnemies(state.wave);

    public JavaWavesTipsMod() {
        Log.info("Loaded ExampleJavaMod constructor.");

        Events.on(UnitDestroyEvent.class, e -> {
            if (e.unit.team == state.rules.waveTeam) {
                if (nrEnemies > 1) {
                    nrEnemies -= 1;
                } else {
                    isWaveAlive = false;
                }
            }
        });

        Events.on(WaveEvent.class, e -> {
            event = "Wave";
            isWaveAlive = true;
            registerWave();
        });

        AtomicBoolean hasBlockEvent = new AtomicBoolean(false);
        Events.run(Trigger.update, () -> {
            if (state.rules.waves) {
                if (!hasBlockEvent.get()) {
                    hasBlockEvent.set(true);
                    Events.on(BlockBuildEndEvent.class, e -> {
                        Building block = e.tile.build;
                        if (block instanceof TurretBuild) {
                            Timer.schedule(() -> {
                                event = "Turret";
                                registerWave();
                            }, 3f);
                        }
                    });
                }
            }
        });
    }

    private float turretDps(TurretBuild t) {
        BulletType bullet = t.peekAmmo();
        float fireRate = 60f / ((Turret) t.block).reload;
        float damage = bullet.damage;
        return damage * fireRate;
    }

    private float waveDps(Weapon w) {
        BulletType bullet = w.bullet;
        float fireRate = w.shotsPerSec();
        float damage = bullet.damage;
        return damage * fireRate;
    }

    private record DefenseStats(float health, float dps, float airDps) {}

    private record WaveStats(float health, float dps, float airHealth, UnitType type) {}

    public void registerWave() {
        noAmmo = false;
        noAirAmmo = false;
        WaveStats enemyStats = calculateWaveStats();
        DefenseStats playerStats = calculateDefenseStats();

        float timeToDie = 0;
        if (enemyStats.dps() > 0) timeToDie = playerStats.health() / enemyStats.dps();
        float timeToKill = 0;
        if (playerStats.airDps() == 0 && enemyStats.airHealth() > 0) {
            if (noAirAmmo) {
                ui.hudfrag.showToast(Icon.turret, "Turret(s) with no ammo!");
            } else {
                ui.hudfrag.showToast(Icon.defense, "You have no air defense!");
            }
            ui.hudfrag.showToast(Icon.warning, "Destruction Eminent!");
        } else {
            if (playerStats.dps() > 0) {
                if (playerStats.airDps() != 0)
                    timeToKill = (enemyStats.health() / playerStats.dps()) + (enemyStats.airHealth() / playerStats.airDps());
                else
                    timeToKill = enemyStats.health() / playerStats.dps();
            }
            if (timeToDie <= timeToKill) {
                if (noAmmo) {
                    ui.hudfrag.showToast(Icon.turret, "Turret(s) with no ammo! \n");
                }
                ui.hudfrag.showToast(Icon.warning, "Destruction Eminent!");
            } else {
                ui.hudfrag.showToast(Icon.ok, "You have enough fire power to resist the wave!");
            }
        }
    }

    private boolean noAmmo = false;
    private boolean noAirAmmo = false;

    private DefenseStats calculateDefenseStats() {
        float health = 0.0f;
        float dps = 0.0f;
        float airDps = 0.0f;
        for (Building b : Groups.build) {
            if (b.team == player.team()) {
                if (b instanceof TurretBuild) {
                    if (((TurretBuild) b).totalAmmo > 0) {
                        health += b.health;
                        if (!((Turret) b.block).targetAir && ((Turret) b.block).targetGround) {
                            dps += turretDps(((TurretBuild) b));
                        } else if (!((Turret) b.block).targetGround && ((Turret) b.block).targetAir) {
                            airDps += turretDps(((TurretBuild) b));
                        } else {
                            dps += turretDps(((TurretBuild) b));
                            airDps += turretDps(((TurretBuild) b));
                        }
                    } else {
                        if (!((Turret) b.block).targetGround && ((Turret) b.block).targetAir) {
                            noAirAmmo = true;
                        } else {
                            noAmmo = true;
                        }
                    }
                }
            }
        }
        return new DefenseStats(health, dps, airDps);
    }

    private WaveStats calculateWaveStats() {
        float health = 0.0f;
        float dps = 0.0f;
        float airHealth = 0.0f;
        UnitType type = null;
        if (event.equals("Turret")) {
            int previousWave;
            if (isWaveAlive)
                previousWave = state.wave - 2;
            else
                previousWave = state.wave - 1;
            for (SpawnGroup group : state.rules.spawns) {
                float nrSpawned = group.getSpawned(previousWave);
                if (nrSpawned > 0) {
                    type = group.type;
                    if (type.flying) {
                        airHealth += (type.health + type.sample.shield) * nrSpawned;
                    } else {
                        health += (type.health + type.sample.shield) * nrSpawned;
                    }
                    if (type.weapons.size > 0) {
                        for (Weapon weapon : type.weapons) {
                            dps += waveDps(weapon);
                        }
                    }
                }
            }
        } else {
            for (Unit u : Groups.unit) {
                if (u.team == state.rules.waveTeam) {
                    type = u.type;
                    if (type.flying) {
                        airHealth += (type.health + type.sample.shield);
                    } else {
                        health += (type.health + type.sample.shield);
                    }
                    if (type.weapons.size > 0) {
                        for (Weapon weapon : type.weapons) {
                            dps += waveDps(weapon);
                        }
                    }
                }
            }
        }
        return new WaveStats(health, dps, airHealth, type);
    }

    private float getNrEnemies(int waveNr) {
        float nrSpawned = 0.0f;
        for (SpawnGroup group : state.rules.spawns) {
            nrSpawned += group.getSpawned(waveNr);
        }
        return nrSpawned;
    }

    @Override
    public void loadContent() {
        Log.info("Loading some example content.");
    }
}