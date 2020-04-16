# Dark Souls Saver

This app allows you to just play Dark Souls as normal and if you die, press F5 within 10 seconds of dying to:
a) replace the game's save file with the last backup where you are alive,
b) close Dark Souls, and
c) start Dark Souls again.

If you want more control, you can use F8 to manually store the game's save file and F1 to replace it with the last one you stored manually.

When you use F5 it will use the last manually stored file if that is newer than the last backup. That is almost always what you want, but if not you can still use F1 to get the manually stored file.

The game updates its save file when you kill an enemy, pick up an item, close the game menu, and at many other times. To force it to update the save file, just open and close the game menu. Then you can press F8 and you've saved where you are currently standing. This is useful for example right outside a boss fog wall!

# Should you use this app?

Ultimately it's up to you. Many players use a guide/walkthrough, forums/reddit, or look up spoilers or other information. All of that is cheating the experience intended by the game designers in some way, as is using this app. Definitely try the game without this app first (and especially without a guide or spoilers!).

Here are a few reasons to use this app:

1) Dark Souls has many difficult bosses and areas that require trekking a long distance while fighting many enemies, just to get back to the place where you die extremely easily. That makes learning how to fight a boss or pass an area very time consuming and frustrating.
2) Some enemies, such as skeleton dogs or a number of bosses, have an attack chosen at random that will almost certainly kill you without having a chance to avoid it. Other times an enemy will use their most powerful attack many times in a row, resulting in an unfair death.
3) Some areas of Dark Souls are just plain unfair and ensure you die before you know the gimmick, for example when a dragon burns you to death without any kind of warning.
4) Unfortunate events can occur that majorly wreck your enjoyment of the game, such as accidentally attacking an NPC because it looks exactly the same as enemies in the area, or because you simply pressed the wrong button or dropped the controller.
5) Some poorly worded dialog, such as that of Alvina the cat, ends in a yes/no choice without having asked a yes/no question. Answering incorrectly may block off your a path you wanted to take.

The game designers intended the game to be frustrating, for dying to be unfair with a high penalty, and for events to be permanent. However, you are playing for your own enjoyment and entertainment. If you dislike how the game was intended to be played and/or don't have enough free time to slog through the same areas of the game repeatedly, you are free to spend your entertainment time playing it however you wish. Many people would not play Dark Souls at all without this app or one like it, and that would be even more unfortunate than experiencing the game differently from how it was designed.

# Running the app

Install Java 8+, download the JAR from the [releases page](https://github.com/EsotericSoftware/dark-souls-saver/releases), and run it like this:

```
java -jar dark-souls-saver.jar "C:\Users\USERNAME\Documents\NBGI\DARK SOULS REMASTERED\SOME_NUMBER\DRAKS0005.sl2" "C:\Games\Steam\Steam.exe"
```

Replace `USERNAME`, `SOME_NUMBER`, and the path to `Steam.exe` as needed. The `save` and `backup` folders are created in the folder where it is run.

# How it works

Dark Souls works by writing a game save file whenever a significant event happens. This is used to restore your game should it crash or close unexpectedly. By keeping copies of the save file, we can later replace the save file with an older copy to restore a previous game state.

This app has a few parts:

1) It watches the save file and if it changes, 10 seconds after it stops changing it is copied into the `backup` folder. The last 100 files are kept.
2) F8: Hotkey to copy the save file to the `save` folder. The last 100 files are kept.
3) F1: Hotkey to replace the save file with the last file in the `save` folder.
4) F5: Hotkey to replace the save file with the last file in the `backup` folder then kill and restart the game.

Should something go wrong, you can always just grab one of the files from the `save` or `backup` folder and manually copy it to replace the `DRAKS0005.sl2` save file (or whatever the name is of the save file for the game you are playing). For example, if you realize you made a mistake 10 minutes ago, you can find a save file from before the mistake.

# Game versions

This app works specifically for Dark Souls: Remastered by default, however the file watching and backups can work for any Dark Souls game, or really any game that uses a game save file. The only parts specific to Dark Souls: Remastered are to restart the game. To restart other games, change the code where you find `DarkSoulsRemastered.exe` and `-applaunch 570940`.
