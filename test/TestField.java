import static org.junit.Assert.*;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import model.Field;

import org.junit.Before;
import org.junit.Test;

import com.sun.j3d.utils.geometry.ColorCube;


public class TestField {

	private Field slices;
	
	@Before
	public void setUp() throws Exception {
		slices = new Field();
	}

	@Test
	public void testField() {
		
		int count = 0;
		slices = new Field();
		// Draw the slices for this field
		for (int z = 0; z < Field.SLICE_COUNT; z++) {
			for (int y = 0; y < Field.SLICE_COUNT; y++) {
				for (int x = 0; x < Field.SLICE_COUNT; x++) {
					if (slices.getCell(x, y, z) > 0) {
						count++;
					}
				}
			}
		}
		assert(count < Math.pow(Field.SLICE_COUNT, 3));
	}

	@Test
	public void testFieldField() {
		fail("Not yet implemented");
	}

	@Test
	public void testInternalSlices() {
		fail("Not yet implemented");
	}

	@Test
	public void testExternalSlices() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCell() {
		fail("Not yet implemented");
	}

	@Test
	public void testRequestRequiredSlices() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasRequiredSlices() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSlice() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateToCopy() {
		fail("Not yet implemented");
	}

}
