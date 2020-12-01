/*
 * License: GPL. For details, see LICENSE file.
 */

package org.wysko.hwfeditor;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.apache.commons.io.output.CountingOutputStream;
import org.wysko.util.WinRegistry;

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
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edits HWF files from MIDIJam.
 *
 * @author Jacob Wysko
 * @version v1.1.1
 */
public class HWFEditor extends JPanel {
	
	static final JFrame frame = new JFrame("HWFEditor");
	
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
	
	static final JDialog exportDialog = new JDialog(frame, true);
	
	static final private Logger log = Logger.getLogger("org.wysko.hwfeditor");
	
	static HWFEditor editor;
	
	static {
		Handler handlerObj = new ConsoleHandler();
		handlerObj.setLevel(Level.ALL);
		log.addHandler(handlerObj);
		log.setLevel(Level.ALL);
		log.setUseParentHandlers(false);
	}
	
	public File currentHWFFile = null;
	public byte[][] assets;
	boolean unsavedChanges = false;
	
	
	/**
	 * Efficiently writes the bytes of the assets to the specified file.
	 *
	 * @param modelsAndTextures the assets
	 * @param file              the file to write to
	 * @throws IOException something went wrong
	 */
	static void writeHWFToFile(byte[][] modelsAndTextures, File file) throws IOException {
		log.fine(String.format("Starting HWF save. File destination: %s", file.getAbsolutePath()));
		
		CountingOutputStream outstream = new CountingOutputStream(new FileOutputStream(file));
		
		log.finer("Writing models and textures.");
		
		for (byte[] modelOrTexture : modelsAndTextures) {
			outstream.write(modelOrTexture);
		}
		
		log.finer("Writing size dictionary.");
		
		/* Write size dictionary */
		for (int i = 0; i < MIDIJam.FILENAMES_IDS.entrySet().size(); i++) {
			String filename = MIDIJam.FILENAMES_IDS.get(i);
			for (int j = 0; j < filename.length(); j++) {
				outstream.write((byte) filename.charAt(j));
			}
			outstream.write((byte) 0x0);
			for (int j = 0; j < 260 - (filename.length() + 1); j++) {
				outstream.write((byte) 0x0);
			}
			byte[] fileLengthAsArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(modelsAndTextures[i].length).array();
			for (int j = 0; j < 4; j++) {
				outstream.write(fileLengthAsArray[j]);
			}
		}
		
		/* EOF */
		outstream.write((byte) 0x88);
		outstream.write((byte) 0x01);
		outstream.write((byte) 0x00);
		outstream.write((byte) 0x00);
		
		outstream.close();
		
		log.info(String.format("Saved %d bytes.", outstream.getByteCount()));
	}
	
	enum Theme {
		DARK("Dark"), LIGHT("Light");
		public String s;
		
		Theme(String s) {
			this.s = s;
		}
		
		@Override
		public String toString() {
			return s;
		}
	}
	
	public static Theme getThemeFromRegistry() {
		String themeString = null;
		try {
			themeString = WinRegistry.readString(WinRegistry.HKEY_CURRENT_USER, "Software\\Wysko\\HWFEditor",
					"Theme").toUpperCase();
		} catch (Exception e) {
			return Theme.DARK;
		}
		if (themeString == null) return Theme.DARK;
		Theme theme = Theme.DARK;
		try {
			theme = Theme.valueOf(themeString);
		} catch (IllegalArgumentException ignored) {
		}
		return theme;
	}
	
