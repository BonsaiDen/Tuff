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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;

public class Player extends PlayerObject {
	// Stuff
	private int oldX = posX;
	public boolean hasControl = true;
	public boolean sleeping = false;

	// Water
	private int waterPosY;
	private boolean inWater;
	private boolean inWaterLow;
	private boolean fullWater = false;
	private int waterJumps = 0;
	private boolean isDiving = false;

	// Transparent
	private boolean isBreaker = false;
	private boolean breakImage = false;

	// Jumping
	private float jumpGrav = 0.0f;
	private int playerHeight = 0;
	private int wallSide = 0;
	private int atWallInit = -1;
	private boolean moveWall;
	private int moveWallX = 0;
	private int moveWallY = 0;
	private boolean wallLeft = false;
	private boolean wallRight = false;
	private boolean superJumping;
	private LinkedList<int[]> dashes = new LinkedList<int[]>();

	// Speed
	private int speedSide = 0;
	private int speedActive = 0;
	private int speedHeight = 0;
	private int speedPos = 0;

	// Images
	private int playerImage;
	private boolean KEY_JUMP;
	private boolean KEY_LEFT;
	private boolean KEY_RIGHT;
	private boolean KEY_DOWN;

	// Abilities
	public boolean hasHighJump = false;
	public boolean hasSuperJump = false;
	public boolean hasWallJump = false;
	public boolean hasDive = false;
	public boolean hasBreak = false;
	public boolean hasSpeed = false;
	public String[] powerMessages =
			new String[] { "HIGHJUMP", "WALLJUMP", "SUPERJUMP", "DIVE",
					"BREAK", "SPEED" };

	// Entities
	public ArrayList<int[]> entitiesCollected = new ArrayList<int[]>();

	// Blocks
	public ArrayList<int[]> blocksOpened = new ArrayList<int[]>();

	// Switches
	public boolean[] switchesToggled = new boolean[4];

	public Player(Tuff g, int x, int y) {
		super(g);
		loadTiles("/images/player.png", 23, 2);

		// Animations
		animation.add("walk", new int[] { 3, 4, 5, 6 }, 25, true);
		animation.add("swim", new int[] { 1, 17, 18, 17 }, 50, true);
		animation.add("idle", new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1,
				1, 1, 1, 1 }, 125, true);
		animation.add("sleep", new int[] { 8, 8, 9, 9, 10, 10, 11, 11 }, 175,
				true);
		animation.add("blink", new int[] { 13, 13 + 23 }, 75, true);
		animation.add("dashup", new int[] { 14, 14 + 23 }, 75, true);
		animation.add("speed", new int[] { 20, 21, 22 }, 25, true);

		// Timers
		timer.add("fullWater", 1000);
		timer.add("outWater", 1000);
		timer.add("waterReset", 250);
		timer.add("inWater", 250);
		timer.add("swim", 500);
		timer.add("dive", 500);
		timer.add("superJump", 1500);
		timer.add("walk", 170);
		timer.add("moved", 25000);
		timer.add("moveWall", 750);
		timer.add("jump", 200);
		timer.add("jumpWater", 100);
		timer.add("offWall", 350);

