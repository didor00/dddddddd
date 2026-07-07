package net.raphimc.immediatelyfast.module.modules.misc;

import net.raphimc.immediatelyfast.event.events.PacketSendListener;
import net.raphimc.immediatelyfast.event.events.PlayerTickListener;
import net.raphimc.immediatelyfast.gui.ClickGui;
import net.raphimc.immediatelyfast.module.Category;
import net.raphimc.immediatelyfast.module.Module;
import net.raphimc.immediatelyfast.module.setting.ModeSetting;
import net.raphimc.immediatelyfast.utils.EncryptedString;
import net.raphimc.immediatelyfast.utils.InputUtils;
import net.raphimc.immediatelyfast.utils.NetworkUtils;
import net.raphimc.immediatelyfast.utils.script.ScriptManager;
import net.raphimc.immediatelyfast.utils.script.ScriptTask;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GuiMove extends Module implements PlayerTickListener, PacketSendListener {
    
    public enum Mode {
        DEFAULT,
        BYPASS,
        LEGIT
    }
    
    private final ModeSetting<Mode> mode = new ModeSetting<>(
        EncryptedString.of("Mode"), 
        Mode.DEFAULT,
        Mode.class
    );
    
    private final List<Packet<?>> delayedPackets = new CopyOnWriteArrayList<>();
    private final ScriptManager scriptManager = new ScriptManager();
    private boolean processingPackets = false;
    private boolean movedInGui = false;
    
    public GuiMove() {
        super(
            EncryptedString.of("GuiMove"),
            EncryptedString.of("Allows you to move while in inventory"),
            -1,
            Category.MISC
        );
        addSettings(mode);
    }
    
    @Override
    public void onEnable() {
        eventManager.add(PlayerTickListener.class, this);
        eventManager.add(PacketSendListener.class, this);
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        eventManager.remove(PlayerTickListener.class, this);
        eventManager.remove(PacketSendListener.class, this);
        cleanup();
        super.onDisable();
    }
    
    @Override
    public void onPlayerTick() {
        if (mc.player == null || mc.world == null) {
            cleanup();
            return;
        }
        
        scriptManager.tick(new PlayerTickListener.PlayerTickEvent());
        
        if (!(mc.currentScreen instanceof InventoryScreen 
            || mc.currentScreen instanceof ClickGui 
            || mc.currentScreen instanceof CreativeInventoryScreen)) {
            if (!processingPackets && delayedPackets.isEmpty()) {
                movedInGui = false;
            }
            return;
        }
        
        movedInGui |= movementKeysDown() && !delayedPackets.isEmpty();
        
        // Allow movement in GUI
        for (KeyBinding binding : getMovementKeys(false)) {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), binding.getDefaultKey().getCode())) {
                binding.setPressed(true);
            }
        }
    }
    
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (mc.player == null || mode.isMode(Mode.DEFAULT)) {
            return;
        }
        
        boolean moving = movedInGui || movementKeysDown();
        movedInGui |= moving && !delayedPackets.isEmpty();
        
        // Intercept ClickSlot packets when moving in inventory
        if (event.packet instanceof ClickSlotC2SPacket clickPacket
            && mc.currentScreen instanceof InventoryScreen
            && moving && shouldAllowMovement()) {
            
            delayedPackets.add(clickPacket);
            event.cancel();
            
        } else if (event.packet instanceof CloseHandledScreenC2SPacket closePacket
            && closePacket.getSyncId() == 0 && moving && !processingPackets) {
            
            if (delayedPackets.isEmpty()) {
                event.cancel();
            } else {
                delayedPackets.add(closePacket);
                event.cancel();
                processDelayedPackets();
            }
        }
        
        // Suppress player input packets during processing
        if (processingPackets && event.packet instanceof PlayerInputC2SPacket) {
            event.cancel();
            NetworkUtils.sendSilentPacket(
                new PlayerInputC2SPacket(
                    new net.minecraft.util.PlayerInput(false, false, false, false, false, false, false)
                )
            );
        }
        
        // Block interaction packets during processing
        if (!delayedPackets.isEmpty() && processingPackets) {
            Packet<?> p = event.packet;
            if (p instanceof HandSwingC2SPacket
                || p instanceof PlayerInteractEntityC2SPacket
                || p instanceof PlayerInteractItemC2SPacket
                || p instanceof PlayerInteractBlockC2SPacket) {
                event.cancel();
            }
        }
    }
    
    private void processDelayedPackets() {
        processingPackets = true;
        ScriptTask task = new ScriptTask();
        scriptManager.addTask(task);
        
        if (mode.isMode(Mode.BYPASS)) {
            // Bypass mode - fast processing with movement lock
            task.schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                InputUtils.LockMovement();
                return true;
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                // Send all packets at once
                for (Packet<?> p : delayedPackets) {
                    NetworkUtils.sendSilentPacket(p);
                }
                delayedPackets.clear();
                processingPackets = false;
                movedInGui = false;
                return true;
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                // Restore close packet if needed
                for (Packet<?> p : delayedPackets) {
                    if (p instanceof CloseHandledScreenC2SPacket) {
                        NetworkUtils.sendSilentPacket(p);
                    }
                }
                InputUtils.UnlockMovement();
                return true;
            });
            
        } else {
            // Legit mode - slower processing with delays to bypass anticheats
            task.schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                InputUtils.LockMovement();
                return true;
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                return true; // Wait 1 tick
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                return true; // Wait 2 ticks
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                // Send non-close packets
                for (Packet<?> p : delayedPackets) {
                    if (!(p instanceof CloseHandledScreenC2SPacket)) {
                        NetworkUtils.sendSilentPacket(p);
                    }
                }
                return true;
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                return true; // Wait 1 tick
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                // Send close packets
                for (Packet<?> p : delayedPackets) {
                    if (p instanceof CloseHandledScreenC2SPacket) {
                        NetworkUtils.sendSilentPacket(p);
                    }
                }
                delayedPackets.clear();
                return true;
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                return true; // Wait 1 tick
            }).schedule(PlayerTickListener.PlayerTickEvent.class, e -> {
                InputUtils.UnlockMovement();
                processingPackets = false;
                movedInGui = false;
                return true;
            });
        }
    }
    
    private boolean movementKeysDown() {
        if (mc == null || mc.getWindow() == null || mc.options == null) {
            return false;
        }
        
        boolean inventory = mc.currentScreen instanceof InventoryScreen 
                         || mc.currentScreen instanceof CreativeInventoryScreen;
        
        for (KeyBinding binding : getMovementKeys(true)) {
            // Skip sneak and sprint in inventory (they have special handling)
            if (inventory && (binding == mc.options.sneakKey
                || (binding == mc.options.sprintKey 
                    && !mc.options.forwardKey.equals(mc.options.sprintKey)))) {
                continue;
            }
            
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), binding.getDefaultKey().getCode())) {
                return true;
            }
        }
        
        return false;
    }
    
    private KeyBinding[] getMovementKeys(boolean includeModifiers) {
        return includeModifiers
            ? new KeyBinding[]{
                mc.options.forwardKey, 
                mc.options.backKey, 
                mc.options.rightKey, 
                mc.options.leftKey, 
                mc.options.jumpKey, 
                mc.options.sneakKey, 
                mc.options.sprintKey
            }
            : new KeyBinding[]{
                mc.options.forwardKey, 
                mc.options.backKey, 
                mc.options.rightKey, 
                mc.options.leftKey, 
                mc.options.jumpKey
            };
    }
    
    private boolean shouldAllowMovement() {
        return mc.player != null 
            && mc.player.currentScreenHandler != null
            && mc.player.currentScreenHandler.slots.size() >= 27;
    }
    
    private void cleanup() {
        delayedPackets.clear();
        processingPackets = false;
        movedInGui = false;
        InputUtils.UnlockMovement();
        scriptManager.clear();
    }
}
