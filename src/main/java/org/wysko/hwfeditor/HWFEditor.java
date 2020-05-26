/*
 * License: GPL. For details, see LICENSE file.
 */

package org.wysko.hwfeditor;


import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;

/*
 * Created by JFormDesigner on Mon May 18 23:39:32 EDT 2020
 */


/**
 * @author unknown
 */
public class HWFEditor extends JPanel {
	
	static File currentHWFFile = null;
	static byte[][] assets;
	static boolean changeAndHaventSaved = false;
	static final JFrame frame = new JFrame("HWFEditor");
	
	private static void writeHWF() {
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			Files.write(currentHWFFile.toPath(), buildHWFFile(assets));
			changeAndHaventSaved = false;
		} catch (Exception exception) {
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error saving.\n\n" + exception.toString())), "Save error", JOptionPane.ERROR_MESSAGE);
		}
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	static byte[] buildHWFFile(byte[][] modelsAndTextures) {
		ArrayList<Byte> buildingBytes = new ArrayList<>();
		
		for (byte[] modelOrTexture : modelsAndTextures)
			for (byte aByte : modelOrTexture) buildingBytes.add(aByte);
		
		for (int i = 0; i < MIDIJam.FILENAMES_IDS.entrySet().size(); i++) {
			String filename = MIDIJam.FILENAMES_IDS.get(i);
			for (int j = 0; j < filename.length(); j++) {
				buildingBytes.add((byte) filename.charAt(j));
			}
			buildingBytes.add((byte) 0x0);
			for (int j = 0; j < 260 - (filename.length() + 1); j++) {
				buildingBytes.add((byte) 0x0);
			}
			byte[] fileLengthAsArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(modelsAndTextures[i].length).array();
			for (int j = 0; j < 4; j++) {
				buildingBytes.add(fileLengthAsArray[j]);
			}
		}
		buildingBytes.add((byte) 0x88);
		buildingBytes.add((byte) 0x01);
		buildingBytes.add((byte) 0x00);
		buildingBytes.add((byte) 0x00);
		
		byte[] finalArray = new byte[buildingBytes.size()];
		for (int i = 0; i < buildingBytes.size(); i++) {
			finalArray[i] = buildingBytes.get(i);
		}
		return finalArray;
	}
	
