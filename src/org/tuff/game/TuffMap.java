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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonsai.dev.GameObject;

public class TuffMap extends GameObject<Tuff> {

	// Data
	public byte[][] mapData;
	public byte[][] drawData;
	public byte[][] drawTransparentData;
	public byte[][] overlayData;
	public byte[][] overlayTransparentData;
	public byte[][] borderData;
	public byte[][] borderTransparentData;
	public byte[][] groundData;
	public byte[][] soundData;
	public byte[][] colData;
	public byte[][] transparentData;

	// Lists
	protected List<int[]> mapObjects = new ArrayList<int[]>();
	private List<int[]> localMapObjects = new ArrayList<int[]>();
	private List<int[]> localTrees = new ArrayList<int[]>();
	private List<int[]> localSwitches = new ArrayList<int[]>();
	private List<int[]> localBlocks = new ArrayList<int[]>();
	private List<int[]> waterTileList = new ArrayList<int[]>();
	private List<int[]> waterBorderList = new ArrayList<int[]>();
	private List<int[]> breakEffects = new ArrayList<int[]>();
	private List<int[]> normalTileList = new ArrayList<int[]>();
	private List<int[]> transparentTileList = new ArrayList<int[]>();
	private Map<Integer, Long> breakedBlocks = new HashMap<Integer, Long>();
	private Map<Integer, Integer> breakedBlocksStatus = new HashMap<Integer, Integer>();

	// Value
	protected String[] powerModes = new String[] { "J", "W", "U", "D", "B", "S" };
	protected int shardCount = 0;
	private Color blockColor = new Color(152, 152, 152);

	// Images
	private BufferedImage[] borderTiles;
	private BufferedImage[] treeTiles;
	private BufferedImage[] leafTiles;
	private BufferedImage[] waterTiles;
	private BufferedImage[] saveTiles;
	private BufferedImage[] warpTiles;
	private BufferedImage[] switchTiles;
	protected BufferedImage[] shardTiles;
	private BufferedImage blockTile;
	private BufferedImage[] blockTiles;

	// Sizes
	protected int mapWidth;
	protected int mapHeight;
	protected int screenWidth;
	protected int screenHeight;
	protected int tileSize = 16;

	// Positions & Offsets
	protected int startX = 0;
	protected int startY = 0;
	protected int mapOffsetX = 0;
	protected int mapOffsetY = 0;
	protected int scrollOffsetX = 0;
	protected int scrollOffsetY = 0;
	protected int screenOffsetX = 0;
	protected int screenOffsetY = 0;
	private int sectorX = -1;
	private int sectorY = -1;
	private int oldSectorX;
	private int oldSectorY;
	private float saveScale = 0f;
	private float warpScale = 0f;

	// Transparency
	private float tileTransparency = 0.5f;
	protected int transparentTile = 2;
	private int transparentX = 0;
	private int transparentY = 0;
	private int transparentOldX = 0;
	private int transparentOldY = 0;
	private boolean transparentMode = false;
	private boolean showTransparentTiles = false;
	private float oldTileTransparency = 0.0f;

	// Editor & Renderer
	private MapRenderer renderer;
	protected MapEditor editor;
	private boolean edit = false;
	private TileFinder finder;
	private boolean isLoading = false;

	// Tiles
	public int objImgCount;
	protected TileGenerator tileGen;

	// Cheats
	protected boolean wasteLand = false;
	protected boolean noWater = false;
	protected boolean showSound = false;
	protected boolean noHide = false;
	protected boolean noBorders = false;

	// Cache
	private BufferedImage mapCache = null;
	public int oldMX = -1;
	public int oldMY = -1;

	// Enemies
	// public List<Enemy> enemies;

	private Color[] colors = new Color[] { Color.RED, Color.GREEN,
			Color.YELLOW, Color.BLUE };

	public TuffMap(Tuff g, boolean edit) {
		super(g);

		// Editor
		if (edit) {
			this.edit = true;
			editor = new MapEditor(g, this);
		}

		// Tiles
		borderTiles = image.gets("/images/border.png", 16, 4);
		treeTiles = image.gets("/images/trees.png", 4, 3);
		leafTiles = image.gets("/images/leafs.png", 5, 3);
		waterTiles = image.gets("/images/water.png", 3, 2);
		saveTiles = image.gets("/images/save.png", 3, 1);
		warpTiles = image.gets("/images/warps.png", 3, 2);
		shardTiles = image.gets("/images/shard.png", 7, 1);
		blockTile = image.get("/images/block.png");
		switchTiles = image.gets("/images/switch.png", 2, 4);
		blockTiles = image.gets("/images/blocks.png", 7, 1);

		// Tile Generator
		tileGen = new TileGenerator();
		mapCache = image.create(game.width() + 16, game.height() + 16, false);

		// Animations
		animation.add("water", new int[] { 0, 1, 2, 1 }, 125, true);
		animation.add("save", new int[] { 0, 1, 2, 1 }, 75, true);
		animation.add("warp", new int[] { 0, 1, 2, 1 }, 75, true);
		animation.add("shard", new int[] { 0, 0, 1, 2, 1, 0, 0, 3, 4, 3, 0, 0,
				5, 6, 5 }, 125, true);

		// Init
		screenWidth = game.width() / tileSize;
		screenHeight = game.height() / tileSize;

		// Load
		loadMap(null);
	}

	// Tile Generator ----------------------------------------------------------
	public class TileGenerator {
		private BufferedImage[] backgrounds;
		private BufferedImage[] overlays;
		public BufferedImage[][][][] tiles;
		private BufferedImage[][] overlayEdges;

