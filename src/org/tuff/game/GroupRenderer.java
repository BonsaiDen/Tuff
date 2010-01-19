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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupRenderer {
	protected final int[][] groupData;
	protected final Map<Integer, Integer> groupInfo;
	protected final TuffMap map;
	
	protected boolean hasBorder = false;

	public GroupRenderer(final TuffMap map) {
		this.map = map;
		groupData = new int[map.mapWidth][map.mapHeight];
		groupInfo = new HashMap<Integer, Integer>();
	}

	public final void render() {
		int groupID = 0;
		groupInfo.put(groupID, 0);
		for (int y = 0; y < map.mapHeight; y++) {
			for (int x = 0; x < map.mapWidth; x++) {
				int type = map.mapData[y][x];
				if (type > 0 && groupData[y][x] == 0) {
					hasBorder = false;
					groupID += 1;
					groupInfo.put(groupID,
							checkSize(group(y, x, groupID, type), hasBorder, type));
				}
			}
		}
	}

	protected int checkSize(final int size, final boolean border, final int type) {
		int g = 0;
		if (size > 300) {
			g = 4;
		} else if (size > 100) {
			g = 3;
		} else if (size > 50) {
			g = 2;
		} else if (size > 3) {
			g = 1;
		}
		return g;
	}

	protected boolean compare(final int x, final int y, final int type) {
		return map.mapData[x][y] != 0 && groupData[x][y] == 0;
	}
	
	protected void set(final int x, final int y, final int type, final int group) {
		groupData[x][y] = group;
	}

	private final int group(final int xi, final int yi, final int group,
			final int type) {
		int groupSize = 0;
		final List<int[]> list = new ArrayList<int[]>();
		list.add(new int[] { xi, yi });
		int size = 1;
		while (size > 0) {
			final int[] i = list.remove(list.size() - 1);
			final int xp = i[0];
			final int y = i[1];
			if (compare(xp, y, type)) {
				// Left
				for(int x = xp; x >= 0; x--) {
					if (compare(x, y, type)) {
						set(x, y, type, group);
						//groupData[x][y] = group;
						groupSize++;

						// Up
						if (y - 1 >= 0 && compare(x, y - 1, type)) {
							list.add(new int[] { x, y - 1 });
						}

						// Down
						if (y + 1 <= map.mapHeight - 1 && compare(x, y + 1, type)) {
							list.add(new int[] { x, y + 1 });
						}
					} else {
						break;
					}
				}

				// Right
				for(int x = xp + 1; x <= map.mapWidth - 1; x++) {
					if (compare(x, y, type)) {
					//	groupData[x][y] = group;
						set(x, y, type, group);
						groupSize++;

						// Up
						if (y - 1 >= 0 && compare(x, y - 1, type)) {
							list.add(new int[] { x, y - 1 });
						}

						// Down
						if (y + 1 <= map.mapHeight - 1 && compare(x, y + 1, type)) {
							list.add(new int[] { x, y + 1 });
						}
					} else {
						break;
					}
				}
			}
			size = list.size();
		}
		return groupSize;
	}
}
