package ChunkUtilities;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import Block_Entities.Block_Types;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ChunkMesh {
    private int vaoID;
    private int vboID;
    private int eboID;
    private int vertexCount;

    // Store the chunk size and voxel size for later use
    private static final int CHUNK_SIZE = Chunk.getChunkSize();
    private static final float VOXEL_SIZE = Chunk.VoxelSize;

    // A list to store the vertices, indices, and texture data
    private List<Float> vertices;
    private List<Integer> indices;
    private List<Integer> textureIDs; // Store texture IDs for each face

    public ChunkMesh(Chunk chunk) {
        // Initialize lists
        vertices = new ArrayList<>();
        indices = new ArrayList<>();
        textureIDs = new ArrayList<>();

        // Build the mesh for the entire chunk
        buildChunkMesh(chunk);
    }
    
    private void clearMeshData() {
        // Clear the vertex, index, and texture ID lists
        vertices.clear();
        indices.clear();
        textureIDs.clear();

        // Delete existing OpenGL buffers
        if (vboID != 0) {
            GL15.glDeleteBuffers(vboID);
            vboID = 0;
        }
        if (eboID != 0) {
            GL15.glDeleteBuffers(eboID);
            eboID = 0;
        }
        if (vaoID != 0) {
            GL30.glDeleteVertexArrays(vaoID);
            vaoID = 0;
        }
    }
    
    // Build the entire chunk mesh
    private void buildChunkMesh(Chunk chunk) {
        clearMeshData();

        // Clear previous data
        vertices.clear();
        indices.clear();
        textureIDs.clear();

        int indexCount = 0;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Voxel voxel = chunk.getVoxel(x, y, z);
                    if (voxel != null && voxel.isSolid()) {
                        // Add faces for visible voxels
                        indexCount = addVoxelFaces(chunk, x, y, z, voxel.getBlockID(), indexCount);
                    }
                }
            }
        }

        // Update OpenGL buffers
        updateOpenGLBuffers();
    }

    // Update the OpenGL buffers (VBO, EBO) with the new vertices and indices
    private void updateOpenGLBuffers() {
        if (vertices.isEmpty()) {
            System.out.println("No vertices found for the mesh.");
            return;
        }

        // Convert List<Float> to float[]
        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(verticesArray.length);
        verticesBuffer.put(verticesArray).flip();

        // Convert List<Integer> to int[]
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indicesArray.length);
        indicesBuffer.put(indicesArray).flip();

        // Generate OpenGL buffers
        if (vaoID == 0) vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        if (vboID == 0) vboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);

        // Use 5 floats per vertex (3 for position, 2 for texture coordinates)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0); // Position
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES); // Texture coordinates
        GL20.glEnableVertexAttribArray(1);

        if (eboID == 0) eboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        vertexCount = indices.size();

        // Unbind VAO
        GL30.glBindVertexArray(0);

        // Clean up
        MemoryUtil.memFree(verticesBuffer);
        MemoryUtil.memFree(indicesBuffer);

        System.out.println("Mesh built with vertex count: " + vertexCount);
    }

    // Add voxel faces with texture coordinates
    private int addVoxelFaces(Chunk chunk, int x, int y, int z, int blockID, int indexCount) {
        float xPos = x * VOXEL_SIZE;
        float yPos = y * VOXEL_SIZE;
        float zPos = z * VOXEL_SIZE;

        // Get the block type to retrieve the appropriate textures
        Block_Types blockType = Block_Types.getBlockTypeByID(blockID);
        if (blockType == null) {
            System.out.println("Block type not found for blockID: " + blockID);
            return indexCount; // Skip this block if no texture is found
        }

        // Retrieve textures for this block
        String[] textures = blockType.getTextures();

        // Add faces if adjacent space is empty or outside chunk bounds
        if (shouldAddFace(chunk, x, y, z, 0, 0, 1)) {
            addFaceForDirection(xPos, yPos, zPos, textures[0], 0, 0, 1);
            indexCount += 6; // Front face
        }
        if (shouldAddFace(chunk, x, y, z, 0, 0, -1)) {
            addFaceForDirection(xPos, yPos, zPos, textures[1], 0, 0, -1);
            indexCount += 6; // Back face
        }
        if (shouldAddFace(chunk, x, y, z, -1, 0, 0)) {
            addFaceForDirection(xPos, yPos, zPos, textures[2], -1, 0, 0);
            indexCount += 6; // Left face
        }
        if (shouldAddFace(chunk, x, y, z, 1, 0, 0)) {
            addFaceForDirection(xPos, yPos, zPos, textures[3], 1, 0, 0);
            indexCount += 6; // Right face
        }
        if (shouldAddFace(chunk, x, y, z, 0, -1, 0)) {
            addFaceForDirection(xPos, yPos, zPos, textures[4], 0, -1, 0);
            indexCount += 6; // Bottom face
        }
        if (shouldAddFace(chunk, x, y, z, 0, 1, 0)) {
            addFaceForDirection(xPos, yPos, zPos, textures[5], 0, 1, 0);
            indexCount += 6; // Top face
        }

        return indexCount;
    }

    // Determines if a face should be added based on the adjacent voxel
    private boolean shouldAddFace(Chunk chunk, int x, int y, int z, int dx, int dy, int dz) {
        // Determine adjacent voxel coordinates
        int nx = x + dx;
        int ny = y + dy;
        int nz = z + dz;

        // Check if adjacent voxel is within bounds and solid
        if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE && nz >= 0 && nz < CHUNK_SIZE) {
            Voxel adjacentVoxel = chunk.getVoxel(nx, ny, nz);
            return adjacentVoxel == null || !adjacentVoxel.isSolid(); // Return true if adjacent voxel is empty or not solid
        } else {
            // Adjacent voxel is out of bounds, so the face should be visible
            return true;
        }
    }

    private void addFaceForDirection(float xPos, float yPos, float zPos, String texturePath, int dx, int dy, int dz) {
        // Load texture and store the texture ID
        int textureID = TextureLoader.loadTexture(texturePath);
        textureIDs.add(textureID); 

        // Get face vertices and texture coordinates
        float[] faceVertices = getFaceVertices(xPos, yPos, zPos, dx, dy, dz);
        float[] texCoords = getDefaultTextureCoords(); // Use default texture coordinates

        // Calculate starting index for the face's vertices
        int currentIndex = vertices.size() / 5; // 5 floats per vertex (3 for position + 2 for UV)

        // Add face vertices and texture coordinates
        for (int i = 0; i < 4; i++) {
            vertices.add(faceVertices[i * 3]);     // x
            vertices.add(faceVertices[i * 3 + 1]); // y
            vertices.add(faceVertices[i * 3 + 2]); // z
            vertices.add(texCoords[i * 2]);        // u
            vertices.add(texCoords[i * 2 + 1]);    // v
        }

        // Indices for the two triangles that make up the face
        indices.add(currentIndex);
        indices.add(currentIndex + 1);
        indices.add(currentIndex + 2);
        indices.add(currentIndex + 2);
        indices.add(currentIndex + 3);
        indices.add(currentIndex);
    }

    public void logVoxelState(Chunk chunk, int x, int y, int z) {
        Voxel voxel = chunk.getVoxel(x, y, z);
        if (voxel != null) {
            System.out.println("Voxel at (" + x + ", " + y + ", " + z + ") isSolid: " + voxel.isSolid() + ", BlockID: " + voxel.getBlockID());
        } else {
            System.out.println("Voxel at (" + x + ", " + y + ", " + z + ") is null.");
        }
    }

    private float[] getFaceVertices(float xPos, float yPos, float zPos, int dx, int dy, int dz) {
        // Define vertex positions for each face based on direction
        if (dz == 1) { // Front face
            return new float[]{
                xPos, yPos, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE,
                xPos, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE
            };
        } else if (dz == -1) { // Back face
            return new float[]{
                xPos, yPos + VOXEL_SIZE, zPos,
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos,
                xPos + VOXEL_SIZE, yPos, zPos,
                xPos, yPos, zPos
            };
        } else if (dx == -1) { // Left face
            return new float[]{
                xPos, yPos, zPos + VOXEL_SIZE,
                xPos, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE,
                xPos, yPos + VOXEL_SIZE, zPos,
                xPos, yPos, zPos
            };
        } else if (dx == 1) { // Right face
            return new float[]{
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos,
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos, zPos
            };
        } else if (dy == -1) { // Bottom face
            return new float[]{
                xPos, yPos, zPos,
                xPos + VOXEL_SIZE, yPos, zPos,
                xPos + VOXEL_SIZE, yPos, zPos + VOXEL_SIZE,
                xPos, yPos, zPos + VOXEL_SIZE
            };
        } else if (dy == 1) { // Top face
            return new float[]{
                xPos, yPos + VOXEL_SIZE, zPos,
                xPos, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos + VOXEL_SIZE,
                xPos + VOXEL_SIZE, yPos + VOXEL_SIZE, zPos
            };
        }
        return new float[0];
    }

    private float[] getDefaultTextureCoords() {
        // Fixed texture coordinates to ensure no texture swapping among blocks
        return new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };
    }

    public void render() {
        if (vertexCount == 0) {
            System.out.println("No vertex to render");
            return;
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL30.glBindVertexArray(vaoID);

        // Render each face using its texture
        int faceCount = indices.size() / 6; // Each face has 6 indices
        for (int i = 0; i < faceCount; i++) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureIDs.get(i));
            GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, i * 6 * Integer.BYTES);
        }

        GL30.glBindVertexArray(0);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboID);
        GL15.glDeleteBuffers(eboID);
        GL30.glDeleteVertexArrays(vaoID);
    }

    public void updateVoxel(Chunk chunk, int x, int y, int z, int blockID) {
        // Set the voxel in the chunk
        // chunk.setVoxel(x, y, z, new Voxel(blockID));

        // Rebuild the entire chunk mesh to maintain consistency
        buildChunkMesh(chunk);
    }
}
