package com.isofrieze;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.isofrieze.SMBLevelDrawer.Game;
import com.isofrieze.SMBLevelDrawer.LevelType;
import com.isofrieze.SpriteSheetManager.Sprite;
import com.isofrieze.SpriteSheetManager.SpriteInstance;
import com.isofrieze.SpriteSheetManager.Tile;
import com.isofrieze.SpriteSheetManager.TileInstance;

public class LevelTileBuilder {

	// whether to draw markers and hex data of level data in the output images
	public static boolean VERBOSE_TILES = false;
	
	// pointer to the level's header bytes
	final int levelHeaderPointer; 
	// pointer to the first tile object in this level's tile data
	final int dataBasePointer;
	// memory
	MemoryManager memory;
	
	// backdrops that modify the level's palette
	// pulled into a separate thing because these properties stick to the level
	// even if overwritten by a backdrop object after X=24
	enum BackdropModifier {
		NONE, NIGHT, SNOW, NIGHT_SNOW, GRAY
	}
	
	// the special platform setting of this level
	enum SpecialPlatform {
		GREEN_TREE, ORANGE_MUSHROOM, GREEN_CANNON, GREEN_CLOUD
	}
	
	// this level's type, backdrop palette, and special platform
	public static LevelType type;
	public static BackdropModifier backdropPalette;
	public static SpecialPlatform specialPlatform;
	
	// whether this level has wind and upsidedown pipes loaded
	boolean laterObjectsLoaded = false;
	
	// there's only one memory location dedicated to staircases
	// so if two are loaded at once, they clash and use the same value
	int staircase = 9;
	
	// list of sprites we happen across that come with certain tile objects
	// piranha plants, springboards, flagpole flag, castle flag, enemy generators, wind
	List<SpriteInstance> comboSprites;
	
	// list of tiles to actually draw on the output image
	List<TileInstance> displayedTiles;
	
	// list of tile object data in the level
	List<TileObject> tileObjectList;
	
	class TileObject {
		String data;
		int a, b;
		int x, y;
		int width;
		
		public TileObject(int a, int b, int x, int y) {
			this.a = a;
			this.b = b;
			this.data = String.format("%2x%2x", a, b).replace(' ', '0').toUpperCase();
			this.x = x;
			this.y = y;
			this.width = tileObjectWidth(this);
		}
	}
	
	public LevelTileBuilder(int dataPointer, LevelType type) {
		levelHeaderPointer = dataPointer;
		dataBasePointer = 0xFFFF & (dataPointer + 2);
		
		this.type = type;
		
		memory = SMBLevelDrawer.memory;
		comboSprites = new ArrayList<>();
		displayedTiles = new ArrayList<>();
		tileObjectList = new ArrayList<>();
		
		laterObjectsLoaded = (SMBLevelDrawer.game == Game.LOST_LEVELS ||
				SMBLevelDrawer.game == Game.ALL_NIGHT_NIPPON) &&
				(SMBLevelDrawer.MY_WORLD >= 5 || SMBLevelDrawer.MY_EXTRA);
	}
	
	public boolean isCloudy() {
		return specialPlatform == SpecialPlatform.GREEN_CLOUD;
	}
	
	// return list of sprites that come 'for free' from tile objects
	public List<SpriteInstance> getComboSprites() {
		return comboSprites;
	}
	
	// a queue class to help with managing the currently loaded tile objects
	class Queue {
		// the size of the queue
		int SIZE = 3;
		
		// pointers to the tile data of each of the objects
		TileObject[] objects;
		
		// how many columns remaining for each tile object
		// by convention a slot is empty if this is less than 1
		int[] columnsToGo;
		
		// keeps track of queue slots that are filled by objects
		// that are currently being skipped and not drawn
		boolean[] isFilledByFake;
		
		protected Queue() {
			this.objects = new TileObject[SIZE];
			this.columnsToGo = new int[SIZE];
			this.isFilledByFake = new boolean[SIZE];
		}
		
		// add an object to the queue, lowest slot first
		void addObject(TileObject obj) {
			assert !isFull();
			
			for (int i = 0; i < SIZE; i++) {
				if (columnsToGo[i] <= 0) {
					objects[i] = obj;
					columnsToGo[i] = obj.width;
					break;
				}
			}
		}
		
		// add a dummy object to the queue
		// this takes up the slot without actually being filled
		// returns true if this object went into the highest slot (and therefore fills the queue)
		boolean addDummy() {
			assert !isFullOfDummies();
			
			for (int i = 0; i < SIZE; i++) {
				if (columnsToGo[i] <= 0 && !isFilledByFake[i]) {
					isFilledByFake[i] = true;
					return i == SIZE - 1;
				}
			}
			return false; // shouldn't happen
		}
		
		// check if the queue is full of dummy objects
		boolean isFullOfDummies() {
			for (int i = 0; i < SIZE; i++) {
				if (columnsToGo[i] <= 0 && !isFilledByFake[i]) return false;
			}
			return true;
		}
		
		// remove all dummy objects from the queue;
		void removeDummies() {
			for (int i = 0; i < SIZE; i++) isFilledByFake[i] = false;
		}
		
		// decrease all widths by one and remove objects if they are done
		void decrementWidthsAndRemove() {
			for (int i = 0; i < SIZE; i++) columnsToGo[i]--;
		}
		
		// get an object
		// returns null if slot is empty
		TileObject getObject(int i) {
			assert i < SIZE;
			if (columnsToGo[i] <= 0) return null;
			return objects[i];
		}
		
