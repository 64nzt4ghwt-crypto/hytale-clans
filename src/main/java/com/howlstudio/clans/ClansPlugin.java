package com.howlstudio.clans;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/** Clans — Create clans, invite members, declare war, track scores. */
public final class ClansPlugin extends JavaPlugin {
    private ClanManager mgr;
    public ClansPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[Clans] Loading...");
        mgr=new ClanManager(getDataDirectory());
        CommandManager.get().register(mgr.getClanCommand());
        System.out.println("[Clans] Ready. "+mgr.getClanCount()+" clans.");
    }
    @Override protected void shutdown(){if(mgr!=null)mgr.save();System.out.println("[Clans] Stopped.");}
}
