package example;

import arc.*;
import arc.util.*;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType.*;
import mindustry.game.SpawnGroup;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.Category;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;


import java.util.concurrent.atomic.AtomicBoolean;

import static mindustry.Vars.*;


public class ExampleJavaMod extends Mod {

    public ExampleJavaMod() {
        Log.info("Loaded ExampleJavaMod constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            //show dialog upon startup
            Time.runTask(10f, () -> {
                BaseDialog dialog = new BaseDialog("camelo");
                dialog.cont.add("behold").row();
                //mod sprites are prefixed with the mod name (this mod is called 'example-java-mod' in its config)
                dialog.cont.image(Core.atlas.find("example-java-mod-frog")).pad(20f).row();
                dialog.cont.button("I see", dialog::hide).size(100f, 50f);
                dialog.show();
            });
        });

        Events.on(WaveEvent.class, e -> {
            registerWave();
        });
        AtomicBoolean hasBlockEvent = new AtomicBoolean(false);
        Events.run(Trigger.update, () -> { //roda a cada update to tick. Só para quando o jogo for reiniciado ou quando fazemos Events.cancel()
            if (state.rules.waves) {
                if (!hasBlockEvent.get()) {
                    hasBlockEvent.set(true);
                    Events.on(BlockBuildEndEvent.class, e -> {
                        Building block = e.tile.build;
                        if (block instanceof TurretBuild) {
                            Timer.schedule(() -> {
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

    private void showMessage(String msg) {
        BaseDialog dialog = new BaseDialog(msg);
        dialog.add("behold").row();
        dialog.cont.button("I see", dialog::hide).size(100f, 50f);
        dialog.show();
    }

    class DefenseStats {
        float health = 0;
        float dps = 0;
        float airDps = 0;
    }

    class WaveStats {
        float health = 0;
        float dps = 0;
        float airHealth = 0;
        UnitType type;
    }


    public void registerWave() {
        noAmmo = false;
        noAirAmmo = false;
        WaveStats enemyStats = calculateWaveStats();
        DefenseStats playerStats = calculateDefenseStats();
        float timeToDie = 0;
        if (enemyStats.dps > 0) timeToDie = playerStats.health / enemyStats.dps;
        float timeToKill = 0;
        if (playerStats.airDps == 0 && enemyStats.airHealth > 0) {
            if(noAirAmmo){
                ui.hudfrag.showToast(Icon.turret, "Turret(s) with no ammo!");
            }else {
                ui.hudfrag.showToast(Icon.defense, "You have no air defense!");
            }
            ui.hudfrag.showToast(Icon.warning, "Destruction Eminent!");
        } else {
            if (playerStats.dps > 0) {
                if (playerStats.airDps == 0)
                    timeToKill = (enemyStats.health / playerStats.dps) + (enemyStats.airHealth / playerStats.airDps);
            }
            if (timeToDie <= timeToKill) {
                if (noAmmo) {
                    ui.hudfrag.showToast(Icon.turret, "Turret(s) with no ammo!");
                }
                ui.hudfrag.showToast(Icon.warning, "Destruction Eminent!");
            } else {
                ui.hudfrag.showToast(Icon.ok, "You have enough fire power to resist the wave!");
            }
        }
        //showMessage("EnemyDPS: " + enemyStats.dps + "  ||  EnemyAirHealth: " + enemyStats.airHealth + "  ||  EnemyGroundHealth: " + enemyStats.health + "  ||  AllyHealth: " + playerStats.health + "  ||  AllyAirDPS: " + playerStats.airDps + "  ||  AllyGroundDPS: " + playerStats.dps);
    }

    private boolean noAmmo = false;
    private boolean noAirAmmo = false;

    private DefenseStats calculateDefenseStats() {
        DefenseStats stats = new DefenseStats();
        for (Building b : Groups.build) {
            if (b.team == player.team()) {
                if (b instanceof TurretBuild) {
                    if (((TurretBuild) b).totalAmmo > 0) {
                        stats.health += b.health;
                        if (!((Turret) b.block).targetAir && ((Turret) b.block).targetGround ) {
                            stats.dps += turretDps(((TurretBuild) b));
                        } else if (!((Turret) b.block).targetGround && ((Turret) b.block).targetAir ) {
                            stats.airDps += turretDps(((TurretBuild) b));
                        } else {
                            stats.dps += turretDps(((TurretBuild) b));
                            stats.airDps += turretDps(((TurretBuild) b));
                        }
                    } else {
                        if(!((Turret) b.block).targetGround && ((Turret) b.block).targetAir ){
                            noAirAmmo = true;
                        }else{
                            noAmmo = true;
                        }
                    }
                }
            }
        }
        return stats;
    }

    private WaveStats calculateWaveStats() {
        WaveStats stats = new WaveStats();
        int currentWave = state.wave;
        for (SpawnGroup group : state.rules.spawns) {
            float nrSpawned = group.getSpawned(currentWave);
            if (nrSpawned > 0) {
                UnitType type = group.type;
                stats.type = type;
                if (type.flying) {
                    stats.airHealth += (type.health  + type.sample.shield) * nrSpawned;
                } else {
                    stats.health += (type.health  + type.sample.shield) * nrSpawned;
                }
                if (type.weapons.size > 0) {
                    for (Weapon weapon : type.weapons) {
                        stats.dps += waveDps(weapon);
                    }
                }
                if(nrSpawned == 1){
                    showMessage("Foi criado um spawnGroup com 1 elemento do tipo " + type + "   " + type.name + " tem airHealth de " + (type.health + type.sample.shield)+ " e ele voa? " + type.flying);
                }
            }
        }
        return stats;
    }

    @Override
    public void loadContent() {
        Log.info("Loading some example content.");
    }

}