		private int[][] lowerTiles = { {}, { 1 }, { 5 }, { 1, 5, 2 }, { 7 },
				{ 1, 7 }, { 5, 7, 8 }, { 1, 5, 7, 2, 8 }, { 3 }, { 1, 3, 0 },
				{ 3, 5 }, { 1, 3, 5, 0, 2 }, { 3, 7, 6 }, { 1, 3, 7, 0, 6 },
				{ 3, 5, 7, 6, 8 }, { 1, 3, 5, 7, 0, 2, 6, 8 } };

		private int[][] upperTiles = { {}, { 0 }, { 1 }, { 0, 1 }, { 3 },
				{ 0, 3 }, { 1, 3 }, { 0, 1, 3 }, { 2 }, { 0, 2 }, { 1, 2 },
				{ 0, 1, 2 }, { 2, 3 }, { 0, 2, 3 }, { 1, 2, 3 }, { 0, 1, 2, 3 }

		};

		private int[][] tileCombinations = { { 1, 0 }, { 1, 4 }, { 1, 8 },
				{ 1, 12 }, { 2, 0 }, { 2, 1 }, { 2, 8 }, { 2, 9 }, { 3, 0 },
				{ 3, 8 }, { 4, 0 }, { 4, 1 }, { 4, 2 }, { 4, 3 }, { 5, 0 },
				{ 6, 0 }, { 6, 1 }, { 7, 0 }, { 8, 0 }, { 8, 2 }, { 8, 4 },
				{ 8, 6 }, { 9, 0 }, { 9, 4 }, { 10, 0 }, { 11, 0 }, { 12, 0 },
				{ 12, 2 }, { 13, 0 }, { 14, 0 }, { 15, 0 }, { 0, 0 }, { 0, 1 },
				{ 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 }, { 0, 6 }, { 0, 7 },
				{ 0, 8 }, { 0, 9 }, { 0, 10 }, { 0, 11 }, { 0, 12 }, { 0, 13 },
				{ 0, 14 }, { 0, 15 } };

		public TileGenerator() {
			backgrounds = image.gets("/images/ground.png", 5, 4);
			overlays = image.gets("/images/overlay.png", 3, 12);
			tiles = new BufferedImage[5][5][1][1];
			overlayEdges = new BufferedImage[4][1];
		}

		public void reset() {
			tiles = new BufferedImage[5][5][1][1];
		}

		private void generateLower(final Graphics2D g, final int id,
				final int type) {
			for (int i = 0; i < lowerTiles[id].length; i++) {
				g.drawImage(overlays[lowerTiles[id][i] + (type - 1) * 9], 0, 0,
						null);
			}
		}

		private void generateUpper(final Graphics2D g, final int id,
				final int type) {
			if (overlayEdges[type - 1][0] == null) {
				overlayEdges[type - 1] = image.gets(
						overlays[4 + (type - 1) * 9], 2, 2);
			}
			for (int i = 0; i < upperTiles[id].length; i++) {
				final int tile = upperTiles[id][i];
				g.drawImage(overlayEdges[type - 1][tile], (tile % 2) * 8,
						(tile / 2) * 8, null);
			}
		}

		public void generate(int type, int ground, int border) {
			if (tiles[type][ground][0][0] == null) {
				tiles[type][ground] = new BufferedImage[16][16];
			}
			for (int i = 0; i < tileCombinations.length; i++) {
				final BufferedImage img = image.create(16, 16, false);
				final Graphics2D g = (Graphics2D) img.getGraphics();
				g.drawImage(backgrounds[ground + (type - 1) * 5], 0, 0, null);
				if (!noBorders) {
					generateLower(g, tileCombinations[i][0], border);
					generateUpper(g, tileCombinations[i][1], border);
				}
				tiles[type][ground][tileCombinations[i][0]][tileCombinations[i][1]] = img;
				g.dispose();
			}
		}
	}

