/*
 * Created by JFormDesigner on Wed May 20 17:17:07 EDT 2020
 */

package org.wysko.hwfeditor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
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
		helpTextPane.setContentType("text/html");
		try {
		URL html = getClass().getResource("/help.html");
					List<String> asdf = Files.readAllLines(new File(html.getPath()).toPath());
					StringBuilder builder = new StringBuilder();
					asdf.forEach(builder::append);
					helpTextPane.setText(builder.toString());
					} catch (Exception e) {
					JOptionPane.showMessageDialog(this,
											"Could not load the help text! Oof", "Help error"
											, JOptionPane.ERROR_MESSAGE
									);
					}

		//======== this ========
		setMaximumSize(new Dimension(500, 500));
		setLayout(new BorderLayout());

		//======== scrollPane1 ========
		{
			scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

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