		// get an objects remaining columns to go
		int getColumnsToGo(int i) {
			assert i < SIZE;
			return columnsToGo[i];
		}
		
		// check if the queue is full
		boolean isFull() {
			for (int i = 0; i < SIZE; i++) {
				if (columnsToGo[i] <= 0) return false;
			}
			return true;
		}
		
		// check if the queue is empty
		boolean isEmpty() {
			for (int i = 0; i < SIZE; i++) {
				if (columnsToGo[i] > 0) return false;
			}
			return true;
		}
	}

	// build the level's tile objects
	// 1) create a list of tile that are drawn to the level for the output image
	// 2) create a technical list of all tile objects in the level for verbose markers
	// 3) return the width of the level (last column of last object)
	public int build() {
		int[] terrain = processLevelHeader();
		
		// offset into the tile data
		int offset = 0;

		// the current X position we are at, in tile columns
		int xExtent = 0;

		// flag to help with drawing objects with next screen flag properly
		boolean processNextScreenObject = false;
		
		// flag for the special case of getting locked up on a screen jump with the next screen flag set
		boolean stuckOnNextScreenScreenJump = false;
		
		// the object queue
		Queue queue = new Queue();
		
		// number of columns to draw after the last object
		int BUFFER_SIZE = 16;
		int endBuffer = BUFFER_SIZE;

		// TODO looping offset
		while (offset < 0x100 && endBuffer >= 0) {
			if (memory.read8(dataBasePointer + offset) == 0xFD && queue.isEmpty()) endBuffer--;
			
			// start with an empty column of tiles, and fill it in as we go
			Tile [] column = new Tile[13];
			
			processScenery(xExtent, terrain[0], column);
			processBackdrop(xExtent, terrain[1], column);
			processFloor(xExtent, terrain[2], column);
			
			// if this is the first column of a screen, set up the new screen
			if (xExtent > 0 && xExtent % 0x10 == 0) {
				// if were stuck on a malfunctioning screen jump object, skip it now
				if (stuckOnNextScreenScreenJump) {
					offset += 2;
					stuckOnNextScreenScreenJump = false;
				}
				
				// skip ahead to the next object with the next screen flag set
				while (memory.read8(dataBasePointer + offset) != 0xFD && // while not end of data
					memory.readBits(dataBasePointer + offset + 1, 0x80) == 0 && // while not next screen
						!(memory.readBits(dataBasePointer + offset, 0x0F) == 13 && // while not screen jump
						memory.readBits(dataBasePointer + offset + 1, 0x40) == 0)) {
					
					// add this object to the list
					addObjectToList((xExtent / 0x10) - 1, offset);
					offset += 2;
					
					// temporarily add this object to the queue
					boolean preprocess = queue.addDummy();

					// if it filled the last slot, preprocess the queue on this column without advancing
					if (preprocess) processTheQueue(xExtent, queue, column, terrain);
					
					// and if we filled the queue, remove all the fake objects
					if (queue.isFullOfDummies()) queue.removeDummies();
				}
				
				// no longer need the dummy objects
				queue.removeDummies();
				
				// if the next tile object is a screen jump 
				if (memory.read8(dataBasePointer + offset) != 0xFD &&
						memory.readBits(dataBasePointer + offset, 0x0F) == 13 &&
						memory.readBits(dataBasePointer + offset + 1, 0x40) == 0) {
					
					// if this screen jump has the next screen flag set, it doesn't actually jump screens
					// it actually locks up this screen
					if (memory.readBits(dataBasePointer + offset + 1, 0x80) == 1) {
						addObjectToList(memory.readBits(dataBasePointer + offset + 1, 0x1F), offset);
						stuckOnNextScreenScreenJump = true;
						
					// otherwise it does screen jump
					// determine if the screen we are on is jumped over or not
					} else {
						int currentScreen = xExtent >> 4;
						int jumpTo = memory.readBits(dataBasePointer + offset + 1, 0x1F);
						
						if (currentScreen == jumpTo) {
							// we are at the target screen, so we can finally skip this object
							addObjectToList(jumpTo, offset);
							offset += 2;
							
						} else if (currentScreen > jumpTo) {
							// we are past the target screen
							// this means this was a backwards jump, so we need to skip forward
							// in the level data to account for the screens that will not be processed
							while (memory.read8(dataBasePointer) != 0xFD && jumpTo < currentScreen) {
								addObjectToList(jumpTo, offset);
								offset += 2;
								if (memory.readBits(dataBasePointer + offset + 1, 0x80) == 1)
									jumpTo++;
							}
						}
						// else we are before the target screen, so we don't skip the object
					}
				}
				
				// we are now ready to draw an object with next screen flag
				processNextScreenObject = true;
			}
			
			// add objects to the queue
			while (memory.read8(dataBasePointer + offset) != 0xFD && // while not at the end of data
					!(memory.readBits(dataBasePointer + offset, 0x0F) == 13 && // while not a screen jump object
					memory.readBits(dataBasePointer + offset + 1, 0x40) == 0) &&
					memory.readBits(dataBasePointer + offset, 0xF0) == (xExtent & 0x0F)) { // while on this column
				
				// don't add objects if this object has the correct X value, but is on the next screen
				if (memory.readBits(dataBasePointer + offset + 1, 0x80) == 1 && !processNextScreenObject) break;
				
				processNextScreenObject = false;
				
				// if the queue is full, we're done adding objects
				if (queue.isFull()) break;
				
				// otherwise, add the object to the queue
				TileObject object = addObjectToList(xExtent / 0x10, offset);
				queue.addObject(object);
				offset += 2;
			}
			
			processTheQueue(xExtent, queue, column, terrain);
			
			// add tiles from this column to the drawing list
			for (int i = 0; i < column.length; i++)
				addDisplayTile(column[i], xExtent, i + 1);
			
			// move to the next column
			xExtent++;
		}
		
		// otherwise just return the last column of the last object + 2
		// this makes big castles at the end of levels look good c:
		return xExtent - BUFFER_SIZE + 1;
	}
	
