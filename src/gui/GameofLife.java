package gui;
import gui.FlyCam;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import model.Field;

public class GameofLife {
	// Physics updates per second (approximate).
	private static final int UPDATE_RATE = 30;
	// Number of full iterations of the collision detection and resolution system.
	private static final int COLLISION_ITERATIONS = 4;
	// Width of the extent in meters.
	private static final float EXTENT_WIDTH = 16;
	
	double DISTANCE = 3d;
	
	private Field slices;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GameofLife().createAndShowGUI();
			}
		});
	}

	public GameofLife() {

	}

	private void createAndShowGUI() {
		// Fix for background flickering on some platforms
		System.setProperty("sun.awt.noerasebackground", "true");

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		final Canvas3D canvas3D = new Canvas3D(config);
		SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.getViewer().getView().setSceneAntialiasingEnable(true);

		// Add a scaling transform that resizes the virtual world to fit
		// within the standard view frustum.
		BranchGroup trueScene = new BranchGroup();
		TransformGroup worldScaleTG = new TransformGroup();
		Transform3D t3D = new Transform3D();
		t3D.setScale(.9 / EXTENT_WIDTH);
		worldScaleTG.setTransform(t3D);
		trueScene.addChild(worldScaleTG);
		BranchGroup scene = new BranchGroup();
		scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		worldScaleTG.addChild(scene);
		
		// View movement
		Point3d focus = new Point3d();
        Point3d camera = new Point3d(1,1,1);
        Vector3d up = new Vector3d(0,1,0);
        TransformGroup lightTransform = new TransformGroup();
        TransformGroup curTransform = new TransformGroup();
        FlyCam fc = new FlyCam(simpleU.getViewingPlatform().getViewPlatformTransform(),focus,camera,up,DISTANCE, lightTransform, curTransform);
        fc.setSchedulingBounds(new BoundingSphere(new Point3d(),1000.0));
        scene.addChild(fc);
		
		slices = new Field();
		// Draw the slices for this field
		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					if (slices.getCell(x, y, z) > 0) {
						TransformGroup cubeGroup = new TransformGroup();
						ColorCube cube = new ColorCube(0.3f);
						cubeGroup.addChild(cube);
						Transform3D cubeTransform = new Transform3D();
						
						cubeTransform.setTranslation(new Vector3f(x, y, z));
//						cubeTransform.setScale(.9 / EXTENT_WIDTH);
						cubeGroup.setTransform(cubeTransform);

						
						scene.addChild(cubeGroup);
					}
				}
			}
		}
		simpleU.addBranchGraph(trueScene);

		JFrame appFrame = new JFrame("Physics Demo");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		appFrame.add(canvas3D);
        canvas3D.setPreferredSize(new Dimension(800,600));
		appFrame.pack();
        appFrame.setLocationRelativeTo(null);
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
//			appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		new Timer(1000 / UPDATE_RATE, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas3D.stopRenderer();
				tick();
				canvas3D.startRenderer();
			}
		}).start();
		
		appFrame.setVisible(true);
	}
	
	private void tick() {
		// Update goes here
	}
}
