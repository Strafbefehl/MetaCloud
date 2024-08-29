package eu.metacloudservice.bungee.listener;

import eu.metacloudservice.CloudAPI;
import eu.metacloudservice.commands.translate.Translator;
import eu.metacloudservice.bungee.BungeeBootstrap;
import eu.metacloudservice.configuration.ConfigDriver;
import eu.metacloudservice.configuration.dummys.serviceconfig.LiveService;
import eu.metacloudservice.groups.dummy.Group;
import eu.metacloudservice.networking.packet.packets.in.service.playerbased.PacketInPlayerConnect;
import eu.metacloudservice.networking.packet.packets.in.service.playerbased.PacketInPlayerDisconnect;
import eu.metacloudservice.networking.packet.packets.in.service.playerbased.PacketInPlayerSwitchService;
import eu.metacloudservice.service.entrys.CloudService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.ArrayList;
import java.util.UUID;


public class CloudConnectListener implements Listener {


    private final ArrayList<UUID> connected = new ArrayList<>();

    public ServerInfo target;

    @EventHandler(priority = - 127)
    public void handle(final ServerConnectEvent event){
        if (this.connected.contains(event.getPlayer().getUniqueId())) {
            if (event.getPlayer().getServer() == null){
                target = ProxyServer.getInstance().getServerInfo(BungeeBootstrap.getInstance().getLobby(event.getPlayer()).getName());
                if (target != null){
                    event.setTarget(target);
                }else event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(final PostLoginEvent event) {
        LiveService service = (LiveService)(new ConfigDriver("./CLOUDSERVICE.json")).read(LiveService.class);
        Group group = CloudAPI.getInstance().getGroupPool().getGroup(service.getGroup());

        if (CloudAPI.getInstance().getPlayerPool().getPlayers().stream().anyMatch(cloudPlayer -> cloudPlayer.getUsername().equalsIgnoreCase(event.getPlayer().getName()))){
            event.getPlayer().disconnect(CloudAPI.getInstance().getMessages().getMessages().get("kickAlreadyOnNetwork").replace("&", "§"));
        }else {
            this.connected.add(event.getPlayer().getUniqueId());
            CloudAPI.getInstance().sendPacketAsynchronous(new PacketInPlayerConnect(event.getPlayer().getName(), service.getService()));

            if (group.isMaintenance()){

                if(ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()) != null && !ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).hasPermission("metacloud.bypass.connection.maintenance")
                        && !CloudAPI.getInstance().getWhitelist().contains(ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).getName())){
                    event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNetworkIsMaintenance"))))[0]);
                }else {
                    if (CloudAPI.getInstance().getPlayerPool().getPlayers().size() >= group.getMaxPlayers()
                            && !ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).hasPermission("metacloud.bypass.connection.full")
                            && !CloudAPI.getInstance().getWhitelist().contains(ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).getName())){
                        event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNetworkIsFull"))))[0]);

                    }else if (ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()) != null
                            && BungeeBootstrap.getInstance().getLobby( ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId())) == null){
                        event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNoFallback"))))[0]);

                    }
                }
            }else {
                if (CloudAPI.getInstance().getPlayerPool().getPlayers().size() >= group.getMaxPlayers()
                        && !ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).hasPermission("metacloud.bypass.connection.full")
                        && !CloudAPI.getInstance().getWhitelist().contains(ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).getName())){
                    event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNetworkIsFull"))))[0]);

                }else if (ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()) != null
                        && BungeeBootstrap.getInstance().getLobby( ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId())) == null){
                    event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNoFallback"))))[0]);

                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(final PlayerDisconnectEvent event) {
        if (this.connected.contains(event.getPlayer().getUniqueId())) {
            this.connected.remove(event.getPlayer().getUniqueId());
            CloudAPI.getInstance().sendPacketAsynchronous(new PacketInPlayerDisconnect(event.getPlayer().getName()));
        }
    }

    @EventHandler
    public void handle(ServerSwitchEvent event){
        if (this.connected.contains(event.getPlayer().getUniqueId())) {
            CloudAPI.getInstance().sendPacketAsynchronous(new PacketInPlayerSwitchService(event.getPlayer().getName(), event.getPlayer().getServer().getInfo().getName()));
        }
    }

    @EventHandler(priority = - 127)
    public void handle(final ServerKickEvent event) {

        if (CloudAPI.getInstance().getGroupPool().getGroup(CloudAPI.getInstance().getService().getGroup()).isMaintenance() && ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()) != null && !ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).hasPermission("metacloud.bypass.connection.maintenance")
                    && !CloudAPI.getInstance().getWhitelist().contains(ProxyServer.getInstance().getPlayer(event.getPlayer().getUniqueId()).getName())){
                event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNetworkIsMaintenance"))))[0]);

        }else {
            CloudService service = BungeeBootstrap.getInstance().getLobby(event.getPlayer(), event.getKickedFrom().getName());
            if (service == null) {
                event.setCancelled(false);
                event.setCancelServer(null);
                event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNoFallback"))))[0]);
            } else {

                if ((event.getKickReasonComponent()[0].toPlainText().startsWith("Outdated server! I'm still on") || event.getKickReasonComponent()[0].toPlainText().startsWith("Outdated client! Please use "))) {
                    if (CloudAPI.getInstance().getServicePool().getService(event.getKickedFrom().getName()).isTypeLobby()){
                        event.setCancelled(false);
                        event.setCancelServer(null);
                        event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("notTheRightVersion")
                                .replace("%current_service_version%", event.getKickReason().replace("Outdated server! I'm still on ", "").replace("Outdated client! Please use ", "")))))[0]);

                    }
                }else {

                    target = ProxyServer.getInstance().getServerInfo(service.getName());
                    if (target != null) {
                        event.setCancelServer(target);
                        event.setCancelled(true);
                    } else {
                        event.setCancelled(false);
                        event.setCancelServer(null);
                        event.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(MiniMessage.miniMessage().deserialize(new Translator().translate(CloudAPI.getInstance().getMessages().getMessages().get("kickNoFallback"))))[0]);

                    }
                }
            }
        }



    }
}