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



public class ExampleJavaMod extends Mod{

    public ExampleJavaMod(){
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
            if(state.rules.winWave <= 0){
                if(!hasBlockEvent.get()){
                    hasBlockEvent.set(true);
                    Events.on(BlockBuildEndEvent.class, e -> {
                        Building block = e.tile.build;
                        if(block instanceof TurretBuild ){
                            registerWave();
                        }
                    });
                }
            }
        });

//
//        Events.on(BuildSelectEvent.class, e -> {
//            showMessage("Criou uma maquina");
//        });
//
//        Events.on(WaveEvent.class, e -> {
//            SpawnGroup spawns = state.rules.spawns.get(state.wave);
//            showMessage("Wave has "+ spawns.unitAmount + "enimies");
//        });
//
//
//        Events.on(WaveEvent.class, e -> {
//            Seq<SpawnGroup> spawns = state.rules.spawns;
//            ListIterator<SpawnGroup> it = spawns.list().listIterator();
//            int j = 0;
//            int i = 0;
//            while(it.hasNext()){
//                if(j == state.wave){
//                    i++;
//                }
//                it.next();
//
//             }
//            showMessage("Wave has "+ i + "enimies");
//        });


  }


    private float turretDps(TurretBuild t){
        BulletType bullet = t.peekAmmo();
        float fireRate = 60f / ((Turret) t.block).reload;
        float damage = bullet.damage;
        return damage * fireRate;
    }

    private void showMessage(String msg){
        BaseDialog dialog = new BaseDialog(msg);
        dialog.add("behold").row();
        dialog.cont.button("I see", dialog::hide).size(100f, 50f);
        dialog.show();
    }

    class DefenseStats {
        float health = 0;
        float dps = 0;
    }

    class WaveStats {
        float health = 0;
        float dps = 0;
    }

    public void registerWave(){
            WaveStats enemyStats = calculateWaveStats();
            DefenseStats playerStats = calculateDefenseStats();
            float timeToDie = 0;
            if(enemyStats.dps > 0) timeToDie = playerStats.health / enemyStats.dps;
            float timeToKill = 0;
            if(playerStats.dps > 0) timeToKill = enemyStats.health / playerStats.dps;
            if(timeToDie >= timeToKill){
                if (noAmmo){
                  ui.hudfrag.showToast(Icon.power,"Turret(s) with no ammo!");
                }
                ui.hudfrag.showToast(Icon.warning,"Destruction Eminent!");

            } else{
                ui.hudfrag.showToast(Icon.ok,"Your defence looks good for now!");
            }
            //showMessage("enemyDps - " + enemyStats.dps + ",enemyHealth" + enemyStats.health + ",playerDPS" + playerStats.dps + ",playerHealth" + playerStats.health);

    }

    private boolean noAmmo = false;
    private DefenseStats calculateDefenseStats(){
        DefenseStats stats = new DefenseStats();
        for (Building b : Groups.build){
            if(b.team == player.team()){
                if (b.block.category == Category.turret){
                    stats.health += b.health;
//                    if((Turret)b.block.targetAir)
                    if(((TurretBuild)b).hasAmmo()) {
//                        stats.dps += ((TurretBuild)b).estimateDps();
                        stats.dps += turretDps(((TurretBuild)b));
                    }else{
                        //showMessage("A turret na posição "+ b.x + b.y +" sem munição");
                        noAmmo = true;
                        Call.label("[scarlet]Sem munição", 2f, b.x, b.y);

                    }
                }
            }
        }
        return stats;
    }

    private WaveStats calculateWaveStats(){
        WaveStats stats = new  WaveStats();
        int currentWave = state.wave;
        for(SpawnGroup group : state.rules.spawns){
            float nrSpawned = group.getSpawned(currentWave);
            if (nrSpawned > 0){
                UnitType type = group.type;
                stats.health += type.health * nrSpawned;
                if(type.weapons.size > 0){
                    for (Weapon weapon : type.weapons){
                        stats.dps += weapon.dps();
                    }
                }
            }
        }
        return stats;
    }

    @Override
    public void loadContent(){
        Log.info("Loading some example content.");
    }

}
