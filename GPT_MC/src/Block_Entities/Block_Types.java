package Block_Entities;

import java.util.HashMap;
import java.util.Map;

public class Block_Types {
    private static final Map<Integer, Block_Types> blockRegistry = new HashMap<>();

    private final int blockID;
    private final float destroyTime;
    private final int durability;
    private final int resistance;
    private final String[] textures; // Textures for all six faces
    private final boolean isSolid;
    private final String Name;

    public Block_Types(int blockID, float destroyTime, int durability, int resistance, String[] textures, boolean isSolid, String Name) {
        this.blockID = blockID;
        this.destroyTime = destroyTime;
        this.durability = durability;
        this.resistance = resistance;
        this.textures = textures;
        this.isSolid = isSolid;
        this.Name = Name;

        // Register the block type with its ID
        blockRegistry.put(blockID, this);
    }


	// Getters
    public int getBlockID() {
        return blockID;
    }
    
    public String getName() {
    	return Name;
    }

    public float getDestroyTime() {
        return destroyTime;
    }

    public int getDurability() {
        return durability;
    }

    public int getResistance() {
        return resistance;
    }

    public String[] getTextures() {
        return textures;
    }

    public boolean isSolid() {
        return isSolid;
    }

    // Retrieve a block type by its ID
    public static Block_Types getBlockTypeByID(int blockID) {
    	
        return blockRegistry.get(blockID);
    }

    // Example block registration (to be moved to a static block in the main game initialization)
    public static Block_Types registerDefaultBlocks() {
        // Register stone block
        new Block_Types(1, 1.5f, 30, 15, new String[]{
            "res/textures/Stone.png", 
            "res/textures/Stone.png", 
            "res/textures/Stone.png", 
            "res/textures/Stone.png", 
            "res/textures/Stone.png", 
            "res/textures/Stone.png"
        }, true, "Stone");

        // Register dirt block
        new Block_Types(2, 0.75f, 10, 5, new String[]{
            "res/textures/Dirt.png", 
            "res/textures/Dirt.png", 
            "res/textures/Dirt.png", 
            "res/textures/Dirt.png", 
            "res/textures/Dirt.png", 
            "res/textures/Dirt.png"
        }, true, "Dirt");
        
        new Block_Types(3, 0.75f, 10, 5, new String[]{ 
                "res/textures/GrassTop.png", 
                "res/textures/Dirt.png",
                "res/textures/GrassTop.png", 
                "res/textures/GrassTop.png", 
                "res/textures/GrassTop.png", 
                "res/textures/GrassTop.png"
            }, true, "Grass");
        
        new Block_Types(4, 0.75f, 10, 5, new String[]{ 
                "res/textures/MissingTexture.png",
                "res/textures/MissingTexture.png", 
                "res/textures/MissingTexture.png", 
                "res/textures/MissingTexture.png", 
                "res/textures/MissingTexture.png", 
                "res/textures/MissingTexture.png"
            }, true, "Block 303");

        new Block_Types(0, 0.0f, 0, 0, new String[]{
            "", "", "", "", "", ""
        }, false, "Air");
        
        return null;
        
    }
}
