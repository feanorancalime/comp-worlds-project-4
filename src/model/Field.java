package model;

import chord.Peer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Field {
    public static final int SLICE_COUNT = 16;

    /**Map of Slice# to Slice data */
    public Map<Integer, Slice> slices = new HashMap<Integer,Slice>();
    Slice previous = null;
    Slice next = null;

    public int version = -1;

    public Field() {
        for(int i = 0; i < SLICE_COUNT; i++) {
            slices.put(i,new Slice(i,0));
        }

        this.version = 0;
    }

    public Field(Field other) {
        this.version = other.version;
//        for(Slice slice : other.slices.values()) {
//            this.slices.put(slice.number,slice.clone());
//        }
    }

    /**
     * The slices this Field manages.
     */
    public Map<Integer,Slice> internalSlices() {
        return slices;
    }

    /**
     * Get the state of a cell
     *
     * @param x first coord in slice
     * @param y second coord in slice
     * @param z slice number
     * @return -1 if not found, 0 if off, 1+ if on (ticks spent on)
     */
    public int getCell( int x , int y , int z ) {
        Slice slice = getSlice(z);
        if(slice != null)
            return slice.cells[x][y];
        else
            return -1;
    }

    /**
     * Get the specified slice
     * @param slice_num
     * @return null if the slice was not found, the slice otherwise. Slice may not be managed by this Field.
     */
    public Slice getSlice(int slice_num) {
        //if too low, return the previous slice (wrapping)
        if(slice_num < 0 && previous != null)
            return previous;
        else if(slice_num < 0)
            slice_num = (slice_num + SLICE_COUNT);

        //if too high, return the next slice (wrapping)
        if(slice_num >= SLICE_COUNT && next != null)
            return next;

        slice_num = slice_num % SLICE_COUNT;

        if(slices.containsKey(slice_num))
            return slices.get(slice_num);
        else
            return null;
    }

    /**
     * Updates the field to the next version. Does not modify original.
     * @return The updated field.
     */
    public Field updateToCopy() {
        Field field = new Field(this);
        field.version = this.version + 1; //returning next version

        for(Slice slice : this.slices.values()) {
            Slice prev = getSlice(slice.number - 1);
            Slice next = getSlice(slice.number + 1);
            if(prev == null || next == null)
                throw new IllegalStateException("Not all required slices were present.");
            field.slices.put(slice.number,slice.updateToCopy(prev, next));
        }

        return field;
    }

    /**
     * Is the passed slice_num an internally managed slice?
     */
    public boolean isInternalSlice(int slice_num) {
        return internalSlices().containsKey(slice_num);
    }

    public void setNext(Slice next) {
        this.next = next;
    }

    public void setPrev(Slice prev) {
        this.previous = prev;
    }

    public Slice getPrev() {
        return previous;
    }


    public Slice getNext() {
        return next;
    }
}
