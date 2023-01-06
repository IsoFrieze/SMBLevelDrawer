package com.isofrieze;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;


public class SMB1LevelDrawer {
	
	public static BufferedImage src;
	
	public static void main(String [] args) {
		String file = "C:\\Users\\Frieze\\Documents\\RGME\\WIP\\SMB1LevelFormat\\data\\Super Mario Bros. (World).nes";
		byte[] rom = null;
		
		try (FileInputStream in = new FileInputStream(file)) {
			rom = new byte[(int)in.getChannel().size()];
			in.read(rom, 0, rom.length);
			src = ImageIO.read(new File("C:\\Users\\Frieze\\Documents\\RGME\\WIP\\SMB1AreaPointer\\img\\sprites.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for (int l = 0; l < 1; l++) {
			l = 0x50;
			int LEVEL = l, HARD = 1, END = 0;
			int type = LEVEL >> 5, number = LEVEL & 0x1F;
			int spriteOffset = getByte(rom, nes2pc(0x9CE0+type));
			int objectOffset = getByte(rom, nes2pc(0x9D28+type));

			int spritePtr = getByte(rom, nes2pc(0x9CE4+spriteOffset+number)) | (getByte(rom, nes2pc(0x9D06+spriteOffset+number)) << 8);
			int objectPtr = getByte(rom, nes2pc(0x9D2C+objectOffset+number)) | (getByte(rom, nes2pc(0x9D4E+objectOffset+number)) << 8);
			
			System.out.printf("SPRITES = %x, OBJECTS = %x%n", spritePtr, objectPtr);
			
			
			int[] cloud = {0};
			BufferedImage tiles = drawTileData(rom, nes2pc(objectPtr), l, type, cloud);
			//BufferedImage sprites = drawSpriteData(rom, nes2pc(spritePtr), HARD, type, cloud[0], END);
			//((Graphics2D)tiles.getGraphics()).drawImage(sprites, 0, 0, null);
			
//			try {
//				String no = String.format("%2x", l).replace(' ', '0').toUpperCase();
//				ImageIO.write(tiles, "png", new File(String.format("C:\\Users\\Frieze\\Documents\\RGME\\WIP\\SMB1LevelFormat\\img\\lvl_floor\\lvl_%03d_(%s).png",l,no)));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
		
	}
	
	public static int nes2pc(int pc) {
		return pc + 0x10 - 0x8000;
	}
	
	public static void drawTile(BufferedImage dest, int dx, int dy, int sx, int sy, int w, int h, int pal) {
		((Graphics2D)dest.getGraphics()).drawImage(src, dx, dy, dx+w, dy+h, 164*pal+sx, sy, 164*pal+sx+w, sy+h, null);
	}
	
	public static int getBits(byte[] data, int offset, int mask) {
		if (mask == 0) return 0;
		int b = getByte(data, offset) & mask;
		while ((mask & 1) == 0) {
			b >>= 1;
			mask >>= 1;
		}
		return b;
	}
	
	public static int getByte(byte[] data, int offset) {
		if (offset < 0 || offset >= data.length) return 0; 
		return data[offset] & 0xFF;
	}
	
	public static int getWord(byte[] data, int offset) {
		return getByte(data, offset) | (getByte(data, offset+1) << 8);
	}
	
	public static BufferedImage drawSpriteData(byte[] data, int ptr, int hard, int palette, int cloud, int end) {
		if (ptr + 1 >= data.length) return null;
		
		BufferedImage bi = new BufferedImage(16*16*25, 16*14, BufferedImage.TYPE_4BYTE_ABGR);
		int workingScreen = 0, xExtent = 0;
		boolean setPage = false;
		
		while (ptr < data.length && getByte(data, ptr) != 0xFF) {
			System.out.printf("%2x %2x ", getByte(data, ptr), getByte(data, ptr+1));
			
			int xpos = getBits(data, ptr, 0xF0);
			int ypos = getBits(data, ptr, 0x0F);
			int nextPage = getBits(data, ptr+1, 0x80);
			if (nextPage > 0 && !setPage) workingScreen++;
			else setPage = false;
			
			if (ypos == 15) {
				System.out.print("   ");
				
				int pageSet = getBits(data, ptr+1, 0x1F);
				workingScreen = pageSet;
				setPage = true;
				
				System.out.printf("  Y15 Page Set: xpos=%d, next=%d, page=%d%n", xpos, nextPage, pageSet);
				
			} else if (ypos == 14) {
				System.out.printf("%2x ", getByte(data, ptr+2));
				
				int areaID = getBits(data, ptr+1, 0x7F);
				int worldFilter = getBits(data, ptr+2, 0xE0);
				int screenNo = getBits(data, ptr+2, 0x1F);
				
				System.out.printf("  Y14 Area Change: xpos=%d, next=%d, area=%d, world=%d, screen=%d%n", xpos, nextPage, areaID, worldFilter, screenNo);
				
				ptr++;
				
			} else {
				System.out.print("   ");
				
				int hardMode = getBits(data, ptr+1, 0x40);
				int spriteType = getBits(data, ptr+1, 0x3F);
				
				System.out.printf("  Standard SPR: xpos=%d, ypos=%d, next=%d, hard=%d, type=%d%n", xpos, ypos, nextPage, hardMode, spriteType);
				
				int overallXPos = workingScreen * 16 + xpos;
				
				if (overallXPos >= xExtent-4 && (hard == hardMode || hard == 1)) {
					switch (spriteType) {
						case 0: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 347, 16, 24, palette); break; // green koopa
						case 1: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 554, 16, 24, palette); break; // red koopa
						case 2: drawTile(bi, 16*overallXPos, 16*ypos, 0, 269, 16, 16, palette); break; // buzzy beetle
						case 3: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 554, 16, 24, palette); break; // red koopa
						case 4: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 347, 16, 24, palette); break; // green koopa
						case 5: drawTile(bi, 16*overallXPos, 16*ypos-16, 72, 407, 16, 32, palette); break; // hammer bro
						case 6: drawTile(bi, 16*overallXPos, 16*ypos, 0, 251, 16, 16, palette); break; // goomba
						case 7: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 287, 16, 24, palette); break; // blooper
						case 8: drawTile(bi, 16*overallXPos, 16*ypos, 54, 287, 16, 16, palette); break; // bullet bill
						case 9: drawTile(bi, 16*overallXPos, 16*ypos-8, 36, 347, 16, 24, palette); break; // green parakoopa
						case 10: drawTile(bi, 16*overallXPos, 16*ypos, 0, 399, 16, 16, palette); break; // gray cheep cheep
						case 11: drawTile(bi, 16*overallXPos, 16*ypos, 0, 606, 16, 16, palette); break; // red cheep cheep
						case 12: drawTile(bi, 16*overallXPos, 16*ypos, 90, 606, 16, 16, palette); break; // podoboo
						case 13: drawTile(bi, 16*overallXPos, 16*ypos-32, 0, 373, 16, 24, palette); break; // piranha plant
						case 14: drawTile(bi, 16*overallXPos, 16*ypos-8, 36, 347, 16, 24, palette); break; // green parakoopa
						case 15: drawTile(bi, 16*overallXPos, 16*ypos-8, 36, 554, 16, 24, palette); break; // red parakoopa
						case 16: drawTile(bi, 16*overallXPos, 16*ypos-8, 36, 347, 16, 24, palette); break; // green parakoopa
						case 17: drawTile(bi, 16*overallXPos, 16*ypos-8, 54, 373, 16, 24, palette); break; // lakitu
						case 18: drawTile(bi, 16*overallXPos, 16*ypos, 72, 588, 16, 16, palette); break; // spiny
						case 19: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 20: drawTile(bi, 16*overallXPos, 16*ypos, 0, 606, 16, 16, palette); break; // flying cheep cheep
						case 21: drawTile(bi, 16*overallXPos, 16*ypos+16, 102, 477, 24, 8, palette); break; // bowser fire
						case 22: break; // fireworks
						case 23: drawTile(bi, 16*overallXPos, 16*ypos+16, 54, 287, 16, 16, palette); break; // bullet/cheep generator
						case 24: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 25: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 26: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 27: drawTile(bi, 16*overallXPos+4, 16*ypos-12, 107, 503, 8, 8*6, palette); break; // firebar
						case 28: drawTile(bi, 16*overallXPos+4, 16*ypos-12, 107, 503, 8, 8*6, palette); break; // firebar
						case 29: drawTile(bi, 16*overallXPos+4, 16*ypos-12, 107, 503, 8, 8*6, palette); break; // firebar
						case 30: drawTile(bi, 16*overallXPos+4, 16*ypos-12, 107, 503, 8, 8*6, palette); break; // firebar
						case 31: drawTile(bi, 16*overallXPos+4, 16*ypos-12, 107, 503, 8, 8*12, palette); break; // big firebar
						case 32: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 33: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 34: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 35: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 36: drawTile(bi, 16*overallXPos, 16*ypos-16, 8*6*cloud, 513-8*hard, 8*6, 8, palette); break; // balance lift
						case 37: drawTile(bi, 16*overallXPos, 16*ypos-16, 8*6*cloud, 513+8*hard, 8*6, 8, palette); break; // lift
						case 38: drawTile(bi, 16*overallXPos+12, 16*ypos-16, 8*6*cloud, 513+8*hard, 8*6, 8, palette); break; // lift
						case 39: drawTile(bi, 16*overallXPos+12, 16*ypos-16, 8*6*cloud, 513+8*hard, 8*6, 8, palette); break; // lift
						case 40: drawTile(bi, 16*overallXPos, 16*ypos-16, 8*6*cloud, 513+8*hard, 8*6, 8, palette); break; // lift
						case 41: drawTile(bi, 16*overallXPos, 16*ypos-16, 8*6*cloud, 513+8*hard, 8*6, 8, palette); break; // lift
						case 42: drawTile(bi, 16*overallXPos, 16*ypos-16, 8*6*cloud, 513, 8*6, 8, palette); break; // lift
						case 43: drawTile(bi, 16*overallXPos+12, 16*ypos-16, 0, 513, 24, 8, palette); break; // short lift
						case 44: drawTile(bi, 16*overallXPos+12, 16*ypos-16, 0, 513, 24, 8, palette); break; // short lift
						case 45: drawTile(bi, 16*overallXPos, 16*ypos-16, 0, 443, 32, 32, 1); break; // bowser
						case 46: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 47: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 48: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 49: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 50: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 51: drawTile(bi, 16*overallXPos, 16*ypos-8, 0, 347, 16, 24, palette); break; // ?
						case 52: break; // warp zone command
						case 53: drawTile(bi, 16*overallXPos, 16*10+8, 108+18*end, 347, 16, 24, palette); break; // toad/princess
						case 54: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
						case 55: 
							drawTile(bi, 16*overallXPos-24, 16*11, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-48, 16*11, 0, 251, 16, 16, palette); break; // 2 goombas y=10
						case 56:
							drawTile(bi, 16*overallXPos, 16*11, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-24, 16*11, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-48, 16*11, 0, 251, 16, 16, palette); break; // 3 goombas y=10
						case 57: 
							drawTile(bi, 16*overallXPos-24, 16*7, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-48, 16*7, 0, 251, 16, 16, palette); break; // 2 goombas y=6
						case 58: 
							drawTile(bi, 16*overallXPos, 16*7, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-24, 16*7, 0, 251, 16, 16, palette);
							drawTile(bi, 16*overallXPos-48, 16*7, 0, 251, 16, 16, palette); break; // 3 goombas y=6
						case 59: 
							drawTile(bi, 16*overallXPos-24, 16*11-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-48, 16*11-8, 0, 347, 16, 24, palette); break; // 2 koopas y=10
						case 60: 
							drawTile(bi, 16*overallXPos, 16*11-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-24, 16*11-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-48, 16*11-8, 0, 347, 16, 24, palette); break; // 3 koopas y=10
						case 61: 
							drawTile(bi, 16*overallXPos-24, 16*7-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-48, 16*7-8, 0, 347, 16, 24, palette); break; // 2 koopas y=6
						case 62: 
							drawTile(bi, 16*overallXPos, 16*7-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-24, 16*7-8, 0, 347, 16, 24, palette);
							drawTile(bi, 16*overallXPos-48, 16*7-8, 0, 347, 16, 24, palette); break; // 3 koopas y=6
						case 63: drawTile(bi, 16*overallXPos, 16*ypos, 54, 588, 16, 16, palette); break; // ?
					}
					
					xExtent = overallXPos;
				}
			}
			
