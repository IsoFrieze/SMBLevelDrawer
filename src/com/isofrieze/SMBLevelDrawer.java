package com.isofrieze;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import com.isofrieze.SpriteSheetManager.Tile;
import com.isofrieze.SpriteSheetManager.TileInstance;

public class SMBLevelDrawer {
	
	// the requested level to draw e.g. "1-1"
	public static String REQUESTED_LEVEL = null;
	
	// the actual level drawn (extra being worlds A-D)
	public static int MY_WORLD = -1, MY_LEVEL = -1, MY_SUBLEVEL = -1;
	public static boolean MY_EXTRA = false;
	
	// the requested level ID to draw e.g. 0x25
	public static int REQUESTED_ID = -1;
	
	// the requested disc file to use if applicable (1 thru 4)
	public static int REQUESTED_FILE = -1;
	
	// the requested game to use if a custom input is given
	public static Game REQUESTED_GAME = Game.NONE;
	
	// the requested pointers to level data and level type to use e.g. 0xA68E, 0x9F01, 1
	// level type is required here because it is normally part of the level ID
	public static int REQUESTED_TILE_ADDRESS = -1, REQUESTED_SPRITE_ADDRESS = -1, REQUESTED_LEVEL_TYPE = -1;
	
	// the requested inline level data to be processed
	public static String REQUESTED_TILE_DATA = null, REQUESTED_SPRITE_DATA = null;
	
	// whether to draw level tiles and sprites
	public static boolean TILES = true, SPRITES = true;
	
	// size scaler of the output images
	public static int ZOOM = 1;
	
	// width of the level to draw in screens
	public static int WIDTH = -1;
	
	// which game we are dealing with
	public static Game game = Game.NONE;
	
	// the simulated address space of the system
	public static MemoryManager memory;
	
	// the sprite manager object
	public static SpriteSheetManager ssm = new SpriteSheetManager();
	
	public static enum Game {
		NONE(0, "smb", 1, 1), SUPER_MARIO_BROS(0, "smb", 5, 3), SUPER_MARIO_BROS_FDS(1, "smb", 5, 3),
		VS_SUPER_MARIO_BROS(2, "smb", 4, 2), LOST_LEVELS(3, "smbll", 4, 4), ALL_NIGHT_NIPPON(4, "annsmb", 5, 2);
		
		// folder for resources
		private String abv;
		
		// index into various arrays
		private int num;
		
		// what world-level hard mode starts in this game
		private int[] hardModeStarts;
		
		Game(int num, String abv, int world, int level) {
			this.abv = abv;
			this.num = num;
			this.hardModeStarts = new int[] {world, level};
		}
		
		public String getAbbreviation() {
			return this.abv;
		}
		
		public int getNum() {
			return this.num;
		}
		
		public int[] getHardModeStart() {
			return this.hardModeStarts;
		}
	}
	
	public enum LevelType {
		UNDERWATER(0), OVERWORLD(1), UNDERGROUND(2), CASTLE(3);
		
		private int num;
		
		LevelType(int num) {
			this.num = num;
		}
		
		public int getNum() {
			return this.num;
		}
		
		public static LevelType getType(int n) {
			switch (n & 3) {
				case 0: return UNDERWATER;
				case 1: return OVERWORLD;
				case 2: return UNDERGROUND;
				default: return CASTLE;
			}
		}
	}
	
