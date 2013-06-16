package model;

import java.util.HashSet;
import java.util.Set;

/**
 * Models a Slice of data.
 */
public class Slice {
    private static final double CELL_ON_CHANCE = 0.60;
    private static int SIZE = 16;

    /**The version (iteration) number of this Slice**/
    int version = -1;
    /**The number (index) of this slice.**/
    int number = -1;
    /**The cells this slice contains**/
    public int[][] cells = new int[SIZE][SIZE]; //-1 = uninitialized; 0 = off; 1+ = on, for num iterations;
    /**The adjacency values on which we should change an empty cell to a filled cell**/
    static Set<Integer> birth = new HashSet<Integer>();
    /**The adjacency values on which we should keep an "on" cell "on"**/
    static Set<Integer> stay_alive = new HashSet<Integer>();
    static {
        //defaults
        birth.add(5);
        stay_alive.add(2);
        stay_alive.add(3);
        stay_alive.add(4);
        stay_alive.add(5);
        stay_alive.add(6);
        stay_alive.add(7);
        stay_alive.add(8);
    }

    /**Construct with defaults**/
    private Slice() {};

    /**Construct with version 0 and random cells**/
    public Slice(int number) {
        this(number,0);
    }

    /**Construct with specified version number and random cells.**/
    public Slice(int number, int version) {
        this.number = number;
        this.version = version;

        //initialize randomly
        for(int i = 0; i < cells.length; i++)
            for(int j = 0; j < cells[0].length; j++ )
                cells[i][j] = (Math.random()<CELL_ON_CHANCE?1:0);
    }

    /**Copy constructor**/
    public Slice(Slice other) {
        this.version = other.version;
        this.number = other.number;
        this.cells = other.cells.clone();
    }

    /**
     * Updates the slice. Does not modify original slice..
     * @param below the slice "below" this one
     * @param above the slice "above" this one
     * @return The next version of this slice.
     */
    public Slice updateToCopy(Slice below, Slice above) {
        Slice slice = new Slice();
        slice.version = this.version + 1;
        slice.number = this.number;

        for(int i = 0; i < cells.length; i++) {
            for(int j = 0; j < cells[0].length; j++) {
                int adjacent = getAdjacent(i,j,below,above);
                if(cells[i][j]==0 && birth.contains(adjacent)) {            //birth
                    slice.cells[i][j] = cells[i][j] + 1;
                } else if(cells[i][j]>0 && stay_alive.contains(adjacent)) { //stay alive
                    slice.cells[i][j] = cells[i][j] + 1;
                } else {                                                    //death
                    slice.cells[i][j] = 0;
                }
            }
        }
        return slice;
    }

    /**
     * Gets the adjacency count for a cell
     * @param x the x index of the cell
     * @param y the y index of the cell
     * @param below the slice "below" this one
     * @param above the slice "above" this one
     * @return the number of adjacent cells
     */
    public int getAdjacent(int x , int y, Slice below, Slice above) {
        int x_prev = (x+cells.length-1) % cells.length;
        int x_next = (x+1) % cells.length;
        int y_prev = (y+cells.length-1) % cells.length;
        int y_next = (y+1) % cells.length;
        int adjacent = 0;

        //below
        if(below.cells[x_prev][y_prev]>0)   adjacent++;
        if(below.cells[x_prev][y]>0)        adjacent++;
        if(below.cells[x_prev][y_next]>0)   adjacent++;

        if(below.cells[x][y_prev]>0)        adjacent++;
        if(below.cells[x][y]>0)             adjacent++;
        if(below.cells[x][y_next]>0)        adjacent++;

        if(below.cells[x_next][y_prev]>0)   adjacent++;
        if(below.cells[x_next][y]>0)        adjacent++;
        if(below.cells[x_next][y_next]>0)   adjacent++;

        //in-line
        if(this.cells[x_prev][y_prev]>0)   adjacent++;
        if(this.cells[x_prev][y]>0)        adjacent++;
        if(this.cells[x_prev][y_next]>0)   adjacent++;

        if(this.cells[x][y_prev]>0)        adjacent++;
        //if(this.cells[x][y]>0)             adjacent++; //this is the current cell, does not count as adjacent
        if(this.cells[x][y_next]>0)        adjacent++;

        if(this.cells[x_next][y_prev]>0)   adjacent++;
        if(this.cells[x_next][y]>0)        adjacent++;
        if(this.cells[x_next][y_next]>0)   adjacent++;

        //above
        if(above.cells[x_prev][y_prev]>0)   adjacent++;
        if(above.cells[x_prev][y]>0)        adjacent++;
        if(above.cells[x_prev][y_next]>0)   adjacent++;

        if(above.cells[x][y_prev]>0)        adjacent++;
        if(above.cells[x][y]>0)             adjacent++;
        if(above.cells[x][y_next]>0)        adjacent++;

        if(above.cells[x_next][y_prev]>0)   adjacent++;
        if(above.cells[x_next][y]>0)        adjacent++;
        if(above.cells[x_next][y_next]>0)   adjacent++;

        return adjacent;
    }
}
