package ChunkUtilities;

public class Chunk {
    private static final int CHUNK_SIZE = 17; // Size of the chunk cube
    private Voxel[][][] voxels;
    private ChunkMesh chunkMesh;
    public static float VoxelSize;

    public Chunk() {
        // Set the voxel size to 1.0f
        VoxelSize = 1f;
        voxels = new Voxel[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        // Initialize the chunk with different layers of blocks (or air)
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (y < 5) { 
                        // Below y = 12, fill with stone
                        voxels[x][y][z] = new Voxel(1); // Block ID 1 for stone
                    } else if (y >= 5 && y < 7) {
                        // Between y = 12 and y = 14, fill with dirt
                        voxels[x][y][z] = new Voxel(2); // Block ID 2 for dirt
                    } else if (y == 7) {
                        // At y = 15, place grass
                        voxels[x][y][z] = new Voxel(3); // Block ID 3 for grass
                    } else {
                        // Above y = 15, leave as air
                        voxels[x][y][z] = new Voxel(0); // Block ID 0 for air
                    }
                }
            }
        }


        // Generate initial mesh
        generateMesh(); // Use the generateMesh method to initialize the mesh
    }

    public Voxel getVoxel(int x, int y, int z) {
        // Check for out-of-bounds access
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            // Return air voxel for out-of-bounds requests
            return Voxel.AIR; 
        }
        // Return the voxel or air if it's null
        return voxels[x][y][z] != null ? voxels[x][y][z] : Voxel.AIR;
    }

    public void setVoxel(int x, int y, int z, Voxel voxel) {
        // Set the voxel in the chunk
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE && z >= 0 && z < CHUNK_SIZE) {
            voxels[x][y][z] = voxel;
            updateMesh(); // Update the mesh after setting a voxel
        }
    }

    // Method to update the mesh after voxel changes
    public void updateMesh() {
        if (chunkMesh != null) {
            chunkMesh.cleanup(); // Clean up the old mesh
        }
        chunkMesh = new ChunkMesh(this); // Generate a new mesh
    }

    public ChunkMesh getChunkMesh() {
        return chunkMesh;
    }

    public static int getChunkSize() {
        return CHUNK_SIZE;
    }

    public ChunkMesh generateMesh() {
        if (chunkMesh != null) {
            chunkMesh.cleanup(); // Clean up the old mesh
        }
        chunkMesh = new ChunkMesh(this); // Rebuild the mesh with the current voxel data
        return chunkMesh;
    }
}
