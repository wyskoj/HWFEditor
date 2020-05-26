# HWFEditor

HWFEditor is an asset editing tool developed for editing the MidiJam.HWF asset file included with the MIDIJam package. This program will not work on other HWF files created in Scott's other games.

## Using HWFEditor
To use this program, you must already have an existing MidiJam.HWF file to edit. Copies of the MIDIJam package include this file and can be edited. To begin editing the file, click File | Open... and select the file. Once the file has been loaded, you can begin making changes to it.
In the large panel, all textures, including their names, sizes, and a preview, is shown. The list of 3D models and their sizes are shown in the "Model" tab.
The easiest way to edit assets is to export them all to a dedicated folder, edit them, then reimport them all. To do this:
* Click File | Export..., then press Ctrl + A to select all assets, then Export to export them to a folder.
* Make any edits to the assets with whichever program you want. Note: Textures must keep their original aspect ratio, otherwise MIDIJam will crash.
* Click File | Import..., and select all the assets to import them.
* Click File | Save to overwrite the HWF file.

## About
Find a bug? Need a feature? [Submit an issue](https://github.com/wyskoj/HWFEditor/issues/new).

HWFEditor, and all its integral parts, are released under the GNU General Public License 3. The GPL v3 is accessible here: http://www.gnu.org/licenses/gpl.html. The source code is available here: https://github.com/wyskoj/HWFEditor.
