package eu.metacloudservice.bungee;

import eu.metacloudservice.CloudAPI;
import eu.metacloudservice.Driver;
import eu.metacloudservice.bungee.command.CloudCommand;
import eu.metacloudservice.bungee.command.EndCommand;
import eu.metacloudservice.bungee.listener.CloudConnectListener;
import eu.metacloudservice.configuration.ConfigDriver;
import eu.metacloudservice.configuration.dummys.message.Messages;
import eu.metacloudservice.configuration.dummys.serviceconfig.LiveService;
import eu.metacloudservice.networking.NettyDriver;
import eu.metacloudservice.process.ServiceState;
import eu.metacloudservice.service.entrys.CloudService;
import eu.metacloudservice.subcommands.*;
import eu.metacloudservice.timebaser.TimerBase;
import eu.metacloudservice.timebaser.utils.TimeUtil;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.*;

public class BungeeBootstrap extends Plugin {


    private static BungeeBootstrap instance;
    public BungeeAudiences audiences;

    @Override
    public void onLoad() {

        new Driver();
    }

    @Override
    public void onEnable() {
        instance = this;
        audiences = BungeeAudiences.builder(instance).build();
        final LiveService service = (LiveService) new ConfigDriver("./CLOUDSERVICE.json").read(LiveService.class);
        CloudAPI.getInstance().setState(ServiceState.LOBBY, service.getService());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new CloudConnectListener());
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CloudCommand("cloud"));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CloudCommand("metacloud"));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new CloudCommand("mc"));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new EndCommand("end"));
        CloudAPI.getInstance().getPluginCommandDriver().register(new VersionCommand());
        CloudAPI.getInstance().getPluginCommandDriver().register(new ReloadCommand());
        CloudAPI.getInstance().getPluginCommandDriver().register(new ServiceCommand());
        CloudAPI.getInstance().getPluginCommandDriver().register(new GroupCommand());
        CloudAPI.getInstance().getPluginCommandDriver().register(new PlayerCommand());
        new TimerBase().schedule(new TimerTask() {
            @Override
            public void run() {
                if (CloudAPI.getInstance().getGroupPool().getGroup(service.getGroup()).isMaintenance()){
                    ProxyServer.getInstance().getPlayers().forEach(player -> {
                       if ( !player.hasPermission("metacloud.connection.maintenance") && !CloudAPI.getInstance().getWhitelist().contains(player.getName())){
                           Messages messages = CloudAPI.getInstance().getMessages();
                           player.disconnect(Driver.getInstance().getMessageStorage().base64ToUTF8(messages.getMessages().get("kickNetworkIsMaintenance")).replace("&", "§"));
                       }
                    });
                }
                    if (!NettyDriver.getInstance().nettyClient.getChannel().isActive()){
                        System.exit(0);
                    }
            }
        }, 2, 2, TimeUtil.SECONDS);
    }

    public static BungeeBootstrap getInstance() {
        return instance;
    }

    public CloudService getLobby(ProxiedPlayer player){
        if (CloudAPI.getInstance().getServicePool().getServices().isEmpty()){
            return null;
        }else if (CloudAPI.getInstance().getServicePool().getServices().stream().noneMatch(service -> service.getGroup().getGroupType().equalsIgnoreCase("LOBBY")  && service.getState() == ServiceState.LOBBY)){
            return null;
        }else {
            final List<CloudService> cloudServices = CloudAPI.getInstance().getServicePool().getServices().stream()
                    .filter(service -> service.getGroup().getGroupType().equalsIgnoreCase("LOBBY"))
                    .filter(service -> {

                        if (!service.getGroup().isMaintenance()){
                            return true;
                        }else if (player.hasPermission("metacloud.bypass.connection.maintenance") || CloudAPI.getInstance().getWhitelist().contains(player.getName())){
                            return true;
                        }else return false;
                    })
                    .filter(service -> service.getState() == ServiceState.LOBBY).toList()
                    .stream().filter(service -> {
                        if (service.getGroup().getPermission().equalsIgnoreCase("")) {
                            return true;
                        } else return player.hasPermission(service.getGroup().getPermission());
                    }).toList();
            if (cloudServices.isEmpty()){
                return null;
            }
            final List<Integer> priority = new ArrayList<>();
            cloudServices.forEach( service -> priority.add(service.getGroup().getPriority()));
            priority.sort(Collections.reverseOrder());
           final int priorty = priority.get(0);
           final List<CloudService> lobbys = cloudServices.stream().filter(service -> service.getGroup().getPriority() == priorty).toList();
           return  lobbys.get(new Random().nextInt(lobbys.size()));
        }
    }

    public CloudService getLobby(ProxiedPlayer player, String kicked){
        if (CloudAPI.getInstance().getServicePool().getServices().isEmpty()){
            return null;
        }else if (CloudAPI.getInstance().getServicePool().getServices().stream().noneMatch(service -> service.getGroup().getGroupType().equals("LOBBY") && service.getState() == ServiceState.LOBBY)){
            return null;
        }
       final List<CloudService> services = CloudAPI.getInstance().getServicePool().getServices().stream()
                .filter(service -> service.getGroup().getGroupType().equals("LOBBY"))
                .filter(service -> {

                    if (!service.getGroup().isMaintenance()){
                        return true;
                    }else if (player.hasPermission("metacloud.bypass.connection.maintenance") || CloudAPI.getInstance().getWhitelist().contains(player.getName())){
                        return true;
                    }else return false;
                })
                .filter(service -> !service.getName().equals(kicked))
                .filter(service -> service.getState() == ServiceState.LOBBY)
                .filter(service -> service.getGroup().getPermission().equals("") || player.hasPermission(service.getGroup().getPermission())).toList();

        if (services.isEmpty()){
            return null;
        }
        final List<Integer> priority = new ArrayList<>();
        services.forEach( service -> priority.add(service.getGroup().getPriority()));
        priority.sort(Collections.reverseOrder());
        final  int priorty = priority.get(0);
        final List<CloudService> lobbys = services.stream().filter(service -> service.getGroup().getPriority() == priorty).toList();
        return  lobbys.size() == 0 ? null :  lobbys.get(new Random().nextInt(lobbys.size()));
    }
}
