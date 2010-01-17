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

import java.util.ArrayList;
import java.util.List;

public class TileFinder {
	private final int mapHeight;
	private final int mapWidth;
	private final TuffMap map;
	public ArrayList<int[]> result;
	public int minX = 0;
	public int minY = 0;
	public int maxX = 0;
	public int maxY = 0;
	
	public TileFinder(TuffMap map) {
		mapWidth = map.mapWidth;
		mapHeight = map.mapHeight;
		this.map = map;
	}
	
	protected boolean compare(final int x, final int y) {
		if (map.colData[x][y] == 1 && map.transparentData[x][y] != 1) {
			int type = map.surroundType(x, y);
			if (type == 4) {
				map.transparentTile = 4;
			}
			return true;
		} else {
			return false;
		}
	}
	
	private void set(final int x, final int y) {
		result.add(new int[]{x, y});
		map.transparentData[x][y] = 1;
		if (x > maxX ) {
			maxX = x;
		}
		if (x < minX) {
			minX = x;
		}
		if (y > maxY) {
			maxY = y;
		}
		if (y < minY) {
			minY = y;
		}
	}

	public int find(final int xi, final int yi) {
		map.transparentTile = 2;
		minX = mapWidth;
		minY = mapHeight;
		maxX = 0;
		maxY = 0;
		result = new ArrayList<int[]>();
		
		int groupSize = 0;
		final List<int[]> list = new ArrayList<int[]>();
		list.add(new int[] { xi, yi });
		while (list.size() > 0) {
			final int[] i = list.remove(list.size() - 1);
			final int xp = i[0];
			final int y = i[1];
			if (compare(xp, y)) {
				// Left
				for(int x = xp; x >= 0; x--) {
					if (compare(x, y)) {
						set(x, y);
						groupSize++;

						// Up
						if (y - 1 >= 0 && compare(x, y - 1)) {
							list.add(new int[] { x, y - 1 });
						}

						// Down
						if (y + 1 <= mapHeight - 1 && compare(x, y + 1)) {
							list.add(new int[] { x, y + 1 });
						}
					} else {
						break;
					}
				}

				// Right
				for(int x = xp + 1; x <= mapWidth - 1; x++) {
					if (compare(x, y)) {
						set(x, y);
						groupSize++;

						// Up
						if (y - 1 >= 0 && compare(x, y - 1)) {
							list.add(new int[] { x, y - 1 });
						}

						// Down
						if (y + 1 <= mapHeight - 1 && compare(x, y + 1)) {
							list.add(new int[] { x, y + 1 });
						}
					} else {
						break;
					}
				}
			}
		}
		return groupSize;
	}
}
