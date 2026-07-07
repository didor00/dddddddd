package net.raphimc.immediatelyfast.utils;

import net.minecraft.network.packet.Packet;

import static net.raphimc.immediatelyfast.Argon.mc;

/**
 * Utility class for network packet operations.
 * Provides methods for sending packets without triggering event listeners.
 */
public final class NetworkUtils {
    
    private static boolean silentPacket = false;
    
    /**
     * Sends a packet silently without triggering PacketSendListener events.
     * This is useful when you need to send packets programmatically without
     * having other modules intercept or cancel them.
     * 
     * @param packet The packet to send
     */
    public static void sendSilentPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        
        silentPacket = true;
        mc.getNetworkHandler().sendPacket(packet);
        silentPacket = false;
    }
    
    /**
     * Sends a regular packet through the network handler.
     * This will trigger PacketSendListener events normally.
     * 
     * @param packet The packet to send
     */
    public static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        
        mc.getNetworkHandler().sendPacket(packet);
    }
    
    /**
     * Checks if the current packet being sent is a silent packet.
     * This is used internally by the event system to avoid triggering
     * listeners for silent packets.
     * 
     * @return true if currently sending a silent packet
     */
    public static boolean isSilentPacket() {
        return silentPacket;
    }
    
    /**
     * Checks if the network handler is available and connected.
     * 
     * @return true if network handler exists and connection is valid
     */
    public static boolean isConnected() {
        return mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null;
    }
}