	public static void main(String[] args) {
		// args = fakeArguments();
		
		String[] filenames = processArguments(args);
		
		if (REQUESTED_TILE_DATA == null && filenames[0] == null) {
			System.err.println("You have to supply an input file!");
			return;
		}

		byte[] suppliedData = null, fdsBios = null;
		
		// if we are getting data from files, load those in
		if (filenames[0] != null) {
			try (FileInputStream in = new FileInputStream(filenames[0])) {
				suppliedData = new byte[(int)in.getChannel().size()];
				in.read(suppliedData, 0, suppliedData.length);
			} catch (IOException e) {
				System.err.println("Error reading the input file!");
				e.printStackTrace();
			}
			
			if (filenames[1] != null) {
				try (FileInputStream in = new FileInputStream(filenames[1])) {
					fdsBios = new byte[(int)in.getChannel().size()];
					in.read(fdsBios, 0, fdsBios.length);
				} catch (IOException e) {
					System.err.println("Error reading the FDS BIOS file!");
					e.printStackTrace();
				}
			}
			
			game = detectGame(suppliedData);
		}
		
		// if the user supplies a game, but doesn't supply a level number or anything similar
		// we are going to loop over all levels in the game
		boolean loopOverAllLevels = game != Game.NONE && REQUESTED_LEVEL == null && REQUESTED_ID < 0 &&
				(REQUESTED_TILE_ADDRESS < 0 || REQUESTED_SPRITE_ADDRESS < 0);
		
		// if we didn't detect a game, but the user supplied one, use that
		if (game == Game.NONE) game = REQUESTED_GAME;
		
		// load in resources
		ssm.initialize();
		
		if (loopOverAllLevels) {
			// LL and ANN go up to world D
			int numberOfWorlds = game == Game.LOST_LEVELS || game == Game.ALL_NIGHT_NIPPON ? 13 : 8;
			
			for (int w = 1; w <= numberOfWorlds; w++) {
				// ANN has no world 9
				if (w == 9 & game == Game.ALL_NIGHT_NIPPON) w++;
				
				for (int l = 1; l <= 4; l++) {
					int numberOfSublevels = 1 + listOfSublevels[game.getNum()][w-1][l-1].length;
					
					for (int s = 1; s <= numberOfSublevels; s++) {
						// fake the input string level number and then run the program as if that was input
						REQUESTED_LEVEL = String.format("%x-%d.%d", w, l, s).toUpperCase();
						
						// append the level number to the output file name
						String outputName = filenames[2];
						if (outputName.indexOf(".") >= 0) outputName = outputName.substring(0, outputName.indexOf("."));
						outputName = String.format("%s_%s.png", outputName, REQUESTED_LEVEL);
						
						processALevel(suppliedData, fdsBios, outputName);
						
						REQUESTED_ID = -1;
						REQUESTED_TILE_DATA = null;
					}
				}
			}
			
		} else {
			// just process the one level requested
			processALevel(suppliedData, fdsBios, filenames[2]);	
		}
	}
	
	private static void processALevel(byte[] data, byte[] bios, String output) {
		int[] levelDataPointers = new int[2];
		
		if (REQUESTED_TILE_DATA == null) {
			// if we are loading data from a game
			
			// first get the "preferred" disc file for whatever level is going to be generated
			int preferredDiscFile = determinePreferredDiscFile();
			
			// load memory with that disc file
			memory = prepareMemorySpace(data, bios);
			loadDiscFile(preferredDiscFile, data);
			
			// get the level data pointers
			levelDataPointers = getLevelDataPointers(preferredDiscFile);
			
			// then load the requested disc file
			if (REQUESTED_FILE > 0) loadDiscFile(REQUESTED_FILE, data);
			
			
		} else {
			// if we are reading inline data from the command line
			
			memory = prepareMemorySpace(levelDataPointers, REQUESTED_TILE_DATA, REQUESTED_SPRITE_DATA);
		}
		
		// the type of level (water, castle, etc.)
		LevelType type = LevelType.getType(REQUESTED_LEVEL_TYPE >= 0 ? REQUESTED_LEVEL_TYPE : (REQUESTED_ID & 0x60) >> 5);
		
		// System.out.printf("detectedGame = %s%n", game.name());
		// System.out.printf("REQUESTED_LEVEL = %s (%d | %d | %d)%n", REQUESTED_LEVEL, MY_WORLD, MY_LEVEL, MY_SUBLEVEL);
		// System.out.printf("REQUESTED_ID = 0x%x, REQUESTED_FILE = %d%n", REQUESTED_ID, REQUESTED_FILE);
		// System.out.printf("TILE_ADDRESS = 0x%x, SPRITE_ADDRESS = 0x%x, LEVEL_TYPE = %s%n", levelDataPointers[0], levelDataPointers[1], type.name());
		
		// the sprites need to know if the tile data header says the level should be cloudy
		boolean isCloudy = memory.readBits(levelDataPointers[0] + 1, 0xC0) == 3;
		
		// build the level's sprite data
		LevelSpriteBuilder spriteBuilder = new LevelSpriteBuilder(levelDataPointers[1], type, isCloudy);
		int spriteExtent = 9 + spriteBuilder.build();
		
		// build the level's tile data
		LevelTileBuilder tileBuilder = new LevelTileBuilder(levelDataPointers[0], type);
		int levelWidth = tileBuilder.build(spriteExtent);
		
		// get some sprite info from the tile data
		spriteBuilder.addSpontaneousSprites(tileBuilder.getComboSprites());
		
		// now that the level has been processed, we can prepare things to be drawn with the correct palettes
		ssm.setPalettes();
		
		// print the level data
		if (WIDTH >= 0) levelWidth = 0x100 * WIDTH;
		BufferedImage tileImage = tileBuilder.print(levelWidth);
		BufferedImage spriteImage = spriteBuilder.print(levelWidth);
		
		// create the final image
		int imageHeight = ((LevelTileBuilder.VERBOSE_TILES || LevelSpriteBuilder.VERBOSE_SPRITES) ? 17 : 14);
		BufferedImage finalImage = new BufferedImage(16 * levelWidth, 16 * imageHeight, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D)finalImage.getGraphics();
		if (TILES) g.drawImage(tileImage, 0, 0, null);
		if (SPRITES) g.drawImage(spriteImage, 0, 0, null);
		
		// write it to a file
		try {
			ImageIO.write(finalImage, "png", new File(output));
		} catch (IOException e) {
			System.err.println("Error writing the output file!");
			e.printStackTrace();
		}
	}
	
