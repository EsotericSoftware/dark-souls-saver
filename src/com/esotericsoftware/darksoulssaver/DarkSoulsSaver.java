/* Copyright (c) 2020, Esoteric Software
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.esotericsoftware.darksoulssaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import javax.swing.KeyStroke;

import java.awt.event.KeyEvent;

/** This app allows you to just play Dark Souls as normal and if you die, press F5 within 10 seconds of dying to:<br>
 * a) replace the game's save file with the last backup where you are alive,<br>
 * b) close Dark Souls, and<br>
 * c) start Dark Souls again.
 * <p>
 * If you want more control, you can use F8 to manually store the game's save file and F1 to replace it with the last one you
 * stored manually.
 * <p>
 * When you use F5 it will use the last manually stored file if that is newer than the last backup. That is almost always what you
 * want, but if not you can still use F1 to get the manually stored file.
 * <p>
 * The game updates its save file when you kill an enemy, pick up an item, close the game menu, and at many other times. To force
 * it to update the save file, just open and close the game menu. Then you can press F8 and you've saved where you are currently
 * standing. This is useful for example right outside a boss fog wall!<br>
 * @author Nathan Sweet */
public class DarkSoulsSaver {
	final ArrayList<File> saveFiles, backupFiles;

	public DarkSoulsSaver (final File saveFile, final File steamExe) {
		final File saveDir = new File("save");
		saveFiles = files(saveDir, "save");

		final File backupDir = new File("backup");
		backupFiles = files(backupDir, "backup");

		Audio audio = new Audio();

		if (!saveFile.exists()) {
			System.out.println("Save file not found: " + saveFile.getAbsolutePath());
			audio.play(Sound.error);
			System.exit(-1);
		}

		Keyboard keyboard = new Keyboard() {
			protected void hotkey (String name, KeyStroke keyStroke) {
				if (!saveFile.exists()) {
					System.out.println("Save file not found: " + saveFile.getAbsolutePath());
					audio.play(Sound.error);
					return;
				}
				if (name.equals("save")) {
					File file = backup(saveFile, saveDir, saveFiles, "save", 100);
					if (file != null) System.out.println("Save: " + file.getName() + " " + new Date());
					audio.play(Sound.save);

				} else if (name.equals("replaceWithLastSave")) {
					if (saveFiles.isEmpty())
						audio.play(Sound.error);
					else {
						System.out.println("Replace with last save: " + saveFile.getName() + " " + new Date());
						copy(last(saveFiles), saveFile);
						audio.play(Sound.replaceSave);
					}

				} else if (name.equals("replaceWithLastBackupAndRestart")) {
					if (backupFiles.isEmpty() && saveFiles.isEmpty()) {
						audio.play(Sound.error);
						System.out.println("No backup or save files.");
					} else {
						// Use newer of last backup file and save file.
						File last = null;
						String type = null;
						if (!backupFiles.isEmpty()) {
							// Use first backup older than 10 seconds ago.
							last = last(backupFiles, System.currentTimeMillis() - 1000 * 10);
							type = "backup";
						}
						if (!saveFiles.isEmpty()) {
							File lastSave = last(saveFiles);
							if (last == null || last.lastModified() < lastSave.lastModified()) {
								last = lastSave;
								type = "save";
							}
						}

						System.out.println("Replace with last " + type + " and restart: " + last.getName() + " " + new Date());
						copy(last, saveFile);
						if (type.equals("save"))
							audio.play(Sound.replaceSave);
						else
							audio.play(Sound.replaceBackup);
					}
					try {
						Runtime.getRuntime().exec("taskkill /IM DarkSoulsRemastered.exe");
						zzz(1000);
						Runtime.getRuntime().exec(steamExe.getAbsolutePath() + " -applaunch 570940");
					} catch (IOException ex) {
						System.out.println("Unable to restart:");
						ex.printStackTrace();
					}
				}
			}
		};
		keyboard.registerHotkey("save", KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
		keyboard.registerHotkey("replaceWithLastSave", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		keyboard.registerHotkey("replaceWithLastBackupAndRestart", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		keyboard.start();

		new Thread("Backup") {
			public void run () {
				long lastModified = saveFile.lastModified();
				while (true) {
					// Check every 5 seconds if the file was modified.
					long newLastModified = saveFile.lastModified();
					if (newLastModified != lastModified) {
						lastModified = newLastModified;

						// Wait a bit after the file size stops changing.
						long length = saveFile.length();
						for (int i = 0; i < 20; i++) {
							zzz(500);
							long newLength = saveFile.length();
							if (length != newLength) {
								i = 0;
								length = newLength;
							}
						}

						// Backup the file.
						File file = backup(saveFile, backupDir, backupFiles, "backup", 100);
						if (file != null) System.out.println("Backup: " + file.getName() + " " + new Date());
					}
					zzz(500);
				}
			}
		}.start();
	}

	ArrayList<File> files (File dir, String prefix) {
		ArrayList<File> prefixFiles = new ArrayList();
		dir.mkdirs();
		File[] files = dir.listFiles();
		if (files == null) return prefixFiles;
		for (File file : files)
			if (file.getName().startsWith(prefix) && suffix(file, prefix) > 0) prefixFiles.add(file);
		prefixFiles.sort(new Comparator<File>() {
			public int compare (File o1, File o2) {
				return Integer.compare(suffix(o1, prefix), suffix(o2, prefix));
			}
		});
		return prefixFiles;
	}

	int suffix (File file, String prefix) {
		try {
			return Integer.parseInt(file.getName().substring(prefix.length()));
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	File last (ArrayList<File> files) {
		return files.get(files.size() - 1);
	}

	/** @param olderThan Return the newest file older than this, otherwise return the oldest file.
	 * @return May be null if files is empty. */
	File last (ArrayList<File> files, long olderThan) {
		File file = null;
		for (int i = 0, n = files.size(); i < n; i++) {
			file = files.get(i);
			if (file.lastModified() < olderThan) return file;
		}
		return file;
	}

	int highestSuffix (ArrayList<File> files, String prefix) {
		if (files.isEmpty()) return 1;
		return suffix(last(files), prefix);
	}

	/** @return May be null. */
	File backup (File from, File toDir, ArrayList<File> files, String prefix, int max) {
		if (!from.exists()) {
			System.out.println("File does not exist: " + from.getAbsolutePath());
			return null;
		}
		int suffix = highestSuffix(files, prefix);
		while (true) {
			File to = new File(toDir, prefix + suffix++);
			if (!to.exists()) {
				if (copy(from, to)) {
					files.add(to);
					while (files.size() > max)
						files.remove(0).delete();
				}
				return to;
			}
		}
	}

	boolean copy (File from, File to) {
		if (!from.exists()) {
			System.out.println("File does not exist: " + from.getAbsolutePath());
			return false;
		}
		try {
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException ex) {
			System.out.println("Error copying file!");
			System.out.println("From: " + from.getAbsolutePath());
			System.out.println("To: " + to.getAbsolutePath());
			ex.printStackTrace(System.out);
			return false;
		}
	}

	void zzz (int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignored) {
		}
	}

	static public void main (String[] args) throws Exception {
		if (args.length != 2) throw new RuntimeException("Usage: save-file steam-exe");
		new DarkSoulsSaver(new File(args[0]), new File(args[1]));
	}
}