		timer.add("dashes", 50);
		timer.add("break", 1500);
		timer.add("breakBlink", 75);
	}

	public void setMap(TuffMap map) {
		this.map = map;
		posX = map.startX * map.tileSize + 8;
		posY = map.startY * map.tileSize + 16;
		onGround = true;
	}

	// Movement ----------------------------------------------------------------
	@Override
	public void control() {
		oldX = posX;

		// Control
		if (hasControl) {

			// Movements
			KEY_JUMP = input.keyPressed(java.awt.event.KeyEvent.VK_SPACE);
			KEY_LEFT =
					input.keyDown(java.awt.event.KeyEvent.VK_A)
							|| input.keyDown(java.awt.event.KeyEvent.VK_LEFT);

			KEY_RIGHT =
					input.keyDown(java.awt.event.KeyEvent.VK_D)
							|| input.keyDown(java.awt.event.KeyEvent.VK_RIGHT);

			KEY_DOWN =
					input.keyDown(java.awt.event.KeyEvent.VK_S)
							|| input.keyDown(java.awt.event.KeyEvent.VK_DOWN);

			// Stuff
			controlSpeedBreakJump();
			controlWater();
			controlMovement();
			controlJump();

			// Gravity
			if (moveWall) {
				maxGrav = 1.25f;
			} else {
				maxGrav = 4;
			}

			// Diving
			if (inWater || map.waterAt(posX, posY - 12, false)) {
				if (hasDive && isDiving) {
					if (maxGrav > 1.5f) {
						maxGrav -= 0.25f;
					}
					maxGrav = 1.0f;
					addGrav = 0.35f;
					if (timer.pending("dive")) {
						maxGrav = 2.5f;
					}
				}
				timer.set("waterReset");

			} else if (timer.expired("waterReset")) {
				map.transparentTile = 2;
			}

			// Swimming
			if ((!hasDive || !isDiving) && inWater && fullWater) {
				if (!map.hasColAt(posX, posY - 14)) {
					onGround = false;
					if (posY > waterPosY + 5) {
						posY -= 1;
						if (grav > 0.0f) {
							grav = 0.0f;
						}
						maxGrav = 1.5f;
					}
					addGrav = 0.0f;
				}
			}
			gravity();
		}
		animate();
	}

	public void controlMovement() {
		// Down Stuff
		if (KEY_DOWN) {
			timer.set("moved");
			moveWall = false;
			timer.set("moveWall", -5000);
			if (inWater && hasDive) {
				if (!isDiving) {
					isDiving = true;
					game.playSound("dive", true);
				}
			}
		}

		// Movement
		if (speedActive == 1) {
			moveX = side == 0 ? -0.5f : 0.5f;
			speed = 4;

		} else if (speedActive == 2) {
			moveX = side == 0 ? -0.5f : 0.5f;
			speed = 5;

		} else if (speedActive == 3) {
			moveX = side == 0 ? -0.5f : 0.5f;
			speed = 7;

		} else if (inWater) {
			speed = 2;

		} else {
			speed = 3;
		}
		if (superJumping && input.keyDown(java.awt.event.KeyEvent.VK_SPACE)) {
			grav = -8f;
		}
		// moveWall = false;
		if (moveX > 0.0) {
			moveX -= 0.09f;
		}
		if (moveX < 0.0) {
			moveX += 0.09f;
		}
		if (moveX > -0.1f && moveX < 0.1f) {
			moveX = 0.0f;
		}
		if (moveX < 0.0f
				|| ((((!KEY_DOWN || !onGround) || !hasSuperJump) || (inWater && !KEY_DOWN))
						&& KEY_LEFT && moveX <= 0.15f)
				&& (timer.expired("offWall") || wallSide != 0)) {
			moveX(0);
			if (KEY_LEFT && inWater) {
				moveX = -0.5f;
			}
		}
		if (moveX > 0.0f
				|| ((((!KEY_DOWN || !onGround) || !hasSuperJump) || (inWater && !KEY_DOWN))
						&& KEY_RIGHT && moveX >= -0.15f)
				&& (timer.expired("offWall") || wallSide != 1)) {
			moveX(1);
			if (KEY_RIGHT && inWater) {
				moveX = 0.5f;
			}
		}
		if (KEY_DOWN) {
			if (input.keyPressed(java.awt.event.KeyEvent.VK_A)) {
				side = 0;
			} else if (input.keyPressed(java.awt.event.KeyEvent.VK_D)) {
				side = 1;
			}
		}

		if (posX > oldX) {
			side = 1;
		} else if (posX < oldX) {
			side = 0;
		}
		// Sounds
		if (posX != oldX) {
			int soundSpeed = 170;
			if (speedActive == 1) {
				soundSpeed = 150;
			} else if (speedActive == 2) {
				soundSpeed = 115;
			} else if (speedActive == 3) {
				soundSpeed = 75;
			}
			if (onGround && timer.expired("walk", soundSpeed)) {
				game.playSound("walk", true);
				timer.set("walk");
			}
			if (inWater && !isDiving && timer.expired("swim")) {
				game.playSound("swim", true);
				timer.set("swim");
			}
		}

	}

	public void controlJump() {
		// Bewteen 2 walls
		boolean doubleWall = false;
		if (!inWater) {
			checkWallJump();
			if (moveWall) {
				moveWallX = posX;
				moveWallY = posY;
			}
			doubleWall =
					map.hasColAt(posX + 11, posY - 7)
							&& map.hasColAt(posX - 12, posY - 7);

			if (timer.expired("moveWall") || doubleWall) {
				moveWall = false;
			}
		} else {
			moveWall = false;
		}

		// Jumping height
		if (hasHighJump) {
			jumpGrav = -7.9f;
			addGrav = 0.75f;
		} else {
			jumpGrav = -7.2f;
			addGrav = 1f;
		}

		if (KEY_JUMP) {
			// Out of Water
			if (inWater && (grav >= 0.0f || onGround)) {
				boolean out = !map.waterAt(posX, posY - 26, true);
				if (!fullWater) {
					out = false;
					waterJumps += 1;
					if (waterJumps > 2) {
						out = true;
						waterJumps = 0;
					}
				}
				if (!hasDive) {
					out = true;
				}
				grav = jumpGrav / (out ? 1.0f : 1.5f);
				game.playSound(out ? "jump" : "dive", false);
				if (out) {
					fullWater = false;
				}
				timer.set("jump");
				timer.set("jumpWater");

			} else if (onGround) {
				grav = jumpGrav;
				game.playSound("jump", false);
				timer.set("jump");
				timer.set("jumpWater");

				// Walljump
			} else if (wallSide != -1 && grav > 1.0f && hasWallJump
					&& timer.pending("moveWall") && !doubleWall
					&& Math.abs(moveWallX - posX) <= 5
					&& Math.abs(moveWallY - posY) <= 5) {

				if (wallSide == 0) {
					moveX += 0.8f;
				} else {
					moveX -= 0.8f;
				}

				atWallInit = wallSide;
				grav = jumpGrav;
				moveWall = false;
				game.playSound("jump", false);
				timer.set("moveWall", -500);
				timer.set("jump");
				timer.set("jumpWater");
				timer.set("offWall");

				// Super Walljump
				if (isBreaker) {
					dashes.clear();
					timer.set("dashes", -350);
					superJumping = true;
				}
			}
		}
	}

	public void controlWater() {
		// Water
		boolean topWater = map.waterAt(posX, posY - 12, true);
		boolean oldWater = inWater;
		boolean oldWaterLow = inWaterLow;
		inWater = map.waterAt(posX, posY - 3, false);
		inWaterLow = map.waterAt(posX, posY - 1, false);
		if (inWater) {
			timer.set("outWater");
		}
		if (inWater && topWater) {
			fullWater = true;
		} else {
			timer.set("fullWater");
		}

		// Swimming
		if (!oldWater && inWater) {
			waterPosY = getWaterLevel();
			if (posY - waterPosY < 20) {
				isDiving = false;
			} else {
				isDiving = true;
			}
			atWallInit = -1;
		}
		if (onGround) {
			waterJumps = 0;
			moveWall = false;
		}

		// Splash Sound
		if (!oldWaterLow && inWaterLow && grav > 0.0f
				&& timer.expired("inWater")
				&& !map.waterAt(posX, posY - 7, false)) {
			atWallInit = -1;
			game.playSound(grav > 1.5f ? "splash" : "swim", true);
			timer.set("inWater");
			timer.set("swim");
			grav = grav / 2;
		}

		if (inWater && !onGround && hasDive) {
			if (input.keyPressed(java.awt.event.KeyEvent.VK_S)) {
				timer.set("dive");
				grav += 3.0f;
				game.playSound("dive", true);
			}
		}
	}

	public void controlSpeedBreakJump() {
		// Init Speed
		if (hasSpeed) {
			if ((KEY_LEFT || KEY_RIGHT) && speedHeight == posY && !inWater) {
				if (speedPos - posX > 48 && speedActive == 0) {
					speedActive = 1;
					speedSide = 0;

				} else if (speedPos - posX < -48 && speedActive == 0) {
					speedActive = 1;
					speedSide = 1;

				} else if (speedPos - posX > 80 && speedActive == 1) {
					speedActive = 2;
					speedSide = 0;

				} else if (speedPos - posX < -80 && speedActive == 1) {
					speedActive = 2;
					speedSide = 1;

				} else if (speedPos - posX > 112 && speedActive == 2) {
					speedActive = 3;
					speedSide = 0;
					dashes.clear();
					timer.set("dashes", -350);

				} else if (speedPos - posX < -112 && speedActive == 2) {
					speedActive = 3;
					speedSide = 1;
					dashes.clear();
					timer.set("dashes", -350);
				}
			} else {
				speedHeight = posY;
				speedPos = posX;
				if (speedActive != 0) {
					speedActive = 0;
					breakImage = false;
				}
			}
			if (speedActive != 0) {
				if (speedHeight != posY || (KEY_RIGHT && speedSide == 0)
						|| (KEY_LEFT && speedSide == 1)) {
					speedActive = 0;
					speedPos = posX;
					breakImage = false;
				}
			}
		}

		// Breaker
		if (!isBreaker) {
			if (!KEY_DOWN || !onGround || !hasBreak) {
				timer.set("break");
			}
			if (timer.expired("break")) {
				timer.set("break");
				isBreaker = true;
			}
		} else {
			if (timer.expired("break", 3000)) {
				isBreaker = false;
				breakImage = false;
			}
		}

		if (isBreaker || speedActive > 2) {
			if (timer.expired("breakBlink")) {
				breakImage = !breakImage;
				if (!isBreaker && speedActive == 0) {
					breakImage = false;
				}
				timer.set("breakBlink");
			}
		}

		// Dashing
		if (!superJumping && hasSuperJump) {
			if (!KEY_DOWN || !onGround || !hasSuperJump) {
				timer.set("superJump");
			}
			if (KEY_JUMP
					&& (timer.expired("superJump") || (isBreaker && onGround))
					&& !superJumping) {
				superJumping = true;
				dashes.clear();
				timer.set("dashes", -350);
				timer.set("superJump");
			}
		} else {
			if (timer.expired("superJump", 220)) {
				superJumping = false;
				timer.set("superJump");
			}
		}

		if (superJumping || speedActive > 2) {
			if (timer.expired("dashes")) {
				if (dashes.size() > 2) {
					dashes.remove(0);
				}
				dashes.add(new int[] { posX, posY });
				timer.set("dashes");
			}
		}
	}

	public boolean underTransparent() {
		return map.transparentAt(posX, posY - 7);
	}

	public boolean hasAbility(String type) {
		if (type.equals("W") && hasWallJump) {
			return true;

		} else if (type.equals("J") && hasHighJump) {
			return true;

		} else if (type.equals("D") && hasDive) {
			return true;

		} else if (type.equals("U") && hasSuperJump) {
			return true;

		} else if (type.equals("B") && hasBreak) {
			return true;

		} else if (type.equals("S") && hasSpeed) {
			return true;

		} else {
			return false;
		}
	}

	public void activateAbility(String type) {
		if (type.equals("W")) {
			hasWallJump = true;
			game.textMessage(powerMessages[1]);

		} else if (type.equals("J")) {
			hasHighJump = true;
			game.textMessage(powerMessages[0]);

		} else if (type.equals("D")) {
			hasDive = true;
			game.textMessage(powerMessages[3]);

		} else if (type.equals("U")) {
			hasSuperJump = true;
			game.textMessage(powerMessages[2]);

		} else if (type.equals("S")) {
			hasSpeed = true;
			game.textMessage(powerMessages[5]);

		} else if (type.equals("B")) {
			hasBreak = true;
			game.textMessage(powerMessages[4]);
		}
	}

	// Gravity -----------------------------------------------------------------
	private int getWaterLevel() {
		int yp = posY;
		while (true) {
			if (!map.waterAt(posX, yp - 1, false)) {
				break;
			}
			yp -= 1;
		}
		return yp;
	}

	private void checkWallJump() {
		int yp = posY;
		playerHeight = 0;
		while (playerHeight < 32) {
			int col = map.colAt(posX, yp, false);
			if (col > 0) {// || map.waterAt(posX, yp, false)) {
				break;
			}
			yp += 1;
			playerHeight += 1;
		}

		if (inWater || inWaterLow) {
			wallLeft = false;
			wallRight = false;

		} else {
			wallLeft =
					map.hasColAt(posX - 6, posY - 8)
							&& map.hasColAt(posX - 6, posY + 9)
							&& !map.hasColAt(posX + 11, posY - 7);

			wallRight =
					map.hasColAt(posX + 5, posY - 9)
							&& map.hasColAt(posX + 5, posY + 9)
							&& !map.hasColAt(posX - 12, posY - 7);
		}

		if (!wallLeft && !wallRight) {
			moveWall = false;
		}
	}

	@Override
	void onJumping() {
	}

	@Override
	void onFalling() {
	}

	@Override
	boolean onTopCollision(int col) {

		// Break
		if (col == 3 && (isBreaker || speedActive > 1)) {
			map.removeAt((posX - 6) / map.tileSize, (posY - 12) / map.tileSize);
			map.removeAt((posX + 6) / map.tileSize, (posY - 12) / map.tileSize);
			game.playSound("wall", false);
			return false;
		} else {
			if (superJumping) {
				superJumping = false;
				grav = 1.5f;
				game.playSound("wall", false);
			}
			if (inWater) {
				grav = 0.125f;
			}
			return true;
		}
	}

	@Override
	boolean onBottomCollision(int col) {
		timer.set("walk");
		atWallInit = -1;
		game.playSound("land", false);
		// Break
		if (col == 3 && (isBreaker || speedActive > 1)) {
			map.removeAt((posX - 6) / map.tileSize, (posY) / map.tileSize);
			map.removeAt((posX + 6) / map.tileSize, (posY) / map.tileSize);
			game.playSound("wall", false);
			return false;
		} else {
			return true;
		}
	}

	@Override
	boolean onLeftCollision(int col) {
		if (speedActive != 0) {
			breakImage = false;
			speedActive = 0;
		}
		wallSide = 0;
		if (atWallInit != wallSide && playerHeight >= 30 && wallLeft) {
			atWallInit = 0;
			if (hasWallJump && !moveWall) {
				moveWall = true;
				timer.set("moveWall");
				if (!onGround) {
					side = 1;
				}
			}
		}
		// Break
		if (col == 3 && (isBreaker || speedActive > 1)) {
			map.removeAt((posX - 8) / map.tileSize, (posY - 1) / map.tileSize);
			map.removeAt((posX - 8) / map.tileSize, (posY - 12) / map.tileSize);
			game.playSound("wall", false);
			return false;
		} else {
			return true;
		}
	}

	@Override
	boolean onRightCollision(int col) {
		if (speedActive != 0) {
			breakImage = false;
			speedActive = 0;
		}
		wallSide = 1;
		if (atWallInit != wallSide && playerHeight >= 30 && wallRight) {
			atWallInit = 1;
			if (hasWallJump && !moveWall) {
				moveWall = true;
				timer.set("moveWall");
				if (!onGround) {
					side = 0;
				}
			}
		}
		// Break
		if (col == 3 && (isBreaker || speedActive > 1)) {
			map.removeAt((posX + 8) / map.tileSize, (posY - 1) / map.tileSize);
			map.removeAt((posX + 8) / map.tileSize, (posY - 12) / map.tileSize);
			game.playSound("wall", false);
			return false;
		} else {
			return true;
		}
	}

	// Drawing
	private void animate() {
		boolean moved = true;
		boolean oldSleeping = sleeping;
		// Wall
		if (moveWall && !onGround && grav > 0.0f) {
			playerImage = 12;

			// Jumping
		} else if (grav < -0.5f && !superJumping) {
			if (timer.pending("jump")) {
				playerImage = 19;

			} else if (timer.pending("jumpWater") && inWater) {
				playerImage = 15;

			} else {
				playerImage = 6;
			}

			// playerImage = (timer.pending("jump") && inWater) ? 15 : 6;
			// System.out.println

			// Falling
		} else if (!onGround && grav > 0.5f) {
			playerImage = timer.pending("dive", 200) ? 16 : 7;

			// Dashing
		} else if (hasControl && (timer.expired("superJump")) || superJumping) {
			if (!superJumping) {
				playerImage = isBreaker ? 13 : animation.get("blink");
			} else {
				playerImage = isBreaker ? 14 : animation.get("dashup");
			}

			// Walking
		} else if (oldX != posX && onGround) {
			playerImage =
					speedActive > 2 ? animation.get("speed")
							: animation.get("walk");

			// Swimming
		} else if (oldX != posX && inWater) {
			playerImage = animation.get("swim");

			// Cower
		} else if (KEY_DOWN && onGround && !superJumping
				&& (hasSuperJump || hasBreak)) {
			playerImage = 13;

			// Idle
		} else {
			playerImage = animation.get("idle");
			moved = false;
		}

		// Moved?
		if (moved) {
			animation.set("idle", 0);
			animation.set("sleep", 0);
			timer.set("moved");
		} else {
			animation.set("walk", 0);
			animation.set("speed", 0);
			animation.set("swim", 0);
		}
		if (timer.expired("moved") && !inWater) {
			playerImage = animation.get("sleep");
			sleeping = true;
		} else {
			sleeping = false;
		}
		if (sleeping != oldSleeping) {
			game.onSleep(sleeping);
		}
	}

	public void draw(Graphics2D g) {
		BufferedImage img;
		if (breakImage) {
			if (playerImage > 23) {
				playerImage -= 23;
			}
			playerImage += 23;
		}
		if (side == 0) {
			img = tilesLeft[playerImage];
		} else {
			img = tilesRight[playerImage];
		}

		// Dash
		if (superJumping || speedActive > 2) {
			Composite tmp = g.getComposite();
			AlphaComposite alphaComposite = null;

			for (int i = 0; i < dashes.size(); i++) {
				int pos[] = dashes.get(i);
				alphaComposite =
						AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
								0.25f + i * 0.25f);
				g.setComposite(alphaComposite);
				g.drawImage(img, pos[0] - 8 - map.screenOffsetX, pos[1] - 16
						- map.screenOffsetY, null);
			}
			g.setComposite(tmp);
		}

		// Player
		g.drawImage(img, posX - 8 - map.screenOffsetX, posY - 16
				- map.screenOffsetY, null);
	}
}
