package com.isofrieze;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.isofrieze.SMBLevelDrawer.Game;
import com.isofrieze.SMBLevelDrawer.LevelType;
import com.isofrieze.SpriteSheetManager.Sprite;
import com.isofrieze.SpriteSheetManager.SpriteInstance;

public class LevelSpriteBuilder {
	
	// this flag enables sprites with hard mode flag set
	public static boolean HARD_MODE = false;
	
	// these flags turn lifts into clouds and make lifts smaller
	public static boolean CLOUD_LIFT = false, SMALL_LIFT = false;
	
	// this flag forces hard mode, and turns goombas into buzzy beetles
	public static boolean SECOND_QUEST = false;
	
	// whether to draw markers and hex data of level data in the output images
	public static boolean VERBOSE_SPRITES = false;
	
	// pointer to the first sprite in this level's sprite data
	final int dataBasePointer;
	// memory
	MemoryManager memory;
	
	// list of sprites to actually draw on the output image
	List<SpriteInstance> displayedSprites;
	
	// list of sprite data in the level
	List<SpriteObject> spriteList;
	
	class SpriteObject {
		String data;
		int x, y;
		
		public SpriteObject(int a, int b, int x, int y) {
			this.data = String.format("%2x%2x", a, b).replace(' ', '0').toUpperCase();
			this.x = x;
			this.y = y;
		}
		
		public SpriteObject(int a, int b, int c, int x, int y) {
			this.data = String.format("%2x%2x%2x", a, b, c).replace(' ', '0').toUpperCase();
			this.x = x;
			this.y = y;
		}
	}
	
	public LevelSpriteBuilder(int dataPointer, LevelType type, boolean clouds) {
		dataBasePointer = dataPointer;
		
		// make lifts cloudy if tile data detected so
		CLOUD_LIFT = clouds;
		
		// enable hard mode if on second quest or after a certain level
		int[] hardModeStarts = SMBLevelDrawer.game.getHardModeStart();
		HARD_MODE = (SECOND_QUEST || SMBLevelDrawer.MY_EXTRA ||
				SMBLevelDrawer.MY_WORLD > hardModeStarts[0] ||
				(SMBLevelDrawer.MY_WORLD == hardModeStarts[0] && SMBLevelDrawer.MY_LEVEL >= hardModeStarts[1]));
		
		// make lifts short if in hard mode or if in a castle
		SMALL_LIFT = (HARD_MODE || type == LevelType.CASTLE);
		
		memory = SMBLevelDrawer.memory;
		displayedSprites = new ArrayList<>();
		spriteList = new ArrayList<>();
	}
	
	public void addSpontaneousSprites(List<SpriteInstance> sprites) {
		displayedSprites.addAll(sprites);
	}
	
	// build the level's sprites
	// 1) create a list of sprites that are drawn to the level for the output image
	// 2) create a technical list of all sprites in the level for verbose markers
	public void build() {
		// offset into the sprite data
		int offset = 0;
		
		// the current screen we are on
		int workingScreen = 0;
		
		// the current X position we are at, in tile columns
		int xExtent = 0;
		
		// flag if a screen jump was just used
		// used so that a screen jump with a next screen flag doesn't skip 2 screens
		boolean screenJump = false;
		
		//TODO looping offset
		while (offset < 0x100 && memory.read8(dataBasePointer + offset) != 0xFF) {
			int a = memory.read8(dataBasePointer + offset);
			offset++;
			int b = memory.read8(dataBasePointer + offset);
			offset++;
			
			// get sprite position data
			int xPos = memory.getBits(a, 0xF0);
			int yPos = memory.getBits(a, 0x0F);
			int nextScreen = memory.getBits(b, 0x80);
			
			// advance to the next screen if necessary
			if (nextScreen > 0 && !screenJump) {
				workingScreen++;
			} else {
				screenJump = false;
			}

			// X position of the sprite within the level (column)
			int globalXPos = workingScreen * 0x10 + xPos;
			SpriteObject sprite;
			
			if (yPos == 15) {
				// screen jump
				int screenNumber = memory.getBits(b, 0x1F);
				
				workingScreen = screenNumber;
				screenJump = true;
				
				sprite = new SpriteObject(a, b, globalXPos, yPos);
				
			} else if (yPos == 14) {
				// level transition
				int c = memory.read8(dataBasePointer + offset);
				offset++;
				
				int levelId = memory.getBits(b, 0x7F);
				int worldFilter = memory.getBits(c, 0xE0);
				int screenNumber = memory.getBits(c, 0x1F);
				
				sprite = new SpriteObject(a, b, c, globalXPos, yPos);
				
			} else {
				// standard sprite
				int hardMode = memory.getBits(b, 0x40);
				int spriteType = memory.getBits(b, 0x3F);
				
				sprite = new SpriteObject(a, b, globalXPos, yPos);
				
				// sprite only spawns if it is within the loading zone (about a 4 column wide area)
				// and if either hard mode is enabled or the sprite's flag is not set
				if (globalXPos >= xExtent - 4 && (HARD_MODE || hardMode == 0)) {
					
					addApplicableDisplaySprites(spriteType, 16 * globalXPos, 16 * yPos);
					xExtent = globalXPos;
				}
					
			}
			
			spriteList.add(sprite);
		}
		
	}

