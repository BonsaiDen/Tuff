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

import java.awt.image.BufferedImage;

import org.bonsai.dev.GameObject;

public abstract class PlayerObject extends GameObject<Tuff> {
	int leftCol = -6;
	int rightCol = 5;
	int topColSize = 12;

	int speed = 3;
	float moveX = 0.0f;
	int posY;
	int posX;
	int side = 0;

	float grav = 0.0f;
	int pixelGrav = 0;
	float maxGrav = 5.0f;
	float addGrav = 0.0f;

	BufferedImage[] tilesLeft;
	BufferedImage[] tilesRight;

	TuffMap map;
	boolean onGround;

	public void loadTiles(String filename, int cols, int rows) {
		tilesLeft = image.gets(filename, cols, rows);
		tilesRight = image.flips(tilesLeft, true, false);
	}

	public PlayerObject(Tuff g) {
		super(g);
	}

	// Control -----------------------------------------------------------------
	public void control() {
		movement();
		gravity();
	}

	// Movement ----------------------------------------------------------------
	protected void movement() {
		if (moveX > 0.0) {
			moveX -= 0.09f;
		}
		if (moveX < 0.0) {
			moveX += 0.09f;
		}
		if (moveX > -0.1f && moveX < 0.1f) {
			moveX = 0.0f;
		}

	}

	public void moveX(int side) {
		int add = side == 1 ? 1 : -1;
		int move = (int) (speed + Math.abs(moveX));
		for (int i = 0; i < move; i++) {
			int col = 0;
			if (side == 1) {
				col = _checkColX(rightCol);
			} else {
				col = _checkColX(leftCol);
			}
			if (col != 0) {
				if (side == 1) {
					if (onRightCollision(col)) {
						break;
					}
				} else {
					if (onLeftCollision(col)) {
						break;
					}
				}

			} else {
				posX += add;
			}
		}
	}

	private int _checkColX(int offset) {
		for (int i = topColSize - 1; i > 0 - map.tileSize; i -= map.tileSize) {
			if (i < 1) {
				i = 1;
			}
			int col = map.colAt(posX + offset, posY - i);
			if (col > 0) {
				return col;
			}
			if (i == 1) {
				break;
			}
		}
		return 0;
	}

	public boolean isIn(int x, int y, int width, int height) {
		return in(x, y, width, height, posX + rightCol, posY)
				|| in(x, y, width, height, posX + leftCol, posY)
				|| in(x, y, width, height, posX + rightCol, posY - topColSize)
				|| in(x, y, width, height, posX + leftCol, posY - topColSize);
	}

	public boolean in(int x, int y, int width, int height, int px, int py) {
		return px >= x && px <= x + width && py >= y && py <= y + height;
	}

	public boolean isOn(int x, int y, int width) {
		return (posY == y && ((posX + rightCol >= x && posX + rightCol <= x
				+ width) || (posX + leftCol >= x && posX + leftCol <= x + width)));
	}

	public boolean onBlock() {
		return map.colAt(posX + rightCol, posY) == 9
				|| map.colAt(posX + leftCol, posY) == 9;
	}

	public boolean onGround() {
		int cl = map.colAt(posX + rightCol, posY);
		int cr = map.colAt(posX + leftCol, posY);
		if (cl == 1 || cl == 3) {
			return true;
		} else if (cr == 1 || cr == 3) {
			return true;
		}
		return false;
	}

	// Gravity -----------------------------------------------------------------
	protected void gravity() {
		grav += addGrav;
		if (grav > maxGrav) {
			grav = maxGrav;
		}

		pixelGrav = (int) grav;
		if (pixelGrav == 0) {
			if (grav < 0.0f) {
				pixelGrav = -1;
			} else if (grav > 0.0f) {
				pixelGrav = 1;
			}
		}
		_moveY(0);
	}

	private void _moveY(int add) {
		int grav = 0;
		if (add == 0) {
			if (pixelGrav > 0) {
				add = 1;

			} else if (pixelGrav < 0) {
				add = -1;
			}
			grav = Math.abs(pixelGrav);

		} else {
			grav = Math.abs(add);
			if (add > 0) {
				add = 1;
			} else if (add < 0) {
				add = -1;
			}
		}

		// Move
		for (int i = 0; i < grav; i++) {
			int col = 0;
			if (add == 1) {
				col = _checkColY(0);
			} else {
				col = _checkColY(0 - topColSize);
			}

			if (col != 0) {
				this.grav = 0.0f;
				if (add == 1) {
					if (!onGround) {
						if (onBottomCollision(col)) {
							onGround = true;
							break;
						}
					}
				} else {
					if (onTopCollision(col)) {
						break;
					}
				}
			} else {
				onGround = false;
				if (add == 1) {
					onFalling();
				} else {
					onJumping();
				}
				posY += add;
			}
		}
	}

	private int _checkColY(int offset) {
		int max = rightCol - 1;
		for (int i = leftCol + 1; i < max + map.tileSize; i += map.tileSize) {
			if (i > max) {
				i = max;
			}
			int col = map.colAt(posX + i, posY + offset);
			if (col > 0) {
				return col;
			}
			if (i == max) {
				break;
			}
		}
		return 0;
	}

	// Events ------------------------------------------------------------------
	abstract boolean onRightCollision(int col);

	abstract boolean onLeftCollision(int col);

	abstract boolean onTopCollision(int col);

	abstract boolean onBottomCollision(int col);

	abstract void onFalling();

	abstract void onJumping();
}
