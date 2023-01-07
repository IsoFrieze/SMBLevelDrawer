package com.isofrieze;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.isofrieze.LevelTileBuilder.BackdropModifier;
import com.isofrieze.LevelTileBuilder.SpecialPlatform;
import com.isofrieze.SMBLevelDrawer.LevelType;

public class SpriteSheetManager {
	
	// whether to display the contents of blocks
	public static boolean CONTENTS = true;
	
	enum PaletteModifier {
		NORMAL, ORANGE, SNOW;
	}
	
	// (x position, y position, palette)
	// according to the spritesheet
	public enum Tile {
		DIRT(0,0,0), SQUARE_BLOCK(1,0,0), SEAFLOOR(2,0,0), CASTLE_MASONRY(3,0,0), USED_BLOCK(4,0,0),
		BRICK_SHINY(0,1,0), BRICK_DULL(2,1,0), CASTLE_WINDOW_LEFT(3,1,0),
		CASTLE_WINDOW_RIGHT(1,1,0), CASTLE_CRENEL(4,1,0), CASTLE_CRENEL_FILLED(5,1,0),
		CASTLE_DOOR_TOP(6,1,0), CASTLE_DOOR_BOTTOM(7,1,0), CASTLE_BRICK(2,1,0),
		TREE_TRUNK(5,0,0), BRIDGE_FLOOR(6,0,0), SPRING_BASE(7,0,0),
		WATER_PIPE_TOP(8,0,0), WATER_PIPE_BOTTOM(8,1,0),
		BULLET_CANNON(9,0,0), BULLET_SKULL(9,1,0), BULLET_SHAFT(9,2,0),
		BG_TREE_TRUNK(0,2,0), BG_FENCE(1,2,0), MUSHROOM_STEM_TOP(2,2,0),
		MUSHROOM_STEM_BOTTOM(3,2,0), PULLEY_LEFT(4,2,0), PULLEY_RIGHT(6,2,0),
		PULLEY_ACROSS(5,2,0), PULLEY_DOWN(7,2,0), UNUSED_FLAG_BALL(8,2,0),
		TREE_LEFT(0,4,1), TREE_MIDDLE(1,4,1), TREE_RIGHT(2,4,1),
		MUSHROOM_LEFT(0,5,1), MUSHROOM_MIDDLE(1,5,1), MUSHROOM_RIGHT(2,5,1),
		BG_BUSH_LEFT(0,6,1), BG_BUSH_MIDDLE(1,6,1), BG_BUSH_RIGHT(2,6,1),
		PIPE_LIP_LEFT(3,4,1), PIPE_LIP_RIGHT(4,4,1), PIPE_LIP_ENTERABLE_LEFT(3,4,1),
		PIPE_LIP_ENTERABLE_RIGHT(4,4,1), PIPE_SHAFT_LEFT(5,4,1),
		PIPE_SHAFT_RIGHT(6,4,1), PIPE_LIP_TOP(3,5,1), PIPE_LIP_BOTTOM(3,6,1),
		PIPE_SHAFT_TOP(4,5,1), PIPE_SHAFT_BOTTOM(4,6,1),
		PIPE_CONNECTION_TOP(5,5,1), PIPE_CONNECTION_BOTTOM(5,6,1),
		CHAIN(6,5,1), CORAL(6,6,1), BRIDGE_ROPES(7,4,1), FLAGPOLE(7,6,1), FLAGPOLE_BALL(7,5,1),
		BG_HILL_LEFT(8,4,1), BG_HILL_RIGHT(8,6,1), BG_HILL_SPOT_LEFT(9,5,1),
		BG_HILL_SPOT_RIGHT(9,4,1), BG_HILL_FILL(8,5,1), BG_HILL_TOP(9,6,1),
		BG_TREE_TOP(10,5,1), BG_TREE_BOTTOM(10,6,1), BG_TREE_SMALL(10,4,1),
		BG_CLOUD_TOP_LEFT(0,8,2), BG_CLOUD_TOP_MIDDLE(1,8,2), BG_CLOUD_TOP_RIGHT(2,8,2),
		BG_CLOUD_BOTTOM_LEFT(0,9,2), BG_CLOUD_BOTTOM_MIDDLE(1,9,2),
		BG_CLOUD_BOTTOM_RIGHT(2,9,2), CLOUD(0,10,2), BOWSER_BRIDGE(1,10,2),
		WATER_TOP(3,8,2), WATER_BOTTOM(3,9,2), LONG_CLOUD_LEFT(4,8,2),
		LONG_CLOUD_MIDDLE(5,8,2), LONG_CLOUD_RIGHT(6,8,2),
		QUESTION_BLOCK_COIN(0,12,3), COIN(1,12,3), COIN_WATER(2,12,3), AXE(0,13,3),
		QUESTION_BLOCK_FIREFLOWER(0,12,3), QUESTION_BLOCK_POISON_MUSHROOM(0,12,3),
		INVISIBLE_BLOCK_COIN(10,0,0), INVISIBLE_BLOCK_LIFE_MUSHROOM(10,0,0),
		INVISIBLE_BLOCK_FIREFLOWER(10,0,0), INVISIBLE_BLOCK_POISON_MUSHROOM(10,0,0),
		BRICK_DULL_FIREFLOWER(2,1,0), BRICK_DULL_VINE(2,1,0), BRICK_DULL_STAR(2,1,0),
		BRICK_DULL_MULTICOIN(2,1,0), BRICK_DULL_POISON_MUSHROOM(2,1,0),
		BRICK_DULL_LIFE_MUSHROOM(2,1,0), BRICK_SHINY_FIREFLOWER(0,1,0),
		BRICK_SHINY_VINE(0,1,0), BRICK_SHINY_STAR(0,1,0), BRICK_SHINY_MULTICOIN(0,1,0),
		BRICK_SHINY_POISON_MUSHROOM(0,1,0), BRICK_SHINY_LIFE_MUSHROOM(0,1,0);
		
