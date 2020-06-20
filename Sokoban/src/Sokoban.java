// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2018T2, Assignment 1
 * Name: Matthew Corfiatis
 * Username: CorfiaMatt
 * ID: 300447277
 */

import ecs100.*;

import java.awt.*;
import java.util.*;
import java.io.*;
import java.util.List;

/** 
 * Sokoban
 */

public class Sokoban {

    public static final int[][] CROSS_OFFSETS = new int[][] { //Offsets for finding adjacent cells in a cross pattern.
            {0, -1}, //Up
            {1, 0}, //Right
            {0, 1}, //Down
            {-1, 0} //Left
    };

    public static final String[] CROSS_OFFSET_NAMES = new String[] { //Offset names for finding adjacent cells in a cross pattern.
            "up", "right", "down", "left"
    };

    public static final int LEFT_MARGIN = 40;
    public static final int TOP_MARGIN = 50;
    public static final int CELL_SIZE = 25;

    public static final int MAX_HISTORY_SIZE = 500; //Maximum length of undo and redo stacks for this game

    private Cell[][] cells;             // the array representing the warehouse
    private int rows;                   // the height of the warehouse
    private int cols;                   // the width of the warehouse
    private int level = 1;              // current level 

    private HistoryManager historyMan;  //History manager object for managing undo and redo actions
    private RouteHelper routeHelper;    //Helper object to find and execute routes between the worker and a point
    private Position workerPos;         // the position of the worker
    private String workerDir = "left";  // the direction the worker is facing



    /** 
     *  Constructor: set up the GUI, and load the 0th level.
     */
    public Sokoban() {
        historyMan = new HistoryManager(this, MAX_HISTORY_SIZE); //Initialize new historyMan manager using the current game as the game instance.
        routeHelper = new RouteHelper(this); //Initialize new route helper using the current game as the game instance.
        setupGUI();
        doLoad();
    }

    /**
     * Add the buttons and set the key listener.
     */
    public void setupGUI(){
        UI.addButton("New Level", () -> {level++; doLoad();});
        UI.addButton("Restart",   this::doLoad);
        UI.addButton("left",      () -> {moveOrPush("left");});
        UI.addButton("up",        () -> {moveOrPush("up");});
        UI.addButton("down",      () -> {moveOrPush("down");});
        UI.addButton("right",     () -> {moveOrPush("right");});
        UI.addButton("undo",      this::doUndoButton);
        UI.addButton("redo",      this::doRedoButton);
        UI.addButton("Quit",      UI::quit);

        UI.setKeyListener(this::doKey);
        UI.setMouseListener(this::doMouse);
        UI.setDivider(0.0);
    }


    /**
     * Get worker position for this game of Sokoban
     * @return Position of the worker
     */
    public Position getWorkerPos() {
        return workerPos;
    }

    /**
     * Callback for undo button
     */
    private void doUndoButton() {
        deselectAllCells();
        if(historyMan.undoAction()) //If undo successful
            UI.printMessage("Action undone.");
        else
            UI.printMessage("Nothing to undo!");
    }

    /**
     * Callback for redo button
     */
    private void doRedoButton() {
        deselectAllCells();
        if(historyMan.redoAction()) //If redo successful
            UI.printMessage("Action redone.");
        else
            UI.printMessage("Nothing to redo!");
    }

    /**
     * Callback for mouse action
     */
    private void doMouse(String action, double x, double y)
    {
        if(action.equals("pressed")) {
            routeHelper.moveWorker((int) x, (int) y); //Find route and move worker along it
        }
    }

    /**
     * Moves the player automatically to any selected cell.
     * Cells are selected by the route helper.
     */
    public void beginAutoMove()
    {
        while(true) //Keep moving player until there are no adjacent selected cells
        {
            boolean availableMove = false; //State to indicate if any cells are available
            for(int i = 0; i < 4; i++) //Loop to process each of the 4 positions adjacent to the last move
            {
                Position nextPosition = new Position(workerPos.row + Sokoban.CROSS_OFFSETS[i][1], workerPos.col + Sokoban.CROSS_OFFSETS[i][0]); //Calculate new position in the cross pattern
                Cell cell = getCell(nextPosition); //Get cell

                if(cell != null && cell.selected) //Ensure a valid cell was found
                {
                    availableMove = true; //A cell was available
                    cell.selected = false; //Deselect cell
                    drawCell(nextPosition); //Redraw the cell to display deselection
                    moveOrPush(CROSS_OFFSET_NAMES[i]); //Move
                    UI.sleep(150); //Wait 150ms
                }
            }
            if(!availableMove) //If no more moves, exit
                break;
        }
    }

