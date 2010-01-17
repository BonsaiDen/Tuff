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

import java.util.Map;

public class MapRenderer {
	private TuffMap map;

	public MapRenderer(TuffMap map) {
		this.map = map;
	}

	// Render ------------------------------------------------------------------
	public void redraw(int xs, int ys, int xm, int ym, boolean transparent) {
		xs = xs < 0 ? 0 : xs;
		ys = ys < 0 ? 0 : ys;
		xm = xm > map.mapWidth ? map.mapWidth : xm;
		ym = ym > map.mapHeight ? map.mapHeight : ym;
		for (int y = ys; y < ym; y++) {
			for (int x = xs; x < xm; x++) {
				if (map.mapData[x][y] == 0) {
					map.drawData[x][y] = 0;
					map.groundData[x][y] = 0;
					map.overlayData[x][y] = 0;
					map.overlayTransparentData[x][y] = 0;
					map.borderData[x][y] = 0;
				}
				setTypeData(x, y, transparent);
			}
		}
	}

	public void render() {
	//	long time = System.nanoTime();
		GroupRenderer ground = new GroupRenderer(map);
		ground.render();
		
		Map<Integer, Integer> groupGround = ground.groupInfo;
		int[][] groupData = ground.groupData;
	//	System.out.println((System.nanoTime() - time) / 1000000);

	//	time = System.nanoTime();
		GroupSoundRenderer sound = new GroupSoundRenderer(map);
		sound.render();
		Map<Integer, Integer> groupSound = sound.groupInfo;
		int[][] groupSoundData = sound.groupData;
	//	System.out.println((System.nanoTime() - time) / 1000000);

		for (int y = 0; y < map.mapHeight; y++) {
			for (int x = 0; x < map.mapWidth; x++) {
				map.drawData[x][y] = 0;
				map.borderData[x][y] = 0;
				setTypeData(x, y, false);
				int g = groupGround.get(groupData[x][y]);
				map.groundData[x][y] = (byte) g;
				int s = groupSound.get(groupSoundData[x][y]);
				map.soundData[x][y] = (byte) s;
			}
		}
	}
	
	private void setTypeData(int x, int y, boolean transparent) {
		int[] type = getTypeData(x, y, transparent);
		if (transparent) {
			map.overlayTransparentData[x][y] = (byte) type[1];
		} else {
			map.overlayData[x][y] = (byte) type[1];
		}
		if (transparent) {
			map.borderTransparentData[x][y] = (byte) type[2];
		} else {
			map.borderData[x][y] = (byte) type[2];
		}
		if (type[0] != 0) {
			if (transparent) {
				map.drawTransparentData[x][y] = (byte) type[0];
			} else {
				map.drawData[x][y] = (byte) type[0];
			}
		} else {
			if (map.mapData[x][y] != 0) {
				if (transparent) {
					map.drawTransparentData[x][y] = 16;
				} else {
					map.drawData[x][y] = 16;
				}
			}
		}
	}

	private int[] getTypeData(int x, int y, boolean transparent) {
		int type = map.mapData[x][y];
		int trans = map.transparentData[x][y];
		int tile = 0;
		int overlay = 0;
		int border = 0;
		if (type > 0) {
			if (type == 2 || (transparent && trans == 1)) {
				border += getAt(x, y - 1, 0, 0) ? 1 : 0; // Up
				border += getAt(x + 1, y, 0, 0) ? 2 : 0; // Right
				border += getAt(x, y + 1, 0, 0) ? 4 : 0; // Down
				border += getAt(x - 1, y, 0, 0) ? 8 : 0; // Left
				if ((transparent && trans == 1) && map.transparentTile == 4) {
					border = 0;
					border += getAt(x, y - 1, 0, 0) ? 1 : 0; // Up
					border += getAt(x, y - 1, 2, 0) ? 1 : 0; // Up		
				}
			} else if (type == 4) {
				border += getAt(x, y - 1, 0, 0) ? 1 : 0; // Up
				border += getAt(x, y - 1, 2, 0) ? 1 : 0; // Up
			}
			tile += getAt(x, y - 1, type, trans) ? 0 : 1; // Up
			tile += getAt(x + 1, y, type, trans) ? 0 : 2; // Right
			tile += getAt(x, y + 1, type, trans) ? 0 : 4; // Down
			tile += getAt(x - 1, y, type, trans) ? 0 : 8; // Left

			// Top Left
			overlay += getAt(x - 1, y, type, trans) && getAt(x, y - 1, type, trans)
					&& !getAt(x - 1, y - 1, type, trans) ? 1 : 0;

			// Top Right
			overlay += getAt(x + 1, y, type, trans) && getAt(x, y - 1, type, trans)
					&& !getAt(x + 1, y - 1, type, trans) ? 2 : 0;

			// Bottom Right
			overlay += getAt(x + 1, y, type, trans) && getAt(x, y + 1, type, trans)
					&& !getAt(x + 1, y + 1, type, trans) ? 4 : 0;

			// Bottom Left
			overlay += getAt(x - 1, y, type, trans) && getAt(x, y + 1, type, trans)
					&& !getAt(x - 1, y + 1, type, trans) ? 8 : 0;

		}
		return new int[] { tile, overlay, border };
	}

	private boolean getAt(int x, int y, int type, int trans) {
		if (x < 0 || y < 0 || x > map.mapWidth - 1 || y > map.mapHeight - 1) {
			return true;
		} else {
			return map.mapData[x][y] == type && map.transparentData[x][y] == trans;
		}
	}
}