	public static void main(String[] args) throws IOException {
		FlatDarculaLaf.install();
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e) {
//			e.printStackTrace();
//		}
		HWFEditor editor = new HWFEditor();
		editor.initComponents();
		final InputStream logo = editor.getClass().getResourceAsStream("/logo.png");
		frame.setIconImage(ImageIO.read(logo));
		
		frame.setContentPane(editor);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setSize(screenSize.width / 2, screenSize.height / 2);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				promptExit();
			}
		});
	}
	
	private static void promptExit() {
		if (changeAndHaventSaved) {
			int i = JOptionPane.showConfirmDialog(new JFrame(),
					"Do you want to save changes?", "HWFEditor",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (i == JOptionPane.YES_OPTION) {
				writeHWF();
				System.exit(0);
			} else if (i == JOptionPane.NO_OPTION) {
				System.exit(0);
			}
		} else {
			System.exit(0);
		}
	}
	
	static int byteArrayToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
	}
	
	static class IllformattedHWFFile extends IOException {
		public IllformattedHWFFile() {
			super();
		}
	}
	
	private void parseHWF(File hwf) throws IOException {
		byte[] bytes;
		bytes = Files.readAllBytes(hwf.toPath());
		ArrayList<Integer> listOfLengths = new ArrayList<>();
		for (int i = bytes.length - 8; i > bytes.length - 8 - (392 * 264); i -= 264) {
			byte[] sizeBytes = new byte[4];
			System.arraycopy(bytes, i, sizeBytes, 0, 4);
			listOfLengths.add(byteArrayToInt(sizeBytes));
		}
		Collections.reverse(listOfLengths);
		byte[][] texturesAndModels = new byte[392][];
		int srcPos = 0;
		for (int i = 0; i < texturesAndModels.length; i++) {
			texturesAndModels[i] = Arrays.copyOfRange(bytes, srcPos, srcPos + listOfLengths.get(i));
			srcPos += listOfLengths.get(i);
		}
		if (bytes.length != listOfLengths.stream().mapToInt(Integer::intValue).sum() + (listOfLengths.size() * 264) + 4) {
			throw new IllformattedHWFFile();
		}
		assets = texturesAndModels;
		
	}
	
	private ArrayList<ImageIcon> textures() {
		ArrayList<ImageIcon> images = new ArrayList<>();
		try {
			for (int i = 280; i < 392; i++) {
				images.add(new ImageIcon(ImageIO.read(new ByteArrayInputStream(assets[i]))));
			}
			return images;
		} catch (Exception e) {
			System.out.println("showing dialog");
			JOptionPane.showMessageDialog(this, new JScrollPane(new JTextArea("There was an error reading a texture.\n\n" + e.toString())), "Texture error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			return null;
		}
	}
	
	private String[][] texturesRows() {
		String[][] strings = new String[112][];
		for (int i = 280; i <= 391; i++) {
			strings[i - 280] = new String[] {MIDIJam.FILENAMES_IDS.get(i), "", ""};
		}
		return strings;
	}
	
	private String[][] modelsRows() {
		String[][] strings = new String[280][];
		for (int i = 0; i <= 279; i++) {
			strings[i] = new String[] {MIDIJam.FILENAMES_IDS.get(i), ""};
		}
		return strings;
	}
	
	
	private void loadHWF(File file) {
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		if (currentHWFFile != null) {
			closeMenuITemActionPerformed(null);
		}
		currentHWFFile = file;
		try {
			parseHWF(file);
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error parsing the HWF file.\n\n" + exception.toString())), "HWF Parse error", JOptionPane.ERROR_MESSAGE);
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			currentHWFFile = null;
			return;
		}
		this.noFileLabel.setVisible(false);
		this.noFileLabel2.setVisible(false);
		
		loadTexturesTable();
		loadModelsTable();
		changeAndHaventSaved = false;
		
		this.importMenuItem.setEnabled(true);
		this.exportMenuItem.setEnabled(true);
		this.saveMenuItem.setEnabled(true);
		this.saveAsMenuItem.setEnabled(true);
		this.closeMenuITem.setEnabled(true);
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	private void loadModelsTable() {
		final String[] colNames = new String[] {"Name", "Size"};
		String[][] data = modelsRows();
		for (int i = 0; i < 280; i++) {
			data[i][1] = String.valueOf(assets[i].length);
		}
		DefaultTableModel model = new DefaultTableModel(data, colNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		modelsTable.setModel(model);
		modelsTable.doLayout();
		modelsTable.setFillsViewportHeight(true);
		modelsTable.setVisible(true);
	}
	
	private void loadTexturesTable() {
		
		final String[] colNames = new String[] {"Name", "Size", "Texture"};
		String[][] data = texturesRows();
		ArrayList<ImageIcon> images = textures();
		for (int i = 0; i < data.length; i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				assert images != null;
				ImageIO.write((RenderedImage) images.get(i).getImage(), "bmp", bos);
				data[i][1] = String.valueOf(bos.toByteArray().length);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error creating the textures table.\n\n" + e.toString())), "Texture table error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			
		}
		DefaultTableModel model = new DefaultTableModel(data, colNames) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		texturesTable.setModel(model);
		texturesTable.setRowHeight(100);
		texturesTable.getColumn("Texture").setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
			assert images != null;
			final ImageIcon srcImg = images.get(row);
			return new JLabel(rescale(srcImg, (int) ((100f / srcImg.getIconHeight()) * srcImg.getIconWidth()), 100));
			
		});
		
		
		texturesTable.doLayout();
		texturesTable.setFillsViewportHeight(true);
		texturesTable.setVisible(true);
	}
	
	private ImageIcon rescale(ImageIcon srcImg, int w, int h) {
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resizedImg.createGraphics();
		
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg.getImage(), 0, 0, w, h, null);
		g2.dispose();
		
		return new ImageIcon(resizedImg);
	}
	
	static final FileFilter HWFFileFilter = new FileFilter() {
		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) return true;
			else
				return f.getName().toLowerCase().endsWith("hwf");
		}
		
		@Override
		public String getDescription() {
			return "HWF Asset Files (*.hwf)";
		}
	};
	
	private void openMenuItemActionPerformed(ActionEvent e) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final JFileChooser chooser = new JFileChooser();
		chooser.setSize(screenSize);
		chooser.addChoosableFileFilter(HWFFileFilter);
		chooser.setFileFilter(HWFFileFilter);
		Action details = chooser.getActionMap().get("viewTypeDetails");
		details.actionPerformed(null);
		int choose = chooser.showDialog(this, "Open");
		if (choose == JFileChooser.APPROVE_OPTION) {
			loadHWF(chooser.getSelectedFile());
		}
	}
	
	private void importMenuItemActionPerformed(ActionEvent e) {
		try {
			JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) return true;
					else
						return f.getName().toLowerCase().endsWith(".bmp") ||
								f.getName().toLowerCase().endsWith(".ms3d");
				}
				
				@Override
				public String getDescription() {
					return "Asset files (*.bmp; *.ms3d)";
				}
			});
			chooser.setAcceptAllFileFilterUsed(false); // get rekt kid
			chooser.setMultiSelectionEnabled(true);
			int chose = chooser.showDialog(this, "Import");
			if (chose == JFileChooser.APPROVE_OPTION) {
				frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				
				File[] files = chooser.getSelectedFiles();
				int count = 0;
				for (File importRes : files) {
					String name = importRes.getName().toLowerCase();
					byte[] bytes = Files.readAllBytes(importRes.toPath());
					ImageIcon icon = null;
					final byte[] fileHeader = Arrays.copyOfRange(bytes, 0, 4);
					boolean bitmap = false;
					if (Arrays.equals(Arrays.copyOfRange(fileHeader, 0, 3), new byte[] {0x42, 0x4D, 0x36})) { // BM6
						try {
							icon = new ImageIcon(ImageIO.read(new ByteArrayInputStream(bytes)));
							bitmap = true;
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(this,
									"Could not read " + name + ". It appears to be broken!", "Error"
									, JOptionPane.ERROR_MESSAGE
							);
							break;
						}
					} else if (!Arrays.equals(fileHeader, new byte[] {0x4D, 0x53, 0x33, 0x44})) { // not MS3D
						JOptionPane.showMessageDialog(this,
								"Could not read " + name + ". Unknown type!", "Error"
								, JOptionPane.ERROR_MESSAGE
						);
						break;
					}
					int resID = -1;
					for (Map.Entry<Integer, String> entry : MIDIJam.FILENAMES_IDS.entrySet()) {
						if (entry.getValue().equalsIgnoreCase(name)) {
							resID = entry.getKey();
							break;
						}
					}
					if (resID == -1) {
						Collection<String> names = MIDIJam.FILENAMES_IDS.values();
						String choseFile = (String) JOptionPane.showInputDialog(new JFrame(),
								name + " is not recognized. Select the correct asset file below."
								, "Unknown file", JOptionPane.WARNING_MESSAGE, null, names.toArray(), "");
						if (choseFile == null) break;
						resID = getKeyByValue(MIDIJam.FILENAMES_IDS, choseFile);
					}
					if (bitmap && (double) icon.getIconHeight() / icon.getIconWidth() != MIDIJam.FILES_ASPECT_RATIOS.get(resID)) {
						JOptionPane.showMessageDialog(new JFrame(),
								"The selected file does not match the aspect ratio of the asset.",
								"Invalid aspect ratio",
								JOptionPane.ERROR_MESSAGE,
								null);
						break;
					}
					assets[resID] = bytes;
					changeAndHaventSaved = true;
					count++;
				}
				
				loadTexturesTable();
				loadModelsTable();
				System.out.println("done loop");
				frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				JOptionPane.showMessageDialog(this, count + " asset" + (count == 1 ? "" : "s") + " imported.",
						"Assets imported",
						JOptionPane.INFORMATION_MESSAGE,
						null);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error importing asset(s).\n\n" + ex.toString())), "Import error", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
		
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
		for (Map.Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	private void exitMenuItemActionPerformed(ActionEvent e) {
		promptExit();
	}
	
	private void saveMenuItemActionPerformed(ActionEvent e) {
		writeHWF();
	}
	
	private void saveAsMenuItemActionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save as...");
		chooser.addChoosableFileFilter(HWFFileFilter);
		chooser.setFileFilter(HWFFileFilter);
		Action details = chooser.getActionMap().get("viewTypeDetails");
		details.actionPerformed(null);
		int userSelection = chooser.showSaveDialog(new JFrame());
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = chooser.getSelectedFile();
			String name = fileToSave.getAbsolutePath();
			if (!name.toLowerCase().endsWith(".hwf")) {
				fileToSave = new File(fileToSave.getAbsolutePath() + ".hwf");
			}
			currentHWFFile = fileToSave;
			writeHWF();
			
		}
	}
	
	private void closeMenuITemActionPerformed(ActionEvent e) {
		if (changeAndHaventSaved) {
			int i = JOptionPane.showConfirmDialog(new JFrame(),
					"Do you want to save changes?", "HWFEditor",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (i == JOptionPane.CANCEL_OPTION) {
				return;
			}
			if (i == JOptionPane.YES_OPTION) {
				writeHWF();
			}
		}
		currentHWFFile = null;
		texturesTable.setModel(new DefaultTableModel());
		texturesTable.doLayout();
		modelsTable.setModel(new DefaultTableModel());
		modelsTable.doLayout();
		noFileLabel.setVisible(true);
		noFileLabel2.setVisible(true);
		importMenuItem.setEnabled(false);
		exportMenuItem.setEnabled(false);
		saveAsMenuItem.setEnabled(false);
		saveMenuItem.setEnabled(false);
		closeMenuITem.setEnabled(false);
	}
	
	static final JDialog exportDialog = new JDialog(frame, true);
	
	private void exportMenuItemActionPerformed(ActionEvent e) {
		
		exportDialog.setContentPane(new Exporter());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		exportDialog.setSize(screenSize.width / 4, screenSize.height / 2);
		exportDialog.setLocationRelativeTo(null);
		exportDialog.setVisible(true);
	}
	
	private void aboutMenuItemActionPerformed(ActionEvent e) {
		JDialog dialog = new JDialog(frame, true);
		dialog.setTitle("About HWFEditor");
		dialog.setContentPane(new About());
		dialog.setSize(255, 400);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	private void helpMenuItemActionPerformed(ActionEvent e) {
		JFrame helpFrame = new JFrame("Help");
		helpFrame.setIconImage(frame.getIconImage());
		helpFrame.setSize(500, 500);
		helpFrame.setContentPane(new HelpScreen());
		helpFrame.setLocationRelativeTo(null);
		helpFrame.setVisible(true);
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		menuBar = new JMenuBar();
		fileMenu = new JMenu();
		openMenuItem = new MenuItemResizedIcon();
		importMenuItem = new MenuItemResizedIcon();
		exportMenuItem = new MenuItemResizedIcon();
		saveMenuItem = new MenuItemResizedIcon();
		saveAsMenuItem = new MenuItemResizedIcon();
		closeMenuITem = new MenuItemResizedIcon();
		exitMenuItem = new MenuItemResizedIcon();
		helpMenu = new JMenu();
		helpMenuItem = new MenuItemResizedIcon();
		aboutMenuItem = new MenuItemResizedIcon();
		tabbedPane1 = new JTabbedPane();
		texturesPanel = new JPanel();
		noFileLabel = new JLabel();
		scrollPane1 = new JScrollPane();
		texturesTable = new JTable();
		panel2 = new JPanel();
		noFileLabel2 = new JLabel();
		scrollPane2 = new JScrollPane();
		modelsTable = new JTable();
		
		//======== this ========
		setLayout(new BorderLayout());
		
		//======== menuBar ========
		{
			
			//======== fileMenu ========
			{
				fileMenu.setText("File");
				
				//---- openMenuItem ----
				openMenuItem.setText("Open...");
				openMenuItem.setIcon(new ImageIcon(getClass().getResource("/open.png")));
				openMenuItem.setMnemonic('O');
				openMenuItem.addActionListener(e -> openMenuItemActionPerformed(e));
				fileMenu.add(openMenuItem);
				fileMenu.addSeparator();
				
				//---- importMenuItem ----
				importMenuItem.setText("Import...");
				importMenuItem.setIcon(new ImageIcon(getClass().getResource("/import.png")));
				importMenuItem.setEnabled(false);
				importMenuItem.setMnemonic('I');
				importMenuItem.addActionListener(e -> importMenuItemActionPerformed(e));
				fileMenu.add(importMenuItem);
				
				//---- exportMenuItem ----
				exportMenuItem.setText("Export...");
				exportMenuItem.setEnabled(false);
				exportMenuItem.setIcon(new ImageIcon(getClass().getResource("/export.png")));
				exportMenuItem.setMnemonic('X');
				exportMenuItem.addActionListener(e -> exportMenuItemActionPerformed(e));
				fileMenu.add(exportMenuItem);
				fileMenu.addSeparator();
				
				//---- saveMenuItem ----
				saveMenuItem.setText("Save");
				saveMenuItem.setIcon(new ImageIcon(getClass().getResource("/save.png")));
				saveMenuItem.setEnabled(false);
				saveMenuItem.setMnemonic('S');
				saveMenuItem.addActionListener(e -> saveMenuItemActionPerformed(e));
				fileMenu.add(saveMenuItem);
				
				//---- saveAsMenuItem ----
				saveAsMenuItem.setText("Save as...");
				saveAsMenuItem.setIcon(new ImageIcon(getClass().getResource("/saveas.png")));
				saveAsMenuItem.setEnabled(false);
				saveAsMenuItem.setMnemonic('S');
				saveAsMenuItem.addActionListener(e -> saveAsMenuItemActionPerformed(e));
				fileMenu.add(saveAsMenuItem);
				
				//---- closeMenuITem ----
				closeMenuITem.setText("Close");
				closeMenuITem.setIcon(new ImageIcon(getClass().getResource("/close.png")));
				closeMenuITem.setEnabled(false);
				closeMenuITem.setMnemonic('C');
				closeMenuITem.addActionListener(e -> closeMenuITemActionPerformed(e));
				fileMenu.add(closeMenuITem);
				fileMenu.addSeparator();
				
				//---- exitMenuItem ----
				exitMenuItem.setText("Exit");
				exitMenuItem.setIcon(new ImageIcon(getClass().getResource("/exit.png")));
				exitMenuItem.setMnemonic('E');
				exitMenuItem.addActionListener(e -> exitMenuItemActionPerformed(e));
				fileMenu.add(exitMenuItem);
			}
			menuBar.add(fileMenu);
			
			//======== helpMenu ========
			{
				helpMenu.setText("Help");
				
				//---- helpMenuItem ----
				helpMenuItem.setText("Help");
				helpMenuItem.setIcon(new ImageIcon(getClass().getResource("/help.png")));
				helpMenuItem.addActionListener(e -> helpMenuItemActionPerformed(e));
				helpMenu.add(helpMenuItem);
				
				//---- aboutMenuItem ----
				aboutMenuItem.setText("About");
				aboutMenuItem.setIcon(new ImageIcon(getClass().getResource("/about.png")));
				aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
				helpMenu.add(aboutMenuItem);
			}
			menuBar.add(helpMenu);
		}
		add(menuBar, BorderLayout.NORTH);
		
		//======== tabbedPane1 ========
		{
			
			//======== texturesPanel ========
			{
				texturesPanel.setLayout(new BorderLayout());
				
				//---- noFileLabel ----
				noFileLabel.setText("No file loaded.");
				noFileLabel.setHorizontalAlignment(SwingConstants.CENTER);
				texturesPanel.add(noFileLabel, BorderLayout.NORTH);
				
				//======== scrollPane1 ========
				{
					scrollPane1.setViewportView(texturesTable);
				}
				texturesPanel.add(scrollPane1, BorderLayout.CENTER);
			}
			tabbedPane1.addTab("Textures", texturesPanel);
			
			//======== panel2 ========
			{
				panel2.setLayout(new BorderLayout());
				
				//---- noFileLabel2 ----
				noFileLabel2.setText("No file loaded.");
				noFileLabel2.setHorizontalAlignment(SwingConstants.CENTER);
				panel2.add(noFileLabel2, BorderLayout.NORTH);
				
				//======== scrollPane2 ========
				{
					scrollPane2.setViewportView(modelsTable);
				}
				panel2.add(scrollPane2, BorderLayout.CENTER);
			}
			tabbedPane1.addTab("Models", panel2);
		}
		add(tabbedPane1, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private MenuItemResizedIcon openMenuItem;
	private MenuItemResizedIcon importMenuItem;
	private MenuItemResizedIcon exportMenuItem;
	private MenuItemResizedIcon saveMenuItem;
	private MenuItemResizedIcon saveAsMenuItem;
	private MenuItemResizedIcon closeMenuITem;
	private MenuItemResizedIcon exitMenuItem;
	private JMenu helpMenu;
	private MenuItemResizedIcon helpMenuItem;
	private MenuItemResizedIcon aboutMenuItem;
	private JTabbedPane tabbedPane1;
	private JPanel texturesPanel;
	private JLabel noFileLabel;
	private JScrollPane scrollPane1;
	private JTable texturesTable;
	private JPanel panel2;
	private JLabel noFileLabel2;
	private JScrollPane scrollPane2;
	private JTable modelsTable;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
