package eu.metacloudservice.manager.networking.service.playerbased;

import eu.metacloudservice.Driver;
import eu.metacloudservice.cloudplayer.CloudPlayerRestCache;
import eu.metacloudservice.configuration.ConfigDriver;
import eu.metacloudservice.configuration.dummys.managerconfig.ManagerConfig;
import eu.metacloudservice.events.listeners.CloudPlayerSwitchEvent;
import eu.metacloudservice.manager.CloudManager;
import eu.metacloudservice.networking.NettyDriver;
import eu.metacloudservice.networking.in.service.playerbased.PacketInPlayerSwitchService;
import eu.metacloudservice.networking.out.service.playerbased.PacketOutPlayerSwitchService;
import eu.metacloudservice.networking.packet.NettyAdaptor;
import eu.metacloudservice.networking.packet.Packet;
import eu.metacloudservice.storage.UUIDDriver;
import eu.metacloudservice.terminal.enums.Type;
import eu.metacloudservice.webserver.RestDriver;
import io.netty.channel.Channel;

public class HandlePacketInPlayerSwitchService implements NettyAdaptor {
    @Override
    public void handle(Channel channel, Packet packet) {
        if (packet instanceof PacketInPlayerSwitchService){
            if (!CloudManager.shutdown){
                CloudPlayerRestCache restCech = (CloudPlayerRestCache)(new RestDriver()).convert(Driver.getInstance().getWebServer().getRoute("/cloudplayer/" + UUIDDriver.getUUID(((PacketInPlayerSwitchService) packet).getName())), CloudPlayerRestCache.class);
                ManagerConfig config = (ManagerConfig)(new ConfigDriver("./service.json")).read(ManagerConfig.class);
                if (config.isShowConnectingPlayers()){
                    Driver.getInstance().getTerminalDriver().logSpeed(Type.NETWORK, "Der Spieler '"+ ((PacketInPlayerSwitchService) packet).getName() + "@" + restCech.getUuid() + "§f' hat sich mit dem Server '"+ ((PacketInPlayerSwitchService) packet).getServer() + "§r' verbunden",
                            "The player '"+ ((PacketInPlayerSwitchService) packet).getName()+ "@" + restCech.getUuid() + "§f' has connected to the '"+ ((PacketInPlayerSwitchService) packet).getServer()+ "§r' server");
                }
                if (!restCech.getCurrentService().equalsIgnoreCase("")){
                    CloudManager.serviceDriver.getService(restCech.getCurrentService()).handelCloudPlayerConnection(false);
                }

                String from = restCech.getCurrentService();
                CloudManager.eventDriver.executeEvent(new CloudPlayerSwitchEvent(((PacketInPlayerSwitchService) packet).getName(), UUIDDriver.getUUID(((PacketInPlayerSwitchService) packet).getName()), restCech.getCurrentService(), ((PacketInPlayerSwitchService) packet).getServer()));
                CloudManager.serviceDriver.getService(((PacketInPlayerSwitchService) packet).getServer()).handelCloudPlayerConnection(true);
                restCech.setCurrentService(((PacketInPlayerSwitchService) packet).getServer());
                Driver.getInstance().getWebServer().updateRoute("/cloudplayer/" + UUIDDriver.getUUID(((PacketInPlayerSwitchService) packet).getName()), (new RestDriver()).convert(restCech));
                NettyDriver.getInstance().nettyServer.sendToAllSynchronized(new PacketOutPlayerSwitchService(((PacketInPlayerSwitchService) packet).getName(), ((PacketInPlayerSwitchService) packet).getServer(), from));


            }
        }
    }
}
