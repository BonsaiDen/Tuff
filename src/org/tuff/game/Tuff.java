/**
 *	Version 0.7
 *	Copyright (C) 2009-2010 Ivo Wetzel
 *	<http://github.com/BonsaiLeaf/Tuff>
 *
 *
 *  This file is part of Tuff.
 *
 *  Tuff is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Tuff is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License along with
 *  Tuff. If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package org.tuff.game;

import java.applet.Applet;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import org.bonsai.dev.SoundObjectWav;

public final class Tuff extends org.bonsai.dev.Game {
	private static final long serialVersionUID = 5085427564220826035L;
	
	// Main
	public Player player;
	public TuffMap map;
	public Color bgColor = new Color(186, 186, 186);
	private List<Integer> consoleKeys = new ArrayList<Integer>();
	{
		consoleKeys.add(java.awt.event.KeyEvent.VK_UP);
		consoleKeys.add(java.awt.event.KeyEvent.VK_UP);
		consoleKeys.add(java.awt.event.KeyEvent.VK_DOWN);
		consoleKeys.add(java.awt.event.KeyEvent.VK_DOWN);
		consoleKeys.add(java.awt.event.KeyEvent.VK_LEFT);
		consoleKeys.add(java.awt.event.KeyEvent.VK_RIGHT);
		consoleKeys.add(java.awt.event.KeyEvent.VK_LEFT);
		consoleKeys.add(java.awt.event.KeyEvent.VK_RIGHT);
		consoleKeys.add(java.awt.event.KeyEvent.VK_B);
		consoleKeys.add(java.awt.event.KeyEvent.VK_A);
	}
	private boolean consoleAcitvated = false;
	private boolean showFPS = false;

	// Message
	private String textMessage = "";
	private long textMessageTime;

	// Flashing
	private BufferedImage flashImage;
	private BufferedImage flashImageDark;
	private BufferedImage currentFlashImage;
	private float flashValue;
	private boolean unflash;

	// Loading
	private BufferedImage loadingImage;

	// Stuff
	protected static boolean release = true;

	// Sound
	private boolean musicActive = true;
	private boolean soundActive = true;
	private float musicVolume = 0.5f;
	private int oldMusic = -1;
	private int currentMusic = 0;
	private int playingMusic = -1;
	private int musicFadeTime = 1000;

	// Init --------------------------------------------------------------------
	@Override
	public void initGame(final boolean loaded) {
		if (loaded) {
			// Menus
			menu.add("Sound");
			menu.addCheckItem("Sound", "Effects", "sound");
			menu.addCheckItem("Sound", "Music", "music");

			// Game
			player = new Player(this, 30, 40);
			map = new TuffMap(this, !release);
			player.setMap(map);

			// Sound
			sound.addType("wav", SoundObjectWav.class);
			sound.addType("ogg", org.bonsai.dev.SoundObjectOgg.class);
			// sound.addType("xm", org.bonsai.dev.SoundObjectXm.class);
			sound.load("walk", "/sounds/walk.wav");
			sound.load("jump", "/sounds/jump.wav");
			sound.load("land", "/sounds/land.wav");
			sound.load("wall", "/sounds/wall.wav");
			sound.load("dive", "/sounds/dive.ogg");
			sound.load("splash", "/sounds/splash.wav");
			sound.load("swim", "/sounds/swim.wav");
			sound.load("save", "/sounds/save.ogg");
			sound.load("warp", "/sounds/warp.ogg");
			sound.load("switch", "/sounds/switches.wav");
			sound.load("music0", "/sounds/music0.ogg");
			sound.load("music1", "/sounds/music1.ogg");
			sound.load("music2", "/sounds/music2.ogg");

			// sound.load("music0", "/sounds/world.xm");
			// sound.load("music1", "/sounds/cave.xm");
			// sound.load("music2", "/sounds/water.xm");

			// Timer
			timer.add("music", 1000);

			// Flash
			flashImage = createFlashImage(Color.WHITE);
			flashImageDark = createFlashImage(Color.BLACK);

			// Load
			load();

			// Music
			currentMusic = map.getSoundAt(player.posX / map.tileSize,
					(player.posY - 7) / map.tileSize);

			// profile((Object)this);

		} else {
			font.load("/images/text.png", 8, 6,
					"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-:.|x/");
			loadingImage = image.get("/images/loading.png");
		}
	}

	public void initApplet(final Applet applet) {
		backgroundColor = bgColor;
	}
	
	// Menus -------------------------------------------------------------------
	public void onMenu(String id) {
		if (id.equals("music")) {
			toggleMusic();

		} else if (id.equals("sound")) {
			toggleSound();

		} else if (id.equals("new")) {
			map.editor.newMap();

		} else if (id.equals("open")) {
			if (map.editor.open(getFrame())) {
				map.editor.openMap();
			}
		} else if (id.equals("save")) {
			if (map.editor.save(getFrame())) {
				map.editor.saveMap();
			}
		}
	}

	public void onConsole(String cmd) {
		if (cmd.equals("max")) {
			player.hasHighJump = true;
			player.hasWallJump = true;
			player.hasDive = true;
			player.hasSuperJump = true;
			player.hasSpeed = true;
			player.hasBreak = true;
			console.print("Gotta da power!");

		} else if (cmd.equals("fps")) {
			showFPS = !showFPS;
			if (showFPS) {
				console.print("What's your FPS?");
			}

		} else if (cmd.equals("dry")) {
			map.noWater = !map.noWater;
			if (map.noWater) {
				console.print("Summertime!");
			}

		} else if (cmd.equals("waste")) {
			map.wasteLand = !map.wasteLand;
			if (map.wasteLand) {
				console.print("In a desert far away...");
			}

		} else if (cmd.equals("warp")) {
			for (int i = 0; i < map.mapObjects.size(); i++) {
				int[] p = map.mapObjects.get(i);
				if (p[0] == 0 && p[3] == 3) {
					p[3] = 4;
				}
			}
			console.print("Woooooooosh");

		} else if (cmd.equals("sound")) {
			map.showSound = !map.showSound;
			if (map.showSound) {
				console.print("Where does the noise come from?");
			}

		} else if (cmd.equals("nohide")) {
			map.noHide = !map.noHide;
			if (map.noHide) {
				console.print("Where are you?");
			}

		} else if (cmd.equals("edge")) {
			map.noBorders = !map.noBorders;
			map.tileGen.reset();
		}
	}

	// Update ------------------------------------------------------------------
	@Override
	public void updateGame(final boolean loaded) {
		if (loaded) {
			// Music
			int m = map.getSoundAt(player.posX / map.tileSize,
					(player.posY - 7) / map.tileSize);
			if (m != -1 && m != currentMusic) {
				if (currentMusic == 2) {
					if (timer.expired("outWater")) {
						currentMusic = m;
						timer.set("music", -1000);
					}

				} else if (m != 2 || timer.expired("fullWater")) {
					currentMusic = m;
					if (m == 2) {
						timer.set("music", -1000);
					}
				}
				if (!musicActive) {
					playingMusic = currentMusic;
				}
			}
			if (musicActive) {
				if (oldMusic == currentMusic) {
					timer.set("music");
				}
				if (timer.expired("music")) {
					if (oldMusic == -1) {
						sound.setFadeVolume("music"
								+ Integer.toString(currentMusic), musicVolume,
								musicFadeTime);

					} else {
						sound.setFadeVolume("music"
								+ Integer.toString(oldMusic), 0.0f,
								musicFadeTime);
						sound.setFadeVolume("music"
								+ Integer.toString(currentMusic), musicVolume,
								musicFadeTime);

					}
					playingMusic = currentMusic;
					oldMusic = currentMusic;
				}
			}

			// Toggle Editor
			if (input.keyPressed(java.awt.event.KeyEvent.VK_ENTER)
					&& (!release)) {
				player.hasControl = !player.hasControl;
				if (player.hasControl) {
					map.editor.saveUndo();
					player.posX = (map.editor.selectX + 1) * map.tileSize - 7;
					player.posY = (map.editor.selectY + 1) * map.tileSize;
				} else {
					map.resetTransparency();
					map.editor.loadUndo();
				}
				map.updateLocal();
			}
			if (!player.hasControl) {
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F4)) {
					player.blocksOpened = new ArrayList<int[]>();
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F5)) {
					player.switchesToggled = new boolean[4];
				}

				if (input.keyPressed(java.awt.event.KeyEvent.VK_F6)) {
					toggleWarps();
				}

				if (input.keyPressed(java.awt.event.KeyEvent.VK_F7)) {
					player.hasHighJump = !player.hasHighJump;
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F8)) {
					player.hasWallJump = !player.hasWallJump;
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F9)) {
					player.hasDive = !player.hasDive;
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F10)) {
					player.hasSuperJump = !player.hasSuperJump;
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F11)) {
					player.hasSpeed = !player.hasSpeed;
				}
				if (input.keyPressed(java.awt.event.KeyEvent.VK_F12)) {
					player.hasBreak = !player.hasBreak;
				}

				if (input.keyPressed(java.awt.event.KeyEvent.VK_F1)) {
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Dimension size = toolkit.getScreenSize();
					int width = (int) size.getWidth()
							- (getFrame().getInsets().left + getFrame().getInsets().right);
					width = (width / 16) * 16;

					int height = (int) size.getHeight()
							- (getFrame().getInsets().top
									+ getFrame().getInsets().bottom
									+ menu.getSize() + 32);
					height = (height / 16) * 16;
					setSize(width, height);

					map.screenWidth = width() / map.tileSize;
					map.screenHeight = height() / map.tileSize;
				}
			} else {
				// Music
				if (input.keyPressed(java.awt.event.KeyEvent.VK_M)) {
					toggleMusic();
				}

				// Sound
				if (input.keyPressed(java.awt.event.KeyEvent.VK_E)) {
					toggleSound();
				}
			}

			// Screenshot
			if (input.keyPressed(java.awt.event.KeyEvent.VK_PRINTSCREEN)) {
				try {
					Calendar cal = Calendar.getInstance();
					String foo = String.format(
							"screenshot %d-%d-%d %d-%d-%d.png",
							cal.get(Calendar.DATE),
							cal.get(Calendar.MONTH) + 1,
							cal.get(Calendar.YEAR),
							cal.get(Calendar.HOUR_OF_DAY),
							cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

					ImageIO.write(image.getScreen(), "png", new File(
							getBasePath() + foo));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Control
			if (textMessage == "") {
				player.control();
			}
			map.control(player);

			// Flash
			if (flashValue > 0.0f) {
				if (unflash) {
					double dec = Math.log(flashValue) * 1.5;
					if (dec < 1) {
						dec = 1;
					}
					flashValue -= dec;
					if (flashValue < 0.0f) {
						flashValue = 0.0f;
					}
				} else {
					unflash = true;
				}
			}
		}
	}

	@Override
	public void renderGame(final boolean loaded, final Graphics2D g) {
		if (loaded) {
			// Draw#
			map.draw(g, player);
			player.draw(g);
			map.drawDefer(g, player);

			// Info
			if (!player.hasControl) {
				showFPS(g, 2, 2);
				String can = "";
				can += "HIGH: " + (player.hasHighJump ? "YES" : "NO") + "\n";
				can += "WALL: " + (player.hasWallJump ? "YES" : "NO") + "\n";
				can += "DIVE: " + (player.hasDive ? "YES" : "NO") + "\n";
				can += "SUPER: " + (player.hasSuperJump ? "YES" : "NO") + "\n";
				can += "SPEED: " + (player.hasSpeed ? "YES" : "NO") + "\n";
				can += "BREAK: " + (player.hasBreak ? "YES" : "NO") + "\n";
				font.draw(g, can, 2, 48);

			} else {
				showShards(g, 2, 2);
				if (showFPS) {
					showFPS(g, 2, 16);
				}
			}

			// Flash
			if (flashValue > 0.0f) {
				Composite tmp = g.getComposite();
				AlphaComposite alphaComposite = AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, (float) (flashValue / 255.0));
				g.setComposite(alphaComposite);
				g.drawImage(currentFlashImage, null, 0, 0);
				g.setComposite(tmp);
			}

			// Pause
			if (isPaused()) {
				fadeScreen(g);
				centerText(g, "PAUSED", -8);

			} else if (textMessage != "") {
				fadeScreen(g);
				centerText(g, "YOU GOT", -10);
				centerText(g, textMessage, 6);
				if (getTime() > textMessageTime + 1500) {
					textMessage = "";
					pauseMusic(false);
					animationTime(false);
				}
			}

		} else {
			g.setColor(bgColor);
			g.fillRect(0, 0, width(), height());
			g.drawImage(loadingImage, (width() - 96) / 2, (height() - 96) / 2,
					null);
			centerText(g, "LOADING", -43);
		}
	}

	private void showShards(Graphics2D g, int x, int y) {
		g.drawImage(map.shardTiles[animation.get("shard")], x - 2, y - 4, null);
		font.draw(g, "x " + player.entitiesCollected.size() + "/"
				+ map.shardCount, x + 12, y);
	}

	private void showFPS(Graphics2D g, int x, int y) {
		font.draw(g, String.format("FPS: %d/%d\nIMG: %d", getFPS(),
				getMaxFPS(), map.objImgCount), x, y);
	}

	@Override
	public void finishGame(final boolean loaded) {
		if (loaded) {
			sound.stop("music0");
			sound.stop("music1");
			sound.stop("music2");
		} else {
			loadingImage = null;
			if (!hasSound()) {
				musicActive = false;
				soundActive = false;
			} else {
				startMusic();
				timer.set("music", -1000);
			}
			menu.enable("Game", true);
			menu.enable("Sound", hasSound());
			menu.select("sound", soundActive);
			menu.select("music", musicActive);
		}
	}

	// Text --------------------------------------------------------------------
	public void centerText(Graphics2D g, String text, int yp) {
		int width = font.width(text) / 2;
		font.draw(g, text, width() / 2 - width, height() / 2 + yp);
	}

	public void textMessage(String text) {
		pauseMusic(true);
		animationTime(true);
		textMessage = text;
		textMessageTime = getTime();
	}

	// Flash -------------------------------------------------------------------
	private BufferedImage createFlashImage(Color color) {
		BufferedImage img = image.create(width(), height());
		Graphics2D g = img.createGraphics();
		g.setColor(color);
		g.fillRect(0, 0, width(), height());
		return img;
	}

	public void flashScreen() {
		flashValue = 200;
		currentFlashImage = flashImage;
		unflash = false;
	}

	public void flashSave() {
		flashValue = 255;
		currentFlashImage = flashImage;
		unflash = false;
	}

	public void flashWarp() {
		flashValue = 255;
		currentFlashImage = flashImageDark;
		unflash = false;
	}

	public void flashSwitch() {
		flashValue = 220;
		currentFlashImage = flashImage;
		unflash = false;
		playSound("switch", false);
	}

	public void flashWarpActivate() {
		flashValue = 200;
		currentFlashImage = flashImageDark;
		unflash = false;
	}

	public void fadeScreen(Graphics2D g) {
		Composite tmp = g.getComposite();
		AlphaComposite alphaComposite = AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, 0.5f);
		g.setComposite(alphaComposite);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width(), height());
		g.setComposite(tmp);
	}

	// Music -------------------------------------------------------------------
	private void pauseMusic(boolean mode) {
		sound.pause("music" + Integer.toString(playingMusic), mode);
	}

	// Music
	public void startMusic() {
		sound.play("music0", false, true, 0.0f);
		sound.play("music1", false, true, 0.0f);
		sound.play("music2", false, true, 0.0f);
	}

	public void playSound(String id, boolean stop) {
		if (soundActive) {
			sound.play(id, stop);
		}
	}

	public void toggleMusic() {
		musicActive = !musicActive;
		sound.setFadeVolume("music" + Integer.toString(playingMusic),
				musicActive ? (player.sleeping ? 0.1f : musicVolume) : 0.0f,
				musicFadeTime);

		oldMusic = -1;
		menu.select("music", musicActive);
	}

	public void onSleep(boolean sleeping) {
		if (musicActive) {
			sound.setFadeVolume("music" + Integer.toString(playingMusic),
					sleeping ? 0.1f : musicVolume, musicFadeTime);
		}
	}

	public void toggleSound() {
		soundActive = !soundActive;
		menu.select("sound", soundActive);
	}

	// Stuff
	public void toggleWarps() {
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if (p[0] == 3) {
				p[0] = 4;
			} else if (p[0] == 4) {
				p[0] = 3;
			}
		}
	}

	// Console
	public boolean consoleKey() {
		if (isConsoleOpen() || consoleAcitvated) {
			return input.keyDown(java.awt.event.KeyEvent.VK_SHIFT, true)
					&& input.keyPressed(java.awt.event.KeyEvent.VK_F1, true);
		} else {
			if (input.keySequence(consoleKeys, true)) {
				consoleAcitvated = true;
				return true;
			}
			return false;
		}
	}

	// Save & Load -------------------------------------------------------------
	public String getBasePath() {
		String path = getPath();
		if (!isJar()) {
			path = path.substring(0, path.length() - 4);
		}
		return path;
	}

	public void save() {
		if (saveGame(getBasePath() + "tuffsave.dat", "save")) {
			flashSave();
			playSound("save", false);
		}
	}

	public void load() {
		loadGame(getBasePath() + "tuffsave.dat", "save");
	}

	public void writeSave(OutputStream stream) throws IOException {
		DataOutputStream data = new DataOutputStream(stream);

		// Settings
		data.writeChars("OPT");
		data.writeBoolean(soundActive);
		data.writeBoolean(musicActive);

		// Position
		data.writeChars("POS");
		data.writeInt(player.posX / map.tileSize);
		data.writeInt(player.posY / map.tileSize);

		// Abilities
		data.writeChars("ABT");
		data.writeBoolean(player.hasHighJump);
		data.writeBoolean(player.hasWallJump);
		data.writeBoolean(player.hasDive);
		data.writeBoolean(player.hasSuperJump);
		data.writeBoolean(player.hasBreak);
		data.writeBoolean(player.hasSpeed);
		data.writeBoolean(false);
		data.writeBoolean(false);

		// Entities
		data.writeChars("ENT");
		data.writeInt(player.entitiesCollected.size());
		for (int e[] : player.entitiesCollected) {
			data.writeInt(e[0]);
			data.writeInt(e[1]);
		}

		// Warps Active
		data.writeChars("WAP");
		List<int[]> warpObjects = map.getWarpsActive();
		data.writeInt(warpObjects.size());
		for (int w[] : warpObjects) {
			data.writeInt(w[1]);
			data.writeInt(w[2]);
		}

		// Blocks opened
		data.writeChars("BLO");
		data.writeInt(player.blocksOpened.size());
		for (int b[] : player.blocksOpened) {
			data.writeInt(b[0]);
			data.writeInt(b[1]);
		}

		// Switches toggled
		data.writeChars("SWI");
		data.writeBoolean(player.switchesToggled[0]);
		data.writeBoolean(player.switchesToggled[1]);
		data.writeBoolean(player.switchesToggled[2]);
		data.writeBoolean(player.switchesToggled[3]);

		data.flush();
		data.close();
	}

	public void readSave(InputStream stream) throws IOException {
		// make sure we can read old save files
		byte[] bytes = new byte[stream.available()];
		int offset = 0;
		int numRead;
		while (offset < bytes.length
				&& (numRead = stream.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// New Save files
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(
				bytes.clone()));
		if (readString(data, 3).equals("OPT")) {
			soundActive = data.readBoolean();
			musicActive = data.readBoolean();

			// Position
			readString(data, 3);
			player.posX = data.readInt() * map.tileSize + 8;
			player.posY = data.readInt() * map.tileSize;
			player.onGround = true;

			// Abilities
			readString(data, 3);
			player.hasHighJump = data.readBoolean();
			player.hasWallJump = data.readBoolean();
			player.hasDive = data.readBoolean();
			player.hasSuperJump = data.readBoolean();
			player.hasBreak = data.readBoolean();
			player.hasSpeed = data.readBoolean();
			data.readBoolean();
			data.readBoolean();

			// Entities
			readString(data, 3);
			int count = data.readInt();
			for (int i = 0; i < count; i++) {
				player.entitiesCollected.add(new int[] { data.readInt(),
						data.readInt() });
			}

			// Warps
			readString(data, 3);
			count = data.readInt();
			for (int i = 0; i < count; i++) {
				map.activateWarp(data.readInt(), data.readInt());
			}

			// Blocks
			try {
				readString(data, 3);
				count = data.readInt();
				for (int i = 0; i < count; i++) {
					player.blocksOpened.add(new int[] { data.readInt(),
							data.readInt() });
				}
			} catch (Exception e) {

			}

			// Switches toggled
			try {
				readString(data, 3);
				player.switchesToggled[0] = data.readBoolean();
				player.switchesToggled[1] = data.readBoolean();
				player.switchesToggled[2] = data.readBoolean();
				player.switchesToggled[3] = data.readBoolean();
			} catch (Exception e) {

			}

			map.updateLocal();

			// Old ones
		} else {
			stream = new ByteArrayInputStream(bytes);

			// Settings
			soundActive = stream.read() == 1;
			musicActive = stream.read() == 1;

			// Position
			player.posX = stream.read() * map.tileSize + 8;
			player.posY = stream.read() * map.tileSize;
			player.onGround = true;

			// Abilities
			player.hasHighJump = stream.read() == 1;
			player.hasWallJump = stream.read() == 1;
			player.hasDive = stream.read() == 1;
			player.hasSuperJump = stream.read() == 1;
			player.hasBreak = stream.read() == 1;
			stream.read();
			stream.read();
			stream.read();

			// Entities
			int count = stream.read();
			for (int i = 0; i < count; i++) {
				player.entitiesCollected.add(new int[] { stream.read(),
						stream.read() });
			}
			map.updateLocal();

			// Warps
			try {
				count = stream.read();
				for (int i = 0; i < count; i++) {
					map.activateWarp(stream.read(), stream.read());
				}
			} catch (Exception e) {

			}
		}
		bytes = null;
		data.close();
	}

	public String readString(DataInputStream in, int length) throws IOException {
		String str = "";
		for (int i = 0; i < length; i++) {
			str += in.readChar();
		}
		return str;
	}

	// public void profile(Object obj) {
	// IObjectProfileNode profile = ObjectProfiler.profile(obj);
	//
	// System.out.println("obj size = " + profile.size() + " bytes");
	// // System.out.println (profile.dump ());
	// // System.out.println ();
	//
	// // dump the same profile, but now only show nodes that are at least
	// // 25% of 'obj' size:
	// System.out.println("size fraction filter with threshold=0.05:");
	// final PrintWriter out = new PrintWriter(System.out);
	// profile.traverse(ObjectProfileFilters.newSizeFractionFilter(0.050),
	// ObjectProfileVisitors.newDefaultNodePrinter(out, null, null,
	// true));
	// }

	// Init --------------------------------------------------------------------
	public static void main(String[] args) {
		new Tuff().frame("Tuff (c) 2009 by Ivo Wetzel", release ? 160 : 480,
				release ? 144 : 480, release, true, true);
	}
}
