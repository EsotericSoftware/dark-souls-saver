# Dark Souls Saver

This app allows you to just play Dark Souls as normal and if you die, press F5 within 10 seconds of dying to:
1) replace the game's save file with the last backup where you are alive,
2) close and start the game again.

This app works specifically for Dark Souls: Remastered by default, however the file watching and backups can work for any Dark Souls game, or really any game that uses a game save file.

If you want more control, you can use F8 to manually store the game's save file and F1 to replace it with the last one you stored manually. F2 gives you the next older backup.

When you use F5 it will use the last manually stored file if that is newer than the last backup. That is almost always what you want, but if not you can still use F1 to get the manually stored file.

The game updates its save file when you kill an enemy, pick up an item, close the game menu, and at many other times. To force it to update the save file, just open and close the game menu. Then you can press F8 and you've saved where you are currently standing. This is useful for example right outside a boss fog wall!

# Running the app

To run the app, install Java 8+, download the JAR from the [releases page](https://github.com/EsotericSoftware/dark-souls-saver/releases), and run it like this:

```
java -jar dark-souls-saver.jar config.txt
```

If you need Java, get it [here](https://adoptium.net/temurin/releases/). Choose `Windows`, `x64`, `JRE`, and the latest version, eg `19`. You don't need to install Java, just unzip and run `java.exe` in the `bin` folder.

The `save` and `backup` folders are created in the folder where it is run.

The `config.txt` file is a text file with the configuration for the Dark Souls game you want to play. It has 3 or 4 lines:

```
save-file
run-command
exe-name
[backup-delay]
```

The `backup-delay` is `10000` (10 seconds) by default and can be omitted. For example, for Dark Souls: Remastered:

```
C:\Users\USERNAME\Documents\NBGI\DARK SOULS REMASTERED\NUMBER\DRAKS0005.sl2
C:\Apps\Steam\steam.exe -applaunch 570940
DarkSoulsRemastered.exe
```

For Dark Souls 2:

```
C:\Users\USERNAME\AppData\Roaming\DarkSoulsII\NUMBER\DS2SOFS0000.sl2
C:\Apps\Steam\steam.exe -applaunch 335300
DarkSoulsII.exe
```

The `save-file` is where the game writes your game save file. Replace `USERNAME`, `NUMBER`, and the path to `Steam.exe` as needed for your computer. 

The `run-command` starts the game. Don't use a path that contains spaces. Each Steam game has a unique app ID.

The `exe-name` is the name of the game's executable file and is used to close the game.

# How it works

Dark Souls works by writing a game save file whenever a significant event happens. This is used to restore your game should it crash or close unexpectedly. By keeping copies of the save file, we can later replace the save file with an older copy to restore a previous game state.

This app has two ways to backup the game save file:

1) It watches the save file and if it changes, 10 seconds after it stops changing it is copied into the `backup` folder. The last 100 files are kept.
2) F8: Copy the save file to the `save` folder. The last 100 files are kept.

A number of hotkeys are provided to make it easy to restore the file you want:

* F1: Replace the save file with the last file in the `save` folder.
* F2: Replace the save file with the last file in the `backup` folder older than the current save file. Pressing this multiple times results in an older backup file.
* F3: Restart the game.
* F4: Close the game.
* F5: Replace the save file with the last file in the `backup` or `save` folders, whichever is newer, then restart the game.

Should something go wrong, you can always just grab one of the files from the `save` or `backup` folder and manually copy it to replace the `DRAKS0005.sl2` save file (or whatever the name is of the save file for the game you are playing). For example, if you realize you made a mistake 10 minutes ago, you can find a save file from before the mistake.

# Should you use this app?

Ultimately it's of course up to you. Many players use a guide/walkthrough, forums/reddit, or look up spoilers or other information. All of that is cheating the experience intended by the game designers in some way, as is using this app. Definitely try the game without this app first (and especially without a guide or spoilers!).

Here are a few reasons to use this app:

1) Dark Souls has many difficult bosses and areas that require trekking a long distance while fighting many enemies, just to get back to the place where you die extremely easily. That makes learning how to fight a boss or pass an area very time consuming and frustrating.
2) Some enemies, such as skeleton dogs or a number of bosses, have an attack chosen at random that will almost certainly kill you without having a chance to avoid it. Other times an enemy will use their most powerful attack many times in a row, resulting in an unfair death.
3) Some areas of Dark Souls are just plain unfair and ensure you die before you know the gimmick, for example when a dragon burns you to death without any kind of warning.
4) Unfortunate events can occur that majorly wreck your enjoyment of the game, such as accidentally attacking an NPC because it looks exactly the same as enemies in the area, or because you simply pressed the wrong button or dropped the controller.
5) Some poorly worded dialog, such as that of Alvina the cat, ends in a yes/no choice without having asked a yes/no question. Answering incorrectly may block off a path you wanted to take.

The game designers intended the game to be frustrating, for dying to be unfair with a high penalty, and for events to be permanent. However, you are playing for your own enjoyment and entertainment. If you dislike how the game was intended to be played and/or don't have enough free time to slog through the same areas of the game repeatedly, you are free to spend your entertainment time playing it however you wish. Many people would not play Dark Souls at all without this app or one like it, and that would be even more unfortunate than experiencing the game differently from how it was designed.
