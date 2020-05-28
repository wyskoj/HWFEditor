/*
 * Created by JFormDesigner on Wed May 20 17:17:07 EDT 2020
 */

package org.wysko.hwfeditor;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Jacob Wysko
 */
public class HelpScreen extends JPanel {
	public HelpScreen() {
		initComponents();
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		scrollPane1 = new JScrollPane();
		helpTextPane = new JTextPane();
		try {
		URL url = Resources.getResource("help.html");
		String text = Resources.toString(url, Charsets.UTF_8);
		helpTextPane.setContentType("text/html");
		helpTextPane.setText(text);
		} catch (Exception e12) {
		e12.printStackTrace();
		JOptionPane.showMessageDialog(this,
											"Could not load the help text!", "Error"
											, JOptionPane.ERROR_MESSAGE
									);
		}

		//======== this ========
		setMaximumSize(new Dimension(500, 500));
		setLayout(new BorderLayout());

		//======== scrollPane1 ========
		{
			scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane1.getVerticalScrollBar().setValue(0);

			//---- helpTextPane ----
			helpTextPane.setEditable(false);
			scrollPane1.setViewportView(helpTextPane);
		}
		add(scrollPane1, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}
	
	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JScrollPane scrollPane1;
	private JTextPane helpTextPane;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