			ptr += 2;
		}
		
		return bi;
	}

	@SuppressWarnings("unused")
	public static BufferedImage drawTileData(byte[] data, int ptr, int lvl, int type, int[] cloud) {
		if (ptr + 1 >= data.length) return null;
		
		BufferedImage bi = new BufferedImage(16*16*100, 16*14, BufferedImage.TYPE_4BYTE_ABGR);
		int[][][] grid = new int[bi.getWidth()/16][bi.getHeight()/16][2];
		int[][] queue = {null,null,null};
		int[] palettes = {type, type, type, type};

		int time = getBits(data, ptr, 0xC0);
		int startPos = getBits(data, ptr, 0x1C);
		int levelMod = getBits(data, ptr, 0x07);
		int specPlat = getBits(data, ptr+1, 0xC0);
		int scenery = getBits(data, ptr+1, 0x30);
		int floor = getBits(data, ptr+1, 0x0F);
		ptr += 2;

		Color blue = new Color(0x92, 0x90, 0xFF);
		Color bg = levelMod == 5 ? blue : ((levelMod >= 4  || type >= 2) ? Color.black : blue);
		System.out.printf("Level header: time=%d, startPos=%d, levelMod=%d, specPlat=%d, scenery=%d, floor=%d%n", time, startPos, levelMod, specPlat, scenery, floor);
		
		int xExtent = 0;
		boolean processNextScreenObject = false;
		int staircase = 9;
		
		while (xExtent < bi.getWidth() / 16 && ptr < data.length) {
			{ // palettes
				palettes = new int[] {type, type, type, type};
				if (levelMod == 7) palettes = new int[] {3, 3, 3, 3};
				if (levelMod == 6 || levelMod == 5) palettes[1] = 5;
				if (specPlat == 1) palettes[1] = 4;
				if (specPlat == 3) cloud[0] = 1;
			} { // scenery
				int col = xExtent % (16*3);
				for (int i = 0; i < scenerys[scenery][col].length; i++) {
					int tile = scenerys[scenery][col][i][1];
					grid[xExtent][scenerys[scenery][col][i][0]] = new int[] {tile, palettes[tile <= 32 ? 0 : tile <= 65 ? 1 : 2]};
				}
				
			} { // backdrop
				if (levelMod < 4) {
					for (int i = 0; i < backdrops[levelMod].length; i++) {
						int tile = backdrops[levelMod][i][1];
						grid[xExtent][backdrops[levelMod][i][0]] = new int[] {tile, palettes[tile <= 32 ? 0 : tile <= 65 ? 1 : 2]};
					}
				}
				
			} { // floor
				for (int i = 0; i < floors[floor].length; i++) {
					int val = floors[floor][i];
					if (val > 0) {
						if (specPlat == 3) {
							if (i <= 8 || i == 12) grid[xExtent][i] = new int[] {70, palettes[2]};
						} else {
							int tile = 1;
							if (type == 0) tile = 17;
							else if (type == 3) tile = 24;
							else if (type == 2 && i < 12) tile = 3;
							if (lvl == 2) tile = 24;
							grid[xExtent][i] = new int[] {tile, palettes[0]};
						}
					}
				}
				
			} { // next screen
				if (xExtent > 0 && xExtent % 16 == 0) {
					while (getByte(data, ptr) != 0xFD && getBits(data, ptr+1, 0x80) == 0 && !(getBits(data, ptr, 0x0F) == 13 && getBits(data, ptr+1, 0x40) == 0)) ptr += 2;
					
					if (getByte(data, ptr) != 0xFD && getBits(data, ptr, 0x0F) == 13 && getBits(data, ptr+1, 0x40) == 0) {
						if (getBits(data, ptr+1, 0x80) == 1) { // next obj is page set with next screen flag on (no skip)
							ptr += 2;
						} else { // next obj is normal page set
							int page = getBits(data, ptr+1, 0x1F);
							if ((xExtent >> 4) < page) { // page is in the future
								// nothing
							} else if ((xExtent >> 4) == page) { // page is in the present
								ptr += 2;
							} else { // page is in the past
								while (getByte(data, ptr) != 0xFD && page < (xExtent >> 4)) {
									ptr += 2;
									if (getBits(data, ptr+1, 0x80) == 1) page++;
								}
							}
						}
					} else { // next obj has next screen flag, or end of data
						// nothing
					}
					processNextScreenObject = true;
				}
				
			} { // add objects
				while (
						(getByte(data, ptr) != 0xFD) &&
						!(getBits(data, ptr, 0x0F) == 13 && getBits(data, ptr+1, 0x40) == 0) &&
						(getBits(data, ptr, 0xF0) == (xExtent & 0x0F))
						) {
					if (getBits(data, ptr+1, 0x80) == 1 && !processNextScreenObject) {
						break;
					} else {
						processNextScreenObject = false;
						boolean full = true;
						for (int i = queue.length-1; i >= 0; i--) {
							if (queue[i] == null) {
								queue[i] = new int[] {ptr, xExtent};
								System.out.printf("added %x [%2x%2x] at x=%x%n", ptr, getByte(data, ptr), getByte(data, ptr+1), xExtent);
								ptr += 2;
								full = false;
								break;
							}
						}
						if (full) break;
					}
				}
				
			} { // process objects
				for (int i = queue.length-1; i >= 0; i--) {
					if (queue[i] != null) {
						//System.out.printf("process %x at x=%x%n", queue[i][0], xExtent);
						int ypos = getBits(data, queue[i][0], 0x0F);
						if (ypos < 12) {
							switch (getBits(data, queue[i][0]+1, 0x70)) {
							case 0:
								switch (getBits(data, queue[i][0]+1, 0x0F)) {
								case 0: case 1:
									write(grid, xExtent, ypos+1, 71, palettes[2]);
									break;
								case 4: case 5: case 6: case 7: case 8:
									write(grid, xExtent, ypos+1, type >= 2 ? 3 : 2, palettes[0]);
									break;
								case 9: 
									write(grid, xExtent, ypos+1, 22, palettes[0]);
									write(grid, xExtent, ypos+2, 29, palettes[0]);
									break;
								case 10:
									write(grid, xExtent, ypos+1, 80, palettes[2]);
									break;
								case 11: 
									write(grid, xExtent, ypos+1, 31, palettes[0]);
									write(grid, xExtent, ypos+2, 32, palettes[0]);
									break;
								case 12:
									switch (xExtent-queue[i][1]) {
									case 0:
										write(grid, xExtent, 10, 53, palettes[1]);
										write(grid, xExtent, 11, 62, palettes[1]);
										break;
									case 1:
										write(grid, xExtent, 10, 54, palettes[1]);
										write(grid, xExtent, 11, 63, palettes[1]);
										break;
									case 2:
										for (int j = 0; j < 8; j++) write(grid, xExtent, j, 0, palettes[0]);
										write(grid, xExtent, 8, 40, palettes[1]);
										write(grid, xExtent, 9, 48, palettes[1]);
										write(grid, xExtent, 10, 55, palettes[1]);
										write(grid, xExtent, 11, 64, palettes[1]);
										break;
									case 3:
										for (int j = 0; j < 8; j++) write(grid, xExtent, j, 0, palettes[0]);
										write(grid, xExtent, 8, 41, palettes[1]);
										write(grid, xExtent, 9, 49, palettes[1]);
										write(grid, xExtent, 10, 49, palettes[1]);
										write(grid, xExtent, 11, 49, palettes[1]);
										break;
									}
									break;
								case 13:
									write(grid, xExtent, 1, 56, palettes[1]);
									for (int j = 0; j < 9; j++) write(grid, xExtent, j+2, 65, palettes[1]);
									write(grid, xExtent, 11, 9, palettes[0]);
									break;
								}
								break;
							case 1: 
								switch (specPlat) {
								case 0: case 3:
									if (xExtent-queue[i][1] == 0) { // left edge
										write(grid, xExtent, ypos+1, 33, palettes[1]);
									} else if (xExtent-queue[i][1] == getBits(data, queue[i][0]+1, 0x0F)) { // right edge
										write(grid, xExtent, ypos+1, 35, palettes[1]);
									} else {
										write(grid, xExtent, ypos+1, 34, palettes[1]);
										for (int j = ypos+2; j < grid[xExtent].length; j++)
											write(grid, xExtent, j, 10, palettes[0]);
									}
									break;
								case 1:
									if (xExtent-queue[i][1] == 0) { // left edge
										write(grid, xExtent, ypos+1, 36, palettes[1]);
									} else if (xExtent-queue[i][1] == getBits(data, queue[i][0]+1, 0x0F)) { // right edge
										write(grid, xExtent, ypos+1, 38, palettes[1]);
									} else {
										write(grid, xExtent, ypos+1, 37, palettes[1]);
										if (getBits(data, queue[i][0]+1, 0x0F) >= 2 && xExtent-queue[i][1] == (1+getBits(data, queue[i][0]+1, 0x0F))/2) {
											write(grid, xExtent, ypos+2, 21, palettes[0]);
											for (int j = ypos+3; j < grid[xExtent].length; j++) 
												write(grid, xExtent, j, 28, palettes[0], 1);
										}
									}
									break;
								case 2:
									for (int j = 0; j < getBits(data, queue[i][0]+1, 0x0F)+1 && ypos+1+j < grid[xExtent].length; j++) {
										if (j == 0) write(grid, xExtent, ypos+1+j, 8, palettes[0]);
										else if (j == 1) write(grid, xExtent, ypos+1+j, 16, palettes[0]);
										else write(grid, xExtent, ypos+1+j, 23, palettes[0]);
									}
									break;
								}
								break;
							case 2:
								if (type == 0) write(grid, xExtent, ypos+1, 45, palettes[1]);
								else if (specPlat == 3) write(grid, xExtent, ypos+1, 70, palettes[2]);
								else write(grid, xExtent, ypos+1, type >= 2 ? 3 : 2, palettes[0]);
								break;
							case 3:
								write(grid, xExtent, ypos+1, type == 3 ? 24 : type == 0 ? 17 : 9, palettes[0]);
								break;
							case 4:
								write(grid, xExtent, ypos+1, type == 0 ? 78 : 77, palettes[2]);
								break;
							case 5:
								for (int j = 0; j < getBits(data, queue[i][0]+1, 0x0F)+1 && ypos+1+j < grid[xExtent].length; j++) {
									if (type == 0) write(grid, xExtent, ypos+1+j, 45, palettes[1]);
									else write(grid, xExtent, ypos+1+j, type >= 2 ? 3 : 2, palettes[0]);
								}
								break;
							case 6:
								for (int j = 0; j < getBits(data, queue[i][0]+1, 0x0F)+1 && ypos+1+j < grid[xExtent].length; j++)
									write(grid, xExtent, ypos+1+j, type == 3 ? 24 : type == 0 ? 17 : 9, palettes[0]);
								break;
							case 7:
								switch (xExtent-queue[i][1]) {
								case 0:
									write(grid, xExtent, ypos+1, 40, palettes[1]);
									write(grid, xExtent, ypos+2, 48, palettes[1]);
									for (int j = 0; j < getBits(data, queue[i][0]+1, 0x07)-1 && ypos+3+j < grid[xExtent].length; j++)
										write(grid, xExtent, ypos+3+j, 48, palettes[1]);
									break;
								case 1:
									write(grid, xExtent, ypos+1, 41, palettes[1]);
									write(grid, xExtent, ypos+2, 49, palettes[1]);
									for (int j = 0; j < getBits(data, queue[i][0]+1, 0x07)-1 && ypos+3+j < grid[xExtent].length; j++)
										write(grid, xExtent, ypos+3+j, 49, palettes[1]);
									break;
								}
								break;
							}
						} else if (ypos == 12) {
							switch (getBits(data, queue[i][0]+1, 0x70)) {
							case 0:
								for (int j = 9; j < 14; j++) write(grid, xExtent, j, type == 0 ? 75 : 0, palettes[2]);
								break;
							case 1:
								if (xExtent-queue[i][1] == 0) { // left edge
									write(grid, xExtent, 1, 18, palettes[0]);
								} else if (xExtent-queue[i][1] == getBits(data, queue[i][0]+1, 0x0F)) { // right edge
									write(grid, xExtent, 1, 20, palettes[0]);
								} else {
									write(grid, xExtent, 1, 19, palettes[0]);
								}
								break;
							case 2:
								write(grid, xExtent, 7, 50, palettes[1]);
								write(grid, xExtent, 8, 11, palettes[0]);
								break;
							case 3:
								write(grid, xExtent, 8, 50, palettes[1]);
								write(grid, xExtent, 9, 11, palettes[0]);
								break;
							case 4:
								write(grid, xExtent, 10, 50, palettes[1]);
								write(grid, xExtent, 11, 11, palettes[0]);
								break;
							case 5:
								write(grid, xExtent, 9, 0, palettes[0]);
								write(grid, xExtent, 10, 0, palettes[0]);
								write(grid, xExtent, 11, 0, palettes[2]);
								write(grid, xExtent, 12, 0, palettes[2]);
								write(grid, xExtent, 13, 0, palettes[2]);
								write(grid, xExtent, 11, 69, palettes[2]);
								write(grid, xExtent, 12, 75, palettes[2]);
								write(grid, xExtent, 13, 75, palettes[2]);
								break;
							case 6:
								write(grid, xExtent, 4, 71, palettes[2]);
								break;
							case 7:
								write(grid, xExtent, 8, 71, palettes[2]);
								break;
							}
						} else if (ypos == 13 && getBits(data, queue[i][0]+1, 0x40) == 1) {
							switch (getBits(data, queue[i][0]+1, 0x3F)) {
							case 0:
								switch (xExtent-queue[i][1]) {
								case 0:
									write(grid, xExtent, 10, 53, palettes[1]);
									write(grid, xExtent, 11, 62, palettes[1]);
									break;
								case 1:
									write(grid, xExtent, 10, 54, palettes[1]);
									write(grid, xExtent, 11, 63, palettes[1]);
									break;
								case 2:
									for (int j = 0; j < 8; j++) write(grid, xExtent, j, 0, palettes[0]);
									write(grid, xExtent, 8, 40, palettes[1]);
									write(grid, xExtent, 9, 48, palettes[1]);
									write(grid, xExtent, 10, 55, palettes[1]);
									write(grid, xExtent, 11, 64, palettes[1]);
									break;
								case 3:
									for (int j = 0; j < 8; j++) write(grid, xExtent, j, 0, palettes[0]);
									write(grid, xExtent, 8, 41, palettes[1]);
									write(grid, xExtent, 9, 49, palettes[1]);
									write(grid, xExtent, 10, 49, palettes[1]);
									write(grid, xExtent, 11, 49, palettes[1]);
									break;
								}
								break;
							case 1:
								write(grid, xExtent, 1, 56, palettes[1]);
								for (int j = 0; j < 9; j++) write(grid, xExtent, j+2, 65, palettes[1]);
								write(grid, xExtent, 11, 9, palettes[0]);
								break;
							case 2:
								write(grid, xExtent, 7, 79, palettes[2]);
								break;
							case 3:
								write(grid, xExtent, 8, 51, palettes[1]);
								break;
							case 4:
								write(grid, xExtent, 9, 76, palettes[2]);
								break;
							case 5: case 6: case 7: case 8: case 9: case 10: case 11:
								break;
							default:
								write(grid, xExtent, 0, 16, palettes[0]); // error
								break;
							}
						} else if (ypos == 14) {
							if (getBits(data, queue[i][0]+1, 0x40) == 0) {
								scenery = getBits(data, queue[i][0]+1, 0x30);
								floor = getBits(data, queue[i][0]+1, 0x0F);
							} else {
								levelMod = getBits(data, queue[i][0]+1, 0x07);
							}
						} else if (ypos == 15) {
							switch (getBits(data, queue[i][0]+1, 0x70)) {
							case 0:
								for (int j = 1; j < grid[xExtent].length; j++) write(grid, xExtent, j, 27, palettes[0]);
								break;
							case 1:
								for (int j = 2; j < grid[xExtent].length; j++) {
									if (j-2 < getBits(data, queue[i][0]+1, 0x0F)+1) write(grid, xExtent, j, 27, palettes[0]);
									else write(grid, xExtent, j, 0, palettes[0]);
								}
								break;
							case 2:
								int height = getBits(data, queue[i][0]+1, 0x0F);
								if (height > 10) {
									write(grid, xExtent, 0, 16, palettes[0]); // error
								} else {
									for (int j = 0; j < 11-height; j++) write(grid, xExtent, height+1+j, castleObject[xExtent-queue[i][1]][j], palettes[0]);
								}
								break;
							case 3:
								if (xExtent == queue[i][1]) staircase = 9;
								staircase = (staircase - 1) & 0xFF;
								int gap = 1+getByte(data, nes2pc(0x9AAE+staircase));
								int blocks = 1+getByte(data, nes2pc(0x9AA5+staircase));
								if (blocks >= 0x80) blocks = 1;
								for (int j = 0; j < blocks && gap + j < grid[xExtent].length; j++) write(grid, xExtent, gap+j, 9, palettes[0]);
								break;
							case 4:
								int height2 = getBits(data, queue[i][0]+1, 0x0F);
								switch (xExtent-queue[i][1]) {
								case 0:
									if (height2 > 0) {
										write(grid, xExtent, height2, 53, palettes[1]);
										write(grid, xExtent, height2+1, 62, palettes[1]);
									}
									break;
								case 1:
									if (height2 > 0) {
										write(grid, xExtent, height2, 54, palettes[1]);
										write(grid, xExtent, height2+1, 63, palettes[1]);
									}
									break;
								case 2:
									if (height2 < 2) height2 = 2;
									for (int j = 0; j <= height2; j++) {
										if (j == height2) write(grid, xExtent, j+1, 64, palettes[1]);
										else if (j == height2-1) write(grid, xExtent, j+1, 55, palettes[1]);
										else write(grid, xExtent, j+1, 48, palettes[1]);
									}
									break;
								case 3:
									if (height2 < 2) height2 = 2;
									for (int j = 0; j <= height2; j++) write(grid, xExtent, j+1, 49, palettes[1]);
									break;
								}
								break;
							case 5:
								for (int j = 0; j < getBits(data, queue[i][0]+1, 0x0F)+1 && 3+j < grid[xExtent].length; j++) write(grid, xExtent, 3+j, 30, palettes[0]);
								break;
							}
						}
					}
				}
				
			} { // remove objects
				xExtent++;
				for (int i = 0; i < queue.length; i++) {
					if (queue[i] != null) {
						int width = 1;
						if (getBits(data, queue[i][0], 0x0F) < 12) {
							if ((getBits(data, queue[i][0]+1, 0x70) <= 4 && getBits(data, queue[i][0]+1, 0x70) >= 1)) {
								if (!(getBits(data, queue[i][0]+1, 0x70) == 1 && specPlat == 2))
									width = getBits(data, queue[i][0]+1, 0x0F)+1;
							} else if (getBits(data, queue[i][0]+1, 0x70) == 7) {
								width = 2;
							} else if (getBits(data, queue[i][0]+1, 0x70) == 0 && getBits(data, queue[i][0]+1, 0x0F) == 12) {
								width = 4;
							}
						} else if (getBits(data, queue[i][0], 0x0F) == 12) {
							width = getBits(data, queue[i][0]+1, 0x0F)+1;
						} else if (getBits(data, queue[i][0], 0x0F) == 13 && getBits(data, queue[i][0]+1, 0x40) == 1) {
							if (getBits(data, queue[i][0]+1, 0x3F) == 0) width = 4;
							else if (getBits(data, queue[i][0]+1, 0x3F) == 4) width = 13;
						} else if (getBits(data, queue[i][0], 0x0F) == 15) {
							if (getBits(data, queue[i][0]+1, 0x70) == 2) width = 5;
							else if (getBits(data, queue[i][0]+1, 0x70) == 3) width = getBits(data, queue[i][0]+1, 0x0F)+1;
							else if (getBits(data, queue[i][0]+1, 0x70) == 4) width = 4;
						}
						
						if (queue[i][1] + width == xExtent) {
							//System.out.printf("removed %x at x=%x%n", queue[i][0], xExtent);
							queue[i] = null;
						}
					}
				}
			}
		}

		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.setColor(bg);
		//g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		
		for (int x = 0; x < grid.length; x++) {
			for (int y = 0; y < grid[x].length; y++) {
				switch (grid[x][y][0]) {
				case 1: drawTile(bi, 16*x, 16*y, 0, 0, 16, 16, grid[x][y][1]); break;
				case 2: drawTile(bi, 16*x, 16*y, 17, 0, 16, 16, grid[x][y][1]); break;
				case 3: drawTile(bi, 16*x, 16*y, 34, 0, 16, 16, grid[x][y][1]); break;
				case 4: drawTile(bi, 16*x, 16*y, 51, 0, 16, 16, grid[x][y][1]); break;
				case 5: drawTile(bi, 16*x, 16*y, 68, 0, 16, 16, grid[x][y][1]); break;
				case 6: drawTile(bi, 16*x, 16*y, 85, 0, 16, 16, grid[x][y][1]); break;
				case 7: drawTile(bi, 16*x, 16*y, 102, 0, 16, 16, grid[x][y][1]); break;
				case 8: drawTile(bi, 16*x, 16*y, 119, 0, 16, 16, grid[x][y][1]); break;
				case 9: drawTile(bi, 16*x, 16*y, 0, 17, 16, 16, grid[x][y][1]); break;
				case 10: drawTile(bi, 16*x, 16*y, 17, 17, 16, 16, grid[x][y][1]); break;
				case 11: drawTile(bi, 16*x, 16*y, 34, 17, 16, 16, grid[x][y][1]); break;
				case 12: drawTile(bi, 16*x, 16*y, 51, 17, 16, 16, grid[x][y][1]); break;
				case 13: drawTile(bi, 16*x, 16*y, 68, 17, 16, 16, grid[x][y][1]); break;
				case 14: drawTile(bi, 16*x, 16*y, 85, 17, 16, 16, grid[x][y][1]); break;
				case 15: drawTile(bi, 16*x, 16*y, 102, 17, 16, 16, grid[x][y][1]); break;
				case 16: drawTile(bi, 16*x, 16*y, 119, 17, 16, 16, grid[x][y][1]); break;
				case 17: drawTile(bi, 16*x, 16*y, 0, 34, 16, 16, grid[x][y][1]); break;
				case 18: drawTile(bi, 16*x, 16*y, 17, 34, 16, 16, grid[x][y][1]); break;
				case 19: drawTile(bi, 16*x, 16*y, 34, 34, 16, 16, grid[x][y][1]); break;
				case 20: drawTile(bi, 16*x, 16*y, 51, 34, 16, 16, grid[x][y][1]); break;
				case 21: drawTile(bi, 16*x, 16*y, 68, 34, 16, 16, grid[x][y][1]); break;
				case 22: drawTile(bi, 16*x, 16*y, 85, 34, 16, 16, grid[x][y][1]); break;
				case 23: drawTile(bi, 16*x, 16*y, 119, 34, 16, 16, grid[x][y][1]); break;
				case 24: drawTile(bi, 16*x, 16*y, 0, 51, 16, 16, grid[x][y][1]); break;
				case 25: drawTile(bi, 16*x, 16*y, 17, 51, 16, 16, grid[x][y][1]); break;
				case 26: drawTile(bi, 16*x, 16*y, 34, 51, 16, 16, grid[x][y][1]); break;
				case 27: drawTile(bi, 16*x, 16*y, 51, 51, 16, 16, grid[x][y][1]); break;
				case 28: drawTile(bi, 16*x, 16*y, 68, 51, 16, 16, grid[x][y][1]); break;
				case 29: drawTile(bi, 16*x, 16*y, 85, 51, 16, 16, grid[x][y][1]); break;
				case 30: drawTile(bi, 16*x, 16*y, 102, 51, 16, 16, grid[x][y][1]); break;
				case 31: drawTile(bi, 16*x, 16*y, 136, 0, 16, 16, grid[x][y][1]); break;
				case 32: drawTile(bi, 16*x, 16*y, 136, 16, 16, 16, grid[x][y][1]); break;
				case 33: drawTile(bi, 16*x, 16*y, 0, 97, 16, 16, grid[x][y][1]); break;
				case 34: drawTile(bi, 16*x, 16*y, 17, 97, 16, 16, grid[x][y][1]); break;
				case 35: drawTile(bi, 16*x, 16*y, 34, 97, 16, 16, grid[x][y][1]); break;
				case 36: drawTile(bi, 16*x, 16*y, 51, 97, 16, 16, grid[x][y][1]); break;
				case 37: drawTile(bi, 16*x, 16*y, 68, 97, 16, 16, grid[x][y][1]); break;
				case 38: drawTile(bi, 16*x, 16*y, 85, 97, 16, 16, grid[x][y][1]); break;
				case 39: drawTile(bi, 16*x, 16*y, 102, 97, 16, 16, grid[x][y][1]); break;
				case 40: drawTile(bi, 16*x, 16*y, 119, 97, 16, 16, grid[x][y][1]); break;
				case 41: drawTile(bi, 16*x, 16*y, 136, 97, 16, 16, grid[x][y][1]); break;
				case 42: drawTile(bi, 16*x, 16*y, 0, 114, 16, 16, grid[x][y][1]); break;
				case 43: drawTile(bi, 16*x, 16*y, 17, 114, 16, 16, grid[x][y][1]); break;
				case 44: drawTile(bi, 16*x, 16*y, 34, 114, 16, 16, grid[x][y][1]); break;
				case 45: drawTile(bi, 16*x, 16*y, 51, 114, 16, 16, grid[x][y][1]); break;
				case 46: drawTile(bi, 16*x, 16*y, 85, 114, 16, 16, grid[x][y][1]); break;
				case 47: drawTile(bi, 16*x, 16*y, 102, 114, 16, 16, grid[x][y][1]); break;
				case 48: drawTile(bi, 16*x, 16*y, 119, 114, 16, 16, grid[x][y][1]); break;
				case 49: drawTile(bi, 16*x, 16*y, 136, 114, 16, 16, grid[x][y][1]); break;
				case 50: drawTile(bi, 16*x, 16*y, 0, 131, 16, 16, grid[x][y][1]); break;
				case 51: drawTile(bi, 16*x, 16*y, 17, 131, 16, 16, grid[x][y][1]); break;
				case 52: drawTile(bi, 16*x, 16*y, 34, 131, 16, 16, grid[x][y][1]); break;
				case 53: drawTile(bi, 16*x, 16*y, 85, 131, 16, 16, grid[x][y][1]); break;
				case 54: drawTile(bi, 16*x, 16*y, 102, 131, 16, 16, grid[x][y][1]); break;
				case 55: drawTile(bi, 16*x, 16*y, 119, 131, 16, 16, grid[x][y][1]); break;
				case 56: drawTile(bi, 16*x, 16*y, 136, 131, 16, 16, grid[x][y][1]); break;
				case 57: drawTile(bi, 16*x, 16*y, 0, 148, 16, 16, grid[x][y][1]); break;
				case 58: drawTile(bi, 16*x, 16*y, 17, 148, 16, 16, grid[x][y][1]); break;
				case 59: drawTile(bi, 16*x, 16*y, 34, 148, 16, 16, grid[x][y][1]); break;
				case 60: drawTile(bi, 16*x, 16*y, 51, 148, 16, 16, grid[x][y][1]); break;
				case 61: drawTile(bi, 16*x, 16*y, 68, 148, 16, 16, grid[x][y][1]); break;
				case 62: drawTile(bi, 16*x, 16*y, 85, 148, 16, 16, grid[x][y][1]); break;
				case 63: drawTile(bi, 16*x, 16*y, 102, 148, 16, 16, grid[x][y][1]); break;
				case 64: drawTile(bi, 16*x, 16*y, 119, 148, 16, 16, grid[x][y][1]); break;
				case 65: drawTile(bi, 16*x, 16*y, 136, 148, 16, 16, grid[x][y][1]); break;
				case 66: drawTile(bi, 16*x, 16*y, 0, 190, 16, 16, grid[x][y][1]); break;
				case 67: drawTile(bi, 16*x, 16*y, 17, 190, 16, 16, grid[x][y][1]); break;
				case 68: drawTile(bi, 16*x, 16*y, 34, 190, 16, 16, grid[x][y][1]); break;
				case 69: drawTile(bi, 16*x, 16*y, 51, 190, 16, 16, grid[x][y][1]); break;
				case 70: drawTile(bi, 16*x, 16*y, 68, 190, 16, 16, grid[x][y][1]); break;
				case 71: drawTile(bi, 16*x, 16*y, 85, 190, 16, 16, grid[x][y][1]); break;
				case 72: drawTile(bi, 16*x, 16*y, 0, 207, 16, 16, grid[x][y][1]); break;
				case 73: drawTile(bi, 16*x, 16*y, 17, 207, 16, 16, grid[x][y][1]); break;
				case 74: drawTile(bi, 16*x, 16*y, 34, 207, 16, 16, grid[x][y][1]); break;
				case 75: drawTile(bi, 16*x, 16*y, 51, 207, 16, 16, grid[x][y][1]); break;
				case 76: drawTile(bi, 16*x, 16*y, 68, 207, 16, 16, grid[x][y][1]); break;
				case 77: drawTile(bi, 16*x, 16*y, 85, 207, 16, 16, grid[x][y][1]); break;
				case 78: drawTile(bi, 16*x, 16*y, 85, 224, 16, 16, grid[x][y][1]); break;
				case 79: drawTile(bi, 16*x, 16*y, 85, 241, 16, 16, grid[x][y][1]); break;
				case 80: drawTile(bi, 16*x, 16*y, 136, 190, 16, 16, grid[x][y][1]); break;
				}
			}
		}
		
		return bi;
	}
	
	public static void write(int[][][] grid, int x, int y, int tile, int pal, int... over) {
		for (int i = 0; i < over.length; i++) if (grid[x][y][0] == over[i]) return;
		if (
				(grid[x][y][0] != 34 && grid[x][y][0] != 37 && grid[x][y][0] != 77 && grid[x][y][0] != 78) ||
				tile == 34 || tile == 37
				) {
			grid[x][y] = new int[] {tile, pal};
		}
	}
	
	public static int[][] castleObject = {
			{0, 0, 5, 3, 3, 6, 3, 3, 3, 7, 15},
			{5, 12, 6, 3, 3, 6, 7, 15, 3, 3, 3},
			{5, 3, 6, 7, 15, 6, 3, 3, 3, 7, 15},
			{5, 14, 6, 3, 3, 6, 7, 15, 3, 3, 3},
			{0, 0, 5, 3, 3, 6, 3, 3, 3, 7, 15},
	};
	
	public static int[][][][] scenerys = {
			{ // none
				{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},
				{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},
				{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},
			}, { // clouds
				{{10,68},{11,74}},{},{},{{2,66},{3,72}},{{2,67},{3,73}},{{2,67},{3,73}},{{2,68},{3,74}},{},{},{{6,66},{7,72}},{{6,67},{7,73}},{{6,68},{7,74}},{},{},{},{},
				{},{},{{1,66},{2,72}},{{1,67},{2,73}},{{1,67},{2,73}},{{1,68},{2,74}},{},{},{},{},{},{},{{10,66},{11,72}},{{10,67},{11,73}},{{10,68},{11,74}},{},
				{},{},{},{{6,66},{7,72}},{{6,67},{7,73}},{{6,68},{7,74}},{{5,66},{6,72}},{{5,67},{6,73}},{{5,68},{6,74}},{},{},{},{},{},{{10,66},{11,72}},{{10,67},{11,73}},
			}, { // mountains
				{{11,57}},{{10,57},{11,58}},{{9,52},{10,58},{11,59}},{{10,61},{11,60}},{{11,61}},{},{},{},{{2,66},{3,72}},{{2,67},{3,73}},{{2,68},{3,74}},{{11,42}},{{11,43}},{{11,43}},{{11,43}},{{11,44}},
				{{11,57}},{{10,52},{11,58}},{{11,61}},{{1,66},{2,72}},{{1,67},{2,73}},{{1,68},{2,74}},{},{{11,42}},{{11,43}},{{11,44}},{},{{2,66},{3,72}},{{2,67},{3,73}},{{2,67},{3,73}},{{2,67},{3,73}},{{2,68},{3,74}},
				{},{},{},{},{{1,66},{2,72}},{{1,67},{2,73}},{{1,67},{2,73}},{{1,68},{2,74}},{},{{11,42}},{{11,43}},{{11,43}},{{11,44}},{},{},{},
			}, { // fence
				{{2,66},{3,72}},{{2,67},{3,73}},{{2,67},{3,73}},{{2,68},{3,74}},{},{},{},{},{},{},{},{{10,46},{11,25}},{},{{9,39},{10,47},{11,25}},{{11,26}},{{11,26}},
				{{11,26}},{{11,26}},{{2,66},{3,72}},{{2,67},{3,73}},{{2,68},{3,74}},{{9,39},{10,47},{11,25}},{},{{10,46},{11,25}},{{10,46},{11,25}},{},{},{{1,66},{2,72}},{{1,67},{2,73}},{{1,68},{2,74}},{{2,66},{3,72}},{{2,67},{3,73}},
				{{2,67},{3,73}},{{2,68},{3,74}},{},{},{},{},{{11,26}},{{11,26}},{{10,46},{11,25}},{{11,26}},{},{{9,39},{10,47},{11,25}},{},{{1,66},{2,72}},{{1,67},{2,73}},{{1,68},{2,74}},
			}
	};
	
	public static int[][][] backdrops = {
			{ // none
				
			}, { // underwater
				{1,69},{2,75},{3,75},{4,75},{5,75},{6,75},{7,75},{8,75},{9,75},{10,75},{11,75},{12,17},{13,17},
			}, { // wall
				{6,5},{7,3},{8,3},{9,3},{10,3},{11,3},
			}, { // over water
				{12,69},{13,75},
			},
			
	};
	
	public static int[][] floors = {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1},
			{0,1,0,0,0,0,0,0,0,0,0,0,1,1},
			{0,1,1,1,0,0,0,0,0,0,0,0,1,1},
			{0,1,1,1,1,0,0,0,0,0,0,0,1,1},
			{0,1,1,1,1,1,1,1,1,0,0,0,1,1},
			{0,1,0,0,0,0,0,0,0,1,1,1,1,1},
			{0,1,1,1,0,0,0,0,0,1,1,1,1,1},
			{0,1,1,1,1,0,0,0,0,1,1,1,1,1},
			{0,1,0,0,0,0,0,0,1,1,1,1,1,1},
			{0,1,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,1,1,1,1,0,0,0,1,1,1,1,1,1},
			{0,1,0,0,0,0,1,1,1,1,1,1,1,1},
			{0,1,0,0,1,1,1,1,1,0,0,0,1,1},
			{0,1,0,0,0,1,1,1,1,0,0,0,1,1},
			{0,1,1,1,1,1,1,1,1,1,1,1,1,1},
	};
	
	public static void listTileData(byte[] data, int ptr, int palette) {
		if (ptr + 1 >= data.length) return;

		System.out.printf("%2x %2x ", getByte(data, ptr), getByte(data, ptr+1));

		int time = getBits(data, ptr, 0xC0);
		int startPos = getBits(data, ptr, 0x1C);
		int levelMod = getBits(data, ptr, 0x07);
		int specPlat = getBits(data, ptr+1, 0xC0);
		int scenery = getBits(data, ptr+1, 0x30);
		int floor = getBits(data, ptr+1, 0x0F);		
		ptr += 2;
		
		if (levelMod == 7) palette = 3;
		
		System.out.printf("Level header: time=%d, startPos=%d, levelMod=%d, specPlat=%d, scenery=%d, floor=%d%n", time, startPos, levelMod, specPlat, scenery, floor);

		while (ptr < data.length && getByte(data, ptr) != 0xFD) {
			System.out.printf("%2x %2x ", getByte(data, ptr), getByte(data, ptr+1));
			
			int xpos = getBits(data, ptr, 0xF0);
			int ypos = getBits(data, ptr, 0x0F);
			int nextPage = getBits(data, ptr+1, 0x80);
			
			if (ypos == 15) {
				int type = getBits(data, ptr+1, 0x70);
				int height = getBits(data, ptr+1, 0x0F);
				
				System.out.printf("  Y15 OBJ: xpos=%d, next=%d, type=%d, height=%d%n", xpos, nextPage, type, height);
				
			} else if (ypos == 14) {
				if (getBits(data, ptr+1, 0x40) == 1) {
					int levelMod2 = getBits(data, ptr+1, 0x07);
					
					System.out.printf("  Y14 Level modifier: xpos=%d, next=%d, levelMod=%d%n", xpos, nextPage, levelMod2);
					
				} else {
					int scenery2 = getBits(data, ptr+1, 0x30);
					int floor2 = getBits(data, ptr+1, 0x0F);
					
					System.out.printf("  Y14 Floor pattern/Scenery: xpos=%d, next=%d, scenery=%d, floor=%d%n", xpos, nextPage, scenery2, floor2);
					
				}
			} else if (ypos == 13) {
				if (getBits(data, ptr+1, 0x40) == 1) {
					int objType = getBits(data, ptr+1, 0x3F);
					
					System.out.printf("  Y13 OBJ: xpos=%d, next=%d, type=%d%n", xpos, nextPage, objType);
					
				} else {
					int pageSet = getBits(data, ptr+1, 0x1F);
					
					System.out.printf("  Y13 Page Set: xpos=%d, next=%d, page=%d", xpos, nextPage, pageSet);
					
				}
			} else if (ypos == 12) {
				int objType = getBits(data, ptr+1, 0x70);
				int width = getBits(data, ptr+1, 0x0F);
				
				System.out.printf("  Y12 OBJ: xpos=%d, next=%d, type=%d, width=%d%n", xpos, nextPage, objType, width);
				
			} else {
				int objType = getBits(data, ptr+1, 0x70);
				if (objType == 7) {
					int exitEnable = getBits(data, ptr+1, 0x08);
					int length = getBits(data, ptr+1, 0x07);
					
					System.out.printf("  Pipe OBJ: xpos=%d, ypos=%d, next=%d, exit=%d, length=%d%n", xpos, ypos, nextPage, exitEnable, length);
					
				} else if (objType == 0) {
					int objType2 = getBits(data, ptr+1, 0x0F);
					
					System.out.printf("  Static OBJ: xpos=%d, ypos=%d, next=%d, type=%d%n", xpos, ypos, nextPage, objType2);
					
				} else {
					int length = getBits(data, ptr+1, 0x0F);
					
					System.out.printf("  Standard OBJ: xpos=%d, ypos=%d, next=%d, length=%d%n", xpos, ypos, nextPage, length);
					
				}
			}
			
			ptr += 2;
		}
	}
}