	// Control -----------------------------------------------------------------
	public void control(Player player) {
		if (saveScale > 0) {
			saveScale -= 1.0f - (1.0f / saveScale);
			if (saveScale < 2.0f) {
				saveScale = 0.0f;
			}
		}
		if (warpScale > 0) {
			warpScale -= 1.0f - (1.0f / warpScale);
			if (warpScale < 2.0f) {
				warpScale = 0.0f;
			}
		}

		oldTileTransparency = tileTransparency;
		if (!player.hasControl) {
			scrollOffsetX = 0;
			scrollOffsetY = 0;
			// Move
			if (!input.keyDown(java.awt.event.KeyEvent.VK_CONTROL)
					&& !input.keyDown(java.awt.event.KeyEvent.VK_ALT)
					&& !input.keyDown(java.awt.event.KeyEvent.VK_SHIFT)) {
				if (input.keyDown(java.awt.event.KeyEvent.VK_A)) {
					mapOffsetX -= 1;
				}
				if (input.keyDown(java.awt.event.KeyEvent.VK_D)) {
					mapOffsetX += 1;
				}
				if (input.keyDown(java.awt.event.KeyEvent.VK_W)) {
					mapOffsetY -= 1;
				}
				if (input.keyDown(java.awt.event.KeyEvent.VK_S)) {
					mapOffsetY += 1;
				}
			}
			limitMapOffset();

			// Editor
			if (edit) {
				editor.control();
			}

		} else {
			// Editor
			if (edit) {
				editor.selectSize = -1;
			}

			// Position
			int xw = screenWidth * (tileSize / 2);
			int yw = screenHeight * (tileSize / 2);
			mapOffsetX = (player.posX - xw) / tileSize;
			mapOffsetY = (player.posY - yw) / tileSize;
			if (mapOffsetX < 0) {
				mapOffsetX = 0;
			}
			if (mapOffsetY < 0) {
				mapOffsetY = 0;
			}
			scrollOffsetX = player.posX - mapOffsetX * tileSize - xw;
			scrollOffsetY = player.posY - mapOffsetY * tileSize - yw;
			limitMapOffset();

			// Transparency
			int tx = player.posX / tileSize;
			int ty = (player.posY - 7) / tileSize;
			boolean transparent = player.underTransparent();
			if (!transparentMode && transparent) {
				transparentX = tx;
				transparentY = ty;
				transparentMode = true;
			}

			if (transparent) {
				// Get Tile List
				if (tileTransparency == 1.0f
						|| (transparentOldX != transparentX || transparentOldY != transparentY)) {
					removeTransparency();
					finder.find(tx, ty);
					transparentTileList = finder.result;
					renderer.redraw(finder.minX - 1, finder.minY - 1,
							finder.maxX + 2, finder.maxY + 2, true);
					addTransparency();
				}
				tileTransparency -= 0.1f;
				showTransparentTiles = true;
				if (tileTransparency < 0.0f) {
					tileTransparency = 0.0f;
				}
			} else if (!transparent) {
				transparentMode = false;
				tileTransparency += 0.1f;
				if (tileTransparency > 1.0f) {
					tileTransparency = 1.0f;
					if (showTransparentTiles) {
						showTransparentTiles = false;
						removeTransparency();
					}
				}
			}
			transparentOldX = transparentX;
			transparentOldY = transparentY;

			// Map Objects
			boolean warped = false;
			for (int i = 0; i < localMapObjects.size(); i++) {
				final int[] p = localMapObjects.get(i);
				final int x = p[1] * tileSize;
				final int y = p[2] * tileSize;

				// PowerUPS
				if (p[3] == 0) {
					if (!player.hasAbility(powerModes[p[4]])) {
						if (player.posX >= x && player.posX < x + 16) {
							if (player.posY - 7 >= y
									&& player.posY - 7 < y + 16) {
								// powerUps.remove(i);
								player.activateAbility(powerModes[p[4]]);
							}
						}
					}

					// Saves
				} else if (p[3] == 1) {
					if (player.posX >= x && player.posX < x + 16) {
						if (player.posY - 7 >= y && player.posY - 7 < y + 16) {
							if (input.keyPressed(KeyEvent.VK_DOWN)
									|| input.keyPressed(KeyEvent.VK_S)) {

								saveScale = 20f;
								game.save();
							}
						}
					}

					// Entities
				} else if (p[3] == 2) {
					if (player.posX >= x && player.posX < x + 16) {
						if (player.posY - 7 >= y && player.posY - 7 < y + 16) {
							player.entitiesCollected.add(new int[] { p[1], p[2] });
							updateLocal();
							game.flashScreen();
						}
					}

					// Activate Warps
				} else if (p[3] == 3) {
					if (player.posX >= x && player.posX < x + 16) {
						if (player.posY - 7 >= y && player.posY - 7 < y + 16) {
							p[3] = 4;
							game.flashWarpActivate();
						}
					}

					// Active Warps
				} else if (p[3] == 4 && !warped) {
					if (player.posX >= x && player.posX < x + 16) {
						if (player.posY - 7 >= y && player.posY - 7 < y + 16) {
							if (input.keyPressed(KeyEvent.VK_DOWN)
									|| input.keyPressed(KeyEvent.VK_S)) {

								warp(player, p);
								warped = true;
							}
						}
					}

					// Blocks
				} else if (p[3] == 5 && p[4] > 3
						&& p[4] <= player.entitiesCollected.size()) {
					final int dx = Math.abs((x + 16) - player.posX);
					final int dy = Math.abs((y + 16) - (player.posY - 7));
					if (Math.sqrt(dx * dx + dy * dy) < 32) {
						player.blocksOpened.add(new int[] { p[1], p[2] });
						removeBlock(p);
						updateLocal();
					}
					// Switches
				} else if (p[3] >= 6 && p[3] <= 9 && p[4] == 0) {
					if (player.posX >= x + 3 && player.posX < x + 12) {
						if (player.posY >= y + 6 && player.posY < y + 16) {
							player.switchesToggled[p[3] - 6] = true;
							p[4] = 1;
							game.flashSwitch();
							for (int e = 0; e < localMapObjects.size(); e++) {
								final int[] c = localMapObjects.get(e);
								if (c[0] == 0 && c[3] == 5
										&& (c[4] == (p[3] - 6))) {
									removeBlock(c);
								}
							}
							updateLocal();
						}
					}
				}
			}
		}

		for (int e = 0; e < localBlocks.size(); e++) {
			final int p[] = localBlocks.get(e);
			if (p[4] == 255) {
				final Integer id = new Integer(p[1] * mapWidth + p[2]);
				if (!breakedBlocks.containsKey(id)) {
					if (player.isOn(p[1] * tileSize, p[2] * tileSize, 16)) {
						if (!player.onGround()) {
							breakedBlocks.put(id, getTime());
							breakedBlocksStatus.put(id, 1);
						}
					}
				} else {
					final long time = getTime() - breakedBlocks.get(id);
					if (time > 75) {
						p[5] = 1;
					}
					if (time > 2000) {
						breakedBlocksStatus.put(id, 2);
					}
					if (!player.isIn(p[1] * tileSize, p[2] * tileSize, 16, 16)
							|| player.onGround()) {
						if (time > 2500) {
							breakedBlocks.remove(id);
							breakedBlocksStatus.remove(id);
							p[5] = 0;
						}
					}
				}
			}
		}
	}

