package ChunkUtilities;

import Block_Entities.Block_Types;

public class Voxel {
    public static final Voxel AIR = new Voxel(0); // Represents an empty block
    private final int blockID;
   

    public Voxel(int blockID) {
        this.blockID = blockID;
    }

    public boolean isSolid() {
        return Block_Types.getBlockTypeByID(blockID).isSolid();
    }

    public int getBlockID() {
        return blockID;
    }

    public String[] getTextures() {
        Block_Types blockType = Block_Types.getBlockTypeByID(blockID);
        return blockType != null ? blockType.getTextures() : null;
    }
}
