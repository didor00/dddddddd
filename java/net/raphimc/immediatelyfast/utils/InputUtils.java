package net.raphimc.immediatelyfast.utils;

import net.minecraft.client.option.KeyBinding;

import static net.raphimc.immediatelyfast.Argon.mc;

/**
 * Utility class for managing player input and movement control.
 * Provides methods to lock and unlock player movement inputs.
 */
public final class InputUtils {
    
    private static boolean movementLocked = false;
    private static boolean[] savedKeyStates = new boolean[7];
    
    /**
     * Locks player movement by saving current key states and setting all
     * movement keys to unpressed. This prevents the player from moving
     * while still allowing the game to function normally.
     */
    public static void LockMovement() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        
        if (movementLocked) {
            return; // Already locked
        }
        
        // Save current states
        savedKeyStates[0] = mc.options.forwardKey.isPressed();
        savedKeyStates[1] = mc.options.backKey.isPressed();
        savedKeyStates[2] = mc.options.leftKey.isPressed();
        savedKeyStates[3] = mc.options.rightKey.isPressed();
        savedKeyStates[4] = mc.options.jumpKey.isPressed();
        savedKeyStates[5] = mc.options.sneakKey.isPressed();
        savedKeyStates[6] = mc.options.sprintKey.isPressed();
        
        // Set all movement keys to unpressed
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        
        movementLocked = true;
    }
    
    /**
     * Unlocks player movement by restoring the previously saved key states.
     * This should be called after LockMovement() to restore normal input.
     */
    public static void UnlockMovement() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        
        if (!movementLocked) {
            return; // Not locked
        }
        
        // Restore saved states
        mc.options.forwardKey.setPressed(savedKeyStates[0]);
        mc.options.backKey.setPressed(savedKeyStates[1]);
        mc.options.leftKey.setPressed(savedKeyStates[2]);
        mc.options.rightKey.setPressed(savedKeyStates[3]);
        mc.options.jumpKey.setPressed(savedKeyStates[4]);
        mc.options.sneakKey.setPressed(savedKeyStates[5]);
        mc.options.sprintKey.setPressed(savedKeyStates[6]);
        
        movementLocked = false;
    }
    
    /**
     * Checks if movement is currently locked.
     * 
     * @return true if movement is locked
     */
    public static boolean isMovementLocked() {
        return movementLocked;
    }
    
    /**
     * Forces all movement keys to be unpressed without saving states.
     * Use this for temporary input suppression.
     */
    public static void suppressMovement() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
    }
    
    /**
     * Checks if any movement key is currently pressed.
     * 
     * @return true if any movement key is pressed
     */
    public static boolean isMoving() {
        if (mc.player == null || mc.options == null) {
            return false;
        }
        
        return mc.options.forwardKey.isPressed() 
            || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed()
            || mc.options.rightKey.isPressed()
            || mc.options.jumpKey.isPressed()
            || mc.options.sneakKey.isPressed();
    }
    
    /**
     * Sets a specific key binding's pressed state.
     * 
     * @param key The key binding to modify
     * @param pressed The new pressed state
     */
    public static void setKeyPressed(KeyBinding key, boolean pressed) {
        if (key != null) {
            key.setPressed(pressed);
        }
    }
}
