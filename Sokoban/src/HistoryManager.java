/* Code for COMP103 - 2018T2, Assignment 1
 * Name: Matthew Corfiatis
 * Username: CorfiaMatt
 * ID: 300447277
 */
import java.util.Stack;

/**
 * Class to assist store and execute undo/redo actions.
 */
public class HistoryManager {
    private Sokoban sokobanInstance; //Instance of the Sokoban game to execute actions on.
    private int maxHistoryLength; //Maximum number of actions that will be stored for undo and redo.
    private Stack<ActionRecord> undoMemory = new Stack<>(); //Stack to store actions that can be undone
    private Stack<ActionRecord> redoMemory = new Stack<>(); //Stack to store actions that can be redone

    HistoryManager(Sokoban sokobanInstance, int maxHistoryLength) {
        this.sokobanInstance = sokobanInstance;
        this.maxHistoryLength = maxHistoryLength;
    }

    /**
     * Getter method for the maximum length of the history stack,
     * @return Integer value of maximum length of history.
     */
    public int getMaxHistoryLength() {
        return maxHistoryLength;
    }

    /**
     * Store an action so it can be undone in the future.
     * @param action The action to store in the undo history.
     */
    public void storeAction(ActionRecord action) {
        undoMemory.push(action);
        redoMemory.clear(); //Empty redo stack when a new action is added to the undo stack.
    }

    /**
     * Clear redo and undo history.
     */
    public void clearHistory()
    {
        redoMemory.clear();
        undoMemory.clear();
    }

    /**
     * Undoes a movement or push and puts the action on the redo stack
     * @return Boolean indicating whether there was anything to undo
     */
    public boolean undoAction() {
        if(undoMemory.size() == 0) return false; //If there are no items on the undo stack return false.
        ActionRecord record = undoMemory.pop();
        limitedStackPush(redoMemory, record); //Push record onto redo stack
        applyReverseAction(record);
        return true; //Undo success
    }

    /**
     * Redoes a movement or push and puts the action on the undo stack
     * @return Boolean indicating whether there was anything to redo
     */
    public boolean redoAction() {
        if(redoMemory.size() == 0) return false; //If there are no items on the redo stack return false.
        ActionRecord record = redoMemory.pop();
        limitedStackPush(undoMemory, record); //Push record onto undo stack
        applyAction(record);
        return true; //Redo success
    }

    /**
     * Pushes an ActionRecord object onto a stack and removes the last
     * element in the stack if the maximum history length has been reached.
     * @param stack The stack to push onto.
     * @param action The action to be pushed onto the stack.
     */
    private void limitedStackPush(Stack<ActionRecord> stack, ActionRecord action) {
        if(stack.size() >= maxHistoryLength) //Check if stack has reached item limit
            stack.remove(stack.size() - 1); //Remove last element on stack

        stack.push(action);
    }

    /**
     * Execute the reverse of the specified action in-game for the current Sokoban Instance.
     * E.g an action with the direction "up" will move the worker down.
     * @param action The action to be reversed and executed.
     */
    private void applyReverseAction(ActionRecord action) {
        String direction = sokobanInstance.opposite(action.direction()); //Get the opposite direction to the direction in the action object
        sokobanInstance.setWorkerDir(action.direction()); //Set the graphical direction of the worker
        if(action.isMove())
            sokobanInstance.move(direction);
        else
            sokobanInstance.pull(direction);
    }

    /**
     * Execute a recorded action in-game for the current Sokoban instance..
     * @param action The action to be executed
     */
    private void applyAction(ActionRecord action) {
        sokobanInstance.setWorkerDir(action.direction());
        if(action.isMove())
            sokobanInstance.move(action.direction());
        else
            sokobanInstance.push(action.direction());
    }
}