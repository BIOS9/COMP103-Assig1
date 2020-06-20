/* Code for COMP103 - 2018T2, Assignment 1
 * Name: Matthew Corfiatis
 * Username: CorfiaMatt
 * ID: 300447277
 */

import ecs100.UI;
import java.util.*;

/**
 * Class to help find routes and move the worker.
 */
public class RouteHelper {
    private Sokoban sokobanInstance; //Instance of the game to run actions against and get positions from.

    RouteHelper(Sokoban sokobanInstance)
    {
        this.sokobanInstance = sokobanInstance;
    }

    /**
     * Attempts to find a route to the specified position and move the worker along it.
     * @param x X position of where to move worker to.
     * @param y Y position of where to move worker to.
     * @return Success indicator. False indicates no route found.
     */
    public boolean moveWorker(int x, int y)
    {
        UI.printMessage("Finding route...");
        int row = (y - Sokoban.TOP_MARGIN) / Sokoban.CELL_SIZE; //Calculate cell row
        int col = (x - Sokoban.LEFT_MARGIN) / Sokoban.CELL_SIZE; //Calculate cell column
        Position position = new Position(row, col);
        Cell cell = sokobanInstance.getCell(position); //Get cell

        if(cell == null || !cell.isFree()) //Ensure a valid cell was found
        {
            UI.printMessage("Invalid destination!");
            return false;
        }

        ArrayList<Position> route = findRoute(sokobanInstance.getWorkerPos(), position); //Find shortest route if it exists
        if(route == null) { //If no route
            UI.printMessage("No route to destination!");
            return false;
        }

        UI.printMessage("Route found!");

        for(int i = 1; i < route.size(); i++) //Select all positions except first position in route.
        {
            Position pos = route.get(i); //Get cell
            sokobanInstance.getCell(pos).selected = true; //Select cell
            sokobanInstance.drawCell(pos); //Redraw cell to show selection
        }

        sokobanInstance.beginAutoMove(); //Begin moving the worker along the route

        return true;
    }

    /**
     * Core route finding algorithm. This is an implementation of Dijkstra's algorithm.
     * @param start The starting position of the route
     * @param end The desired end position of the route
     * @return Null if no route, or the set of positions that make up the shortest route.
     */
    private ArrayList<Position> findRoute(Position start, Position end)
    {
        PriorityQueue<ArrayList<Position>> dijkstraRoutes = new PriorityQueue<>(Comparator.comparing(ArrayList::size)); //Map of routes. Key is route cost/distance, value is the path.

        if(start.equals(end)) //Ensure that the start position doesn't equal the end position
            return null;

        ArrayList<Position> routeBegin = new ArrayList<>(); //Begin the first route with the starting position.
        routeBegin.add(start); //Add start pos
        dijkstraRoutes.add(routeBegin); //Add start route to route list

        while (dijkstraRoutes.size() > 0) //Main loop to calculate optimal path
        {
            ArrayList<Position> route = dijkstraRoutes.poll(); //Get route with lowest distance/cost
            if(route == null) //Ensure route exists and the route list is not empty
                return null; //No route to target

            Position lastMove = route.get(route.size() - 1); //Get the last move in the route

            /*
                Cross pattern that the following for loop iterates over:
                0  #  0
                #  0  #
                0  #  0
             */
            for(int i = 0; i < 4; i++) //Loop to process each of the 4 positions adjacent to the last move
            {
                Position nextPosition = new Position(lastMove.row + Sokoban.CROSS_OFFSETS[i][1], lastMove.col + Sokoban.CROSS_OFFSETS[i][0]); //Calculate new position in the cross pattern

                if(route.contains(nextPosition) || nextPosition.equals(start))
                    continue; //Skip cells that are already in the route

                Cell cell = sokobanInstance.getCell(nextPosition); //Get the cell at the position in the cross
                if(cell != null && cell.isFree()) //Check if the cell is valid and free to be moved into
                {
                    ArrayList<Position> routeCopy = new ArrayList<>(route); //Clone route to avoid immutability issues
                    routeCopy.add(nextPosition); //Add the new move
                    dijkstraRoutes.add(routeCopy); //Add the route back to the queue
                }
            }

            if(route.get(route.size() - 1).equals(end)) //If route has reached the end
                return route;
        }
        return null; //Queue had no items so loop ended. No route to target.
    }
}