	public void removeBlock(int[] p) {
		breakEffects.add(new int[] { (int) game.getTime(), p[1] + 2, p[2] + 2,
				2 });
		breakEffects.add(new int[] { (int) game.getTime() + 125, p[1] + 2,
				p[2] + 2, 2 });

		breakEffects.add(new int[] { (int) game.getTime() + 250, p[1] + 2,
				p[2] + 2, 2 });
	}

	public void resetTransparency() {
		tileTransparency = 0.5f;
		removeTransparency();
	}

	private void removeTransparency() {
		if (transparentTileList.size() > 0) {
			for (int y = finder.minY - 1; y <= finder.maxY + 1; y++) {
				for (int x = finder.minX - 1; x <= finder.maxX + 1; x++) {
					overlayTransparentData[x][y] = 0;
					drawTransparentData[x][y] = 0;
					transparentData[x][y] = 0;
					borderTransparentData[x][y] = 0;
				}
			}
		}
		// transparentTile = 2;
	}

	private void addTransparency() {
		if (transparentTileList.size() > 0) {
			for (int y = finder.minY - 1; y <= finder.maxY + 1; y++) {
				for (int x = finder.minX - 1; x <= finder.maxX + 1; x++) {
					transparentData[x][y] = 2;
				}
			}
			for (int[] tile : transparentTileList) {
				transparentData[tile[0]][tile[1]] = 1;
			}
		}
	}

	public List<int[]> getWarpsActive() {
		List<int[]> warpObjects = new ArrayList<int[]>();
		for (int i = 0; i < mapObjects.size(); i++) {
			int[] p = mapObjects.get(i);
			if (p[0] == 0 && p[3] == 4) {
				warpObjects.add(p);
			}
		}
		return warpObjects;
	}

	public void activateWarp(int x, int y) {
		for (int i = 0; i < mapObjects.size(); i++) {
			int[] p = mapObjects.get(i);
			if (p[0] == 0 && p[3] == 3 && p[1] == x && p[2] == y) {
				p[3] = 4;
			}
		}
	}

	private void warp(Player player, int[] w) {
		List<int[]> warpObjects = getWarpsActive();
		if (warpObjects.size() > 1) {
			int c = 0;
			for (int i = 0; i < warpObjects.size(); i++) {
				int[] p = warpObjects.get(i);
				if (p[1] == w[1] && p[2] == p[2]) {
					c = i;
					break;
				}
			}
			int next[] = warpObjects.get((c + 1) % warpObjects.size());
			player.posX = next[1] * tileSize + 8;
			player.posY = next[2] * tileSize + 16;
			game.flashWarp();
			game.playSound("warp", false);
			warpScale = 14f;
		}
	}

	private void limitMapOffset() {
		if (mapOffsetX < 0) {
			mapOffsetX = 0;
		}
		if (mapOffsetY < 0) {
			mapOffsetY = 0;
		}

		if (scrollOffsetX < 0) {
			scrollOffsetX = 0;
		}
		if (mapOffsetX >= mapWidth - screenWidth) {
			mapOffsetX = mapWidth - screenWidth;
			scrollOffsetX = 0;
		}

		if (scrollOffsetY < 0) {
			scrollOffsetY = 0;
		}
		if (mapOffsetY >= mapHeight - screenHeight) {
			mapOffsetY = mapHeight - screenHeight;
			scrollOffsetY = 0;
		}
		screenOffsetX = mapOffsetX * tileSize + scrollOffsetX;
		screenOffsetY = mapOffsetY * tileSize + scrollOffsetY;
	}

	// Render ------------------------------------------------------------------
	public void render() {
		renderer.render();
		if (edit) {
			editor.updateMap();
		}
	}

	protected int getAt(final int x, final int y) {
		if (x < 0 || y < 0 || x > mapWidth - 1 || y > mapWidth - 1) {
			return 0;
		} else {
			return mapData[x][y];
		}
	}

	private int getAtTree(final int x, final int y) {
		if (x < 0 || y < 0 || x > mapWidth - 1 || y > mapWidth - 1) {
			return 0;
		} else {
			if (colData[x][y] == 1) {
				return transparentTile;

			} else {
				return mapData[x][y];
			}
		}
	}

	public int getSoundAt(final int x, final int y) {
		if (x < 0 || y < 0 || x > mapWidth - 1 || y > mapWidth - 1) {
			return -1;

		} else {
			if (soundData[x][y] >= 1 && mapData[x][y] == 4) {
				return 2;

			} else if (soundData[x][y] >= 1 && mapData[x][y] == 2) {
				return 1;

			} else if (mapData[x][y] == 0) {
				return 0;

			} else {
				return -1;
			}
		}
	}

