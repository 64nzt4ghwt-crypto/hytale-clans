package com.howlstudio.clans;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.*; import java.util.*;
public class ClanManager {
    record Clan(String name,String leader,List<String> members,int kills,int deaths){}
    private final Path dataDir;
    private final Map<String,Clan> clans=new LinkedHashMap<>();
    private final Map<String,String> playerClan=new LinkedHashMap<>(); // player->clan
    private final Map<String,String> invites=new LinkedHashMap<>(); // player->clanName
    public ClanManager(Path d){dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}load();}
    public int getClanCount(){return clans.size();}
    private String getClanOf(String p){return playerClan.get(p.toLowerCase());}
    public void save(){try{StringBuilder sb=new StringBuilder();for(var e:clans.entrySet()){Clan c=e.getValue();sb.append("CLAN:"+c.name()+"|"+c.leader()+"|"+c.kills()+"|"+c.deaths()+"|"+String.join(",",c.members())+"\n");}Files.writeString(dataDir.resolve("clans.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("clans.txt");if(!Files.exists(f))return;for(String l:Files.readAllLines(f)){if(!l.startsWith("CLAN:"))continue;String[]p=l.substring(5).split("\\|",5);if(p.length<5)continue;List<String>m=new ArrayList<>(Arrays.asList(p[4].split(",")));Clan c=new Clan(p[0],p[1],m,Integer.parseInt(p[2]),Integer.parseInt(p[3]));clans.put(p[0].toLowerCase(),c);for(String mem:m)playerClan.put(mem.toLowerCase(),p[0].toLowerCase());}}catch(Exception e){}}
    public AbstractPlayerCommand getClanCommand(){
        return new AbstractPlayerCommand("clan","Clan commands. /clan create|info|invite|join|leave|kick|top|chat"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> store,Ref<EntityStore> ref,PlayerRef playerRef,World world){
                String[]args=ctx.getInputString().trim().split("\\s+",3);
                if(args.length==0||args[0].isEmpty()){playerRef.sendMessage(Message.raw("§6=== Clan Commands ==="));playerRef.sendMessage(Message.raw("/clan create <name>  invite <player>  join  leave"));playerRef.sendMessage(Message.raw("/clan kick <player>  info [name]  top  chat <msg>"));return;}
                String pname=playerRef.getUsername();String plower=pname.toLowerCase();
                switch(args[0].toLowerCase()){
                    case"create"->{if(args.length<2){playerRef.sendMessage(Message.raw("Usage: /clan create <name>"));return;}if(getClanOf(plower)!=null){playerRef.sendMessage(Message.raw("[Clan] Already in a clan. /clan leave first."));return;}String cn=args[1];if(clans.containsKey(cn.toLowerCase())){playerRef.sendMessage(Message.raw("[Clan] Name taken."));return;}List<String>m=new ArrayList<>();m.add(plower);Clan c=new Clan(cn,plower,m,0,0);clans.put(cn.toLowerCase(),c);playerClan.put(plower,cn.toLowerCase());save();playerRef.sendMessage(Message.raw("[Clan] §aCreated§r clan §6"+cn+"§r! You're the leader."));}
                    case"invite"->{if(args.length<2){playerRef.sendMessage(Message.raw("Usage: /clan invite <player>"));return;}String myClan=getClanOf(plower);if(myClan==null){playerRef.sendMessage(Message.raw("[Clan] You're not in a clan."));return;}Clan c=clans.get(myClan);if(!c.leader().equalsIgnoreCase(plower)){playerRef.sendMessage(Message.raw("[Clan] Only the leader can invite."));return;}invites.put(args[1].toLowerCase(),myClan);playerRef.sendMessage(Message.raw("[Clan] Invited §e"+args[1]+"§r to "+c.name()));for(PlayerRef p:Universe.get().getPlayers())if(p.getUsername().equalsIgnoreCase(args[1]))p.sendMessage(Message.raw("[Clan] §e"+pname+"§r invited you to §6"+c.name()+"§r. /clan join to accept."));}
                    case"join"->{String inv=invites.remove(plower);if(inv==null){playerRef.sendMessage(Message.raw("[Clan] No invite pending."));return;}if(getClanOf(plower)!=null){playerRef.sendMessage(Message.raw("[Clan] Leave your current clan first."));return;}Clan c=clans.get(inv);if(c==null){playerRef.sendMessage(Message.raw("[Clan] Clan no longer exists."));return;}c.members().add(plower);playerClan.put(plower,inv);save();playerRef.sendMessage(Message.raw("[Clan] Joined §6"+c.name()+"§r!"));}
                    case"leave"->{String myClan=getClanOf(plower);if(myClan==null){playerRef.sendMessage(Message.raw("[Clan] Not in a clan."));return;}Clan c=clans.get(myClan);if(c.leader().equalsIgnoreCase(plower)){clans.remove(myClan);for(String m:c.members())playerClan.remove(m);playerRef.sendMessage(Message.raw("[Clan] §cDisbanded§r clan "+c.name()+"(you were leader)."));}else{c.members().remove(plower);playerClan.remove(plower);playerRef.sendMessage(Message.raw("[Clan] Left §6"+c.name()));}save();}
                    case"info"->{String look=args.length>1?args[1].toLowerCase():getClanOf(plower);if(look==null){playerRef.sendMessage(Message.raw("[Clan] Not in a clan. Use /clan info <name>"));return;}Clan c=clans.get(look);if(c==null){playerRef.sendMessage(Message.raw("[Clan] Clan not found: "+look));return;}playerRef.sendMessage(Message.raw("§6=== "+c.name()+" ==="));playerRef.sendMessage(Message.raw("Leader: §e"+c.leader()+"§r | Members: "+c.members().size()+" | K/D: "+c.kills()+"/"+c.deaths()));}
                    case"top"->{var sorted=new ArrayList<>(clans.values());sorted.sort((a,b)->b.kills()-a.kills());playerRef.sendMessage(Message.raw("[Clan] §6Top Clans by Kills:"));for(int i=0;i<Math.min(5,sorted.size());i++){Clan c=sorted.get(i);playerRef.sendMessage(Message.raw("  "+(i+1)+". §6"+c.name()+"§r — "+c.kills()+" kills, "+c.members().size()+" members"));}}
                    case"chat"->{String myClan=getClanOf(plower);if(myClan==null){playerRef.sendMessage(Message.raw("[Clan] Not in a clan."));return;}Clan c=clans.get(myClan);String msg=args.length>1?ctx.getInputString().substring(5).trim():"...";for(String m:c.members())for(PlayerRef p:Universe.get().getPlayers())if(p.getUsername().equalsIgnoreCase(m))p.sendMessage(Message.raw("§6["+c.name()+"]§r §e"+pname+"§r: "+msg));}
                    default->playerRef.sendMessage(Message.raw("Unknown subcommand. /clan for help."));
                }
            }
        };
    }
}
