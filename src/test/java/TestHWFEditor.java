import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wysko.hwfeditor.Exporter;
import org.wysko.hwfeditor.HWFEditor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestHWFEditor {
	HWFEditor editor;
	URL sourceHWF;
	
	@Before
	public void setUp() {
		editor = new HWFEditor();
		sourceHWF = TestHWFEditor.class.getResource("/MidiJam.HWF");
	}
	
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	@Test
	public void parseHWFFile() throws IOException {
		
		editor.betterParseHWF(new File(sourceHWF.getFile()));
	}
	
	@Test
	public void editHWFFile() throws IOException {
		File tempHWF = folder.newFile("testout.hwf");
		// Load files
		editor.betterParseHWF(new File(sourceHWF.getFile()));
		URL resource = TestHWFEditor.class.getResource("/testIMG.bmp");
		byte[] testImage = Files.readAllBytes(Paths.get(resource.getPath().substring(1)));
		
		// Change asset
		editor.assets[1] = testImage;
		
		// Write file
		
		editor.currentHWFFile = tempHWF;
		editor.saveHWF();
		
		// Check equality
		editor.betterParseHWF(tempHWF);
		Assert.assertArrayEquals(testImage, editor.assets[1]);
	}
	
	@Test
	public void export() throws IOException {
		editor.betterParseHWF(new File(sourceHWF.getFile()));
		Exporter exporter = new Exporter();
	}
}