	public static void setThemeInRegistry(Theme theme) {
		try {
			WinRegistry.createKey(WinRegistry.HKEY_CURRENT_USER, "Software\\Wysko\\HWFEditor");
			WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, "Software\\Wysko\\HWFEditor", "Theme", theme.toString());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frame, "Welp. That didn't work. Oh well. You can live with this theme.",
					"Error", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	public static void main(String... args) throws IOException, IllegalAccessException {
		
		Theme theme = getThemeFromRegistry();
		if (theme == null) {
			setThemeInRegistry(Theme.DARK);
			FlatDarculaLaf.install();
		} else if (theme == Theme.LIGHT) {
			FlatIntelliJLaf.install();
		} else {
			FlatDarculaLaf.install();
		}
		
		editor = new HWFEditor();
		editor.initComponents();
		final InputStream logo = editor.getClass().getResourceAsStream("/logo.png");
		frame.setIconImage(ImageIO.read(logo));
		frame.setContentPane(editor);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setSize(screenSize.width / 2, screenSize.height / 2);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				editor.promptExit();
			}
		});
	}
	
	static int byteArrayToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
		return map.entrySet().stream().filter(entry -> Objects.equals(value, entry.getValue())).findFirst().map(Map.Entry::getKey).orElse(null);
	}
	
	public static String readableFileSize(long size) {
		if (size <= 0) return "0";
		final String[] units = new String[] {"B", "kB", "MB", "GB", "TB"};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
	
	public void saveHWF() {
		frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		try {
			writeHWFToFile(assets, currentHWFFile);
			unsavedChanges = false;
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error saving.\n\n" + exception.toString())), "Save error", JOptionPane.ERROR_MESSAGE);
		} finally {
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	public void promptExit() {
		if (unsavedChanges) {
			int i = JOptionPane.showConfirmDialog(new JFrame(),
					"Do you want to save changes?", "HWFEditor",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (i == JOptionPane.YES_OPTION) {
				saveHWF();
				System.exit(0);
			} else if (i == JOptionPane.NO_OPTION) {
				System.exit(0);
			}
		} else {
			System.exit(0);
		}
	}
	
	/**
	 * @param HWF the file to parse
	 * @throws IOException if an error occurs
	 */
	public void betterParseHWF(File HWF) throws IOException {
		
		RandomAccessFile file = new RandomAccessFile(HWF, "r");
		ArrayList<Integer> fileSizes = new ArrayList<>();
		
		long size = file.length();
		file.seek(size - 4);
		
		// Check EOF
		int eof = Integer.reverseBytes(file.readInt());
		if (eof != 392) {
			throw new IllformattedHWFFile("Illegal EOF", file.getFilePointer());
		}
		file.seek(file.getFilePointer() - 8);
		
		// For each file, get file lengths
		int numFiles = MIDIJam.FILENAMES_IDS.size();
		for (int i = 0; i < numFiles; i++) {
			int i1 = file.readInt();
			fileSizes.add(Integer.reverseBytes(i1));
			file.seek(file.getFilePointer() - 268);
		}
		Collections.reverse(fileSizes);
		// Read each file
		file.seek(0);
		byte[][] readAssets = new byte[numFiles][];
		for (int i = 0; i < numFiles; i++) {
			int fileSize = fileSizes.get(i);
			byte[] arr = new byte[fileSize];
			file.read(arr, 0, fileSize);
			readAssets[i] = arr;
		}
		assets = readAssets;
	}
	
	/**
	 * @param HWF the file to parse
	 * @throws IOException if an error occurs
	 * @deprecated superseded by {@code betterParseHWF}
	 */
	public void parseHWF(File HWF) throws IOException {
		byte[] bytes;
		bytes = Files.readAllBytes(HWF.toPath());
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
	
	public ArrayList<ImageIcon> textures() {
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
	
	public String[][] texturesRows() {
		String[][] strings = new String[112][];
		for (int i = 280; i <= 391; i++) {
			strings[i - 280] = new String[] {MIDIJam.FILENAMES_IDS.get(i), "", ""};
		}
		return strings;
	}
	
	public String[][] modelsRows() {
		String[][] strings = new String[280][];
		for (int i = 0; i <= 279; i++) {
			strings[i] = new String[] {MIDIJam.FILENAMES_IDS.get(i), ""};
		}
		return strings;
	}
	
	public void loadHWF(File file) {
		try {
			frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			if (currentHWFFile != null) {
				closeMenuITemActionPerformed(null);
			}
			currentHWFFile = file;
			
			betterParseHWF(file);
			
			
			this.noFileLabel.setVisible(false);
			this.noFileLabel2.setVisible(false);
			
			loadTexturesTable();
			loadModelsTable();
			unsavedChanges = false;
			
			this.importMenuItem.setEnabled(true);
			this.exportMenuItem.setEnabled(true);
			this.saveMenuItem.setEnabled(true);
			this.saveAsMenuItem.setEnabled(true);
			this.closeMenuITem.setEnabled(true);
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea("There was an error parsing the HWF file.\n\n" + exception.toString())), "HWF Parse error", JOptionPane.ERROR_MESSAGE);
			currentHWFFile = null;
			exception.printStackTrace();
		} finally {
			frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}
	
	public void loadModelsTable() {
		final String[] colNames = new String[] {"Name", "Size"};
		String[][] data = modelsRows();
		for (int i = 0; i < 280; i++) {
			data[i][1] = readableFileSize(assets[i].length);
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
	
	public void loadTexturesTable() {
		
		final String[] colNames = new String[] {"Name", "Size", "Texture"};
		String[][] data = texturesRows();
		ArrayList<ImageIcon> images = textures();
		for (int i = 0; i < data.length; i++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				assert images != null;
				ImageIO.write((RenderedImage) images.get(i).getImage(), "bmp", bos);
				data[i][1] = readableFileSize(bos.toByteArray().length);
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
	
	public ImageIcon rescale(ImageIcon srcImg, int w, int h) {
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resizedImg.createGraphics();
		
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg.getImage(), 0, 0, w, h, null);
		g2.dispose();
		
		return new ImageIcon(resizedImg);
	}
	
	public void openMenuItemActionPerformed(ActionEvent e) {
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
	
	public void importMenuItemActionPerformed(ActionEvent e) {
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
					if (Arrays.equals(Arrays.copyOfRange(fileHeader, 0, 2), new byte[] {0x42, 0x4D})) { // BM6
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
					unsavedChanges = true;
					count++;
				}
				
				loadTexturesTable();
				loadModelsTable();
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
	
	public void exitMenuItemActionPerformed(ActionEvent e) {
		promptExit();
	}
	
	public void saveMenuItemActionPerformed(ActionEvent e) {
		saveHWF();
	}
	
	public void saveAsMenuItemActionPerformed(ActionEvent e) {
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
			saveHWF();
		}
	}
	
	public void closeMenuITemActionPerformed(ActionEvent e) {
		if (unsavedChanges) {
			int i = JOptionPane.showConfirmDialog(new JFrame(),
					"Do you want to save changes?", "HWFEditor",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (i == JOptionPane.CANCEL_OPTION) {
				return;
			}
			if (i == JOptionPane.YES_OPTION) {
				saveHWF();
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
	
	public void exportMenuItemActionPerformed(ActionEvent e) {
		
		exportDialog.setContentPane(new Exporter());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		exportDialog.setSize(screenSize.width / 4, screenSize.height / 2);
		exportDialog.setLocationRelativeTo(null);
		exportDialog.setVisible(true);
	}
	
	public void aboutMenuItemActionPerformed(ActionEvent e) {
		JDialog dialog = new JDialog(frame, true);
		dialog.setTitle("About HWFEditor");
		dialog.setContentPane(new About());
		dialog.setSize(255, 400);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	public void helpMenuItemActionPerformed(ActionEvent e) {
		JFrame helpFrame = new JFrame("Help");
		helpFrame.setIconImage(frame.getIconImage());
		helpFrame.setSize(500, 500);
		helpFrame.setContentPane(new HelpScreen());
		helpFrame.setLocationRelativeTo(null);
		helpFrame.setVisible(true);
	}
	
	private void preferencesMenuItemActionPerformed(ActionEvent e) {
		Preferences preferences = new Preferences();
		preferences.setIconImage(frame.getIconImage());
		preferences.setLocationRelativeTo(null);
		preferences.setModal(true);
		preferences.setVisible(true);
	}
	
	static class IllformattedHWFFile extends IOException {
		public IllformattedHWFFile() {
			super();
		}
		
		public IllformattedHWFFile(String message, long pos) {
			super(message + " at position " + pos + ".");
		}
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
		editMenu = new JMenu();
		preferencesMenuItem = new MenuItemResizedIcon();
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
			
			//======== editMenu ========
			{
				editMenu.setText("Edit");
				
				//---- preferencesMenuItem ----
				preferencesMenuItem.setText("Preferences...");
				preferencesMenuItem.setIcon(new ImageIcon(getClass().getResource("/preferences.png")));
				preferencesMenuItem.addActionListener(e -> preferencesMenuItemActionPerformed(e));
				editMenu.add(preferencesMenuItem);
			}
			menuBar.add(editMenu);
			
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
	private JMenu editMenu;
	private MenuItemResizedIcon preferencesMenuItem;
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