	// Collision ---------------------------------------------------------------
	public int colAt(final int x, final int y, final boolean player) {
		final int tx = x / tileSize;
		final int ty = y / tileSize;
		if (x < 0 || tx < 0 || tx > mapWidth - 1 || y < 0 || ty < 0
				|| ty > mapHeight - 1) {

			return 10;
		}

		for (int e = 0; e < localSwitches.size(); e++) {
			final int i[] = localSwitches.get(e);
			if (x >= i[1] * tileSize + 2 && x <= i[1] * tileSize + 14) {
				if (y >= i[2] * tileSize + 6 + (i[4] == 1 ? 6 : 0)
						&& y <= i[2] * tileSize + 16) {
					return 1;
				}
			}
		}

		for (int e = 0; e < localBlocks.size(); e++) {
			final int i[] = localBlocks.get(e);
			if (i[4] == 255) {
				if (i[5] == 0) {
					if (x >= i[1] * tileSize && x <= i[1] * tileSize + 16) {
						if (y >= i[2] * tileSize && y <= i[2] * tileSize + 16) {
							return 9;
						}
					}
				}
			} else {
				if (x >= i[1] * tileSize && x <= i[1] * tileSize + 32) {
					if (y >= i[2] * tileSize && y <= i[2] * tileSize + 32) {
						return 1;
					}
				}
			}
		}

		if ((mapData[tx][ty] == 1 || mapData[tx][ty] == 3)
				&& colData[tx][ty] == 0) {
			return mapData[tx][ty];
		} else {
			return colData[tx][ty] == 2 ? 1 : 0;
		}
	}

	public boolean hasColAt(final int x, final int y) {
		int col = colAt(x, y, false);
		return col == 1 || col == 3;
	}

	public boolean waterAt(final int x, final int y, final boolean both) {
		int tx = x / tileSize;
		int ty = y / tileSize;
		if (x < 0 || tx < 0 || tx > mapWidth - 1 || y < 0 || ty < 0
				|| ty > mapHeight - 1) {

			return false;
		}
		return mapData[tx][ty] == 4 || (both && mapData[tx][ty] == 1)
				|| (colData[tx][ty] == 1 && transparentTile == 4);
	}

	public boolean transparentAt(final int x, final int y) {
		int tx = x / tileSize;
		int ty = y / tileSize;
		if (x < 0 || tx < 0 || tx > mapWidth - 1 || y < 0 || ty < 0
				|| ty > mapHeight - 1) {

			return false;
		}
		return colData[tx][ty] == 1;
	}

