import org.junit.Test;
import org.wysko.hwfeditor.HWFEditor;

import java.io.File;
import java.io.IOException;

public class TestHWFEditor {
	@Test
	public void testBetterHWFParse() throws IOException {
		HWFEditor editor = new HWFEditor();
		editor.betterParseHWF(new File("C:\\Users\\wysko\\Documents\\MIDIJam\\MidiJam.HWF"));
	}
}
