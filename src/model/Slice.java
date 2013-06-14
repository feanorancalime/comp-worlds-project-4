package model;

import java.util.HashSet;
import java.util.Set;

/**
 * Models a Slice of data.
 */
public class Slice {
    private static final double CELL_ON_CHANCE = 0.60;

    int version = -1;
    int number = -1;
    public int[][] cells = new int[1024][1024]; //-1 = uninitialized; 0 = off; 1+ = on, for num iterations;
    static Set<Integer> birth = new HashSet<Integer>();
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

    private Slice() {};

    public Slice(int number) {
        this(number,0);
    }

    public Slice(int number, int version) {
        this.number = number;
        this.version = version;

        //initialize randomly
        for(int i = 0; i < cells.length; i++)
            for(int j = 0; j < cells[0].length; j++ )
                cells[i][j] = (Math.random()<CELL_ON_CHANCE?1:0);
    }

    public Slice(Slice other) {
        this.version = other.version;
        this.number = other.number;
        this.cells = other.cells.clone();
    }

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
        //if(this.cells[x][y]>0)             adjacent++;
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