	public void removeAt(final int x, final int y) {
		if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
			if (mapData[x][y] == 3) {
				mapData[x][y] = (byte) surroundType(x, y);
				breakEffects.add(new int[] { (int) game.getTime(), x, y, 1 });
				renderer.redraw(x - 1, y - 1, x + 2, y + 2, false); // drawData
			}
		}
	}

	public int surroundType(final int x, final int y) {
		int cl = getAt(x - 1, y);
		int cr = getAt(x + 1, y);
		int cu = getAt(x, y - 1);
		int cd = getAt(x, y + 1);
		if (cl == 2 || cr == 2 || cu == 2 || cd == 2) {
			return 2;
		} else if (cl == 4 || cr == 4 || cu == 4 || cd == 4) {
			return 4;
		}
		return 0;
	}

	// Items -------------------------------------------------------------------
	public void addMapObject(int type, int x, int y, int subtype, int extra) {
		mapObjects.add(new int[] { type, x, y, subtype, extra, 0 });
		updateLocal();
	}

	public void addMapObjectDirect(int type, int x, int y, int subtype,
			int extra) {
		mapObjects.add(new int[] { type, x, y, subtype, extra, 0 });
	}

	public void removeObject(int obj) {
		mapObjects.remove(obj);
		updateLocal();
	}

	// Local -------------------------------------------------------------------
	public void updateLocal() {
		localMapObjects.clear();
		localTrees.clear();
		localSwitches.clear();
		localBlocks.clear();

		final int width = screenWidth / 2;
		final int height = screenHeight / 2;
		final int sectorStartX = sectorX * width;
		final int sectorStartY = sectorY * height;

		objects: for (int[] object : mapObjects) {
			if (object[1] >= sectorStartX - width
					&& object[2] >= sectorStartY - height
					&& object[1] < sectorStartX + screenWidth + width + 1
					&& object[2] < sectorStartY + screenHeight + height + 1) {

				if (object[0] == 1) {
					localTrees.add(object);

				} else {
					if (object[3] == 2 && game.player.hasControl) {
						for (int[] p : game.player.entitiesCollected) {
							if (p[0] == object[1] && p[1] == object[2]) {
								continue objects;
							}
						}
					}

					// Blocks
					if (object[3] == 5 && game.player.hasControl) {
						if (object[4] > 3) {
							for (int[] p : game.player.blocksOpened) {
								if (p[0] == object[1] && p[1] == object[2]) {
									continue objects;
								}
							}
							localBlocks.add(object);

						} else if (object[4] == 0
								&& game.player.switchesToggled[0]) {
							continue objects;

						} else if (object[4] == 1
								&& game.player.switchesToggled[1]) {
							continue objects;

						} else if (object[4] == 2
								&& game.player.switchesToggled[2]) {
							continue objects;

						} else if (object[4] == 3
								&& game.player.switchesToggled[3]) {
							continue objects;
						}
						localBlocks.add(object);

					} else if (object[3] == 5) {
						localBlocks.add(object);
					}

					if (object[3] >= 6) {
						if (game.player.hasControl) {
							if (game.player.switchesToggled[object[3] - 6]) {
								object[4] = 1;
							}
						} else {
							object[4] = 0;
						}
					}

					if (object[3] >= 6) {
						localSwitches.add(object);
					}

					if (object[1] >= sectorStartX - width
							&& object[2] >= sectorStartY - height
							&& object[1] < sectorStartX + screenWidth + width
									+ 1
							&& object[2] < sectorStartY + screenHeight + height
									+ 1) {

						// if (object[3] != 5) {
						// colData[object[1]][object[2]] = 2;
						// colData[object[1] + 1][object[2]] = 2;
						// colData[object[1]][object[2] + 1] = 2;
						// colData[object[1] + 1][object[2] + 1] = 2;
						localMapObjects.add(object);
						// }
					}
				}
			}
		}
	}

	private void checkSector() {
		oldSectorX = sectorX;
		oldSectorY = sectorY;
		int width = screenWidth / 2;
		int height = screenHeight / 2;
		sectorX = mapOffsetX / width;
		sectorY = mapOffsetY / height;
		if (oldSectorX != sectorX || oldSectorY != sectorY) {
			updateLocal();
		}
	}

	// Drawing -----------------------------------------------------------------
	private boolean isVisible(int x, int y, int w, int h) {
		return (x <= screenWidth * tileSize && x + w >= 0
				&& y <= screenHeight * tileSize && (y + h) >= 0);
	}

	public void draw(Graphics2D g, Player player) {
		if (isLoading) {
			return;
		}
		checkSector();

		// Draw Map
		objImgCount = 0;
		if (oldMX != mapOffsetX || oldMY != mapOffsetY
				|| tileTransparency != oldTileTransparency) {
			Graphics2D bg = (Graphics2D) mapCache.getGraphics();
			bg.setColor(game.bgColor);
			bg.fillRect(0, 0, game.width() + 16, game.height() + 16);
			waterTileList.clear();
			waterBorderList.clear();
			normalTileList.clear();
			for (int y = mapOffsetY; y < mapOffsetY + screenHeight + 1; y++) {
				if (y > 0 && y < mapHeight) {
					for (int x = mapOffsetX; x < mapOffsetX + screenWidth + 1; x++) {
						if (x > 0 && x < mapWidth) {
							final int type = mapData[x][y];
							final int tile = drawData[x][y];
							if (tile > 0) {
								if (colData[x][y] == 1) {
									drawTile(bg, transparentTile, tile, x, y,
											transparentTile, false);
									normalTileList.add(new int[] { 1, tile, x,
											y });
								} else {
									drawTile(bg, type, tile, x, y, 0, false);
								}
							}
						}
					}
				}
			}
			
			// Draw Trees
			if (!wasteLand) {
				for (int e = 0; e < localTrees.size(); e++) {
					final int tree[] = localTrees.get(e);
					final int ground = getAtTree(tree[1], tree[2]);
					final int add = ground == 4 ? 1 : (ground == 2 ? 2 : 0);
					final int x = (tree[1] - mapOffsetX) * tileSize;
					if (tree[3] <= 3) {
						final int y = ((tree[2] - mapOffsetY) - 1) * tileSize;
						if (isVisible(x, y, 32, 32)) {
							bg.drawImage(treeTiles[tree[3] + add * 4], x, y, null);
						}
					} else {
						final int y = (tree[2] - mapOffsetY) * tileSize;
						if (isVisible(x, y, 16, 16)) {
							bg.drawImage(leafTiles[tree[3] - 4 + add * 5], x, y,
									null);
						}
					}
				}
			}
			bg.dispose();
			oldMX = mapOffsetX;
			oldMY = mapOffsetY;
		}

		// Draw Cache Image
		g.drawImage(mapCache, 0 - scrollOffsetX, 0 - scrollOffsetY, null);

		// Draw Enemies
		// for(Enemy e : enemies) {
		// e.draw(g);
		// }
	}

	// Draw a Tile
	private synchronized void drawTile(final Graphics2D g, final int type,
			final int tile, final int x, final int y, final int trans,
			final boolean offset) {
		final int px = (x - mapOffsetX) * tileSize
				- (offset ? scrollOffsetX : 0);
		final int py = (y - mapOffsetY) * tileSize
				- (offset ? scrollOffsetY : 0);

		// Ground
		final int ground = groundData[x][y];
		if (tileGen.tiles[type][ground][0][0] == null) {
			tileGen.generate(type, ground, type);
		}

		// Background Tiles
		final int tra = transparentData[x][y];
		if (tra != 2 || showTransparentTiles) {
			g.drawImage(
					tileGen.tiles[type][ground][tile < 16 ? tile : 0][overlayData[x][y]],
					px, py, null);
		}

		// Border
		if (borderData[x][y] > 0) {
			if (type == 4) {
				waterBorderList.add(new int[] { px, py });

			} else {
				g.drawImage(borderTiles[borderData[x][y] + (type - 1) * 16],
						px, py, null);
			}
		}

		// Water
		if (type == 4) {
			waterTileList.add(new int[] { px, py,
					(x % 2 == 1 && y % 2 == 1 ? 3 : 0) });
		}

		// Transparency Overlay
		if (tra == 2) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.0f - tileTransparency));
			final int ttile = drawTransparentData[x][y];
			g.drawImage(
					tileGen.tiles[type][ground][ttile < 16 ? ttile : 0][overlayTransparentData[x][y]],
					px, py, null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
					1.0f));

		} else if (tra == 1) {
			if (borderTransparentData[x][y] > 0) {
				if (trans == 4) {
					waterBorderList.add(new int[] { px, py });
				} else {
					g.drawImage(borderTiles[borderTransparentData[x][y]
							+ (type - 1) * 16], px, py, null);
				}
			}
		}

		// Show sound
		if (showSound) {
			final int snd = getSoundAt(x, y);
			if (snd != -1) {
				g.setColor(colors[snd + 1]);
				g.fillRect(px + 6, py + 6, 4, 4);
			}
		}
	}

	public void drawDefer(Graphics2D g, Player player) {
		if (isLoading) {
			return;
		}

		// Map Objects
		for (int e = 0; e < localMapObjects.size(); e++) {
			final int p[] = localMapObjects.get(e);

			// PowerUPS
			if (p[3] == 0) {
				final int x = (p[1] * tileSize - screenOffsetX) + 3;
				final int y = (p[2] * tileSize - screenOffsetY) + 3;
				if (isVisible(x, y, 9, 11)
						&& (!player.hasAbility(powerModes[p[4]]) || !player.hasControl)) {
					font.draw(g, powerModes[p[4]], x, y);
				}
				// Save Points
			} else if (p[3] == 1) {
				final int x = (p[1] * tileSize - screenOffsetX);
				final int y = (p[2] * tileSize - screenOffsetY) + 10
						- (int) saveScale;
				
				if (isVisible(x, y, 16, 6 + (int) saveScale)) {
					g.drawImage(saveTiles[animation.get("save")], x, y, 16,
							6 + (int) saveScale, null);
					objImgCount++;
				}

				// Warps
			} else if (p[3] == 3 || p[3] == 4) {
				final int x = (p[1] * tileSize - screenOffsetX);
				final int y = (p[2] * tileSize - screenOffsetY) + 10
						- (p[3] == 4 ? (int) warpScale + 6 : 0);
				
				if (isVisible(x, y, 16, 6 + (p[3] == 4 ? (int) warpScale + 6
						: 0))) {
					g.drawImage(warpTiles[animation.get("warp")
							+ (p[3] == 4 ? 0 : 3)], x, y, 16,
							6 + (p[3] == 4 ? (int) warpScale + 6 : 0), null);
					objImgCount++;
				}

				// Entity
			} else if (p[3] == 2) {
				final int x = p[1] * tileSize - screenOffsetX;
				final int y = p[2] * tileSize - screenOffsetY;
				if (isVisible(x, y, 16, 16)) {
					g.drawImage(shardTiles[animation.get("shard")], x, y, null);
					objImgCount++;
				}

			} else if (p[3] >= 6) {
				final int x = p[1] * tileSize - screenOffsetX;
				final int y = p[2] * tileSize - screenOffsetY;
				if (isVisible(x, y, 32, 32)) {
					g.drawImage(switchTiles[0 + (p[4] == 1 ? 1 : 0)
							+ (p[3] - 6) * 2], x, y, null);
					objImgCount++;
				}
			}
		}

		// Water
		if (!noWater) {
			for (int e = 0; e < waterTileList.size(); e++) {
				final int tile[] = waterTileList.get(e);
				g.drawImage(waterTiles[animation.get("water") + tile[2]],
						tile[0] - scrollOffsetX, tile[1] - scrollOffsetY, null);
				objImgCount++;
			}

			for (int e = 0; e < waterBorderList.size(); e++) {
				final int tile[] = waterBorderList.get(e);
				g.drawImage(borderTiles[1 + (4 - 1) * 16], tile[0]
						- scrollOffsetX, tile[1] - scrollOffsetY, null);
				objImgCount++;
			}
		}

		// Blocks
		Composite tmp = g.getComposite();
		for (int e = 0; e < localBlocks.size(); e++) {
			final int p[] = localBlocks.get(e);
			final int x = p[1] * tileSize - screenOffsetX;
			final int y = p[2] * tileSize - screenOffsetY;
			if (isVisible(x, y, 32, 32)) {
				if (p[4] == 255) {
					final Integer id = p[1] * mapWidth + p[2];
					if (breakedBlocks.containsKey(id)) {
						long time = getTime() - breakedBlocks.get(id);
						int img = breakedBlocksStatus.get(id) == 1 ? (int) Math.min(
								6, time / 50)
								: 6 - (int) Math.min(6, (time - 2000) / 50);
						if (img < 0) {
							img = 0;
						}
						g.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 1.0f - img * 0.16f));
						g.drawImage(blockTiles[img], x, y, null);
						
					} else {
						g.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, 1f));
						g.drawImage(blockTiles[0], x, y, null);
					}

				} else {
					g.setComposite(AlphaComposite.getInstance(
							AlphaComposite.SRC_OVER, 1f));
					g.drawImage(blockTile, x, y, null);
					String str = Integer.toString(p[4]);
					if (p[4] == 0) {
						str = "A";
					} else if (p[4] == 1) {
						str = "B";
					} else if (p[4] == 2) {
						str = "C";
					} else if (p[4] == 3) {
						str = "D";
					}
					final int w = font.width(str) / 2;
					font.draw(g, str, (x + 16) - w, y + 11);
				}
				objImgCount++;
			}
		}

		// Transparent Overlay
		if (!noHide) {
			for (int e = 0; e < normalTileList.size(); e++) {
				final int t[] = normalTileList.get(e);
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER,
						transparentData[t[2]][t[3]] == 1 ? tileTransparency
								: (player.hasControl ? 1.0f : 0.5f)));
				drawTile(g, t[0], t[1], t[2], t[3], 0, true);
				objImgCount++;
			}
		}

		// Break Effects
		for (int i = 0; i < breakEffects.size(); i++) {
			final int[] effect = breakEffects.get(i);
			final float d = (float) (100.0 / 500.0 * (game.getTime() - effect[0])) / 100;
			if (d > 1.0) {
				breakEffects.remove(i);
			} else if (d < 1.0 && d > 0.0) {
				final int type = effect[3];
				final int x = effect[1] * tileSize - screenOffsetX - (type - 1)
						* 24;
				final int y = effect[2] * tileSize - screenOffsetY - (type - 1)
						* 24;

				int size = (int) (20 + 24 * d) * type;
				g.setColor(type == 1 ? Color.GRAY : blockColor);
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.5f - 0.5f * d));
				g.fillRect(x + 8 - size / 2, y + 8 - size / 2, size, size);

				size = (int) (8 + 16 * d) * type;
				g.setColor(type == 1 ? Color.GRAY : blockColor);
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 1f - 1.0f * d));
				g.fillRect(x + 8 - size / 2, y + 8 - size / 2, size, size);
			}
		}
		g.setComposite(tmp);

		// Start
		if (!player.hasControl) {
			int x = (startX * tileSize - screenOffsetX);
			int y = (startY * tileSize - screenOffsetY);
			if (isVisible(x, y, 16, 16)) {
				g.setColor(Color.RED);
				g.drawRect(x, y, 16, 16);
			}
		}

		// Edit
		if (edit) {
			editor.draw(g);
		}
	}

	// New / Load / Save -------------------------------------------------------
	public void newMap(int width, int height) {
		isLoading = true;
		mapWidth = width;
		mapHeight = height;
		renderer = new MapRenderer(this);
		mapData = new byte[mapWidth][mapHeight];
		colData = new byte[mapWidth][mapHeight];
		transparentData = new byte[mapWidth][mapHeight];
		drawTransparentData = new byte[mapWidth][mapHeight];
		overlayTransparentData = new byte[mapWidth][mapHeight];
		borderTransparentData = new byte[mapWidth][mapHeight];
		drawData = new byte[mapWidth][mapHeight];
		groundData = new byte[mapWidth][mapHeight];
		overlayData = new byte[mapWidth][mapHeight];
		borderData = new byte[mapWidth][mapHeight];
		soundData = new byte[mapWidth][mapHeight];
		mapObjects = new ArrayList<int[]>();
		startX = 0;
		startY = 0;
		finder = new TileFinder(this);
		for (int i = 0; i < 5; i++) {
			mapData[i][1] = 1;
		}
		render();
		updateLocal();
		isLoading = false;
	}

	private InputStream getMapFile(String filename) {
		try {
			File file = new File(filename != null ? filename : getPath()
					+ "levels/level.lvl");
			return new FileInputStream(file);

		} catch (Exception e) {
			return getClass().getResourceAsStream("/levels/level.lvl");
		}
	}

	public boolean loadMap(String file) {
		// long time = System.nanoTime();
		isLoading = true;
		InputStream stream = getMapFile(file);
		if (stream == null) {
			newMap(255, 255);
			isLoading = false;
			return false;
		}

		// Clean up
		waterTileList.clear();
		waterBorderList.clear();
		normalTileList.clear();
		localMapObjects.clear();
		localTrees.clear();
		transparentTileList.clear();

		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(
					stream));

			// Header
			if (!game.readString(in, 5).equals("TUFF2")) {
				newMap(255, 255);
				return false;
			}
			mapWidth = in.readInt();
			mapHeight = in.readInt();
			startX = in.readInt();
			startY = in.readInt();

			// Arrays
			colData = new byte[mapWidth][mapHeight];
			mapData = new byte[mapWidth][mapHeight];
			drawData = new byte[mapWidth][mapHeight];
			groundData = new byte[mapWidth][mapHeight];
			overlayData = new byte[mapWidth][mapHeight];
			borderData = new byte[mapWidth][mapHeight];
			soundData = new byte[mapWidth][mapHeight];
			transparentData = new byte[mapWidth][mapHeight];
			drawTransparentData = new byte[mapWidth][mapHeight];
			overlayTransparentData = new byte[mapWidth][mapHeight];
			borderTransparentData = new byte[mapWidth][mapHeight];

			// Data
			if (!game.readString(in, 3).equals("MAP")) {
				return false;
			}
			for (int y = 0; y < mapHeight; y++) {
				for (int x = 0; x < mapWidth; x++) {
					mapData[x][y] = in.readByte();
				}
			}

			// Col data
			if (!game.readString(in, 3).equals("COL")) {
				return false;
			}
			int colCount = in.readInt();
			for (int i = 0; i < colCount; i++) {
				colData[in.readInt()][in.readInt()] = 1;
			}

			// Objects
			if (!game.readString(in, 3).equals("OBJ")) {
				return false;
			}
			shardCount = 0;
			int objectCount = in.readInt();
			mapObjects = new ArrayList<int[]>(objectCount);
			for (int i = 0; i < objectCount; i++) {
				int tree = in.readByte();
				int type = in.readByte();
				int x = in.readInt();
				int y = in.readInt();
				if (tree == 1) {
					addMapObjectDirect(1, x, y, type, 0);

				} else {
					if (type == 0) {
						addMapObjectDirect(0, x, y, 0, in.readByte());

					} else if (type == 1) {
						addMapObjectDirect(0, x, y, 1, 0);

					} else if (type == 2) {
						addMapObjectDirect(0, x, y, 2, 0);
						shardCount++;

					} else if (type == 3) {
						addMapObjectDirect(0, x, y, 3, 0);

					} else if (type == 5) {
						addMapObjectDirect(0, x, y, 5, in.readInt());

					} else if (type >= 6) {
						addMapObjectDirect(0, x, y, type, 0);
					}
				}
			}
			in.close();

			// Render
			finder = new TileFinder(this);
			renderer = new MapRenderer(this);
			render();

			updateLocal();

			if (edit) {
				editor.saveUndo();
				// System.out.printf("Took %dms\n",
				// (System.nanoTime() - time) / 1000000);
			}
		} catch (IOException e) {

		}

		isLoading = false;
		return true;
	}

	private String getPath() {
		String path = game.getPath();
		try {
			path = path.substring(0, path.length() - 4) + "src/";
		} catch (Exception e) {
			return "";
		}
		return path;
	}
}