		// the position of the tile in the sprite sheet
		int x, y;
		
		// the palette this tile uses
		int palette;
		
		Tile(int x, int y, int palette) {
			this.x = 0x10 * x;
			this.y = 0x10 * y;
			this.palette = palette;
		}
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
	private PaletteModifier tilePaletteModifier;
	private LevelType mainPalette;
	private boolean isNight;
	
	// the entire palette of this level
	private Color[][] tilePalettes, spritePalettes;
	
	// bowser always uses this palette
	private Color[] bowserPalette;
	
	// the ANN celebs always use these palettes
	private Color[][] celebPalettes;
	
	// the background color
	private Color backgroundColor;
	
	// load in the proper resources
	public void initialize() {
		String s = SMBLevelDrawer.game.getAbbreviation();
		
		try {
			tilesUncolored = ImageIO.read(new File("res/"+s+"/tiles.png"));
			spritesUncolored = ImageIO.read(new File("res/"+s+"/sprites.png"));
			palettes = ImageIO.read(new File("res/palettes.png"));
		} catch (IOException e) {
			System.err.println("Couldn't load spritesheets!");
			e.printStackTrace();
		}
	}
	
	public void setPalettes() {
		// the main palette is based off of the level type
		// (or forced to castle palette if gray backdrop is set)
		mainPalette = LevelTileBuilder.backdropPalette == BackdropModifier.GRAY ?
				LevelType.CASTLE : LevelTileBuilder.type;
		
		// the secondary palette is changed via snow backdrop or orange mushroom special platform
		tilePaletteModifier = PaletteModifier.NORMAL;
		if (LevelTileBuilder.backdropPalette == BackdropModifier.SNOW ||
				LevelTileBuilder.backdropPalette == BackdropModifier.NIGHT_SNOW)
			tilePaletteModifier = PaletteModifier.SNOW;
		if (LevelTileBuilder.specialPlatform == SpecialPlatform.ORANGE_MUSHROOM)
			tilePaletteModifier = PaletteModifier.ORANGE;
		
		// background color is based off of level type and backdrop
		isNight = LevelTileBuilder.backdropPalette != BackdropModifier.SNOW &&
				(LevelTileBuilder.type == LevelType.UNDERGROUND || LevelTileBuilder.type == LevelType.CASTLE ||
				LevelTileBuilder.backdropPalette != BackdropModifier.NONE);
		
		tilePalettes = new Color[4][4];
		for (int i = 0; i < tilePalettes.length; i++) {
			for (int j = 0; j < tilePalettes[i].length; j++) {
				tilePalettes[i][j] = new Color(palettes.getRGB(0x10 * (4 * mainPalette.getNum() + j), 0x10 * i));
			}
		}
		
		if (tilePaletteModifier == PaletteModifier.ORANGE) {
			for (int i = 0; i < tilePalettes[1].length; i++)
				tilePalettes[1][i] = new Color(palettes.getRGB(0x10 * (4 + i), 0x10 * 5));
		} else if (tilePaletteModifier == PaletteModifier.SNOW) {
			for (int i = 0; i < tilePalettes[1].length; i++)
				tilePalettes[1][i] = new Color(palettes.getRGB(0x10 * (4 + i), 0x10 * 6));
		}
		
		spritePalettes = new Color[3][4];
		for (int i = 0; i < spritePalettes.length; i++) {
			for (int j = 0; j < spritePalettes[i].length; j++) {
				spritePalettes[i][j] = new Color(palettes.getRGB(0x10 * (4 * mainPalette.getNum() + j), 0x10 * (8 + i)));
			}
		}
		
		bowserPalette = new Color[4];
		for (int i = 0; i < bowserPalette.length; i++) {
			bowserPalette[i] = new Color(palettes.getRGB(0x10 * (4 + i), 0x10 * 9));
		}
		
		celebPalettes = new Color[4][4];
		for (int i = 0; i < celebPalettes.length; i++) {
			for (int j = 0; j < celebPalettes[i].length; j++) {
				celebPalettes[i][j] = new Color(palettes.getRGB(0x10 * (4 + j), 0x10 * (12 + i)));
			}
		}
		
		backgroundColor = new Color(palettes.getRGB(0, 0x10 * (isNight ? 6 : 5)));
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	// draw the tile to the image associated with this graphics object
	public void drawTile(Graphics2D g, TileInstance tile) {
		Tile t = tile.tile;
		int x = 0x10 * tile.x, y = 0x10 * tile.y;
		
		g.drawImage(getTileFromSheet(t), x, y, null);
		
		// draw extra stuff if setting is enabled
		if (CONTENTS) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			
			if (t == Tile.INVISIBLE_BLOCK_COIN || t == Tile.INVISIBLE_BLOCK_POISON_MUSHROOM ||
					t == Tile.INVISIBLE_BLOCK_LIFE_MUSHROOM)
				g.drawImage(getTileFromSheet(Tile.QUESTION_BLOCK_COIN), x, y, null);
			
			if (t == Tile.QUESTION_BLOCK_FIREFLOWER || t == Tile.INVISIBLE_BLOCK_FIREFLOWER ||
					t == Tile.BRICK_SHINY_FIREFLOWER || t == Tile.BRICK_DULL_FIREFLOWER)
				g.drawImage(getSpriteFromSheet(Sprite.FIRE_FLOWER), x, y, null);
			
			if (t == Tile.QUESTION_BLOCK_POISON_MUSHROOM || t == Tile.INVISIBLE_BLOCK_POISON_MUSHROOM ||
					t == Tile.BRICK_SHINY_POISON_MUSHROOM || t == Tile.BRICK_DULL_POISON_MUSHROOM)
				g.drawImage(getSpriteFromSheet(Sprite.POISON_MUSHROOM), x, y, null);
			
			if (t == Tile.INVISIBLE_BLOCK_LIFE_MUSHROOM || t == Tile.BRICK_SHINY_LIFE_MUSHROOM ||
					t == Tile.BRICK_DULL_LIFE_MUSHROOM)
				g.drawImage(getSpriteFromSheet(Sprite.LIFE_MUSHROOM), x, y, null);
			
			if (t == Tile.BRICK_SHINY_VINE ||t == Tile.BRICK_DULL_VINE)
				g.drawImage(getSpriteFromSheet(Sprite.VINE), x, y, null);
			
			if (t == Tile.BRICK_SHINY_STAR ||t == Tile.BRICK_DULL_STAR)
				g.drawImage(getSpriteFromSheet(Sprite.SUPER_STAR), x, y, null);
			
			if (t == Tile.BRICK_SHINY_MULTICOIN ||t == Tile.BRICK_DULL_MULTICOIN)
				g.drawImage(getSpriteFromSheet(Sprite.COIN), x, y, null);
			
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		}
	}
	
	private BufferedImage getTileFromSheet(Tile tile) {
		BufferedImage img = new BufferedImage(0x10, 0x10, BufferedImage.TYPE_4BYTE_ABGR);
		
		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				img.setRGB(i, j, tilesUncolored.getRGB(tile.x + i, tile.y + j));
			}
		}
		
		return img;
	}

	// draw the sprite to the image associated with this graphics object
	// TODO offset sprites positions properly
	public void drawSprite(Graphics2D g, SpriteInstance sprite) {
		g.drawImage(getSpriteFromSheet(sprite.sprite), sprite.x, sprite.y, null);
	}
	
	private BufferedImage getSpriteFromSheet(Sprite sprite) {
		BufferedImage img = new BufferedImage(0x10, 0x10, BufferedImage.TYPE_4BYTE_ABGR);
		
		
		
		return img;
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