	private void processTheQueue(int x, Queue queue, Tile[] column, int[] terrain) {
		// process each object in the queue in order
		for (int i = 0; i < queue.SIZE; i++) {
			TileObject object = queue.getObject(i);
			if (object != null) processTileObject(x, column, object, queue.getColumnsToGo(i), terrain);
		}
		
		// decrease all object's widths and remove those that hit zero
		queue.decrementWidthsAndRemove();
	}
	
	// process this object at column index and put the relevent tiles in the column
	// index starts at 1 from the right side
	private void processTileObject(int x, Tile[] column, TileObject object, int index, int[] terrain) {
		int y = object.y;
		
		if (y < 12) {
			int id = memory.getBits(object.b, 0x70);
			int length = memory.getBits(object.b, 0x0F) + 1;
			
			if (id == 0) { // various
				// remap all games to same IDs, since things are shifted around
				int kind = remapPowerupBlocks[SMBLevelDrawer.game.getNum()][length - 1];
				
				if (kind == 0) { // ? block with flower
					renderTile(column, y, Tile.QUESTION_BLOCK_FIREFLOWER);
					
				} else if (kind == 1) { // ? block with poison mushroom
					renderTile(column, y, Tile.QUESTION_BLOCK_POISON_MUSHROOM);
					
				} else if (kind == 2) { // ? block with coin
					renderTile(column, y, Tile.QUESTION_BLOCK_COIN);
					
				} else if (kind == 3) { // invisible block with coin
					renderTile(column, y, Tile.INVISIBLE_BLOCK_COIN);
					
				} else if (kind == 4) { // invisible block with 1up
					renderTile(column, y, Tile.INVISIBLE_BLOCK_LIFE_MUSHROOM);
					
				} else if (kind == 5) { // invisible block with poison mushroom
					renderTile(column, y, Tile.INVISIBLE_BLOCK_POISON_MUSHROOM);
					
				} else if (kind == 6) { // invisible block with fireflower
					renderTile(column, y, Tile.INVISIBLE_BLOCK_FIREFLOWER);
					
				} else if (kind == 7) { // brick with fireflower
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_FIREFLOWER : Tile.BRICK_DULL_FIREFLOWER);
					
				} else if (kind == 8) { // brick with poison mushroom
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_POISON_MUSHROOM : Tile.BRICK_DULL_POISON_MUSHROOM);
					
				} else if (kind == 9) { // brick with vine
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_VINE : Tile.BRICK_DULL_VINE);
					
				} else if (kind == 10) { // brick with star
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_STAR : Tile.BRICK_DULL_STAR);
					
				} else if (kind == 11) { // brick with multicoin
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_MULTICOIN : Tile.BRICK_DULL_MULTICOIN);
					
				} else if (kind == 12) { // brick with 1up
					renderTile(column, y, type == LevelType.OVERWORLD ?
							Tile.BRICK_SHINY_LIFE_MUSHROOM : Tile.BRICK_DULL_LIFE_MUSHROOM);
					
				} else if (kind == 13) { // horizontal pipe
					renderTile(column, y, Tile.WATER_PIPE_TOP);
					renderTile(column, y + 1, Tile.WATER_PIPE_BOTTOM);
					
				} else if (kind == 14) { // used block
					renderTile(column, y, Tile.USED_BLOCK);
					
				} else if (kind == 15) { // springboard
					renderTile(column, y + 1, Tile.SPRING_BASE);

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
					addDisplayComboSprite(spring, x, y);
					
				} else if (kind == 16) { // L pipe
					if (index == 1) {
						renderTile(column, 7, Tile.PIPE_LIP_ENTERABLE_RIGHT);
						renderUnderPart(column, 8, Tile.PIPE_SHAFT_RIGHT);
						for (int i = 9; i <= 10; i++) renderTile(column, i, Tile.PIPE_SHAFT_RIGHT);
						
					} else if (index == 2) {
						renderTile(column, 7, Tile.PIPE_LIP_ENTERABLE_LEFT);
						renderUnderPart(column, 8, Tile.PIPE_SHAFT_LEFT);
						renderTile(column, 9, Tile.PIPE_CONNECTION_TOP);
						renderTile(column, 10, Tile.PIPE_CONNECTION_BOTTOM);
						
					} else if (index == 3) {
						renderTile(column, 9, Tile.PIPE_SHAFT_TOP);
						renderTile(column, 10, Tile.PIPE_SHAFT_BOTTOM);
						
					} else if (index == 4) {
						renderTile(column, 9, Tile.PIPE_LIP_TOP);
						renderTile(column, 10, Tile.PIPE_LIP_BOTTOM);
					}
					
				} else if (kind == 17) { // flagpole
					renderTile(column, 0, Tile.FLAGPOLE_BALL);
					for (int i = 1; i <= 9; i++) renderUnderPart(column, i, Tile.FLAGPOLE);
					renderTile(column, 10, Tile.SQUARE_BLOCK);
					addDisplayComboSprite(Sprite.FLAGPOLE_FLAG, x, 1);
				}
				
			} else if (id == 1) { // special platform
				if (specialPlatform == SpecialPlatform.GREEN_CANNON) { // bullet bill cannon
					renderTile(column, y, Tile.BULLET_CANNON);
					if (length > 1) renderTile(column, y + 1, Tile.BULLET_SKULL);
					for (int i = 2; i < length && y + i <= 12; i++)
						renderUnderPart(column, y + i, Tile.BULLET_SHAFT);
					
				} else if (specialPlatform == SpecialPlatform.ORANGE_MUSHROOM) {
					if (SMBLevelDrawer.game == Game.LOST_LEVELS) { // long cloud
						if (index == 1) {
							renderTile(column, y, Tile.LONG_CLOUD_RIGHT);
						} else if (x == object.x) {
							renderTile(column, y, Tile.LONG_CLOUD_LEFT);
						} else {
							renderTile(column, y, Tile.LONG_CLOUD_MIDDLE);
						}
						
					} else { // orange mushroom
						if (index == 1) {
							renderTile(column, y, Tile.MUSHROOM_RIGHT);
						} else if (x == object.x) {
							renderTile(column, y, Tile.MUSHROOM_LEFT);
						} else {
							renderTile(column, y, Tile.MUSHROOM_MIDDLE);
						}
						
						if (index == (object.width + 1) / 2) {
							renderTile(column, y + 1, Tile.MUSHROOM_STEM_TOP);
							for (int i = y + 2; i <= 12; i++)
								renderUnderPart(column, i, Tile.MUSHROOM_STEM_BOTTOM);
						} 
					}
				} else { // green tree
					if (index == 1) {
						renderTile(column, y, Tile.TREE_RIGHT);
					} else if (x == object.x) {
						renderTile(column, y, Tile.TREE_LEFT);
					} else {
						renderTile(column, y, Tile.TREE_MIDDLE);
						for (int i = y + 1; i <= 12; i++)
							renderUnderPart(column, i, Tile.TREE_TRUNK);
					}
				}
			} else if (id == 2) { // brick row
				renderTile(column, y, specialPlatform == SpecialPlatform.GREEN_CLOUD ? Tile.CLOUD : 
					type == LevelType.UNDERWATER ? Tile.CORAL :
					type == LevelType.OVERWORLD ? Tile.BRICK_SHINY : Tile.BRICK_DULL);
				
			} else if (id == 3) { // square block row
				renderTile(column, y, type == LevelType.CASTLE ? Tile.CASTLE_MASONRY :
					type == LevelType.UNDERWATER ? Tile.SEAFLOOR : Tile.SQUARE_BLOCK);
				
			} else if (id == 4) { // coin row
				renderTile(column, y, type == LevelType.UNDERWATER ? Tile.COIN_WATER : Tile.COIN);
				
			} else if (id == 5) { // brick column
				Tile tile =  specialPlatform == SpecialPlatform.GREEN_CLOUD ? Tile.CLOUD : 
					type == LevelType.UNDERWATER ? Tile.CORAL :
					type == LevelType.OVERWORLD ? Tile.BRICK_SHINY : Tile.BRICK_DULL;
				for (int i = 0; i < length && y + i <= 12; i++) renderTile(column, y + i, tile);
				
			} else if (id == 6) { // square block column
				Tile tile = type == LevelType.CASTLE ? Tile.CASTLE_MASONRY :
					type == LevelType.UNDERWATER ? Tile.SEAFLOOR : Tile.SQUARE_BLOCK;
				for (int i = 0; i < length && y + i <= 12; i++) renderTile(column, y + i, tile);
				
			} else if (id == 7) { // pipe
				boolean canEnter = memory.getBits(object.b, 0x08) == 1;
				length = ((length - 1) & 0x07) + 1;
				if (length == 1) length = 2;
				
				if (index == 1) {
					renderTile(column, y, canEnter ? Tile.PIPE_LIP_ENTERABLE_RIGHT : Tile.PIPE_LIP_RIGHT);
					for (int i = 1; i < length && y + i <= 12; i++) 
						renderUnderPart(column, y + i, Tile.PIPE_SHAFT_RIGHT);
					
				} else if (index == 2) {
					renderTile(column, y, canEnter ? Tile.PIPE_LIP_ENTERABLE_LEFT : Tile.PIPE_LIP_LEFT);
					for (int i = 1; i < length && y + i <= 12; i++) 
						renderUnderPart(column, y + i, Tile.PIPE_SHAFT_LEFT);		
					
					// piranha plants in pipes only after 1-1 (except LL)
					if (SMBLevelDrawer.MY_WORLD > 1 || SMBLevelDrawer.MY_LEVEL > 1 ||
						SMBLevelDrawer.game == Game.LOST_LEVELS)
							addDisplayComboSprite(SMBLevelDrawer.game == Game.LOST_LEVELS && SMBLevelDrawer.MY_WORLD >= 4 ?
								Sprite.RED_PIRANHA_PLANT : Sprite.GREEN_PIRANHA_PLANT, x, y - 1);	
				}
			}
			
		} else if (y == 12) {
			int id = memory.getBits(object.b, 0x70);
			
			if (id == 0) { // hole
				for (int i = 8; i <= 12; i++)
					renderUnderPart(column, i, type == LevelType.UNDERWATER ? Tile.WATER_BOTTOM : null);
				
			} else if (id == 1) { // horizontal pulley rope
				if (index == 1) renderTile(column, 0, Tile.PULLEY_RIGHT);
				else if (index == object.width) renderTile(column, 0, Tile.PULLEY_LEFT);
				else renderTile(column, 0, Tile.PULLEY_ACROSS);
				
			} else if (id == 2) { // bridge at Y=7
				renderTile(column, 6, Tile.BRIDGE_ROPES);
				renderUnderPart(column, 7, Tile.BRIDGE_FLOOR);
				
			} else if (id == 3) { // bridge at Y=8
				renderTile(column, 7, Tile.BRIDGE_ROPES);
				renderUnderPart(column, 8, Tile.BRIDGE_FLOOR);
				
			} else if (id == 4) { // bridge at Y=10
				renderTile(column, 9, Tile.BRIDGE_ROPES);
				renderUnderPart(column, 10, Tile.BRIDGE_FLOOR);
				
			} else if (id == 5) { // hole with water
				renderUnderPart(column, 10, Tile.WATER_TOP);
				renderUnderPart(column, 11, Tile.WATER_BOTTOM);
				renderUnderPart(column, 12, Tile.WATER_BOTTOM);
				
			} else if (id == 6) { // ? blocks at Y=3
				renderTile(column, 3, Tile.QUESTION_BLOCK_COIN);
				
			} else if (id == 7) { // ? blocks at Y=7
				renderTile(column, 7, Tile.QUESTION_BLOCK_COIN);
			}
			
		} else if (y == 13) {
			int id = memory.getBits(object.b, 0x3F);
			
			if (id == 0) { // L pipe
				if (index == 1) {
					renderTile(column, 7, Tile.PIPE_LIP_ENTERABLE_RIGHT);
					renderUnderPart(column, 8, Tile.PIPE_SHAFT_RIGHT);
					for (int i = 9; i <= 10; i++) renderTile(column, i, Tile.PIPE_SHAFT_RIGHT);
					
				} else if (index == 2) {
					renderTile(column, 7, Tile.PIPE_LIP_ENTERABLE_LEFT);
					renderUnderPart(column, 8, Tile.PIPE_SHAFT_LEFT);
					renderTile(column, 9, Tile.PIPE_CONNECTION_TOP);
					renderTile(column, 10, Tile.PIPE_CONNECTION_BOTTOM);
					
				} else if (index == 3) {
					renderTile(column, 9, Tile.PIPE_SHAFT_TOP);
					renderTile(column, 10, Tile.PIPE_SHAFT_BOTTOM);
					
				} else if (index == 4) {
					renderTile(column, 9, Tile.PIPE_LIP_TOP);
					renderTile(column, 10, Tile.PIPE_LIP_BOTTOM);
				}
			} else if (id == 1) { // flagpole & flag
				renderTile(column, 0, Tile.FLAGPOLE_BALL);
				for (int i = 1; i <= 9; i++) renderUnderPart(column, i, Tile.FLAGPOLE);
				renderTile(column, 10, Tile.SQUARE_BLOCK);
				addDisplayComboSprite(Sprite.FLAGPOLE_FLAG, x, 1);
				
			} else if (id == 2) { // axe
				renderTile(column, 6, Tile.AXE);
				
			} else if (id == 3) { // chain
				renderTile(column, 7, Tile.CHAIN);
				
			} else if (id == 4) { // bowser bridge
				renderTile(column, 8, Tile.BOWSER_BRIDGE);
				
			} else if (id == 8) { // jumping cheep cheep generator
				addDisplayComboSprite(Sprite.RED_CHEEP_CHEEP, x, 1);
				
			} else if (id == 9) { // swimming cheep / bullet generator
				addDisplayComboSprite(type == LevelType.UNDERWATER ?
						Sprite.RED_CHEEP_CHEEP : Sprite.BULLET_BILL, x, 1);
				
			} else if (id == 12 && laterObjectsLoaded) { // wind, but only if loaded
				addDisplayComboSprite(Sprite.WIND, x, 1);
				
			} else {
				// CRASH likely
			}
			
		} else if (y == 14) {
			if (memory.getBits(object.b, 0x40) == 0) { // scenery and floor pattern
				terrain[0] = memory.getBits(object.b, 0x30);
				terrain[2] = memory.getBits(object.b, 0x0F);
				
			} else { // backdrop
				terrain[1] = memory.getBits(object.b, 0x07);
				
				// change level palette only if this object is in the first screen and a half
				if (x < 24) backdropPalette = changeLevelBackdropPalette(terrain[1]);
			}
			
		} else if (y == 15) {
			int id = memory.getBits(object.b, 0x70);
			int length = memory.getBits(object.b, 0x0F) + 1;
			
			if (id == 0) { // full lift rope
				for (int i = 0; i <= 12; i++) renderUnderPart(column, i, Tile.PULLEY_DOWN);
				
			} else if (id == 1) { // partial pulley lift rope
				for (int i = 1; i <= length; i++) renderUnderPart(column, i, Tile.PULLEY_DOWN);
				for (int i = length+1; i <= 12; i++) renderUnderPart(column, i, null);
				
			} else if (id == 2) { // castle & castle flag
				if (length <= 11) {
					for (int i = 0; i < 12-length; i++)
						renderTile(column, length - 1 + i, getTileFromChar(compressedCastleObject[index-1].charAt(i)));
					
					// the solid brick that Mario gets stuck on
					if (index == 2) renderTile(column, 10, Tile.BRICK_DULL);
					
					// the flag on the castle
					if (index == 3) addDisplayComboSprite(Sprite.CASTLE_FLAG, x, length - 2);
				} else {
					// CRASH likely
				}
				
			} else if (id == 3) { // staircase
				// if this is the start of a new staircase, reset the staircase counter
				if (x == object.x) staircase = 9;
				
				// move to the next stair
				staircase = 0xFF & (staircase - 1);
				
				// get the number of air blocks and solid blocks in this step
				int blocks = 1 + memory.read8(staircasePointers[SMBLevelDrawer.game.getNum()][0] + staircase);
				int gap = memory.read8(staircasePointers[SMBLevelDrawer.game.getNum()][1] + staircase);
				if (blocks >= 0x80) blocks = 1;
				
				for (int i = 0; i < blocks && gap + i <= 12; i++)
					renderUnderPart(column, gap + i, Tile.SQUARE_BLOCK);
				
			} else if (id == 4) { // ending L pipe
				if (index == 1) {
					if (length < 3) length = 3;
					for (int i = 0; i < length - 2; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_RIGHT);
					for (int i = length - 2; i < length; i++) renderTile(column, i, Tile.PIPE_SHAFT_RIGHT);
					
				} else if (index == 2) {
					if (length < 3) length = 3;
					for (int i = 0; i < length - 2; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_LEFT);
					renderTile(column, length - 2, Tile.PIPE_CONNECTION_TOP);
					renderTile(column, length - 1, Tile.PIPE_CONNECTION_BOTTOM);
					
				} else if (index == 3 && length > 1) {
					renderTile(column, length - 2, Tile.PIPE_SHAFT_TOP);
					renderTile(column, length - 1, Tile.PIPE_SHAFT_BOTTOM);
					
				} else if (index == 4 && length > 1) {
					renderTile(column, length - 2, Tile.PIPE_LIP_TOP);
					renderTile(column, length - 1, Tile.PIPE_LIP_BOTTOM);
				}
				
			} else if (id == 5) { // vertical balls
				for (int i = 0; i < length; i++) {
					if (2 + i <= 12) renderUnderPart(column, 2 + i, Tile.UNUSED_FLAG_BALL);
				}
				
			} else if (id == 6 && laterObjectsLoaded) { // upside-down pipe with Y=1
				if (index == 1) {
					for (int i = 1; i < length && i <= 12; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_RIGHT);
					if (length <= 12) renderTile(column, length, Tile.PIPE_LIP_RIGHT);
					
				} else if (index == 2) {
					for (int i = 1; i < length && i <= 12; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_LEFT);
					if (length <= 12) renderTile(column, length, Tile.PIPE_LIP_LEFT);
					
					addDisplayComboSprite(Sprite.UPSIDE_DOWN_RED_PIRANHA_PLANT, x, length + 1);
				}
				
			} else if (id == 7 && laterObjectsLoaded) { // upside-down pipe with Y=4
				if (index == 1) {
					for (int i = 4; i < length + 3 && i <= 12; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_RIGHT);
					if (length + 3 <= 12) renderTile(column, length + 3, Tile.PIPE_LIP_RIGHT);
					
				} else if (index == 2) {
					for (int i = 4; i < length + 3 && i <= 12; i++) renderUnderPart(column, i, Tile.PIPE_SHAFT_LEFT);
					if (length + 3 <= 12) renderTile(column, length + 3, Tile.PIPE_LIP_LEFT);
					
					addDisplayComboSprite(Sprite.UPSIDE_DOWN_RED_PIRANHA_PLANT, x, length + 4);
				}
			}
		}
	}
	
	// remappings of powerup block indices to LL powerup block indices
	private int[][] remapPowerupBlocks = new int[][] {
		{0, 2, 3, 4, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 18}, // SMB
		{0, 2, 3, 4, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 18}, // SMBFDS
		{0, 2, 3, 4, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 18}, // VS
		{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}, // LL
		{0, 2, 3, 4, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18}  // ANN
	};
	
	// routine copied from the games that determines which tiles have priority over others
	private void renderUnderPart(Tile[] column, int i, Tile tile) {
		// if the spot is empty, write the tile
		if (column[i] == null) {
			column[i] = tile;
			
		// don't overwrite the centers of trees and mushrooms and long clouds
		} else if (column[i] == Tile.TREE_MIDDLE || column[i] == Tile.MUSHROOM_MIDDLE ||
				column[i] == Tile.LONG_CLOUD_MIDDLE) {
			
		// overwrite ? blocks with coins
		} else if (column[i] == Tile.QUESTION_BLOCK_COIN) {
			column[i] = tile;
			
		// don't overwrite any other tiles that are shiny
		} else if (column[i] == Tile.QUESTION_BLOCK_FIREFLOWER ||
				column[i] == Tile.QUESTION_BLOCK_POISON_MUSHROOM ||
				column[i] == Tile.COIN || column[i] == Tile.COIN_WATER ||
				column[i] == Tile.AXE || column[i] == Tile.USED_BLOCK) {
			
		// overwrite everything else but dirt
		} else if (column[i] != Tile.DIRT) {
			column[i] = tile;
			
		// unless you are mushroom stem, those can never overwrite anything
		} else if (tile != Tile.MUSHROOM_STEM_BOTTOM) {
			column[i] = tile;
		}
	}
	
	// center tiles of trees/mushrooms/long clouds are never overwritten
	private void renderTile(Tile[] column, int i, Tile tile) {
		if (column[i] != Tile.TREE_MIDDLE && column[i] != Tile.MUSHROOM_MIDDLE &&
				column[i] != Tile.LONG_CLOUD_MIDDLE)
			column[i] = tile;
	}
	
	private int tileObjectWidth(TileObject object) {
		switch (object.y) {
			case 12: { // width encoded in object
				return memory.getBits(object.b, 0x0F) + 1;
			}
			case 13: { // various
				switch (memory.getBits(object.b, 0x3F)) {
					case 0: return 4; // L pipe
					case 4: return 13; // bowser bridge
					default: return 1;
				}
			}
			case 14: { // backdrop/scenery
				return 1;
			}
			case 15: { // various
				switch (memory.getBits(object.b, 0x70)) {
					case 2: return 5; // castle
					case 3: return memory.getBits(object.b, 0x0F) + 1; // staircase
					case 4: return 4; // underground L pipe
					case 6: case 7: // upside down pipes
						if (SMBLevelDrawer.game == Game.LOST_LEVELS ||
								SMBLevelDrawer.game == Game.ALL_NIGHT_NIPPON)
							return 2;
					default: return 1;
				}
			}
			default: {
				switch (memory.getBits(object.b, 0x70)) {
					case 1: { // special platform
						if (specialPlatform == SpecialPlatform.GREEN_CANNON) return 1;
					}
					case 2: case 3: case 4: { // row of tiles
						return memory.getBits(object.b, 0x0F) + 1;
					}
					case 7: return 2; // pipe
					case 0: { // powerup blocks et al
						
						// check for L pipe
						if (remapPowerupBlocks[SMBLevelDrawer.game.getNum()]
								[memory.getBits(object.b, 0x0F)] == 16)
							return 4;
					}
					default: return 1;
				}
			}
		}
	}

	private TileObject addObjectToList(int screen, int offset) {
		int a = memory.read8(dataBasePointer + offset);
		int b = memory.read8(dataBasePointer + offset + 1);
		int x = memory.getBits(a, 0xF0);
		int y = memory.getBits(a, 0x0F);
		int fullXPosition = 0x10 * screen + x;
		
		TileObject object = new TileObject(a, b, fullXPosition, y);
		tileObjectList.add(object);
		return object;
	}
	
	private void addDisplayComboSprite(Sprite sprite, int x, int y) {
		comboSprites.add(SMBLevelDrawer.ssm.new SpriteInstance(sprite, 16 * x, 16 * (y + 1)));
	}
	
	private void addDisplayTile(Tile tile, int x, int y) {
		if (tile != null) {
			displayedTiles.add(SMBLevelDrawer.ssm.new TileInstance(tile, x, y));
		}
	}
	
	// get data from the level header
	// return initial scenery, backdrop, and floor pattern
	private int[] processLevelHeader() {
		int a = memory.read8(levelHeaderPointer);
		int b = memory.read8(0xFFFF & (levelHeaderPointer + 1));
		
		int time = memory.getBits(a, 0xC0);
		int marioStartingYPos = memory.getBits(a, 0x1C);
		int initialBackdrop = memory.getBits(a, 0x07);
		int specPlatform = memory.getBits(b, 0xC0);
		int initialScenery = memory.getBits(b, 0x30);
		int initialFloorPattern = memory.getBits(b, 0x0F);
		
		// set the modified palette of the level based on the initial backdrop
		backdropPalette = changeLevelBackdropPalette(initialBackdrop);
		
		// set the special platform for the level
		switch (specPlatform) {
			case 0: specialPlatform = SpecialPlatform.GREEN_TREE; break;
			case 1: specialPlatform = SpecialPlatform.ORANGE_MUSHROOM; break;
			case 2: specialPlatform = SpecialPlatform.GREEN_CANNON; break;
			default: specialPlatform = SpecialPlatform.GREEN_CLOUD; break;
		}
		
		return new int[] {initialScenery, initialBackdrop, initialFloorPattern};
	}
	
	// get the level modified palette from the backdrop
	private BackdropModifier changeLevelBackdropPalette(int backdrop) {
		switch (backdrop) {
			case 4: return BackdropModifier.NIGHT;
			case 5: return BackdropModifier.SNOW;
			case 6: return BackdropModifier.NIGHT_SNOW;
			case 7: return BackdropModifier.GRAY;
			default: return BackdropModifier.NONE;
		}
	}
	
	// draw the current scenery to this column
	private void processScenery(int x, int scenery, Tile[] column) {
		int offset = x % compressedScenery[scenery][0].length();
		
		for (int i = 0; i < compressedScenery[scenery].length; i++) {
			Tile tile = getTileFromChar(compressedScenery[scenery][i].charAt(offset));
			
			if (tile != null) column[i] = tile;
		}
	}
	
	// draw the current backdrop to this column
	private void processBackdrop(int x, int backdrop, Tile[] column) {
		if (backdrop < 4) {
			for (int i = 0; i < compressedBackdrops[backdrop].length(); i++) {
				Tile tile = getTileFromChar(compressedBackdrops[backdrop].charAt(i));
				
				if (tile != null) column[i] = tile;
			}
		}
	}
	
	// draw the current floor pattern to this column
	private void processFloor(int x, int floorPattern, Tile[] column) {
		for (int i = 0; i < compressedFloorPatterns[floorPattern].length(); i++) {
			Tile tile = getTileFromChar(compressedFloorPatterns[floorPattern].charAt(i));
			
			if (tile != null) {
				if (specialPlatform == SpecialPlatform.GREEN_CLOUD) {
					tile = Tile.CLOUD;
					
					// cloud levels always have a gap at the bottom
					if (i == 8 || i == 9 || i == 10 || i == 12) continue;
				} else {
					// default tile to dirt
					
					// water levels use seafloor
					if (type == LevelType.UNDERWATER)
						tile = Tile.SEAFLOOR;
					
					// castle levels use masonry
					// water levels in world 8 use masonry TODO only in SMB, check this for LL
					if (type == LevelType.CASTLE ||
							(type == LevelType.UNDERWATER && SMBLevelDrawer.MY_WORLD == 8))
						tile = Tile.CASTLE_MASONRY;
					
					// underground levels use bricks (except floor)
					if (type == LevelType.UNDERGROUND && i < 11)
						tile = Tile.BRICK_DULL;
				}
				
				column[i] = tile;
			}
		}
	}
	
	// staircase sizes come from a table which can be indexed out of bounds to
	// produce weird stuff so here's locations of those tables
	private int[][] staircasePointers = new int[][] {
		{0x9AA5, 0x9AAE}, // SMB
		{0x7AAF, 0x7AB8}, // SMBFDS
		{0xA9D1, 0xA9DA}, // VS
		{0x78EC, 0x78F5}, // LL
		{0x7954, 0x795D}, // ANN
	};
	
	private Tile getTileFromChar(char c) {
		switch (c) {
			case 'c': return Tile.BG_CLOUD_TOP_LEFT;
			case 'l': return Tile.BG_CLOUD_TOP_MIDDLE;
			case 'o': return Tile.BG_CLOUD_TOP_RIGHT;
			case 'u': return Tile.BG_CLOUD_BOTTOM_LEFT;
			case 'd': return Tile.BG_CLOUD_BOTTOM_MIDDLE;
			case 's': return Tile.BG_CLOUD_BOTTOM_RIGHT;
			case '[': return Tile.BG_HILL_LEFT;
			case ']': return Tile.BG_HILL_RIGHT;
			case '<': return Tile.BG_HILL_SPOT_RIGHT;
			case '>': return Tile.BG_HILL_SPOT_LEFT;
			case '#': return Tile.BG_HILL_FILL;
			case '_': return Tile.BG_HILL_TOP;
			case '.': return Tile.BG_BUSH_LEFT;
			case '-': return Tile.BG_BUSH_MIDDLE;
			case ',': return Tile.BG_BUSH_RIGHT;
			case '|': return Tile.BG_TREE_TRUNK;
			case 'e': return Tile.BG_TREE_SMALL;
			case '^': return Tile.BG_TREE_TOP;
			case 'v': return Tile.BG_TREE_BOTTOM;
			case 't': return Tile.BG_FENCE;
			case '~': return Tile.WATER_TOP;
			case '=': return Tile.WATER_BOTTOM;
			case 'x': return Tile.SEAFLOOR;
			case 'i': return Tile.CASTLE_CRENEL;
			case '+': return Tile.CASTLE_BRICK;
			case 'I': return Tile.CASTLE_CRENEL_FILLED;
			case '{': return Tile.CASTLE_DOOR_TOP;
			case '}': return Tile.CASTLE_DOOR_BOTTOM;
			case '(': return Tile.CASTLE_WINDOW_RIGHT;
			case ')': return Tile.CASTLE_WINDOW_LEFT;
			case '*': return Tile.BRICK_DULL;
			case '@': return Tile.DIRT;
			default: return null;
		}
	}
	
	private String[][] compressedScenery = new String[][] {
		{ // nothing
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                "
		}, { // clouds
			"                  cllo                          ",
			"   cllo           udds                          ",
			"   udds                                         ",
			"                                                ",
			"                                      clo       ",
			"         clo                       clouds       ",
			"         uds                       uds          ",
			"                                                ",
			"                                                ",
			"o                           clo               cl",
			"s                           uds               ud"
		}, { // mountains
			"                   clo              cllo        ",
			"        clo        uds     clllo    udds        ",
			"        uds                uddds                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"  _                                             ",
			" [<]             _                              ",
			"[<#>]      .---,[<]    .-,               .--,   "
		}, { // fences
			"                           clo               clo",
			"cllo              clo      udscllo           uds",
			"udds              uds         udds              ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"                                                ",
			"             ^       ^                     ^    ",
			"           e v       v ee               e  v    ",
			"           | |tttt   | ||             tt|t |    "
		}
	};
	
	private String[] compressedBackdrops = new String[] {
			"             ", // none
			"~==========xx", // underwater
			"     i+++++  ", // castle wall
			"           ~="  // over water
	};
	
	private String[] compressedFloorPatterns = new String[] {
			"             ",
			"           @@",
			"@          @@",
			"@@@        @@",
			"@@@@       @@",
			"@@@@@@@@   @@",
			"@       @@@@@",
			"@@@     @@@@@",
			"@@@@    @@@@@",
			"@      @@@@@@",
			"@            ",
			"@@@@   @@@@@@",
			"@   @@@@@@@@@",
			"@  @@@@@   @@",
			"@   @@@@   @@",
			"@@@@@@@@@@@@@"
	};
	
	private String[] compressedCastleObject = new String[] {
		"  i++I+++{}",
		"i)I++I{}++*",
		"i+I{}I+++{}",
		"i(I++I{}++*",
		"  i++I+++{}"
	};
	
	public BufferedImage print(int width) {
		BufferedImage img = new BufferedImage(16*width,16*17,BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D)img.getGraphics();
		
		g.setBackground(SMBLevelDrawer.ssm.getBackgroundColor());
		g.clearRect(0, 0, 16*width, 16*14);
		
		for (int i = 0; i < displayedTiles.size(); i++) {
			SMBLevelDrawer.ssm.drawTile(g, displayedTiles.get(i));
		}
		
		if (VERBOSE_TILES) {
			g.setColor(Color.RED);
			for (int i = 0; i < tileObjectList.size(); i++) {
				TileObject t = tileObjectList.get(i);
				g.drawRect(0x10*t.x+4, 0x10*(t.y+1)+4, 8, 8);
				g.drawString(t.data, 0x10*t.x+12, 0x10*(t.y+1)+4);
			}
		}
		
		return img;
	}
}