	// add the corresponding graphic to the list of sprites to draw
	// (x,y) position is tied to logical position--graphical offsets taken care of later
	private void addApplicableDisplaySprites(int type, int x, int y) {
		switch (type) {
			case 0x00: addDisplaySprite(Sprite.GREEN_KOOPA, x, y); break;
			case 0x01: addDisplaySprite(Sprite.RED_KOOPA, x, y); break; // red koopa that stands still
			case 0x02: addDisplaySprite(Sprite.BUZZY_BEETLE, x, y); break;
			case 0x03: addDisplaySprite(Sprite.RED_KOOPA, x, y); break;
			case 0x04: addDisplaySprite(Sprite.GREEN_KOOPA, x, y); break; // green koopa that stands still
			case 0x05: addDisplaySprite(Sprite.HAMMER_BRO, x, y); break;
			case 0x06: addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x, y); break;
			case 0x07: addDisplaySprite(Sprite.BLOOPER, x, y); break;
			case 0x08: addDisplaySprite(Sprite.BULLET_BILL, x, y); break;
			case 0x09: addDisplaySprite(Sprite.GREEN_PARAKOOPA, x, y); break; // green parakoopa that stands still
			case 0x0A: addDisplaySprite(Sprite.GREEN_CHEEP_CHEEP, x, y); break;
			case 0x0B: addDisplaySprite(Sprite.RED_CHEEP_CHEEP, x, y); break;
			case 0x0C: addDisplaySprite(Sprite.PODOBOO, x, y); break;
			case 0x0D: {
				// red piranha plants in world 4 and later in LL
				Sprite plant = 
						(SMBLevelDrawer.game == Game.LOST_LEVELS &&
						(SMBLevelDrawer.MY_EXTRA || SMBLevelDrawer.MY_WORLD >= 4)) ?
								Sprite.RED_PIRANHA_PLANT : Sprite.GREEN_PIRANHA_PLANT;
				addDisplaySprite(plant, x, y); break;
			}
			case 0x0E: addDisplaySprite(Sprite.GREEN_PARAKOOPA, x, y); break; // jumping
			case 0x0F: addDisplaySprite(Sprite.RED_PARAKOOPA, x, y); break; // up and down
			
			case 0x10: addDisplaySprite(Sprite.GREEN_PARAKOOPA, x, y); break; // left and right
			case 0x11: addDisplaySprite(Sprite.LAKITU, x, y); break;
			case 0x12: addDisplaySprite(Sprite.SPINY, x, y); break;
			// case 0x13: // glitchy koopa
			case 0x14: addDisplaySprite(Sprite.RED_CHEEP_CHEEP, x, y); break;
			case 0x15: addDisplaySprite(Sprite.BOWSER_FIREBALL, x, y); break;
			case 0x16: addDisplaySprite(Sprite.FIREWORK, x, y); break;
			case 0x17: addDisplaySprite(Sprite.BULLET_BILL, x, y); break;
			// case 0x18: // nothing
			// case 0x19: // nothing
			// case 0x1A: // nothing
			case 0x1B: case 0x1C: case 0x1D: case 0x1E:
						addDisplaySprite(Sprite.FIREBAR, x, y); break;
			case 0x1F: addDisplaySprite(Sprite.LONG_FIREBAR, x, y); break;
			
			case 0x20: case 0x21: case 0x22: // glitchy firebars
						addDisplaySprite(Sprite.FIREBAR, x-4, y-52); break;
			// case 0x23: // nothing
			case 0x24: case 0x25: case 0x26: case 0x27: case 0x28: case 0x29: case 0x2A: {
				// lifts can be smaller and/or clouds
				Sprite lift = CLOUD_LIFT ?
						(SMALL_LIFT ? Sprite.MEDIUM_CLOUDS : Sprite.LONG_CLOUDS) :
						(SMALL_LIFT ? Sprite.MEDIUM_LIFT : Sprite.LONG_LIFT);
				addDisplaySprite(lift, x, y); break;
			}
			case 0x2B: case 0x2C:
						addDisplaySprite(Sprite.SHORT_LIFT, x, y); break;
			case 0x2D: addDisplaySprite(Sprite.BOWSER, x, y); break;
			case 0x2E: addDisplaySprite(Sprite.SUPER_MUSHROOM, x, y); break;
			case 0x2F: addDisplaySprite(Sprite.VINE, x, y); break;
			
			case 0x30: addDisplaySprite(Sprite.FLAGPOLE_FLAG, x, y); break;
			case 0x31: addDisplaySprite(Sprite.CASTLE_FLAG, x, y); break;
			case 0x32: {
				Sprite spring = Sprite.RED_SPRINGBOARD;
				
				// springboards are green in LL in worlds 2/B, 3/C, and 7
				if (SMBLevelDrawer.game == Game.LOST_LEVELS) {
					if (SMBLevelDrawer.MY_WORLD == 2 || SMBLevelDrawer.MY_WORLD == 3 || SMBLevelDrawer.MY_WORLD == 7) {
						spring = Sprite.GREEN_SPRINGBOARD;
					}
				// springsboards are green in ANN in worlds B and C only
				} else if (SMBLevelDrawer.game == Game.ALL_NIGHT_NIPPON) {
					if (SMBLevelDrawer.MY_EXTRA && (SMBLevelDrawer.MY_WORLD == 2 || SMBLevelDrawer.MY_WORLD == 3)) {
						spring = Sprite.GREEN_SPRINGBOARD;
					}
				}
				addDisplaySprite(spring, x, y); break;
			}
			case 0x33: addDisplaySprite(Sprite.BULLET_BILL, x, y); break;
			// case 0x34: // warp zone
			case 0x35: {
				Sprite npc = Sprite.TOAD;
				
				// in ANN, the NPCs are all different per world
				if (SMBLevelDrawer.game == Game.ALL_NIGHT_NIPPON) {
					if (SMBLevelDrawer.MY_EXTRA) {
						npc = Sprite.ANN_NPC_8;
					} else {
						switch (SMBLevelDrawer.MY_WORLD) {
							case 1: npc = Sprite.ANN_NPC_1; break;
							case 2: npc = Sprite.ANN_NPC_2; break;
							case 3: npc = Sprite.ANN_NPC_3; break;
							case 4: npc = Sprite.ANN_NPC_4; break;
							case 5: npc = Sprite.ANN_NPC_5; break;
							case 6: npc = Sprite.ANN_NPC_6; break;
							case 7: npc = Sprite.ANN_NPC_7; break;
							case 8: npc = Sprite.DOOR; break;
						}
					}
					
				} else {
					// the sprite is a door in world 8/D in LL
					if (SMBLevelDrawer.game == Game.LOST_LEVELS) {
						if (SMBLevelDrawer.MY_WORLD == 8 || (SMBLevelDrawer.MY_EXTRA && SMBLevelDrawer.MY_WORLD == 4))
							npc = Sprite.DOOR;
							
					// the sprite is Peach in world 8 in SMB/FDS
					} else {
						if (SMBLevelDrawer.MY_WORLD == 8)
							npc = Sprite.PEACH;
					}
				}
				addDisplaySprite(npc, x, y); break;
			}
			// case 0x36: // CRASH likely
			case 0x38:
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x, 0xB0);
			case 0x37: {
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x-0x18, 0xB0);
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x-0x30, 0xB0);
				break;
			}
			case 0x3A:
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x, 0x70);
			case 0x39: {
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x-0x18, 0x70);
				addDisplaySprite(SECOND_QUEST ? Sprite.BUZZY_BEETLE : Sprite.GOOMBA, x-0x30, 0x70);
				break;
			}
			case 0x3C:
				addDisplaySprite(Sprite.GREEN_KOOPA, x, 0xB0);
			case 0x3B: {
				addDisplaySprite(Sprite.GREEN_KOOPA, x-0x18, 0xB0);
				addDisplaySprite(Sprite.GREEN_KOOPA, x-0x30, 0xB0);
				break;
			}
			case 0x3E:
				addDisplaySprite(Sprite.GREEN_KOOPA, x, 0x70);
			case 0x3D: {
				addDisplaySprite(Sprite.GREEN_KOOPA, x-0x18, 0x70);
				addDisplaySprite(Sprite.GREEN_KOOPA, x-0x30, 0x70);
				break;
			}
			// case 0x3F: // CRASH likely
		}
	}
	
	private void addDisplaySprite(Sprite sprite, int x, int y) {
		displayedSprites.add(SMBLevelDrawer.ssm.new SpriteInstance(sprite, x, y));
	}
	
	public BufferedImage print() {
		return new BufferedImage(16,16,BufferedImage.TYPE_4BYTE_ABGR);
	}
}