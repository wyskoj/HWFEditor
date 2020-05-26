/*
 * License: GPL. For details, see LICENSE file.
 */

package org.wysko.hwfeditor;

import java.awt.*;
import javax.swing.*;

/*
 * Created by JFormDesigner on Tue May 19 18:25:45 EDT 2020
 */



/**
 * @author Jacob Wysko
 */
public class About extends JPanel {
	
	public About() {
		initComponents();
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		label1 = new JLabel();
		label2 = new JLabel();
		textPane1 = new JTextPane();

		//======== this ========
		setLayout(new GridLayout(3, 1));

		//---- label1 ----
		label1.setText("HWFEditor");
		label1.setFont(new Font("Tahoma", Font.PLAIN, 20));
		label1.setHorizontalAlignment(SwingConstants.CENTER);
		add(label1);

		//---- label2 ----
		label2.setIcon(new ImageIcon(getClass().getResource("/logo.png")));
		label2.setHorizontalAlignment(SwingConstants.CENTER);
		add(label2);

		//---- textPane1 ----
		textPane1.setText("HWFEditor, and all its integral parts, are released under the GNU General Public License 3.\n\nThe GPL v3 is accessible here: http://www.gnu.org/licenses/gpl.html");
		textPane1.setEditable(false);
		textPane1.setFont(UIManager.getFont("TextField.font"));
		add(textPane1);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	private JLabel label1;
	private JLabel label2;
	private JTextPane textPane1;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