    /**
     * Deselect all cells to cancel auto-move
     */
    public void deselectAllCells()
    {
        for(int i = 0; i < cells.length; i++) //Iterate over rows
        {
            for(int j = 0; j < cells[i].length; j++) //Iterate over cols
            {
                cells[i][j].selected = false; //Deselect
                drawCell(i, j); //Redraw cell
            }
        }
        drawWorker(); //Redraw the worker
    }

    /** 
     * Respond to key actions
     */
    public void doKey(String key) {
        deselectAllCells();
        key = key.toLowerCase();
        if (key.equals("i")|| key.equals("w") ||key.equals("up")) {
            moveOrPush("up");
        }
        else if (key.equals("k")|| key.equals("s") ||key.equals("down")) {
            moveOrPush("down");
        }
        else if (key.equals("j")|| key.equals("a") ||key.equals("left")) {
            moveOrPush("left");
        }
        else if (key.equals("l")|| key.equals("d") ||key.equals("right")) {
            moveOrPush("right");
        }
    }

    /**
     * Set the direction of the worker to change the worker graphic on the next redraw.
     * @param direction The new direction of the worker
     */
    public void setWorkerDir(String direction)
    {
        if(direction.equals("left") || direction.equals("right") || direction.equals("up") || direction.equals("down")) //Ensure direction is valid
            workerDir = direction;
    }

    /** 
     *  Moves the worker in the given direction, if possible.
     *  If there is box in front of the Worker and a space in front of the box,
     *  then push the box.
     *  Otherwise, if the worker can't move, do nothing.
     */
    public void moveOrPush(String direction) {
        workerDir = direction;                  // turn worker to face in this direction
        Position nextP = workerPos.next(direction);  // where the worker would move to
        Position nextNextP = nextP.next(direction);  // where a box would be pushed to

        // is there a box in that direction which can be pushed?
        if ( cells[nextP.row][nextP.col].hasBox() &&
             cells[nextNextP.row][nextNextP.col].isFree() ) {
            push(direction);

            if (isSolved()) { reportWin(); }
            historyMan.storeAction(new ActionRecord("push", direction)); //Store push action in undo history
        }
        // is the next cell free for the worker to move into?
        else if ( cells[nextP.row][nextP.col].isFree() ) { 
            move(direction);
            historyMan.storeAction(new ActionRecord("move", direction)); //Store move action in undo history
        }
    }

    /**
     * Moves the worker into the new position (guaranteed to be empty) 
     * @param direction the direction the worker is heading
     */
    public void move(String direction) {
        drawCell(workerPos);                   // redisplay cell under worker
        workerPos = workerPos.next(direction); // put worker in new position
        drawWorker();                          // display worker at new position

        Trace.println("Move " + direction);    // for debugging
    }

    
    /**
     * Push: Moves the Worker, pushing the box one step 
     *  @param direction the direction the worker is heading
     */
    public void push(String direction) {
        Position boxPos = workerPos.next(direction);   // where box is
        Position newBoxPos = boxPos.next(direction);   // where box will go

        cells[boxPos.row][boxPos.col].removeBox();     // remove box from current cell
        cells[newBoxPos.row][newBoxPos.col].addBox();  // place box in its new position

        drawCell(workerPos);                           // redisplay cell under worker
        drawCell(boxPos);                              // redisplay cell without the box
        drawCell(newBoxPos);                           // redisplay cell with the box
     
        workerPos = boxPos;                            // put worker in new position
        drawWorker();                                  // display worker at new position

        Trace.println("Push " + direction);   // for debugging
    }


    /**
     * Pull: (could be useful for undoing a push)
     *  move the Worker in the direction,
     *  pull the box into the Worker's old position
     */
    public void pull(String direction) {
        Position boxPos = workerPos.next(opposite(direction));   // where box is
        Position newBoxPos = boxPos.next(direction);   // where box will go

        cells[boxPos.row][boxPos.col].removeBox();     // remove box from current cell
        cells[newBoxPos.row][newBoxPos.col].addBox();  // place box in its new position

        drawCell(workerPos);                           // redisplay cell under worker
        drawCell(boxPos);                              // redisplay cell without the box
        drawCell(newBoxPos);                           // redisplay cell with the box

        workerPos = workerPos.next(direction);                            // put worker in new position
        drawWorker();                                  // display worker at new position

        Trace.println("Pull " + direction);   // for debugging
    }

