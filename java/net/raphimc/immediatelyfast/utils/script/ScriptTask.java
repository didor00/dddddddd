package net.raphimc.immediatelyfast.utils.script;

import net.raphimc.immediatelyfast.event.Event;
import net.raphimc.immediatelyfast.event.Listener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * Represents a task that can be scheduled to execute in sequence.
 * Each step in the task is executed when a specific event type fires,
 * and the predicate determines when to move to the next step.
 */
public class ScriptTask {
    
    private final Queue<TaskStep<?>> steps = new LinkedList<>();
    private TaskStep<?> currentStep = null;
    private boolean completed = false;
    private boolean cancelled = false;
    
    /**
     * Schedules a step to execute on a specific event type.
     * The predicate should return true when the step is complete and
     * the task should move to the next step.
     * 
     * @param eventClass The event class to listen for
     * @param predicate The predicate that determines when this step is complete
     * @param <E> The event type
     * @return This task for chaining
     */
    public <E extends Event<?>> ScriptTask schedule(Class<E> eventClass, Predicate<E> predicate) {
        steps.add(new TaskStep<>(eventClass, predicate));
        return this;
    }
    
    /**
     * Schedules a step to execute after a specific number of ticks.
     * 
     * @param eventClass The event class to listen for (typically PlayerTickEvent)
     * @param ticks The number of ticks to wait
     * @param <E> The event type
     * @return This task for chaining
     */
    public <E extends Event<?>> ScriptTask scheduleDelay(Class<E> eventClass, int ticks) {
        final int[] counter = {0};
        return schedule(eventClass, e -> {
            counter[0]++;
            return counter[0] >= ticks;
        });
    }
    
    /**
     * Processes an event and advances the task if applicable.
     * 
     * @param event The event to process
     * @return true if the task is still active, false if completed or cancelled
     */
    public boolean processEvent(Event<?> event) {
        if (completed || cancelled) {
            return false;
        }
        
        // Start first step if not started
        if (currentStep == null) {
            currentStep = steps.poll();
            if (currentStep == null) {
                completed = true;
                return false;
            }
        }
        
        // Check if this event matches current step
        if (currentStep.eventClass.isInstance(event)) {
            try {
                @SuppressWarnings("unchecked")
                TaskStep<Event<?>> step = (TaskStep<Event<?>>) currentStep;
                
                // Execute predicate
                if (step.predicate.test(event)) {
                    // Move to next step
                    currentStep = steps.poll();
                    if (currentStep == null) {
                        completed = true;
                        return false;
                    }
                }
            } catch (Exception e) {
                // Error in predicate execution
                cancel();
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if the task is completed.
     * 
     * @return true if all steps have been executed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Checks if the task has been cancelled.
     * 
     * @return true if the task was cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Cancels the task, preventing any further execution.
     */
    public void cancel() {
        cancelled = true;
        steps.clear();
        currentStep = null;
    }
    
    /**
     * Gets the event class that the current step is waiting for.
     * 
     * @return The event class, or null if no current step
     */
    public Class<?> getCurrentEventClass() {
        return currentStep != null ? currentStep.eventClass : null;
    }
    
    /**
     * Internal class representing a single step in the task.
     */
    private static class TaskStep<E extends Event<?>> {
        final Class<E> eventClass;
        final Predicate<E> predicate;
        
        TaskStep(Class<E> eventClass, Predicate<E> predicate) {
            this.eventClass = eventClass;
            this.predicate = predicate;
        }
    }
}
