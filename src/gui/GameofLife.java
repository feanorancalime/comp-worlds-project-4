package gui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.media.j3d.*;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Sphere;
import model.Field;
import chord.Peer;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class GameofLife {
	
	// Time between generations in milliseconds.
	private static int PAUSE_RATE = 500;

    // Width of the extent in meters.
    private static float EXTENT_WIDTH = 16;

    // Brightness Steps for lifespan display.
    private static float BRIGHTNESS_STEPS = 16;
	
	// Starting distance of the camera from the game world.
	private static final double DISTANCE = 3d;
	
	// The model for the game.
	private Field slices;
	
	// Map of nodes to display so we don't have to keep recreating them.
	private Map<Vector3f, BranchGroup> cellMap;
    private Map<Vector3f, Sphere> sphereMap;
    private Map<Vector3f, Appearance> appearanceMap;
    private Map<Vector3f, ColoringAttributes> coloringMap;
	
	// This instance's peer object.
	private Peer p2p;
	
	// New game dialog.
	private NewGameDialog newGameDialog;
	
	// UI frame.
	private JFrame appFrame;

	// Main scene object.
	private BranchGroup scene;

    /**
	 * Main method.
	 * @param String args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GameofLife().createAndShowGUI();
			}
		});
	}

	private void tick() {
        System.out.println("updating");
		slices = slices.updateToCopy();
		// Draw the slices for this field
		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					Vector3f key = new Vector3f(x, y, z);
                    BranchGroup cellGroup = cellMap.get(key);
                    Sphere cell = sphereMap.get(key);
                    Appearance appearance = appearanceMap.get(key);
                    ColoringAttributes ca = coloringMap.get(key);
					if (slices.getCell(x, y, z) > 0) {
						// If the cell should be alive and is not already alive, cell is born
						if (scene.indexOfChild(cellGroup) == -1) {
							scene.addChild(cellGroup);
						}
                        ca.setColor(mapLifeToColor(slices.getCell(x,y,z)));
					} else {
						// Otherwise, cell dies
						cellGroup.detach();
					}
				}
			}
		}
	}

    /**
     * Maps a cell's lifespan to a color
     * @param cell_lifespan
     * @return
     */
    private Color3f mapLifeToColor(int cell_lifespan) {
        float brightness = cell_lifespan / BRIGHTNESS_STEPS;
        brightness = Math.min(1f,brightness);
        brightness = Math.max(0f,brightness);


        return new Color3f(brightness,brightness,brightness);
    }

    /*
      * Launches the UI for the game.
      */
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
		scene = new BranchGroup();
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
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
        
        // Map of cell objects
        cellMap = new HashMap<>();
        sphereMap = new HashMap<>();
        appearanceMap = new HashMap<>();
        coloringMap = new HashMap<>();

		for (int z = 0; z < EXTENT_WIDTH; z++) {
			for (int y = 0; y < EXTENT_WIDTH; y++) {
				for (int x = 0; x < EXTENT_WIDTH; x++) {
					TransformGroup sphereGroup = new TransformGroup();
					Sphere sphere = new Sphere(0.3f);
                    Appearance appearance = new Appearance();
                    ColoringAttributes ca = new ColoringAttributes(new Color3f(1,0,0),ColoringAttributes.FASTEST);
                    appearance.setColoringAttributes(ca);
                    sphere.setAppearance(appearance);
                    sphere.setCapability(Sphere.ENABLE_APPEARANCE_MODIFY);
                    ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);

					sphereGroup.addChild(sphere);
					Transform3D cubeTransform = new Transform3D();
					
					cubeTransform.setTranslation(new Vector3f(x, y, z));
					sphereGroup.setTransform(cubeTransform);
					// Added branchgroup to deal with exceptions
					BranchGroup bg = new BranchGroup();
                    bg.setCapability(BranchGroup.ALLOW_DETACH);
					bg.addChild(sphereGroup);
                    cellMap.put(new Vector3f(x, y, z), bg);
                    sphereMap.put(new Vector3f(x, y, z), sphere);
                    appearanceMap.put(new Vector3f(x, y, z), appearance);
                    coloringMap.put(new Vector3f(x, y, z), ca);
				}
			}
		}
		
		slices = new Field();

		simpleU.addBranchGraph(trueScene);

		appFrame = new JFrame("Physics Demo");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		appFrame.add(canvas3D);
        canvas3D.setPreferredSize(new Dimension(800,600));
        
        appFrame.setJMenuBar(buildMenuBar());
        
		appFrame.pack();
        appFrame.setLocationRelativeTo(null);
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
//			appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		new Timer(PAUSE_RATE, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas3D.stopRenderer();
				tick();
				canvas3D.startRenderer();
			}
		}).start();
		
		appFrame.setVisible(true);
	}
	
    /** Creates a slider **/
	private final JSlider buildSlider(int min, int max, int value, int spacing) {
		JSlider slider = new JSlider(min, max, value);
		slider.setMinorTickSpacing(spacing);
		slider.setPaintTicks(true);
		return slider;
	}

	/**
	 * Builds the menu bar.
	 * 
	 * @return A JMenuBar.
	 */
	private JMenuBar buildMenuBar() {
		final JMenuBar menuBar = new JMenuBar();
		// Build the menus
		final JMenu chordMenu = buildChordMenu("Chord",
				KeyEvent.VK_C);
		// Add the menus to the menu bar
		final JMenu newGameMenu = buildNewGameMenu("New Game...", KeyEvent.VK_N);
		menuBar.add(chordMenu);
		menuBar.add(newGameMenu);
		
		return menuBar;
	}

	private JMenu buildNewGameMenu(String string, int vkN) {
		return new JMenu(new NewGameAction(string));
	}

	private JMenu buildChordMenu(String string, int vkA) {
		return new JMenu(string);
	}
	
	/**
	 * Provides an action to add an icosahedron.
	 */
	@SuppressWarnings("serial")
	private class NewGameAction extends AbstractAction  {
		
		/**
		 * Constructs an action for the menu.
		 * 
		 * @param actionName
		 *            The name to be displayed on the menu.
		 */
		public NewGameAction(final String actionName) {
			super(actionName);
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_N);
		}
		
		/**
		 * Adds a icosahedron to the scene.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
			if (newGameDialog == null) {
				newGameDialog = new NewGameDialog(); 
			}
			newGameDialog.showDialog(appFrame, "New Game");
		}
	}
	
	@SuppressWarnings("serial")
	class NewGameDialog extends JPanel {

		// UI components.
		private JSlider sizeSlider;
		private JSlider timeSlider;
		private JButton startGameButton;
		private JButton cancelButton;
		private JDialog dialog;
		private boolean ok;

		public NewGameDialog() {
			JPanel optionsPanel = new JPanel();
			optionsPanel.setLayout(new GridLayout(2, 2));
			optionsPanel.add(new JLabel("World Size: "));
			sizeSlider = buildSlider(4, 32, 16, 2);
			optionsPanel.add(sizeSlider);
			optionsPanel.add(new JLabel("Time Between Generations: "));
			timeSlider = buildSlider(100, 1000, PAUSE_RATE, 100);
			optionsPanel.add(timeSlider);
			add(optionsPanel, BorderLayout.CENTER);
			
			startGameButton = new JButton("Create Game");
			startGameButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					EXTENT_WIDTH = sizeSlider.getValue();
					PAUSE_RATE = timeSlider.getValue();
					slices = new Field();
					ok = true;
					dialog.setVisible(false);
				}
			});
			
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.setVisible(false);
				}
			});

			JPanel buttonPanel = new JPanel();
			buttonPanel.add(startGameButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, BorderLayout.SOUTH);
		}
		
		public boolean showDialog(Component parent, String title) {
			ok = false;
			Frame owner = null;
			if (parent instanceof Frame) owner = (Frame) parent;
			else owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
			if (dialog == null || dialog.getOwner() != owner) {
				dialog = new JDialog(owner, true);
				dialog.add(this);
				dialog.getRootPane().setDefaultButton(cancelButton);
				dialog.pack();
			}
			dialog.setTitle(title);
			dialog.setVisible(true);
			return ok;
		}
	}
//
//	/**
//	 * Build the "Remove Shape" menu.
//	 * 
//	 * @param label
//	 *            The name displayed on the menu.
//	 * @param mnemonic
//	 *            The mnemonic key associated with this menu.
//	 * @return A JMenu.
//	 */
//	private JMenu buildRemoveShapeMenu(final String label, final int mnemonic) {
//		JMenu removeObjectMenu = new JMenu(label);
//		removeObjectMenu.setMnemonic(mnemonic);
//		removeObjectMenu.addMenuListener(new RemoveShapeMenuListener());
//		return removeObjectMenu;
//	}
//
//	/** Builds the control panel **/
//	private final JPanel buildControlPanel() {
//		// Basic panel setups
//		JPanel controlPanel = new JPanel();
//		JPanel checkBoxPanel = new JPanel();
//		JPanel sliderPanel = new JPanel();
//		
//		GridLayout radioButtonGrid = new GridLayout(0, 1);
//		GridLayout sliderGrid = new GridLayout(0, 3);
//		checkBoxPanel.setLayout(radioButtonGrid);
//		sliderPanel.setLayout(sliderGrid);
//		
//		controlPanel.add(checkBoxPanel, BorderLayout.EAST);
//		controlPanel.add(sliderPanel, BorderLayout.WEST);
//
//		// Add controls for forces
//		
//		// Add control for force field
//        JCheckBox forceFieldEnable = new JCheckBox();
//        forceFieldEnable.addItemListener(new ItemListener() {
//
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                JCheckBox source = (JCheckBox) e.getSource();
//                if (source.isSelected()) {
//                    setForceFieldEnabled(true);
//                } else {
//                    setForceFieldEnabled(false);
//                }
//            }
//        });
//     
//        forceFieldEnable.setText("Force Field");
//        forceFieldEnable.setSelected(true);
//        checkBoxPanel.add(forceFieldEnable);
//
//
//        for(final ForceBehavior fb : forceBehaviors) {
//        	// Checkboxes for behaviors
//            JCheckBox behaviorEnable = new JCheckBox();
//            behaviorEnable.addChangeListener(new ChangeListener() {
//				
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					JCheckBox source = (JCheckBox) e.getSource();
//					if (source.isSelected()) {
//						addBehavior(fb);
//					} else {
//						removeBehavior(fb);
//					}
//				}
//			});
//         
//            behaviorEnable.setText(fb.getName());
//            behaviorEnable.setSelected(true);
//            checkBoxPanel.add(behaviorEnable);
//            
//            // Sliders for magnitude of forces
//            final float max = fb.getForceMaximum();
//            final float min = fb.getForceMinimum();
//            final float cur = fb.getForceMagnitude();     
//            
//    		final JSlider forceMagnitudeSlider = new JSlider();
//            
//	        sliderPanel.add(new JLabel(fb.getName()));
//			sliderPanel.add(forceMagnitudeSlider);
//			final JLabel forceMagLabel = new JLabel("" + (int) cur);
//			sliderPanel.add(forceMagLabel);
//			
//			forceMagnitudeSlider.setMinorTickSpacing(1);
//            forceMagnitudeSlider.setMaximum((int) (max * 100));
//            forceMagnitudeSlider.setMinimum((int) (min * 100));
//            forceMagnitudeSlider.setValue((int) (cur * 100));
//            forceMagnitudeSlider.addChangeListener(new ChangeListener() {
//            	
//				ForceBehavior associatedBehavior = fb;
//				
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					JSlider slider = (JSlider) e.getSource();
//    				forceMagLabel.setText("" + Math.round(slider.getValue() / 100f));
//                    associatedBehavior.setForceMagnitude(slider.getValue() / 100f);
//                    forceField.resetMaxLength(); //clear the max length so it can adjust quickly
//                    System.out.printf("Slider Changed: Min (%02.2f) Max (%02.2f) Cur (%02.2f)\n", min, max, cur);
//				}
//			});
//            
//        }
//		
//    	JSlider coefficientOfRestitutionSlider = buildSlider(0, 100, (int)(coefficientOfRestitution*100));
//		sliderPanel.add(new JLabel("Coefficient of restitution"));
//		sliderPanel.add(coefficientOfRestitutionSlider);
//		final JLabel coefficientLabel = new JLabel("" + coefficientOfRestitutionSlider.getValue());
//		sliderPanel.add(coefficientLabel);
//		
//		ChangeListener coefficientListener = new ChangeListener() {
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				JSlider source = (JSlider) e.getSource();
//				coefficientOfRestitution = source.getValue()/100f;
//				coefficientLabel.setText("" + source.getValue() + "%");
//                //update the coefficients
//                for(CollisionBehavior cb : collisionBehaviors)
//                    cb.setCoefficientOfRestitution(coefficientOfRestitution);
//			}
//		};
//		coefficientOfRestitutionSlider.addChangeListener(coefficientListener);
//
//		return controlPanel;
//	}
//

}
