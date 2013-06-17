package gui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import model.Field;
import chord.Peer;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import model.Slice;

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
	
	// UI frame.
	private JFrame appFrame;

	// Main scene object.
	private BranchGroup scene;

	// SimpleUniverse object.
	private SimpleUniverse simpleU;

	private Timer gameTimer;

	/**
	 * Main method.
	 * @param  args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new GameofLife().createAndShowGUI();
			}
		});
	}

    /**Updates the field**/
	private void tick() {
        System.out.println("updating");
		slices = slices.updateToCopy();
        //handle peers
        if(p2p != null) {
            p2p.sendSlices(slices); //wrap the slices

            //handle slice wrapping input
            List<Slice> received = p2p.getReceived();
            if(received != null) {
                for(Slice slice : received) {
                    //next
                    if(slice.getNumber() == 0) {
                        slices.setNext(slice);
                    } else { //prev
                        slices.setPrev(slice);
                    }
                }
            }
        }

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
                        if(slices.isInternalSlice(z))
                            ca.setColor(mapLifeToColor(slices.getCell(x,y,z))); //color depends on life duration
                        else
                            ca.setColor(new Color3f(0,1,0));
					} else {
						// Otherwise, cell dies
						cellGroup.detach();
					}
				}
			}
		}
	}


    /**
     * Maps a cell's lifespan to a color. For nodes we don't
     * @param cell_lifespan
     * @return
     */
    private Color3f mapOutsourcedLifeToColor(int cell_lifespan) {
        float brightness = cell_lifespan / BRIGHTNESS_STEPS;
        brightness = Math.min(1f,brightness);
        brightness = Math.max(0f,brightness);


        return new Color3f(brightness,0,0);
    }

    /**
     * Maps a cell's lifespan to a color. For nodes we control.
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
		
//		final JCanvas3D jCanvas3d = new JCanvas3D();
//		jCanvas3d.setPreferredSize(new Dimension(800,600));
//		jCanvas3d.setSize(new Dimension(800,600));
//		final Canvas3D canvas3D = jCanvas3d.getOffscreenCanvas3D();
		
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
		scene.setCapability(BranchGroup.ALLOW_DETACH);
		worldScaleTG.addChild(scene);
		
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		final Canvas3D canvas3D = new Canvas3D(config);
		simpleU = new SimpleUniverse(canvas3D);
		simpleU.getViewingPlatform().setNominalViewingTransform();
		simpleU.getViewer().getView().setSceneAntialiasingEnable(true);
		simpleU.addBranchGraph(trueScene);
		
		// View movement
		Point3d focus = new Point3d();
        Point3d camera = new Point3d(1,1,1);
        Vector3d up = new Vector3d(0,1,0);
        TransformGroup lightTransform = new TransformGroup();
        TransformGroup curTransform = new TransformGroup();
        FlyCam fc = new FlyCam(simpleU.getViewingPlatform().getViewPlatformTransform(),focus,camera,up,DISTANCE, lightTransform, curTransform);
        fc.setSchedulingBounds(new BoundingSphere(new Point3d(),1000.0));
        BranchGroup fcGroup = new BranchGroup();
        fcGroup.addChild(fc);
        scene.addChild(fcGroup);
		        
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
		p2p = new Peer();

		appFrame = new JFrame("Physics Demo");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        appFrame.add(jCanvas3d);
        appFrame.add(canvas3D, BorderLayout.CENTER);
		canvas3D.setPreferredSize(new Dimension(800,600));
        appFrame.setJMenuBar(buildMenuBar());
        appFrame.add(buildControlPanel(), BorderLayout.SOUTH);
        
		appFrame.pack();
        appFrame.setLocationRelativeTo(null);
//		if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
//			appFrame.setExtendedState(appFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		gameTimer = new Timer(PAUSE_RATE, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canvas3D.startRenderer();
				tick();
				canvas3D.startRenderer();
			}
		});
		gameTimer.start();
		
		appFrame.setVisible(true);
	}
	
    /** Creates a slider **/
	private final JSlider buildSlider(int min, int max, int value, int spacing) {
		JSlider slider = new JSlider(min, max, value);
		slider.setMinorTickSpacing(spacing);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
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
		final JMenu chordMenu = buildChordMenu("Chord", KeyEvent.VK_C);
		final JMenu newGameMenu = buildNewGameMenu("New Game", KeyEvent.VK_N);
		// Add the menus to the menu bar
		menuBar.add(chordMenu);
		menuBar.add(newGameMenu);
		
		return menuBar;
	}

	/**
	 * Builds the new game menu.
	 * @param label The label.
	 * @param vk The key mnemonic.
	 * @return A JMenu.
	 */
	private JMenu buildNewGameMenu(String label, int vk) {
		JMenu newGameMenu = new JMenu(label);
		newGameMenu.add(new JMenuItem(
				new NewGameAction("Settings...")));
		return newGameMenu;
	}

	/**
	 * Builds the new game menu.
	 * @param label The label.
	 * @param vk The key mnemonic.
	 * @return A JMenu.
	 */
	private JMenu buildChordMenu(String label, int vk) {
		JMenu chordMenu = new JMenu(label);
		chordMenu.add(new JMenuItem(
				new CreateNetworkAction("Create network...")));
		chordMenu.add(new JMenuItem(
				new PeerConnectAction("Connect to network...")));

		return chordMenu;
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
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_G);
		}
		
		/**
		 * Opens a new game dialog.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
			JSlider sizeSlider;
			JSlider timeSlider;
			
			sizeSlider = buildSlider(4, 32, 16, 2);
			timeSlider = buildSlider(100, 1000, PAUSE_RATE, 100);
			final JComponent[] inputs = new JComponent[] {
					new JLabel("World Size: "),
					sizeSlider,
					new JLabel("Time Between Generations (ms): "),
					timeSlider
			};
			int input = JOptionPane.showConfirmDialog(null, inputs, "Create New Game", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION) {
				EXTENT_WIDTH = sizeSlider.getValue();
				PAUSE_RATE = timeSlider.getValue();
				for (int z = 0; z < EXTENT_WIDTH; z++) {
					for (int y = 0; y < EXTENT_WIDTH; y++) {
						for (int x = 0; x < EXTENT_WIDTH; x++) {
							Vector3f key = new Vector3f(x, y, z);
							BranchGroup cellGroup = cellMap.get(key);
								cellGroup.detach();
							}
						}
					}
                Field temp = new Field();
                temp.setNext(slices.getNext());
                temp.setPrev(slices.getPrev());
				slices = new Field();
			}
		}
	}
	/**
	 * Provides an action to add an icosahedron.
	 */
	@SuppressWarnings("serial")
	private class PeerConnectAction extends AbstractAction  {
		
		/**
		 * Constructs an action for the menu.
		 * 
		 * @param actionName
		 *            The name to be displayed on the menu.
		 */
		public PeerConnectAction(final String actionName) {
			super(actionName);
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_O);
		}
		
		/**
		 * Opens a connect dialog.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
            JTextField ipAddress = new JTextField();
            JTextField chordId = new JTextField();
			
			final JComponent[] inputs = new JComponent[] {
					new JLabel("IP Address: "),
					ipAddress,
					new JLabel("Chord ID: "),
					chordId
			};
			int input = JOptionPane.showConfirmDialog(null, inputs, "Connect to Peer", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION) {
				InetAddress host = null;
				try {
					host = InetAddress.getByName(ipAddress.getText());
				} catch (UnknownHostException e) {
					System.out.println("Unable to reach host " + ipAddress.getText() +
							". Check that the hostname is correct and that the host is available.");
					e.printStackTrace();
				}
				long id = Integer.parseInt(chordId.getText());
				p2p.connectToNetwork(host, id);
			}
		}
	}
	
	/**
	 * Provides an action to add a peer.
	 */
	@SuppressWarnings("serial")
	private class CreateNetworkAction extends AbstractAction  {
		
		/**
		 * Constructs an action for the menu.
		 * 
		 * @param actionName
		 *            The name to be displayed on the menu.
		 */
		public CreateNetworkAction(final String actionName) {
			super(actionName);
			putValue(NewGameAction.MNEMONIC_KEY, KeyEvent.VK_R);
		}
		
		/**
		 * Opens a connect dialog.
		 * 
		 * @param event
		 *            The event which triggers the Action.
		 */
		@Override
		public void actionPerformed(final ActionEvent event) {
			JTextField chordId = new JTextField();
			
			final JComponent[] inputs = new JComponent[] {
					new JLabel("Chord ID: "),
					chordId
			};
			int input = JOptionPane.showConfirmDialog(null, inputs, "Create Network?", JOptionPane.YES_NO_OPTION);
			if (input == JOptionPane.YES_OPTION) {
				long id = Integer.parseInt(chordId.getText());
				p2p.createNetwork(id);
			}
		}
	}

	/** Builds the control panel **/
	private final JPanel buildControlPanel() {
		// Basic panel setups
		JPanel controlPanel = new JPanel();
		
		GridLayout buttonGrid = new GridLayout(0, 1);
		controlPanel.setLayout(buttonGrid);
		
		final JButton button = new JButton("Stop");
		controlPanel.add(button);
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (gameTimer.isRunning()) {
					gameTimer.stop();
					button.setText("Start");
				} else {
					gameTimer.start();
					button.setText("Stop");
				}
			}
		});
		return controlPanel;
	}
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

}