    /**
     * Get cell at specified at position in the table
     * @param row Row of the cell to select
     * @param column Column in the row to select
     * @return Null or the requested cell
     */
    public Cell getCell(int row, int column) {
        try {
            return cells[row][column];
        }
        catch (IndexOutOfBoundsException ex) //If cell index is invalid
        {
            return null;
        }
    }

    /**
     * Overload for getCell, uses position object instead of co-ordinates
     * @param position Position of cell to get
     * @return Cell or null
     */
    public Cell getCell(Position position) {
        return getCell(position.row, position.col);
    }

    /**
     * Report a win by flickering the cells with boxes
     */
    public void reportWin(){
        for (int i=0; i<12; i++) {
            for (int row=0; row<cells.length; row++)
                for (int column=0; column<cells[row].length; column++) {
                    Cell cell=cells[row][column];

                    // toggle shelf cells
                    if (cell.hasBox()) {
                        cell.removeBox();
                        drawCell(row, column);
                    }
                    else if (cell.isEmptyShelf()) {
                        cell.addBox();
                        drawCell(row, column);
                    }
                }

            UI.sleep(100);
        }
    }
    
    /** 
     *  Returns true if the warehouse is solved, 
     *  i.e., all the shelves have boxes on them 
     */
    public boolean isSolved() {
        for(int row = 0; row<cells.length; row++) {
            for(int col = 0; col<cells[row].length; col++)
                if(cells[row][col].isEmptyShelf())
                    return  false;
        }

        return true;
    }

    /** 
     * Returns the direction that is opposite of the parameter
     * useful for undoing!
     */
    public String opposite(String direction) {
        if ( direction.equals("right")) return "left";
        if ( direction.equals("left"))  return "right";
        if ( direction.equals("up"))    return "down";
        if ( direction.equals("down"))  return "up";
        throw new RuntimeException("Invalid  direction");
    }





    // Drawing the warehouse



    /**
     * Draw the grid of cells on the screen, and the Worker 
     */
    public void drawWarehouse() {
        UI.clearGraphics();
        // draw cells
        for(int row = 0; row<cells.length; row++)
            for(int col = 0; col<cells[row].length; col++)
                drawCell(row, col);

        drawWorker();

        UI.setColor(Color.black);
        UI.drawString("Click any valid cell to auto-move.", 30,30);
    }

    /**
     * Draw the cell at a given position
     */
    public void drawCell(Position pos) {
        drawCell(pos.row, pos.col);
    }

    /**
     * Draw the cell at a given row,col
     */
    public void drawCell(int row, int col) {
        double left = LEFT_MARGIN+(CELL_SIZE* col);
        double top = TOP_MARGIN+(CELL_SIZE* row);
        cells[row][col].draw(left, top, CELL_SIZE);
    }

    /**
     * Draw the worker at its current position.
     */
    private void drawWorker() {
        double left = LEFT_MARGIN+(CELL_SIZE* workerPos.col);
        double top = TOP_MARGIN+(CELL_SIZE* workerPos.row);
        UI.drawImage("worker-"+workerDir+".gif",
                     left, top, CELL_SIZE,CELL_SIZE);
    }



    /**
     * Load a grid of cells (and Worker position) for the current level from a file
     */
    public void doLoad() {
        historyMan.clearHistory();
        File f = new File("warehouse" + level + ".txt");

        if (! f.exists()) {
            UI.printMessage("Run out of levels!");
            level--;
        }
        else {
            List<String> lines = new ArrayList<String>();
            try {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()){
                    lines.add(sc.nextLine());
                }
                sc.close();
            } catch(IOException e) {UI.println("File error: " + e);}

            int rows = lines.size();
            cells = new Cell[rows][];

            for(int row = 0; row < rows; row++) {
                String line = lines.get(row);
                int cols = line.length();
                cells[row]= new Cell[cols];
                for(int col = 0; col < cols; col++) {
                    char ch = line.charAt(col);
                    if (ch=='w'){
                        cells[row][col] = new Cell("empty");
                        workerPos = new Position(row,col);
                    }
                    else if (ch=='.') cells[row][col] = new Cell("empty");
                    else if (ch=='#') cells[row][col] = new Cell("wall");
                    else if (ch=='s') cells[row][col] = new Cell("shelf");
                    else if (ch=='b') cells[row][col] = new Cell("box");
                    else {
                        throw new RuntimeException("Invalid char at "+row+","+col+"="+ch);
                    }
                }
            }
            drawWarehouse();
            UI.printMessage("Level "+level+": Push the boxes to their target positions. Use buttons or put mouse over warehouse and use keys (arrows, wasd, ijkl, u)");
        }
    }

    public static void main(String[] args) {
        new Sokoban();
    }
}
