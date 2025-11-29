package example;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
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


public class ExampleJavaMod extends Mod{

    private String event;

    public ExampleJavaMod(){
        Log.info("Loaded ExampleJavaMod constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            //show dialog upon startup
            Time.runTask(10f, () -> {
                BaseDialog dialog = new BaseDialog("frog");
                dialog.cont.add("behold").row();
                //mod sprites are prefixed with the mod name (this mod is called 'example-java-mod' in its config)
                dialog.cont.image(Core.atlas.find("example-java-mod-frog")).pad(20f).row();
                dialog.cont.button("I see", dialog::hide).size(100f, 50f);
                dialog.show();
            });
        });

        Events.on(WaveEvent.class, e -> {
            event = "Wave";
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
        int nrTurretsWithAmmo = 0; ///////////////////////////////
        int nrTurretsWithoutAmmo = 0; ///////////////////////////////
    }

    class WaveStats {
        float health = 0;
        float dps = 0;
        float airHealth = 0;
        UnitType type;
        int nrEnemies = 0; ///////////////////////////////
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
                if (playerStats.airDps != 0)
                    timeToKill = (enemyStats.health / playerStats.dps) + (enemyStats.airHealth / playerStats.airDps);
                else
                    timeToKill = enemyStats.health / playerStats.dps;
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
        showMessage("NrEnemies wave " + enemyStats.nrEnemies + " \n NrTurretsWithAmmo wave " + playerStats.nrTurretsWithAmmo + " \n NrTurretsWithoutAmmo wave " + playerStats.nrTurretsWithoutAmmo);
        //showMessage("EnemyDPS: " + enemyStats.dps + "  \n  EnemyAirHealth: " + enemyStats.airHealth + "  \n  EnemyGroundHealth: " + enemyStats.health + "  \n  AllyHealth: " + playerStats.health + "  \n  AllyAirDPS: " + playerStats.airDps + "  \n  AllyGroundDPS: " + playerStats.dps + "  \n  nrEnemies: " + enemyStats.nrEnemies + "  \n  nrTurretsWithAmmo: " + playerStats.nrTurretsWithAmmo + "  \n  nrTurretsWithoutAmmo: " + playerStats.nrTurretsWithoutAmmo);
    }

    private boolean noAmmo = false;
    private boolean noAirAmmo = false;

    private DefenseStats calculateDefenseStats() {
        DefenseStats stats = new DefenseStats();
        //showMessage("NrTurretsWithAmmo before wave " + stats.nrTurretsWithAmmo + " \n NrTurretsWithoutAmmo before wave " + stats.nrTurretsWithoutAmmo);
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
                        stats.nrTurretsWithAmmo ++;
                    } else {
                        if(!((Turret) b.block).targetGround && ((Turret) b.block).targetAir ){
                            noAirAmmo = true;
                        }else{
                            noAmmo = true;
                        }
                        stats.nrTurretsWithoutAmmo ++;
                    }
                }
            }
        }
        //showMessage("NrTurretsWithAmmo after wave " + stats.nrTurretsWithAmmo + " \n NrTurretsWithoutAmmo after wave " + stats.nrTurretsWithoutAmmo);
        return stats;
    }

    private WaveStats calculateWaveStats() {
        WaveStats stats = new WaveStats();
        //showMessage("NrEnemies before wave " + stats.nrEnemies);
        if(event ==  "Turret"){
            int previousWave = state.wave - 1;
            for (SpawnGroup group : state.rules.spawns) {
                float nrSpawned = group.getSpawned(previousWave);
                if (nrSpawned > 0) {
                    setWaveStatus(stats, group.type, nrSpawned);
                }
            }
        }else{
            int i = 0;
            for(Unit u : Groups.unit){
                if(u.team == state.rules.waveTeam) {
                    showMessage("" + i);
                    i++;
                    setWaveStatus(stats, u.type, 1); //1?
                }
            }
        }

        //showMessage("NrEnemies after wave " + stats.nrEnemies);
        return stats;
    }

    private void setWaveStatus(WaveStats stats, UnitType type, float nrSpawned) {
        stats.nrEnemies += nrSpawned; ///////////////////////////////
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
    }

    @Override
    public void loadContent(){
        Log.info("Loading some example content.");
    }

}
