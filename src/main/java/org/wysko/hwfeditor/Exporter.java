/*
 * License: GPL. For details, see LICENSE file.
 */

package org.wysko.hwfeditor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
/*
 * Created by JFormDesigner on Tue May 19 15:48:13 EDT 2020
 */



/**
 * @author Jacob Wysko
 */
public class Exporter extends JPanel {
	public Exporter() {
		initComponents();
		DefaultListModel model = new DefaultListModel<>();
		MIDIJam.FILENAMES_IDS.values().forEach(model::addElement);
		exportList.setModel(model);
		exportList.validate();
		exportList.setVisible(true);
		
	}
	
	private void exportListValueChanged() {
		exportButton.setEnabled(!exportList.isSelectionEmpty());
	}
	
	private void exportButtonActionPerformed() {
		try {
			HWFEditor.exportDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Choose export folder");
			int chose = chooser.showDialog(this, "Export");
			int count = 0;
			if (chose == JFileChooser.APPROVE_OPTION) {
				int[] indices = exportList.getSelectedIndices();
				for (int index : indices) {
					count++;
					Files.write(new File(chooser.getSelectedFile().getAbsolutePath() + "/" + MIDIJam.FILENAMES_IDS.get(index)).toPath(), HWFEditor.editor.assets[index], StandardOpenOption.CREATE);
				}
			}
			
			JOptionPane.showMessageDialog(this, count + " assets exported.",
					"Assets exported",
					JOptionPane.INFORMATION_MESSAGE,
					null);
			HWFEditor.exportDialog.dispose();
			HWFEditor.exportDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		} catch (IOException ioException) {
			JOptionPane.showMessageDialog(HWFEditor.exportDialog, new JScrollPane(new JTextArea("There was an error exporting assets.\n\n" + ioException.toString())), "Export error", JOptionPane.ERROR_MESSAGE);
			HWFEditor.exportDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			HWFEditor.exportDialog.dispose();
			ioException.printStackTrace();
		}
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Jacob Wysko
		// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
		// Generated using JFormDesigner Evaluation license - Jacob Wysko
		JLabel label1 = new JLabel();
		JScrollPane scrollPane1 = new JScrollPane();
		exportList = new JList();
		exportButton = new JButton();
		
		//======== this ========
		setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(
				0, 0, 0, 0), "", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder
				.BOTTOM, new java.awt.Font("Dia\u006cog", java.awt.Font.BOLD, 12), java.awt.Color.
				red), getBorder()));
		addPropertyChangeListener(e -> {
			if ("bord\u0065r".equals(e.getPropertyName())) throw new RuntimeException();
		});
		
		//---- label1 ----
		label1.setText("Select assets to export:");
		label1.setHorizontalAlignment(SwingConstants.CENTER);
		
		//======== scrollPane1 ========
		{
			
			//---- exportList ----
			exportList.addListSelectionListener(e -> exportListValueChanged());
			scrollPane1.setViewportView(exportList);
		}
		
		//---- exportButton ----
		exportButton.setText("Export");
		exportButton.setEnabled(false);
		exportButton.addActionListener(e -> exportButtonActionPerformed());
		
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup()
						.addComponent(label1, GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
						.addGroup(layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(layout.createParallelGroup()
										.addComponent(exportButton, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
										.addComponent(scrollPane1))
								.addContainerGap())
		);
		layout.setVerticalGroup(
				layout.createParallelGroup()
						.addGroup(layout.createSequentialGroup()
								.addContainerGap()
								.addComponent(label1, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(exportButton)
								.addContainerGap())
		);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	private JList exportList;
	private JButton exportButton;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
