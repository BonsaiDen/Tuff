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

public class GroupSoundRenderer extends GroupRenderer {

	public GroupSoundRenderer(final TuffMap map) {
		super(map);
	}

	@Override
	protected int checkSize(final int size, final boolean border, final int type) {
		if (border) {
			if (type == 4) {
				return size > 25 ? 1 : 0;
			} else {
				return size > 50 ? 1 : 0;
			}
		} else {
			return 1;
		}
		// return size > 10 ? 1 : 0;
	}

	@Override
	protected void set(final int x, final int y, final int type, final int group) {
		groupData[x][y] = group;
		if (!hasBorder) {
			int cl = map.getAt(x - 1, y);
			int cr = map.getAt(x + 1, y);
			int cu = map.getAt(x, y - 1);
			int cd = map.getAt(x, y + 1);
			if (type == 2 && (cl == 0 || cr == 0 || cu == 0 || cd == 0)) {
				hasBorder = true;
			}
			if (type == 4 && (cl == 0 || cr == 0 || cu == 0 || cd == 0)) {
				hasBorder = true;
			} else if (type == 4 && (cl == 2 || cr == 2 || cu == 2 || cd == 2)) {
				hasBorder = true;
			}
		}
	}

	@Override
	protected boolean compare(final int x, final int y, final int type) {
		return map.mapData[x][y] == type && groupData[x][y] == 0;
	}
}
