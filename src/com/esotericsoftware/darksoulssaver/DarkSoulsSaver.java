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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;

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
 * standing. This is useful for example right outside a boss fog wall!
 * <p>
 * May need to run as Administrator for some games (eg Dark Souls 2).<br>
 * @author Nathan Sweet */
public class DarkSoulsSaver {
	static final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy kk:mm:ss");

	File saveDir = new File("save"), backupDir = new File("backup");
	final ArrayList<File> saveFiles, backupFiles;
	long lastModified, lastBackupTime, skipBackupTime;
	Audio audio;

	File saveFile;
	String runCommand, exeName;
	int backupDelay;

	public DarkSoulsSaver (File configFile) throws Exception {
		loadConfig(configFile);

		saveFiles = files(saveDir, "save");
		backupFiles = files(backupDir, "backup");
		audio = new Audio();

		Keyboard keyboard = new Keyboard() {
			protected void hotkey (String key, KeyStroke keyStroke) {
				keyPressed(key);
			}
		};
		keyboard.registerHotkey("replaceWithLastSave", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		keyboard.registerHotkey("replaceWithPreviousBackup", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
		keyboard.registerHotkey("restart", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
		keyboard.registerHotkey("stop", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
		keyboard.registerHotkey("replaceWithLatestAndRestart", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		keyboard.registerHotkey("save", KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
		keyboard.start();

		new Thread("Backup") {
			public void run () {
				lastModified = saveFile.lastModified();
				while (true) {
					backup();
					zzz(3000);
				}
			}
		}.start();
	}

	void loadConfig (File configFile) throws FileNotFoundException {
		runCommand = "C:\\Program Files\\Steam\\steam.exe -applaunch 570940";
		exeName = "DarkSoulsRemastered.exe";
		backupDelay = 10;

		System.out.println("Config: " + configFile.getAbsolutePath());
		if (!configFile.exists()) {
			System.out.println("Config file not found.");
			return;
		}
		Scanner scanner = new Scanner(configFile);
		for (int i = 0; scanner.hasNextLine(); i++) {
			String line = scanner.nextLine();
			if (i == 0)
				saveFile = new File(line);
			else if (i == 1)
				runCommand = line;
			else if (i == 2)
				exeName = line;
			else if (i == 3)
				backupDelay = Integer.parseInt(line);
			else
				break;
		}

		System.out.println("Save file: " + saveFile.getAbsolutePath());
		System.out.println("Run command: " + runCommand);
		System.out.println("Executable: " + exeName);
		System.out.println("Backup delay: " + backupDelay + " s");

		if (!saveFile.exists()) {
			print("Save file not found: " + saveFile.getAbsolutePath());
			audio.play(Sound.stop);
			System.exit(-1);
		}
	}

	synchronized void backup () {
		if (System.currentTimeMillis() < skipBackupTime) return;

		long newLastModified = saveFile.lastModified();
		if (newLastModified != lastModified) {
			lastModified = newLastModified;

			// Wait a bit after the file size stops changing.
			long length = saveFile.length();
			for (int i = 0; i < 10; i++) {
				zzz(100);
				long newLength = saveFile.length();
				if (length != newLength) {
					i = 0;
					length = newLength;
				}
			}

			// Backup the file.
			File file = backup(saveFile, backupDir, backupFiles, "backup", 100);
			if (file != null) print("Backup: " + file.getName());
		}
	}

	synchronized void keyPressed (String key) {
		if (!saveFile.exists()) {
			print("Save file not found: " + saveFile.getAbsolutePath());
			audio.play(Sound.stop);
			System.exit(-1);
		}

		if (key.equals("replaceWithLastSave")) {
			if (saveFiles.isEmpty()) {
				print("No save files.");
				audio.play(Sound.stop);
				return;
			}

			File last = last(saveFiles);
			print("Replace with last save: " + fileNameAndDate(last));
			audio.play(Sound.replace);
			copy(last, saveFile);
			skipBackup(20);

		} else if (key.equals("replaceWithPreviousBackup")) {
			if (backupFiles.isEmpty()) {
				print("No backup files.");
				audio.play(Sound.stop);
				return;
			}

			// Last backup older than the current save file.
			File last;
			if (lastBackupTime == 0)
				lastBackupTime = backupFiles.get(backupFiles.size() == 1 ? 0 : backupFiles.size() - 2).lastModified();
			last = last(backupFiles, lastBackupTime);
			print("Replace with previous backup: " + fileNameAndDate(last));
			audio.play(Sound.replace);
			copy(last, saveFile);
			lastBackupTime = last.lastModified();
			skipBackup(20);

		} else if (key.equals("restart")) {
			print("Restart game.");
			stopGame();
			startGame();
			skipBackup(15);

		} else if (key.equals("stop")) {
			print("Stop game.");
			stopGame();
			skipBackup(15);

		} else if (key.equals("replaceWithLatestAndRestart")) {
			if (backupFiles.isEmpty() && saveFiles.isEmpty()) {
				print("No backup or save files.");
				audio.play(Sound.stop);
				return;
			}

			// Use newer of last backup file and save file.
			File last = null;
			String type = null;
			if (!backupFiles.isEmpty()) {
				// Last backup older than X seconds ago.
				last = last(backupFiles, System.currentTimeMillis() - backupDelay * 1000);
				type = "backup";
			}
			if (!saveFiles.isEmpty()) {
				File lastSave = last(saveFiles);
				if (last == null || last.lastModified() < lastSave.lastModified()) {
					last = lastSave;
					type = "save";
				}
			}

			print("Replace with last " + type + " and restart: " + fileNameAndDate(last));
			lastBackupTime = last.lastModified();
			stopGame();
			audio.play(Sound.replace);
			copy(last, saveFile);
			startGame();
			skipBackup(20);

		} else if (key.equals("save")) {
			File file = backup(saveFile, saveDir, saveFiles, "save", 100);
			if (file != null) {
				print("Save: " + file.getName());
				audio.play(Sound.save);
				skipBackup(10);
			} else {
				print("Unable to save.");
				audio.play(Sound.stop);
			}
		}
	}

	boolean isRunning () throws IOException {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("tasklist", "/NH", "/FI", "IMAGENAME eq " + exeName);
		builder.redirectErrorStream(true);

		Process process = builder.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while (true) {
			String line = reader.readLine();
			if (line == null) break;
			if (line.startsWith(exeName)) return true;
		}
		return false;
	}

	void stopGame () {
		try {
			if (!isRunning()) return;
			audio.play(Sound.stop);
			String killCommand = "taskkill /IM " + exeName;
			Runtime.getRuntime().exec(killCommand).waitFor();
			zzz(500);
			for (int i = 1;; i++) {
				if (!isRunning()) return;
				if (Runtime.getRuntime().exec(killCommand).waitFor() != 0) break;
				if (i == 18) {
					print("Unable to terminate process.");
					return;
				}
				zzz(250);
			}
		} catch (Throwable ex) {
			print("Unable to stop:");
			ex.printStackTrace();
		}
	}

	void startGame () {
		audio.play(Sound.start);
		try {
			Runtime.getRuntime().exec(runCommand);
		} catch (Throwable ex) {
			print("Unable to start:");
			ex.printStackTrace();
		}
	}

	void skipBackup (int seconds) {
		skipBackupTime = System.currentTimeMillis() + seconds * 1000;
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
		String name = file.getName();
		try {
			return Integer.parseInt(name.substring(prefix.length(), name.length() - 4));
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
		for (int i = files.size() - 1; i >= 0; i--) {
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
			print("File does not exist: " + from.getAbsolutePath());
			return null;
		}
		int suffix = highestSuffix(files, prefix);
		while (true) {
			File to = new File(toDir, prefix + suffix++ + ".sl2");
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
			print("File does not exist: " + from.getAbsolutePath());
			return false;
		}
		try {
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException ex) {
			print("Error copying file!");
			print("From: " + from.getAbsolutePath());
			print("To: " + to.getAbsolutePath());
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

	static String timestamp (long time) {
		synchronized (dateFormat) {
			return dateFormat.format(time);
		}
	}

	static void print (String message) {
		StringBuilder buffer = new StringBuilder(128);
		buffer.append(timestamp(System.currentTimeMillis()));
		buffer.append(' ');
		buffer.append(message);
		System.out.println(buffer);
	}

	String fileNameAndDate (File file) {
		return file.getName() + " (" + timestamp(file.lastModified()) + ')';
	}

	static public void main (String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: config-file");
			System.out.println("Config file contents:");
			System.out.println("save-file");
			System.out.println("run-command");
			System.out.println("exe-name");
			System.out.println("[backup-delay]");
			System.out.println("Example: java -jar dark-souls-saver.jar ds1.txt");
			System.out.println("ds1.txt:");
			System.out.println("C:\\Users\\USERNAME\\Documents\\NBGI\\DARK SOULS REMASTERED\\NUMBER\\DRAKS0005.sl2");
			System.out.println("C:\\Program Files\\Steam\\steam.exe -applaunch 570940");
			System.out.println("DarkSoulsRemastered.exe");
			System.out.println("ds2.txt:");
			System.out.println("C:\\Users\\USERNAME\\AppData\\Roaming\\DarkSoulsII\\NUMBER\\DS2SOFS0000.sl2");
			System.out.println("C:\\Program Files\\Steam\\steam.exe -applaunch 335300");
			System.out.println("DarkSoulsII.exe");
		} else
			new DarkSoulsSaver(new File(args[0]));
	}
}