	private static String[] fakeArguments() {
		Scanner in = new Scanner(System.in);
		return (in.nextLine()).split(" ");
	}
	
	private static String[] processArguments(String[] args) {
		String filename = null, bios = null, output = "output.png";
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-i": case "-input":
					i++;
					filename = args[i];
					break;
				case "-b": case "-bios":
					i++;
					bios = args[i];
					break;
				case "-n": case "-inline":
					i++;
					REQUESTED_TILE_DATA = args[i];
					i++;
					REQUESTED_SPRITE_DATA = args[i];
					i++;
					REQUESTED_LEVEL_TYPE = Integer.parseInt(args[i]);
					break;
				case "-o": case "-output":
					i++;
					output = args[i];
					break;
				case "-l": case "-level":
					i++;
					REQUESTED_LEVEL = args[i];
					break;
				case "-d": case "-id":
					i++;
					REQUESTED_ID = Integer.parseInt(args[i]);
					break;
				case "-a": case "-address":
					i++;
					REQUESTED_TILE_ADDRESS = Integer.parseInt(args[i]);
					i++;
					REQUESTED_SPRITE_ADDRESS = Integer.parseInt(args[i]);
					i++;
					REQUESTED_LEVEL_TYPE = Integer.parseInt(args[i]);
					break;
				case "-f": case "-file":
					i++;
					REQUESTED_FILE = Integer.parseInt(args[i]);
					break;
				case "-g": case "-game":
					i++;
					switch (args[i].toUpperCase()) {
						case "SMB": REQUESTED_GAME = Game.SUPER_MARIO_BROS; break;
						case "SMBFDS": REQUESTED_GAME = Game.SUPER_MARIO_BROS_FDS; break;
						case "SMBLL": REQUESTED_GAME = Game.LOST_LEVELS; break;
						case "ANNSMB": REQUESTED_GAME = Game.ALL_NIGHT_NIPPON; break;
						case "VSSMB": REQUESTED_GAME = Game.VS_SUPER_MARIO_BROS; break;
					}
					break;
				case "-second-quest":
					LevelSpriteBuilder.SECOND_QUEST = true;
					break;
				case "-no-tiles":
					TILES = false;
					break;
				case "-no-sprites":
					SPRITES = false;
					break;
				case "-no-block-contents":
					SpriteSheetManager.CONTENTS = false;
					break;
				case "-width":
					i++;
					WIDTH = Integer.parseInt(args[i]);
					break;
				case "-zoom":
					i++;
					ZOOM = Integer.parseInt(args[i]);
					break;
				case "-verbose-tiles":
					LevelTileBuilder.VERBOSE_TILES = true;
					break;
				case "-verbose-sprites":
					LevelSpriteBuilder.VERBOSE_SPRITES = true;
					break;
			}
		}
		
		return new String[] {filename, bios, output};
	}
	
	private static Game detectGame(byte[] data) {
		if (data == null) return Game.NONE;
		
		int checksum32 = 0;
		
		for (int i = 0 ; i < data.length; i++) {
			checksum32 += ((int)data[i]) & 0xFF;
		}
		
		// TODO support actual arcade version of VS? It's split into multiple files though
		switch (checksum32) {
			case 0x004067E5: return Game.SUPER_MARIO_BROS; // .nes
			case 0x003FF271: return Game.SUPER_MARIO_BROS_FDS; // .fds
			case 0x00502082: return Game.VS_SUPER_MARIO_BROS; // .nes conversion by Morgan Johansson
			case 0x00533AD4: return Game.LOST_LEVELS; // .fds
			case 0x0055A7D1: return Game.ALL_NIGHT_NIPPON; // .fds
		}
		
		return Game.NONE;
	}
	
	// for LL and ANN, determine what file should be loaded
	private static int determinePreferredDiscFile() {
		if (game == Game.LOST_LEVELS || game == Game.ALL_NIGHT_NIPPON) {
			// if the user specifies an address but no file, default to 1
			if (REQUESTED_TILE_ADDRESS >= 0) {
				return 1;
				
			// if the user specifies an ID but no file,
			// default to "proper" file for worlds 1-9 table
			// if an ID doesn't match any file, default to file 1
			} else if (REQUESTED_ID >= 0) {
				if (game == Game.LOST_LEVELS) {
					for (int i = 0; i < defaultFilePerLevelIdLL[0].length; i++) {
						if (defaultFilePerLevelIdLL[0][i] == REQUESTED_ID) return 2;
					}
					for (int i = 0; i < defaultFilePerLevelIdLL[1].length; i++) {
						if (defaultFilePerLevelIdLL[1][i] == REQUESTED_ID) return 3;
					}
					return 1;
					
				} else { // ANN
					for (int i = 0; i < defaultFilePerLevelIdANN.length; i++) {
						if (defaultFilePerLevelIdANN[i] == REQUESTED_ID) return 2;
					}
					return 1;
				}
				
			// else if the user specifies a level number but no file,
			// default to "proper" file based on world number
			// note that ANN doesn't have a world 9, so file 3 can't be default
			} else {
				String requestedWorld = REQUESTED_LEVEL.substring(0, REQUESTED_LEVEL.indexOf("-"));
				
				if (!Character.isDigit(REQUESTED_LEVEL.charAt(0)))
					return 4;
				
				int world = Integer.parseInt(requestedWorld);
				return world <= 4 ? 1 : world <= 8 || game == Game.ALL_NIGHT_NIPPON ? 2 : 3;
			}
		}
		
		return 1;
	}
	
	private static int[][] defaultFilePerLevelIdLL = {
		// level IDs that are associated with file 2
		{0x01, 0x03, 0x04, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
			0x30, 0x31, 0x32, 0x35, 0x36, 0x37, 0x3C, 0x43,
			0x44, 0x46, 0x48, 0x49, 0x64, 0x65, 0x66, 0x67,
			0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7F},
		// level IDs that are associated with file 3
		{0x05, 0x06, 0x07, 0x38, 0x39, 0x3A, 0x4A, 0x4B,
				0x4C, 0x68, 0x69},
	};
	
	private static int[] defaultFilePerLevelIdANN = {
		// level IDs that are associated with file 2
		0x00, 0x02, 0x03, 0x21, 0x23, 0x2A, 0x2D, 0x2E,
		0x30, 0x31, 0x32, 0x33, 0x36, 0x37, 0x39, 0x3A,
		0x3B, 0x3F, 0x43, 0x44, 0x46, 0x47, 0x64, 0x65,
		0x66, 0x67, 0x69, 0x6B, 0x72, 0x75, 0x76, 0x78,
		0x79, 0x7A, 0x7B, 0x7E, 0x7F
	};
	
	// copy ROM, FDS BIOS, and main disc files into memory
	private static MemoryManager prepareMemorySpace(byte[] data, byte[] bios) {
		MemoryManager memory = new MemoryManager(0x10000);
		
		switch (game) {
			// for SMB NES, just copy ROM region where it belongs
			case SUPER_MARIO_BROS: {
				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x10 + i], 0x8000 + i);
				break;
			}

			// for SMB NES, copy ROM region where it belongs
			// and as a hacky thing, copy the extra bank with level data to the start of memory (gets deleted)
			case VS_SUPER_MARIO_BROS: {
				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x10 + i], 0x8000 + i);
				for (int i = 0; i < 0x2000; i++) memory.write8(data[0xA010 + i], i);
				break;
			}
			
			// for SMBFDS, copy FDS bios to its region
			// then copy the two disc files to program RAM
			case SUPER_MARIO_BROS_FDS: {
				if (bios == null) {
					System.err.println("BIOS not supplied!");
				} else {
					for (int i = 0; i < 0x2000; i++) memory.write8(bios[0x6010 + i], 0xE000 + i);
				}
				
				for (int i = 0; i < 0x4000; i++) memory.write8(data[0x214D + i], 0x6000 + i);
				for (int i = 0; i < 0x4000; i++) memory.write8(data[0x615E + i], 0xA000 + i);
				break;
			}
			
			// for LL, copy FDS bios to its region, then copy main file
			case LOST_LEVELS: {
				if (bios == null) {
					System.err.println("BIOS not supplied!");
				} else {
					for (int i = 0; i < 0x2000; i++) memory.write8(bios[0x6010 + i], 0xE000 + i);
				}

				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x219E + i], 0x6000 + i);
				break;
			}
			
			// for ANN, copy FDS bios to its region, then copy main file
			case ALL_NIGHT_NIPPON: {
				if (bios == null) {
					System.err.println("BIOS not supplied!");
				} else {
					for (int i = 0; i < 0x2000; i++) memory.write8(bios[0x6010 + i], 0xE000 + i);
				}

				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x25BE + i], 0x6000 + i);
				break;
			}
			
			// for custom data, copy the data raw into 0x0000
			case NONE: {
				for (int i = 0; i < data.length; i++) memory.write8(data[i], i);
				break;
			}
		}
		
		return memory;
	}
	
	// copy data from inline strings into memory
	private static MemoryManager prepareMemorySpace(int[] pointers, String tileData, String spriteData) {
		List<Integer> tileByteList = new ArrayList<>(), spriteByteList = new ArrayList<>();
		
		// add all the bytes of the tile data
		for (int i = 0; i < tileData.length(); i += 2) {
			int data = Integer.parseInt(tileData.substring(i, i+2), 0x10);
			tileByteList.add(data);
		}
		
		// add all the bytes of the sprite data
		for (int i = 0; i < spriteData.length(); i += 2) {
			int data = Integer.parseInt(spriteData.substring(i, i+2), 0x10);
			spriteByteList.add(data);
		}
		
		// create memory space as big as needed
		MemoryManager memory = new MemoryManager(tileByteList.size() + spriteByteList.size());
		// sprite data is located directly after tile data
		pointers[0] = 0;
		pointers[1] = tileByteList.size();
		
		// write the tile data
		for (int i = 0; i < tileByteList.size(); i++)
			memory.write8(tileByteList.get(i), pointers[0] + i);
		
		// write the sprite data
		for (int i = 0; i < spriteByteList.size(); i++)
			memory.write8(spriteByteList.get(i), pointers[1] + i);
		
		return memory;
	}
	
	// load a disc file into memory
	// always load in the order of 1, 2, 3, 4
	private static void loadDiscFile(int file, byte[] data) {
		switch (game) {
			// write files in order
			case LOST_LEVELS: {
				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x219E + i], 0x6000 + i);

				if (file >= 2) for (int i = 0; i < 0xE2F; i++) memory.write8(data[0xA1AF + i], 0xC470 + i);
				if (file >= 3) for (int i = 0; i < 0xCCF; i++) memory.write8(data[0xAFEF + i], 0xC5D0 + i);
				if (file >= 4) for (int i = 0; i < 0xF4C; i++) memory.write8(data[0xBCCF + i], 0xC2B4 + i);
				
				break;
			}

			// write files in order
			case ALL_NIGHT_NIPPON: {
				for (int i = 0; i < 0x8000; i++) memory.write8(data[0x25BE + i], 0x6000 + i);

				if (file >= 2) for (int i = 0; i < 0xE00; i++) memory.write8(data[0xA5CF + i], 0xC470 + i);
				if (file >= 3) for (int i = 0; i < 0xD12; i++) memory.write8(data[0xB3E0 + i], 0xC5D0 + i);
				if (file >= 4) for (int i = 0; i < 0xDF0; i++) memory.write8(data[0xC103 + i], 0xC296 + i);
				
				break;
			}
		}
	}
	
	private static int[] getLevelDataPointers(int file) {
		// if an address is requested, just use that, easy
		if (REQUESTED_TILE_ADDRESS >= 0 && REQUESTED_SPRITE_ADDRESS >= 0) {
			return new int[] {REQUESTED_TILE_ADDRESS, REQUESTED_SPRITE_ADDRESS};
		}
		
		// if there is no requested address or ID, find the ID from the requested level number
		// form a-b.c, a = world, b = level within world, c = sublevel within level
		// .c not required, same as c = 0 (main level)
		if (REQUESTED_ID < 0) {
			MY_WORLD = 1;
			int locationOfDash = REQUESTED_LEVEL.indexOf("-");
			int locationOfDot = REQUESTED_LEVEL.indexOf(".");
			
			if (Character.isAlphabetic(REQUESTED_LEVEL.charAt(0))) {
				// if letter world, take the letter's value where A=10, B=11, etc.
				// we'll flatten this later after finding a sublevel if necessary
				MY_WORLD = Character.getNumericValue(REQUESTED_LEVEL.charAt(0));
				MY_EXTRA = true;
			} else {
				// if number world, just take that value
				MY_WORLD = 0xFF & Integer.parseInt(REQUESTED_LEVEL.substring(0, locationOfDash));
			}
			
			MY_LEVEL = Integer.parseInt(locationOfDot >= 0 ?
					REQUESTED_LEVEL.substring(1 + locationOfDash, locationOfDot) :
					REQUESTED_LEVEL.substring(1 + locationOfDash));
			
			MY_SUBLEVEL = locationOfDot >= 0 ? Integer.parseInt(REQUESTED_LEVEL.substring(locationOfDot + 1)) : 1;
			
			if (MY_SUBLEVEL > 1) {
				// getting a sublevel from the hardcoded tables
				
				int[] subLevelsInThisLevel = listOfSublevels[game.getNum()][MY_WORLD - 1][MY_LEVEL - 1];
				if (MY_SUBLEVEL - 1 <= subLevelsInThisLevel.length) {
					// if that level has the requested sublevel, use that level ID
					REQUESTED_ID = subLevelsInThisLevel[MY_SUBLEVEL - 2];
				} else {
					// if it doesn't have that sublevel, just default to the main level
					MY_SUBLEVEL = 1;
				}
				
				// treat worlds A-D as worlds 1-4
				if (Character.isAlphabetic(REQUESTED_LEVEL.charAt(0))) MY_WORLD -= 9;
			}
			
			if (MY_SUBLEVEL == 1) {
				// getting a main level from world table
				
				// treat worlds A-D as worlds 1-4
				if (Character.isAlphabetic(REQUESTED_LEVEL.charAt(0))) MY_WORLD -= 9;
				
				// get the index of the first level of the world
				int levelIndex = memory.read8(worldNumberIndices[game.getNum()][0] + MY_WORLD - 1);
				
				// skip over autowalking levels, since they don't count in the world-level number
				for (int i = 1; i < MY_LEVEL; i++) {
					boolean skipAutowalkLevels = false;
					do {
						// get the tile data pointer of this level
						int levelId = memory.read8(worldNumberIndices[game.getNum()][1] + levelIndex);
						int[] pointersForThatLevel = getPointersFromLevelID(file, levelId);
						
						// check the Mario starting position bits (6 or 7 = autowalk)
						int marioStartPosition = memory.readBits(pointersForThatLevel[0], 0x1C);
						if (skipAutowalkLevels = (marioStartPosition >= 6)) {
							levelIndex++;
						}
					} while (skipAutowalkLevels);
					
					// even if this level didn't have an autowalking intro, still need to skip the main level
					levelIndex++;
				}
				
				REQUESTED_ID = memory.read8(worldNumberIndices[game.getNum()][1] + levelIndex);
			}
		}
		
		return getPointersFromLevelID(file, REQUESTED_ID);
	}
	
	// given the level ID, the disc file, and the game, get the tile data pointer and sprite data pointer
	private static int[] getPointersFromLevelID(int file, int id) {
		// level type given by 2 high bits of level ID
		int levelType = (id & 0x60) >> 5;
		int levelIndex = id & 0x1F;
		
		int g = game.getNum();
		
		if (game == Game.LOST_LEVELS || game == Game.ALL_NIGHT_NIPPON) {
			// use one of two level tables depending on current file loaded
			int table = file == 4 ? 1 : 0;
			
			// get pointer by indexing level type tables
			// this pointer is already one 16 bit value
			int tilePointerIndex = memory.read8(levelDataTilePointers[g][table] + levelType) + levelIndex;
			int tilePointer = memory.read16(levelDataTilePointers[g][2 + table] + 2 * tilePointerIndex);
			
			int spritePointerIndex = memory.read8(levelDataSpritePointers[g][table] + levelType) + levelIndex;
			int spritePointer = memory.read16(levelDataSpritePointers[g][2 + table] + 2 * spritePointerIndex);
			
			return new int[] {tilePointer, spritePointer};
			
		} else if (game == Game.VS_SUPER_MARIO_BROS) {
			// level type tables consist of offsets instead of pointers
			int tileOffsetIndex = memory.read8(levelDataTilePointers[g][0] + levelType) + levelIndex;
			int tileOffset = memory.read16(levelDataTilePointers[g][1] + 2 * tileOffsetIndex);
			
			int spriteOffsetIndex = memory.read8(levelDataSpritePointers[g][0] + levelType) + levelIndex;
			int spriteOffset = memory.read16(levelDataSpritePointers[g][1] + 2 * spriteOffsetIndex);
			
			// TODO what gets copied when these pointers are out of bounds?? check mame memory mappings!
			
			// copy the level data into WRAM
			// earlier we copied the level data bank into 0x0000 in memory, so offset = pointer in this case
			int read = 0;
			for (int i = 0; i < 0x100 && read != 0xFD; i++) {
				read = memory.read8(tileOffset + i);
				memory.write8(read, levelDataTilePointers[g][2] + i);
			}
			read = 0;
			for (int i = 0; i < 0x100 && read != 0xFF; i++) {
				read = memory.read8(spriteOffset + i);
				memory.write8(read, levelDataSpritePointers[g][2] + i);
			}
			
			// but that level data bank was only there as a hack to make this easy, so we should delete it now
			// I don't even think it can ever be accessed anyway but who knows
			//for (int i = 0; i < 0x2000; i++) memory.write8(0, i);
			
			return new int[] {levelDataTilePointers[g][2], levelDataSpritePointers[g][2]};
			
		} else {
			// get pointer by indexing level type table
			// then using that index into two pointer tables, and concatenating those bytes
			int tilePointerIndex = memory.read8(levelDataTilePointers[g][0] + levelType) + levelIndex;
			int tilePointerHi = memory.read8(levelDataTilePointers[g][1] + tilePointerIndex);
			int tilePointerLo = memory.read8(levelDataTilePointers[g][2] + tilePointerIndex);
			int tilePointer = tilePointerLo | (tilePointerHi << 8);
			
			int spritePointerIndex = memory.read8(levelDataSpritePointers[g][0] + levelType) + levelIndex;
			int spritePointerHi = memory.read8(levelDataSpritePointers[g][1] + spritePointerIndex);
			int spritePointerLo = memory.read8(levelDataSpritePointers[g][2] + spritePointerIndex);
			int spritePointer = spritePointerLo | (spritePointerHi << 8);
			
			return new int[] {tilePointer, spritePointer};
		}
	}
	
	// hardcoded list of all sublevels in levels
	// this could be automated by like scanning levels for level transition objects but screw that
	private static int[][][][] listOfSublevels = new int[][][][] {
		// SMB1
		{
			{{0x42},{0x40,0x42,0x25},{},{}},
			{{0x2B,0x42},{0x01,0x25},{},{}},
			{{0x42,0x34},{},{},{}},
			{{0x42},{0x41,0x2F,0x42,0x25},{},{}},
			{{0x42},{0x00,0x2B},{},{}},
			{{},{0x42,0x00,0x34,0x42},{},{}},
			{{0x42},{0x01,0x25},{},{}},
			{{0x42},{0x42},{},{0x65,0x65,0x02,0x65}}
		},
		// SMBFDS
		{
			{{0x42},{0x40,0x42,0x25},{},{}},
			{{0x2B,0x42},{0x01,0x25},{},{}},
			{{0x42,0x34},{},{},{}},
			{{0x42},{0x41,0x2F,0x42,0x25},{},{}},
			{{0x42},{0x00,0x2B},{},{}},
			{{},{0x42,0x00,0x34,0x42},{},{}},
			{{0x42},{0x01,0x25},{},{}},
			{{0x42},{0x42},{},{0x65,0x65,0x02,0x65}}
		},
		// VSSMB TODO double check these
		{
			{{0x42},{0x40,0x42,0x25},{},{}},
			{{0x2B,0x42},{0x03,0x25},{},{}},
			{{0x42,0x34},{},{},{}},
			{{0x42},{0x41,0x2F,0x42,0x25},{},{}},
			{{0x42},{0x00,0x2B},{},{}},
			{{},{0x42,0x00,0x34,0x42},{},{}},
			{{0x42},{0x01,0x25},{},{}},
			{{0x42},{0x42},{},{0x65,0x65,0x02,0x65}}
		},
		// LL
		{
			{{0x42},{0x40,0x34,0x3B,0x41,0x42},{},{}},
			{{0x42,0x33},{0x42},{},{}},
			{{0x42,0x33,0x42,0x25},{0x00,0x3B},{},{}},
			{{0x02,0x33},{0x42},{},{}},
			{{0x44,0x3C,0x2B},{0x43,0x34,0x3B},{0x44,0x2C},{}},
			{{0x04},{0x01,0x3B},{},{}},
			{{0x44,0x2F,0x44},{0x44,0x30},{},{}},
			{{0x04,0x2B},{0x35,0x44},{0x3C},{0x03,0x67,0x67,0x67}},
			{{0x05},{},{0x69},{}},
			{{0x2B,0x41},{0x40,0x2D},{},{}},
			{{0x2B},{0x00,0x2D},{},{0x21}},
			{{0x41},{},{},{}},
			{{0x41},{0x41,0x2B},{},{0x2A,0x41,0x63}}
		},
		// ANN
		{
			{{0x42},{0x40,0x42,0x25},{},{}},
			{{0x2B,0x42},{0x01,0x38},{},{}},
			{{0x42,0x34},{},{},{}},
			{{0x42},{0x41,0x2F,0x42,0x38},{},{}},
			{{0x43},{0x00,0x39},{},{}},
			{{},{0x43,0x00,0x3A,0x43},{},{}},
			{{0x43},{0x03,0x38},{},{}},
			{{0x43},{0x43},{},{0x02,0x65,0x65,0x65}},
			{{},{},{},{}}, // no world 9
			{{0x2B,0x41},{0x40,0x2D},{},{}},
			{{0x41,0x2B},{0x00,0x2D},{},{0x21}},
			{{0x41,0x2B},{},{},{}},
			{{0x41,0x2B,0x28,0x41,0x2B},{0x41,0x2B},{},{0x2A,0x41,0x63}}
		}
	};
	
	// locations of world number tables in memory
	// always in same place regardless of file
	private static int[][] worldNumberIndices = {
			{0x9CB4, 0x9CBC}, // SMB
			{0x7CBC, 0x7CC4}, // SMBFDS
			{0xAC77, 0xAC7F}, // VSSMB
			{0xC357, 0xC360}, // LL
			{0xC339, 0xC341}, // ANN
	};
	
	
	// locations of tile pointer tables in memory
	private static int[][] levelDataTilePointers = {
			{0x9D28, 0x9D4E, 0x9D2C}, // SMB table idx, hi, lo
			{0x7D30, 0x7D56, 0x7D34}, // SMBFDS table idx, hi, lo
			{0xACF5, 0xACF9, 0x6200}, // VSSMB table idx, ptr, WRAM buffer
			{0xC400, 0xC3A0, 0xC404, 0xC3A4}, // LL table idx 1-3, table idx 4, ptrs 1-3, ptrs 4
			{0xC3CD, 0xC381, 0xC3D1, 0xC385}, // ANN table idx 1-3, table idx 4, ptrs 1-3, ptrs 4
	};
	
	// locations of sprite pointer tables in memory
	private static int[][] levelDataSpritePointers = {
			{0x9CE0, 0x9D06, 0x9CE4}, // SMB table idx, hi, lo
			{0x7CE8, 0x7D0E, 0x7CEC}, // SMBFDS table idx, hi, lo
			{0xACA3, 0xACA7, 0x6100}, // VSSMB table idx, ptr, WRAM buffer
			{0xC394, 0xC372, 0xC398, 0xC376}, // LL table idx 1-3, table idx 4, ptrs 1-3, ptrs 4
			{0xC36F, 0xC353, 0xC373, 0xC357}, // ANN table idx 1-3, table idx 4, ptrs 1-3, ptrs 4
	};
}