package com.isofrieze;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.isofrieze.SMBLevelDrawer.LevelType;

public class SpriteSheetManager {
	
	enum PaletteModifier {
		NORMAL, ORANGE, SNOW;
	}
	
	public enum Tile {
		DIRT, SQUARE_BLOCK, SEAFLOOR, CASTLE_MASONRY, USED_BLOCK,
		BRICK_SHINY, BRICK_DULL, CASTLE_WINDOW_LEFT,
		CASTLE_WINDOW_RIGHT, CASTLE_CRENEL, CASTLE_CRENEL_FILLED,
		CASTLE_DOOR_TOP, CASTLE_DOOR_BOTTOM, CASTLE_BRICK,
		TREE_TRUNK, BRIDGE_FLOOR, SPRING_BASE,
		WATER_PIPE_TOP, WATER_PIPE_BOTTOM,
		BULLET_CANNON, BULLET_SKULL, BULLET_SHAFT,
		BG_TREE_TRUNK, BG_FENCE, MUSHROOM_STEM_TOP,
		MUSHROOM_STEM_BOTTOM, PULLEY_LEFT, PULLEY_RIGHT,
		PULLEY_ACROSS, PULLEY_DOWN, UNUSED_FLAG_BALL,
		TREE_LEFT, TREE_MIDDLE, TREE_RIGHT,
		MUSHROOM_LEFT, MUSHROOM_MIDDLE, MUSHROOM_RIGHT,
		BG_BUSH_LEFT, BG_BUSH_MIDDLE, BG_BUSH_RIGHT,
		PIPE_LIP_LEFT, PIPE_LIP_RIGHT, PIPE_LIP_ENTERABLE_LEFT,
		PIPE_LIP_ENTERABLE_RIGHT, PIPE_SHAFT_LEFT,
		PIPE_SHAFT_RIGHT, PIPE_LIP_TOP, PIPE_LIP_BOTTOM,
		PIPE_SHAFT_TOP, PIPE_SHAFT_BOTTOM,
		PIPE_CONNECTION_TOP, PIPE_CONNECTION_BOTTOM,
		CHAIN, CORAL, BRIDGE_ROPES, FLAGPOLE, FLAGPOLE_BALL,
		BG_HILL_LEFT, BG_HILL_RIGHT, BG_HILL_SPOT_LEFT,
		BG_HILL_SPOT_RIGHT, BG_HILL_FILL, BG_HILL_TOP,
		BG_TREE_TOP, BG_TREE_BOTTOM, BG_TREE_SMALL,
		BG_CLOUD_TOP_LEFT, BG_CLOUD_TOP_MIDDLE, BG_CLOUD_TOP_RIGHT,
		BG_CLOUD_BOTTOM_LEFT, BG_CLOUD_BOTTOM_MIDDLE,
		BG_CLOUD_BOTTOM_RIGHT, CLOUD, BOWSER_BRIDGE,
		WATER_TOP, WATER_BOTTOM, LONG_CLOUD_LEFT,
		LONG_CLOUD_MIDDLE, LONG_CLOUD_RIGHT,
		QUESTION_BLOCK_COIN, COIN, COIN_WATER, AXE,
		QUESTION_BLOCK_FIREFLOWER, QUESTION_BLOCK_POISON_MUSHROOM,
		INVISIBLE_BLOCK_COIN, INVISIBLE_BLOCK_LIFE_MUSHROOM,
		INVISIBLE_BLOCK_FIREFLOWER, INVISIBLE_BLOCK_POISON_MUSHROOM,
		BRICK_DULL_FIREFLOWER, BRICK_DULL_VINE, BRICK_DULL_STAR,
		BRICK_DULL_MULTICOIN, BRICK_DULL_POISON_MUSHROOM,
		BRICK_DULL_LIFE_MUSHROOM, BRICK_SHINY_FIREFLOWER,
		BRICK_SHINY_VINE, BRICK_SHINY_STAR, BRICK_SHINY_MULTICOIN,
		BRICK_SHINY_POISON_MUSHROOM, BRICK_SHINY_LIFE_MUSHROOM;
	}
	
	public class TileInstance {
		Tile tile;
		
		// position of the tile (tile resolution)
		int x, y;
		
		public TileInstance(Tile t, int x, int y) {
			this.x = x;
			this.y = y;
			this.tile = t;
		}
	}
	
