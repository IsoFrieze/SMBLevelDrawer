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
	
	// (x position, y position, width, height, palette, origin x, origin y)
	// according to the spritesheet
	public enum Sprite {
		GREEN_KOOPA(0,8,2,3,1,0,8), RED_KOOPA(0,18,2,3,2,0,8), BUZZY_BEETLE(2,0,2,2,0,0,0), HAMMER_BRO(0,11,2,3,1,0,8),
		GOOMBA(0,0,2,2,0,0,0), BLOOPER(4,0,2,3,0,0,8), BULLET_BILL(6,0,2,2,0,0,0), GREEN_CHEEP_CHEEP(2,12,2,2,1,0,0),
		RED_CHEEP_CHEEP(8,18,2,2,2,0,0), PODOBOO(10,18,2,2,2,0,0), GREEN_PIRANHA_PLANT(4,8,2,3,1,-8,8),
		UPSIDE_DOWN_GREEN_PIRANHA_PLANT(4,11,2,3,1,-8,0), RED_PIRANHA_PLANT(4,18,2,3,2,-8,8),
		UPSIDE_DOWN_RED_PIRANHA_PLANT(4,21,2,3,2,-8,0), GREEN_PARAKOOPA(2,8,2,3,1,0,8),
		RED_PARAKOOPA(2,18,2,3,2,0,8), LAKITU(6,8,2,3,1,0,8), SPINY(10,16,2,2,2,0,0), SPINY_EGG(8,16,2,2,2,0,0),
		BOWSER_FIREBALL(1,21,3,1,2,0,0), FIREWORK(20,18,2,2,2,0,0), FIREBAR(6,16,1,6,2,-4,12), LONG_FIREBAR(6,16,1,6,2,-4,12),
		SHORT_LIFT(0,16,3,1,2,-12,15), MEDIUM_LIFT(0,16,4,1,2,0,15), LONG_LIFT(0,16,6,1,2,0,15),
		SHORT_CLOUDS(0,17,3,1,2,-12,15), MEDIUM_CLOUDS(0,17,4,1,2,0,15), LONG_CLOUDS(0,17,6,1,2,0,15),
		BOWSER(8,8,4,4,1,0,16), SUPER_MUSHROOM(18,18,2,2,2,0,0), POISON_MUSHROOM(8,0,2,2,0,0,0), LIFE_MUSHROOM(16,10,2,2,1,0,0),
		FIRE_FLOWER(16,8,2,2,1,0,0), SUPER_STAR(18,16,2,2,2,0,0), VINE(14,10,2,2,1,0,0), COIN(18,20,2,2,2,0,0),
		WIND(12,8,2,1,1,0,0), FLAGPOLE_FLAG(14,8,2,2,1,8,-1), CASTLE_FLAG(20,16,2,2,2,0,0), RED_SPRINGBOARD(16,17,2,3,2,0,-1),
		GREEN_SPRINGBOARD(12,9,2,3,1,0,-1), TOAD(12,17,2,3,2,0,8), PEACH(14,17,2,3,2,0,8), DOOR(14,20,2,3,2,0,8),
		ANN_NPC_1(0,24,2,3,2,0,8), ANN_NPC_2(2,24,2,3,2,0,8), ANN_NPC_3(4,24,2,3,2,0,8), ANN_NPC_4(6,24,2,3,2,0,8),
		ANN_NPC_5(8,24,2,3,2,0,8), ANN_NPC_6(10,24,2,3,2,0,8), ANN_NPC_7(12,24,2,3,2,0,8), ANN_NPC_8(14,24,2,3,2,0,8),
		ANN_NPC_9(12,20,2,3,2,0,8);

		// the position of the sprite in the sprite sheet
		int x, y;
		
		// the size of the sprite in the sprite sheet
		int w, h;
		
		// the origin of this sprite in pixels relative to its top left corner
		int ox, oy;
		
		// the palette this sprite uses
		int palette;
		
		Sprite(int x, int y, int w, int h, int palette, int ox, int oy) {
			this.x = 8 * x;
			this.y = 8 * y;
			this.w = 8 * w;
			this.h = 8 * h;
			this.ox = ox;
			this.oy = oy;
			this.palette = palette;
		}
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
	
	private BufferedImage tiles, sprites, palettes;
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
			tiles = ImageIO.read(new File("res/"+s+"/tiles.png"));
			sprites = ImageIO.read(new File("res/"+s+"/sprites.png"));
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
				// colors are swapped on the sprite sheet because I think it looks better
				int k = (j % 3) + 1;
				
				spritePalettes[i][j] = new Color(palettes.getRGB(0x10 * (4 * mainPalette.getNum() + k), 0x10 * (8 + i)));
			}
		}
		
		bowserPalette = new Color[4];
		for (int i = 0; i < bowserPalette.length; i++) {
			// colors are swapped on the sprite sheet because I think it looks better
			int k = (i % 3) + 1;
			
			bowserPalette[i] = new Color(palettes.getRGB(0x10 * (4 + k), 0x10 * 9));
		}
		
		celebPalettes = new Color[4][4];
		for (int i = 0; i < celebPalettes.length; i++) {
			for (int j = 0; j < celebPalettes[i].length; j++) {
				// colors are swapped on the sprite sheet because I think it looks better
				int k = (i % 3) + 1;
				
				celebPalettes[i][j] = new Color(palettes.getRGB(0x10 * (4 + k), 0x10 * (12 + i)));
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
		return getGraphicsAndRecolor(tiles, tile.x, tile.y, 0x10, 0x10, tilePalettes[tile.palette]);
	}

	// draw the sprite to the image associated with this graphics object
	public void drawSprite(Graphics2D g, SpriteInstance sprite) {
		g.drawImage(getSpriteFromSheet(sprite.sprite), sprite.x - sprite.sprite.ox, sprite.y - sprite.sprite.oy, null);
		
		// the big firebar is so big that it's two sprites (my spritesheet is too smol)
		// TODO maybe handle all sprites that use multiple of the same graphic like this
		// (firebars, lifts)
		if (sprite.sprite == Sprite.LONG_FIREBAR) {
			g.drawImage(getSpriteFromSheet(sprite.sprite), sprite.x - sprite.sprite.ox, sprite.y - sprite.sprite.oy + 48, null);
		}
	}
	
	private BufferedImage getSpriteFromSheet(Sprite sprite) {
		Color[] palette = spritePalettes[sprite.palette];
		
		// bowser always uses the bright green palette regardless of level setting
		// TODO except not always? Only the real bowser on the bridge? I think it may actually be the bridge
		// or axe or something that changes the palette, in that case oof
		if (sprite == Sprite.BOWSER)
			palette = bowserPalette;
			
		// the ANN celebs use special hardcoded palettes
		else if (sprite == Sprite.ANN_NPC_1 || sprite == Sprite.ANN_NPC_4 || sprite == Sprite.ANN_NPC_8)
			palette = celebPalettes[0]; 
		else if (sprite == Sprite.ANN_NPC_2 || sprite == Sprite.ANN_NPC_5)
			palette = celebPalettes[1]; 
		else if (sprite == Sprite.ANN_NPC_3 || sprite == Sprite.ANN_NPC_6)
			palette = celebPalettes[2]; 
		else if (sprite == Sprite.ANN_NPC_7)
			palette = celebPalettes[3]; 
			
		return getGraphicsAndRecolor(sprites, sprite.x, sprite.y, sprite.w, sprite.h, palette);
	}
	
	// given a grayscale source image and a bounding rectangle and palette
	// apply the palette and return that rectangle
	public BufferedImage getGraphicsAndRecolor(BufferedImage src, int x, int y, int width, int height, Color[] palette) {
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				// get the raw grayscale color from the spritesheet
				int raw = src.getRGB(x + i, y + j);
				
				// if the pixel is not transparent
				if (raw < 0) {
					// figure out what index it should be
					int idx = raw == -0x01000000 ? 3 : raw == -1 ? 1 : 2;
					
					// use that index into the color palette and write to the colored image
					out.setRGB(i, j, palette[idx].getRGB());
				}
			}
		}
		
		return out;
	}	
}