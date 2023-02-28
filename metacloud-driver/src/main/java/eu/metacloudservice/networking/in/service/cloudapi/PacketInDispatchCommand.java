package eu.metacloudservice.networking.in.service;

import eu.metacloudservice.networking.packet.NettyBuffer;
import eu.metacloudservice.networking.packet.Packet;
import lombok.Getter;

public class PacketInDispatchCommand extends Packet {


    @Getter
    private String service;

    @Getter
    private String command;

    public PacketInDispatchCommand() {
        setPacketUUID(921234);
    }

    public PacketInDispatchCommand(String service, String command) {
        setPacketUUID(921234);
        this.service = service;
        this.command = command;
    }

    @Override
    public void readPacket(NettyBuffer buffer) {
        service = buffer.readString();
        command = buffer.readString();
    }

    @Override
    public void writePacket(NettyBuffer buffer) {

        buffer.writeString(service);
        buffer.writeString(command);
    }
}