	public enum Sprite {
		GREEN_KOOPA, RED_KOOPA, BUZZY_BEETLE, HAMMER_BRO,
		GOOMBA, BLOOPER, BULLET_BILL, GREEN_CHEEP_CHEEP,
		RED_CHEEP_CHEEP, PODOBOO, GREEN_PIRANHA_PLANT,
		UPSIDE_DOWN_GREEN_PIRANHA_PLANT, RED_PIRANHA_PLANT,
		UPSIDE_DOWN_RED_PIRANHA_PLANT, GREEN_PARAKOOPA,
		RED_PARAKOOPA, LAKITU, SPINY, SPINY_EGG,
		BOWSER_FIREBALL, FIREWORK, FIREBAR, LONG_FIREBAR,
		SHORT_LIFT, MEDIUM_LIFT, LONG_LIFT,
		SHORT_CLOUDS, MEDIUM_CLOUDS, LONG_CLOUDS,
		BOWSER, SUPER_MUSHROOM, POISON_MUSHROOM, LIFE_MUSHROOM,
		FIRE_FLOWER, SUPER_STAR, VINE, COIN, WIND,
		FLAGPOLE_FLAG, CASTLE_FLAG, RED_SPRINGBOARD,
		GREEN_SPRINGBOARD, TOAD, PEACH, DOOR,
		ANN_NPC_1, ANN_NPC_2, ANN_NPC_3, ANN_NPC_4,
		ANN_NPC_5, ANN_NPC_6, ANN_NPC_7, ANN_NPC_8;
	}
	
	public class SpriteInstance {
		Sprite sprite;
		
		// position of the sprite (piexl resolution)
		int x, y;
		
		public SpriteInstance(Sprite s, int x, int y) {
			this.x = x;
			this.y = y;
			this.sprite = s;
		}
	}
	
	private BufferedImage tilesUncolored, spritesUncolored, palettes;
	private BufferedImage tiles, sprites;
	private LevelType tilePalette, spritePalette;
	private PaletteModifier tilePaletteModifier;
	
	public SpriteSheetManager() {
		String s = SMBLevelDrawer.game.getAbbreviation();
		
		try {
			tilesUncolored = ImageIO.read(new File("res/"+s+"/tiles.png"));
			spritesUncolored = ImageIO.read(new File("res/"+s+"/sprites.png"));
			palettes = ImageIO.read(new File("res/palettes.png"));
		} catch (IOException e) {
			System.err.println("Couldn't load spritesheets!");
			e.printStackTrace();
		}
		
		setPalettes(LevelType.OVERWORLD, PaletteModifier.NORMAL, LevelType.OVERWORLD);
	}
	
	public void setPalettes(LevelType tilePalette, PaletteModifier tilePaletteModifier, LevelType spritePalette) {
		setTilePalette(tilePalette, tilePaletteModifier);
		setSpritePalette(spritePalette);
	}
	
	public void setTilePalette(LevelType tilePalette, PaletteModifier tilePaletteModifier) {
		Color[][] colors = new Color[4][3];
		
		for (int i = 0; i < colors.length; i++) {
			for (int j = 0; j < colors[i].length; j++) {
				// Grab colors from palettes.png
				colors[i][j] = new Color(palettes.getRGB(0x40*tilePalette.getNum() + 0x10*j + 0x08, 0x10*i + 0x08));
			}
		}
		
		// Overwrite tile palette 1 if orange or snow
		if (tilePaletteModifier == PaletteModifier.ORANGE) {
			for (int i = 0; i < colors[1].length; i++) {
				colors[1][i] = new Color(palettes.getRGB(0x10*i + 0x48, 0x58));
			}
		} else if (tilePaletteModifier == PaletteModifier.SNOW) {
			for (int i = 0; i < colors[1].length; i++) {
				colors[1][i] = new Color(palettes.getRGB(0x10*i + 0x48, 0x68));
			}
		}
		
		tiles = new BufferedImage(tilesUncolored.getWidth(), tilesUncolored.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		
		for (int y = 0; y < tiles.getHeight(); y++) {
			for (int x = 0; x < tiles.getWidth(); x++) {
				// get the raw grayscale color from the spritesheet
				int raw = tilesUncolored.getRGB(x, y);
				
				// if the pixel is not transparent
				if (raw < 0) {
					// figure out what index it should be
					int idx = raw == -0x01000000 ? 2 : raw == -1 ? 0 : 1;
					
					// use that index into the color palette and write to the colored image
					tiles.setRGB(x, y, colors[y/0x40][idx].getRGB());
				}
			}
		}
	}
	
	public void setSpritePalette(LevelType spritePalette) {		
		Color[][] colors = new Color[4][3];
		
		for (int i = 0; i < colors.length; i++) {
			for (int j = 0; j < colors[i].length; j++) {
				// Grab colors from palettes.png
				colors[i][j] = new Color(palettes.getRGB(0x40*spritePalette.getNum() + 0x10*j + 0x08, 0x10*i + 0x88));
			}
		}
		
		sprites = new BufferedImage(spritesUncolored.getWidth(), spritesUncolored.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		
		for (int y = 0; y < sprites.getHeight(); y++) {
			for (int x = 0; x < sprites.getWidth(); x++) {
				// get the raw grayscale color from the spritesheet
				int raw = spritesUncolored.getRGB(x, y);
				
				// if the pixel is not transparent
				// and we aren't dealing with ANN celebrities
				if (raw < 0 && y < 0xC0) {
					// figure out what index it should be
					int idx = raw == -0x01000000 ? 0 : raw == -1 ? 1 : 2;
					
					// use that index into the color palette and write to the colored image
					sprites.setRGB(x, y, colors[y/0x40][idx].getRGB());
				}
			}
		}
	}
	
}