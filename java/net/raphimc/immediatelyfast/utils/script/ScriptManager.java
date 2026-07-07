package net.raphimc.immediatelyfast.utils.script;

import net.raphimc.immediatelyfast.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages multiple ScriptTasks and processes events for them.
 * This allows for scheduling complex sequences of actions that execute
 * over multiple game ticks.
 */
public class ScriptManager {
    
    private final List<ScriptTask> tasks = new CopyOnWriteArrayList<>();
    
    /**
     * Adds a task to be managed and executed.
     * 
     * @param task The task to add
     */
    public void addTask(ScriptTask task) {
        if (task != null) {
            tasks.add(task);
        }
    }
    
    /**
     * Removes a task from management.
     * 
     * @param task The task to remove
     */
    public void removeTask(ScriptTask task) {
        tasks.remove(task);
    }
    
    /**
     * Processes an event by forwarding it to all active tasks.
     * Completed or cancelled tasks are automatically removed.
     * 
     * @param event The event to process
     */
    public void processEvent(Event<?> event) {
        List<ScriptTask> toRemove = new ArrayList<>();
        
        for (ScriptTask task : tasks) {
            // Process the event
            boolean stillActive = task.processEvent(event);
            
            // Mark completed or cancelled tasks for removal
            if (!stillActive || task.isCompleted() || task.isCancelled()) {
                toRemove.add(task);
            }
        }
        
        // Remove all completed tasks
        tasks.removeAll(toRemove);
    }
    
    /**
     * Convenience method for processing tick events.
     * This should be called in your tick listener.
     * 
     * @param event The tick event
     */
    public void tick(Event<?> event) {
        processEvent(event);
    }
    
    /**
     * Clears all tasks, cancelling any that are in progress.
     */
    public void clear() {
        for (ScriptTask task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }
    
    /**
     * Gets the number of active tasks.
     * 
     * @return The number of tasks currently managed
     */
    public int getTaskCount() {
        return tasks.size();
    }
    
    /**
     * Checks if there are any active tasks.
     * 
     * @return true if at least one task is active
     */
    public boolean hasTasks() {
        return !tasks.isEmpty();
    }
    
    /**
     * Gets a list of all active tasks.
     * 
     * @return A new list containing all active tasks
     */
    public List<ScriptTask> getTasks() {
        return new ArrayList<>(tasks);
    }
    
    /**
     * Cancels all tasks matching a specific event class.
     * 
     * @param eventClass The event class to match
     */
    public void cancelTasksForEvent(Class<?> eventClass) {
        for (ScriptTask task : tasks) {
            if (eventClass.equals(task.getCurrentEventClass())) {
                task.cancel();
            }
        }
    }
}
