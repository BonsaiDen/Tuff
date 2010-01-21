/**
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bonsai.dev.GameObject;

public class MapEditor extends GameObject<Tuff> {

	// Images
	private BufferedImage mapImage;
	private BufferedImage mapBuffer;
	private BufferedImage[] select = new BufferedImage[2];

	// Modes
	private int editMode = 0;
	private int editMode2 = 0;
	private boolean changed = false;
	private boolean edited = false;
	private long mapTime = 0;
	private boolean giantMap = false;

	// Edit
	private TuffMap map;
	private ArrayList<int[][]> undoData = new ArrayList<int[][]>();
	private String[] editModes = new String[] { "NORMAL", "BACKGROUND",
			"BREAKABLE", "WATER", "COLLISION" };

	// Offsets
	private int mapOffsetXOld = -1;
	private int mapOffsetYOld = -1;
	protected int selectX;
	protected int selectY;
	private int oldTX = -1;
	private int oldTY = -1;
	public int selectSize = -1;
	private int treeEdit = -1;
	private int blockEdit = -1;

	// Files
	private JFileChooser fc;
	private String filename = "level.lvl";

	public MapEditor(Tuff g, TuffMap map) {
		super(g);
		this.map = map;
		select[0] = createSelectImage(map.tileSize);
		select[1] = createSelectImage(map.tileSize * 2);
		menu.add("Editor");
		menu.addItem("Editor", "New", "new");
		menu.addItem("Editor", "Open...", "open");
		menu.addItem("Editor", "Save...", "save");
	}

	// Dialogs -----------------------------------------------------------------
	public String levelPath() {
		String path = getClass().getResource("/").getPath().substring(1)
				.replace("%20", " ");
		path = path.substring(0, path.length() - 4) + "src/levels";
		return path;
	}

	public String getLevelName(String filename) {
		if (filename.indexOf('/') != -1) {
			return filename.substring(filename.lastIndexOf('/') + 1);
		} else {
			return filename.substring(filename.lastIndexOf('\\') + 1);
		}
	}

	public void initDialog() {
		if (fc == null) {
			fc = new JFileChooser(levelPath());
			FileFilter filter = new FileNameExtensionFilter("Tuff Level", "lvl");
			fc.setFileFilter(filter);
		}
		fc.setSelectedFile(new File(getLevelName(filename)));
	}

	public boolean open(Component parent) {
		initDialog();
		int data = fc.showOpenDialog(parent);
		if (data == JFileChooser.APPROVE_OPTION) {
			filename = fc.getSelectedFile().getPath();
			return true;

		} else {
			return false;
		}
	}

	public boolean save(Component parent) {
		initDialog();
		int overwrite = JOptionPane.NO_OPTION;
		while (overwrite == JOptionPane.NO_OPTION) {
			int data = fc.showSaveDialog(parent);
			if (data == JFileChooser.APPROVE_OPTION) {
				filename = fc.getSelectedFile().getPath();
				if (new File(filename).exists()) {
					overwrite = JOptionPane.showConfirmDialog(parent,
							"Are you sure to overwrite the file \""
									+ getLevelName(filename) + "\"?",
							"Overwrite", JOptionPane.YES_NO_OPTION);

					if (overwrite == JOptionPane.YES_OPTION) {
						return true;
					}
				} else {
					return true;
				}
			} else {
				return false;
			}
		}
		return false;
	}

	public void newMap() {
		synchronized (map) {
			map.newMap(255, 255);
			undoData.clear();
		}
	}

	public void openMap() {
		synchronized (map) {
			// System.out.println("open map: " + filename);
			if (!map.loadMap(filename)) {
				undoData.clear();
				JOptionPane.showMessageDialog(game.getFrame(),
						"The file isn't a valid Tuff map.", "Error",
						JOptionPane.OK_OPTION);
			}
		}
	}

	public void saveMap() {
		// System.out.println("save map: " + filename);
		saveMap(filename);
		// level = filename;
	}

	private BufferedImage createSelectImage(int size) {
		BufferedImage img = image.create(size, size);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.RED);
		g.drawRect(0, 0, size - 1, size - 1);
		return img;
	}

	// Save --------------------------------------------------------------------
	public void saveMap(String filename) {
		long time = System.nanoTime();
		try {
			if (!filename.endsWith(".lvl")) {
				filename += ".lvl";
			}

			File file = new File(filename);
			OutputStream stream = new BufferedOutputStream(
					new FileOutputStream(file));
			DataOutputStream out = new DataOutputStream(stream);

			// Header
			out.writeChars("TUFF2");
			out.writeInt(map.mapWidth);
			out.writeInt(map.mapHeight);
			out.writeInt(map.startX);
			out.writeInt(map.startY);

			// Data
			out.writeChars("MAP");
			int colCount = 0;
			for (int y = 0; y < map.mapHeight; y++) {
				for (int x = 0; x < map.mapWidth; x++) {
					out.writeByte(map.mapData[x][y]);
					if (map.colData[x][y] == 1) {
						colCount++;
					}
				}
			}

			// Coldata
			out.writeChars("COL");
			out.writeInt(colCount);
			for (int y = 0; y < map.mapHeight; y++) {
				for (int x = 0; x < map.mapWidth; x++) {
					if (map.colData[x][y] == 1) {
						out.writeInt(x);
						out.writeInt(y);
					}
				}
			}

			// Objects
			out.writeChars("OBJ");
			out.writeInt(map.mapObjects.size());
			for (int[] p : map.mapObjects) {
				out.writeByte(p[0]);
				if (p[0] == 0) {
					out.writeByte(p[3] == 4 ? 3 : p[3]);
					out.writeInt(p[1]);
					out.writeInt(p[2]);
					if (p[3] == 0) {
						out.writeByte(p[4]);
					}
					if (p[3] == 5) {
						out.writeInt(p[4]);
					}
				} else {
					out.writeByte(p[3]);
					out.writeInt(p[1]);
					out.writeInt(p[2]);
				}
			}

			// // Trees
			// out.writeChars("TRE");
			// out.writeInt(map.trees.size());
			// for (int[] tree : map.trees) {
			// out.writeInt(tree[0]);
			// out.writeInt(tree[1]);
			// out.writeByte(tree[2]);
			// }

			out.close();
			stream.close();
			System.out.println("Saved: " + filename);
			System.out.printf("Took %dms\n",
					(System.nanoTime() - time) / 1000000);
			System.out.println(Integer.toString(colCount) + " Cols");
			System.out.println(Integer.toString(map.mapObjects.size())
					+ " Objects");
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		} catch (IOException e) {

		}
	}

	// Control -----------------------------------------------------------------
	public void control() {

		// Save Map
		// if (input.keyPressed(java.awt.event.KeyEvent.VK_F1)) {
		// saveMap(level);
		// }

		// Update Minimap
		if (game.getTime() > mapTime + 250) {
			if (changed) {
				updateMap();
			}
			if (map.mapOffsetX != mapOffsetXOld
					|| map.mapOffsetY != mapOffsetYOld) {
				refreshMap(giantMap);
				mapOffsetXOld = map.mapOffsetX;
				mapOffsetYOld = map.mapOffsetY;
			}
			mapTime = game.getTime();
		}

		// Mouse Position
		int mouseX = input.mouseX();
		int mouseY = input.mouseY();
		int tx = ((mouseX + map.scrollOffsetX) / map.tileSize) + map.mapOffsetX;
		int ty = ((mouseY + map.scrollOffsetY) / map.tileSize) + map.mapOffsetY;
		if (tx != oldTX || ty != oldTY) {
			treeEdit = getTreeAt(tx, ty);
			blockEdit = getBlockAt(tx, ty);
		}

		// Edit
		if (treeEdit == -1 && blockEdit == -1) {
			selectX = tx;
			selectY = ty;
			selectSize = 0;
			if (tx != oldTX || ty != oldTY) {
				if (editMode < 4) {
					if (input.mouseDown(MouseEvent.BUTTON1)) {
						map.mapData[tx][ty] = (byte) (1 + editMode);
						map.drawData[tx][ty] = (byte) (1 + editMode);
						edited = true;
						if (map.mapData[tx][ty] != 1) {
							map.colData[tx][ty] = 0;
						}
					} else if (input.mouseDown(MouseEvent.BUTTON3)) {
						map.mapData[tx][ty] = (byte) editMode2;
						map.drawData[tx][ty] = (byte) editMode2;
						edited = true;
						if (map.mapData[tx][ty] != 1) {
							map.colData[tx][ty] = 0;
						}
					}

				} else if (editMode == 4) {
					if (input.mouseDown(MouseEvent.BUTTON1)
							&& map.mapData[tx][ty] == 1) {
						map.colData[tx][ty] = 1;
						edited = true;
					} else if (input.mouseDown(MouseEvent.BUTTON3)) {
						map.colData[tx][ty] = 0;
						edited = true;
					}
				}
				oldTX = tx;
				oldTY = ty;
			}
			if (!input.mouseDown(MouseEvent.BUTTON1)
					&& !input.mouseDown(MouseEvent.BUTTON3)) {
				if (edited) {
					changed = true;
					edited = false;
				}
				oldTX = -1;
				oldTY = -1;
			}

			// Trees
		} else if (blockEdit != -1) {
			int block[] = map.mapObjects.get(blockEdit);
			if (block != null) {
				selectX = block[1];
				selectY = block[2];
				selectSize = block[4] == 255 ? 0 : 1;
			}

			if (input.mousePressed(MouseEvent.BUTTON3)) {
				removeBlock(tx, ty);
				oldTX = tx;
				oldTY = ty;
			}

			// Blocks
		} else if (treeEdit != -1) {
			int tree[] = map.mapObjects.get(treeEdit);
			if (tree != null) {
				selectX = tree[1];
				selectY = tree[2];
				if (tree[3] <= 3) {
					selectY -= 1;
					selectSize = 1;
				} else {
					selectSize = 0;
				}
			}

			if (input.mousePressed(MouseEvent.BUTTON3)) {
				removeTree(tx, ty);
				oldTX = tx;
				oldTY = ty;
			}
		}

		// Switches
		if (input.keyDown(java.awt.event.KeyEvent.VK_ALT)) {
			if (input.keyPressed(java.awt.event.KeyEvent.VK_A)) {
				createSwitch(tx, ty, 6);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_B)) {
				createSwitch(tx, ty, 7);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_C)) {
				createSwitch(tx, ty, 8);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_D)) {
				createSwitch(tx, ty, 9);
			}

			// if (input.keyPressed(java.awt.event.KeyEvent.VK_1)) {
			// map.enemies.add(new Worm(game, tx, ty));
			// }

			// Switches Blocks
		} else if (input.keyDown(java.awt.event.KeyEvent.VK_SHIFT)) {
			if (input.keyPressed(java.awt.event.KeyEvent.VK_A)) {
				addBlock(tx, ty, 0);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_B)) {
				addBlock(tx, ty, 1);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_C)) {
				addBlock(tx, ty, 2);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_D)) {
				addBlock(tx, ty, 3);
			}

			// PowerUPS
		} else if (input.keyDown(java.awt.event.KeyEvent.VK_CONTROL)) {
			if (input.keyPressed(java.awt.event.KeyEvent.VK_W)) {
				createPowerUp(tx, ty, "W");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_J)) {
				createPowerUp(tx, ty, "J");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_D)) {
				createPowerUp(tx, ty, "D");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_U)) {
				createPowerUp(tx, ty, "U");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_S)) {
				createPowerUp(tx, ty, "S");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_B)) {
				createPowerUp(tx, ty, "B");
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_R)) {
				createEntity(tx, ty);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_Y)) {
				addBlock(tx, ty, 5);
			}

			if (input.keyPressed(java.awt.event.KeyEvent.VK_SPACE)) {
				map.startX = tx;
				map.startY = ty;
				changed = true;
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_F)) {
				createSave(tx, ty);
				changed = true;
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_G)) {
				createWarp(tx, ty);
				changed = true;
			}
		} else {
			// Add Trees
			if (input.keyPressed(java.awt.event.KeyEvent.VK_1)) {
				addTree(tx, ty, 0);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_2)) {
				addTree(tx, ty, 1);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_3)) {
				addTree(tx, ty, 2);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_4)) {
				addTree(tx, ty, 3);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_5)) {
				addTree(tx, ty, 4);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_6)) {
				addTree(tx, ty, 5);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_7)) {
				addTree(tx, ty, 6);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_8)) {
				addTree(tx, ty, 7);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_9)) {
				addTree(tx, ty, 8);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_PAGE_UP)) {
				changeBlock(tx, ty, 5);
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_PAGE_DOWN)) {
				changeBlock(tx, ty, -5);
			}

			if (input.keyPressed(java.awt.event.KeyEvent.VK_X)) {
				addBlock(tx, ty, 255);
			}

			// Undo
			if (input.keyPressed(java.awt.event.KeyEvent.VK_Z)) {
				if (undoData.size() > 0) {
					loadUndo();
				}
			}

			// Fill
			if (input.keyPressed(java.awt.event.KeyEvent.VK_F3)) {
				fill(tx, ty, editMode2, map.mapData[tx][ty]);
				map.render();
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_F4)) {
				fill(tx, ty, editMode + 1, map.mapData[tx][ty]);
				map.render();
			}

			// Mode
			if (input.keyPressed(java.awt.event.KeyEvent.VK_M)) {
				editMode = 2;
				editMode2 = 0;
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_B)) {
				editMode = 1;
				editMode2 = 1;
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_N)) {
				editMode = 0;
				editMode2 = 0;
			}
			if (input.keyPressed(java.awt.event.KeyEvent.VK_V)) {
				editMode = 3;
				editMode2 = 1;
			}

			if (input.keyPressed(java.awt.event.KeyEvent.VK_C)) {
				editMode = 4;
				editMode2 = 4;
			}
		}
		// Render Map
		if (changed && !input.mouseDown(MouseEvent.BUTTON1)
				&& !input.mouseDown(MouseEvent.BUTTON3)) {
			map.render();
			changed = false;
		}
	}

	public void saveUndo() {
		int[][] undo = new int[map.mapWidth][map.mapHeight];
		for (int y = 0; y < map.mapHeight; y++) {
			for (int x = 0; x < map.mapWidth; x++) {
				undo[x][y] = map.mapData[x][y];
			}
		}
		undoData.add(undo);
	}

	public void loadUndo() {
		if (undoData.size() > 0) {
			int[][] undo = undoData.remove(undoData.size() - 1);
			for (int y = 0; y < map.mapHeight; y++) {
				for (int x = 0; x < map.mapWidth; x++) {
					map.mapData[x][y] = (byte) undo[x][y];
				}
			}
			map.render();
		}
	}

	private void fill(int xi, int yi, int type, int replace) {
		if (type != replace) {
			saveUndo();
			List<int[]> list = new ArrayList<int[]>();
			list.add(new int[] { xi, yi });
			while (list.size() > 0) {
				int[] i = list.remove(0);
				int x = i[0];
				int y = i[1];
				if (map.mapData[x][y] == replace) {
					int xp = x;
					while (x >= 0) {
						if (map.mapData[x][y] == replace) {
							map.mapData[x][y] = (byte) type;

							// Up
							if (y - 1 >= 0 && map.mapData[x][y - 1] == replace) {
								list.add(new int[] { x, y - 1 });
							}

							// Down
							if (y + 1 <= map.mapHeight - 1
									&& map.mapData[x][y + 1] == replace) {
								list.add(new int[] { x, y + 1 });
							}

							x -= 1;
						} else {
							break;
						}
					}
					x = xp + 1;
					while (x <= map.mapWidth - 1) {
						if (map.mapData[x][y] == replace) {
							map.mapData[x][y] = (byte) type;

							// Up
							if (y - 1 >= 0 && map.mapData[x][y - 1] == replace) {
								list.add(new int[] { x, y - 1 });
							}

							// Down
							if (y + 1 <= map.mapHeight - 1
									&& map.mapData[x][y + 1] == replace) {
								list.add(new int[] { x, y + 1 });
							}

							x += 1;
						} else {
							break;
						}
					}
				}
			}
		}
	}

	// Rendering ---------------------------------------------------------------

	public void draw(Graphics2D g) {
		if (selectSize != -1) {
			g.setColor(Color.RED);
			g.drawRect(selectX * map.tileSize - map.screenOffsetX - 72, selectY
					* map.tileSize - map.screenOffsetY - 64, 160, 144);

			g.drawImage(select[selectSize], selectX * map.tileSize
					- map.screenOffsetX, selectY * map.tileSize
					- map.screenOffsetY, null);

			// Map
			Composite tmp = g.getComposite();

			if (input.keyDown(java.awt.event.KeyEvent.VK_SPACE)) {
				if (!giantMap) {
					refreshMap(true);
					giantMap = true;
				}
			} else {
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.75f));
				if (giantMap) {
					refreshMap(false);
					giantMap = false;
				}
			}
			g.drawImage(mapImage, map.screenWidth * map.tileSize
					- mapImage.getWidth(), map.screenHeight * map.tileSize
					- mapImage.getHeight(), null);
			g.setComposite(tmp);
			font.draw(g, "MODE: " + editModes[editMode], 2, 32);
		}
	}

	public void updateMap() {
		mapBuffer = image.create(map.mapWidth, map.mapHeight, false);
		Graphics2D g = mapBuffer.createGraphics();
		g.setColor(game.bgColor);
		g.fillRect(0, 0, map.mapWidth, map.mapHeight);
		g.setColor(Color.WHITE);
		for (int y = 0; y < map.mapHeight; y++) {
			for (int x = 0; x < map.mapWidth; x++) {
				int xp = x;
				int yp = y;
				int tile = map.mapData[x][y];
				if (tile > 0) {
					if (map.colData[x][y] == 1) {
						g.setColor(Color.LIGHT_GRAY);
					} else if (tile == 1) {
						g.setColor(Color.WHITE);
					} else if (tile == 2) {
						g.setColor(Color.DARK_GRAY);
					} else if (tile == 4) {
						g.setColor(Color.GRAY);
					}
					g.fillRect(xp, yp, 1, 1);
				}
			}
		}

		g.setColor(Color.RED);
		g.fillRect(map.startX - 4, map.startY - 4, 8, 8);

		for (int[] p : map.mapObjects) {
			if (p[0] == 0) {
				if (p[3] == 0) {
					g.setColor(new Color(255, 100, 0));
					g.fillRect(p[1] - 4, p[2] - 4, 8, 8);
				} else if (p[3] == 1) {
					g.setColor(new Color(0, 200, 0));
					g.fillRect(p[1] - 4, p[2] - 4, 8, 8);
				} else if (p[3] == 2) {
					g.setColor(new Color(0, 200, 255));
					g.fillRect(p[1] - 2, p[2] - 2, 4, 4);
				} else if (p[3] == 3 || p[3] == 4) {
					g.setColor(new Color(100, 0, 255));
					g.fillRect(p[1] - 4, p[2] - 4, 8, 8);
				} else if (p[3] >= 6) {
					g.setColor(new Color(255, 255, 0));
					g.fillRect(p[1] - 4, p[2] - 4, 8, 8);
				}
			}
		}
		refreshMap(giantMap);
	}

	private void refreshMap(boolean giant) {
		int ratio = map.mapWidth / map.mapHeight;
		boolean small = game.width() == 160;
		int scale = small ? 4 : 2;
		int width = small ? 64 : 128;
		int height = small ? 64 * ratio : 128 * ratio;
		if (giant) {
			scale = 1;
			width = 256;
			height = 256;
		}

		mapImage = image.create(width, height, false);
		Graphics2D bg = mapImage.createGraphics();
		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		bg.drawImage(mapBuffer, 0, 0, width, height, 0, 0, map.mapWidth,
				map.mapHeight, null);

		bg.setColor(Color.RED);
		bg.drawRect(map.mapOffsetX / scale, map.mapOffsetY / scale,
				map.screenWidth / scale, map.screenHeight / scale);
	}

	// Trees -------------------------------------------------------------------
	private boolean treeFree(int x, int y) {
		if (x < 0 || y < 0 || x > map.mapWidth - 1 || y > map.mapWidth - 1) {
			return true;
		} else {
			return map.mapData[x][y] == 1 && map.colData[x][y] == 0;
		}
	}

	private int getTreeAt(int x, int y) {
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int tree[] = map.mapObjects.get(i);
			if (tree[0] == 1) {
				if (tree[3] <= 3) {
					if (x >= tree[1] && x <= tree[1] + 1) {
						if (y >= tree[2] - 1 && y <= tree[2]) {
							return i;
						}
					}
				} else if (tree[1] == x && tree[2] == y) {
					return i;
				}
			}
		}
		return -1;
	}

	private void addTree(int x, int y, int type) {
		if ((!treeFree(x, y) && type > 3)
				|| (!treeFree(x, y) && !treeFree(x + 1, y)
						&& !treeFree(x, y - 1) && !treeFree(x + 1, y - 1) && type <= 3)) {

			int id = getTreeAt(x, y);
			if (id != -1) {
				int tree[] = map.mapObjects.get(id);
				map.mapObjects.set(id,
						new int[] { 1, tree[1], tree[2], type, 0 });

			} else {
				if (type > 3
						|| (type <= 3 && getTreeAt(x, y) == -1
								&& getTreeAt(x, y - 1) == -1
								&& getTreeAt(x + 1, y) == -1 && getTreeAt(
								x + 1, y - 1) == -1)) {
					map.addMapObjectDirect(1, x, y, type, 0);
				}
			}
			map.updateLocal();
		}
	}

	private void removeTree(int x, int y) {
		int id = getTreeAt(x, y);
		if (id != -1) {
			map.removeObject(id);
			treeEdit = -1;
		}
	}

	// Blocks ------------------------------------------------------------------
	private int getBlockAt(int x, int y) {
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int block[] = map.mapObjects.get(i);
			if (block[0] == 0 && block[3] == 5) {
				if (block[4] == 255) {
					if (x == block[1] && y == block[2]) {
						return i;
					}
				} else {
					if (x >= block[1] && x <= block[1] + 1) {
						if (y >= block[2] && y <= block[2] + 1) {
							return i;
						}
					}
				}
			}
		}
		return -1;
	}

	public void addBlock(int x, int y, int count) {
		boolean set = false;
		if (count == 255) {
			if (!treeFree(x, y)) {
				set = true;
			}
		} else {
			if (!treeFree(x, y) && !treeFree(x + 1, y) && !treeFree(x, y + 1)
					&& !treeFree(x + 1, y + 1)) {
				set = true;
			}
		}

		if (set) {
			int id = getBlockAt(x, y);
			if (id != -1) {
				if (count != 255) {
					int block[] = map.mapObjects.get(id);
					map.mapObjects.set(id, new int[] { 0, block[1], block[2],
							5, count });
				}
			} else {
				if (count != 255) {
					if (getBlockAt(x, y) == -1 && getBlockAt(x, y + 1) == -1
							&& getBlockAt(x + 1, y) == -1
							&& getBlockAt(x + 1, y + 1) == -1) {
						map.addMapObjectDirect(0, x, y, 5, count);
					}
				} else if (getBlockAt(x, y) == -1) {
					map.addMapObjectDirect(0, x, y, 5, count);
				}
			}
			map.updateLocal();
		}
	}

	public void changeBlock(int x, int y, int count) {
		int id = getBlockAt(x, y);
		if (id != -1) {
			int block[] = map.mapObjects.get(id);
			if (block[4] > 3 && block[4] + count > 0 && block[4] + count < 255
					&& block[4] != 255) {
				map.mapObjects.set(id, new int[] { 0, block[1], block[2], 5,
						block[4] + count });
				map.updateLocal();
			}
		}
	}

	private void removeBlock(int x, int y) {
		int id = getBlockAt(x, y);
		if (id != -1) {
			map.removeObject(id);
			blockEdit = -1;
		}
	}

	// Other Objects -----------------------------------------------------------
	public int getPowerID(String type) {
		for (int i = 0; i < map.powerModes.length; i++) {
			if (map.powerModes[i].equals(type)) {
				return i;
			}
		}
		return -1;
	}

	public void createPowerUp(int x, int y, String type) {
		int powerID = getPowerID(type);
		int remove = -1;
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if (p[3] == 0 && p[4] == powerID && p[0] == 0) {
				if (p[1] == x && p[2] == y) {
					remove = i;
					break;
				} else {
					p[1] = x;
					p[2] = y;
					map.updateLocal();
					return;
				}
			}
		}
		if (remove != -1) {
			map.removeObject(remove);
		} else {
			map.addMapObject(0, x, y, 0, powerID);
		}
	}

	public void createSave(int x, int y) {
		int remove = -1;
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if (p[3] == 1 && p[1] == x && p[2] == y) {
				remove = i;
			}
		}
		if (remove != -1) {
			map.removeObject(remove);
		} else {
			map.addMapObject(0, x, y, 1, 0);
		}
	}

	public void createWarp(int x, int y) {
		int remove = -1;
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if ((p[3] == 3 || p[3] == 4) && p[1] == x && p[2] == y) {
				remove = i;
			}
		}
		if (remove != -1) {
			map.removeObject(remove);
		} else {
			map.addMapObject(0, x, y, 3, 0);
		}
	}

	public void createEntity(int x, int y) {
		int remove = -1;
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if (p[3] == 2 && p[1] == x && p[2] == y) {
				remove = i;
			}
		}
		if (remove != -1) {
			map.removeObject(remove);
			map.shardCount -= 1;
		} else {
			map.addMapObject(0, x, y, 2, 0);
			map.shardCount += 1;
		}
	}

	public void createSwitch(int x, int y, int type) {
		int remove = -1;
		for (int i = 0; i < map.mapObjects.size(); i++) {
			int[] p = map.mapObjects.get(i);
			if (p[3] == type && p[0] == 0) {
				if (p[1] == x && p[2] == y) {
					remove = i;
					break;
				} else {
					p[1] = x;
					p[2] = y;
					map.updateLocal();
					return;
				}
			}
		}
		if (remove != -1) {
			map.removeObject(remove);
		} else {
			map.addMapObject(0, x, y, type, 0);
		}
	}
}
