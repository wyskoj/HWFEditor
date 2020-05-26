/*
 * License: GPL. For details, see LICENSE file.
 */

package org.wysko.hwfeditor;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class MenuItemResizedIcon extends JMenuItem implements Serializable {
	public MenuItemResizedIcon() {
	
	}
	
	@Override
	public void setIcon(Icon defaultIcon) {
		/* Why isn't this a thing already */
		final ImageIcon defaultIcon1 = new ImageIcon(((ImageIcon) defaultIcon).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		super.setIcon(defaultIcon1);
	}
}
