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
    private int vaoID; // Vertex Array Object ID for storing vertex attribute configurations
    private int vboID; // Vertex Buffer Object ID for storing vertices
    private int eboID; // Element Buffer Object ID for storing indices
    private int vertexCount; // Number of vertices in the mesh

    // Define chunk and voxel size
    private static final int CHUNK_SIZE = Chunk.getChunkSize(); // Static chunk size
    private static final float VOXEL_SIZE = Chunk.VoxelSize; // Static voxel size

    // Lists for storing vertices, indices, and texture IDs
    private List<Float> vertices; // Stores vertex data
    private List<Integer> indices; // Stores indices for drawing faces
    private List<Integer> textureIDs; // Stores texture IDs for each voxel face

    public ChunkMesh(Chunk chunk) {
        // Initialize lists to store mesh data
        vertices = new ArrayList<>();
        indices = new ArrayList<>();
        textureIDs = new ArrayList<>();

        // Build the mesh for the given chunk
        buildChunkMesh(chunk);
    }

    private void clearMeshData() {
        // Clear mesh data lists
        vertices.clear();
        indices.clear();
        textureIDs.clear();

        // Delete OpenGL buffers if they exist
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

    // This method builds the entire mesh for the chunk
    private void buildChunkMesh(Chunk chunk) {
        clearMeshData(); // First clear any existing mesh data

        int indexCount = 0; // Track the current index count for the mesh

        // Loop through each voxel in the chunk
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Voxel voxel = chunk.getVoxel(x, y, z); // Get the voxel at this position
                    // If the voxel exists and is solid, add its faces to the mesh
                    if (voxel.isSolid() != false && voxel != null) {
                        indexCount = addVoxelFaces(chunk, x, y, z, voxel.getBlockID(), indexCount);
                    }
                }
            }
        }

        // Update OpenGL buffers with the vertex and index data
        updateOpenGLBuffers();
    }

    // This method updates OpenGL buffers (VBO, EBO) with the new vertices and indices
    private void updateOpenGLBuffers() {
        if (vertices.isEmpty()) {
            System.out.println("No vertices found for the mesh.");
            return;
        }

        // Convert vertices List<Float> to float[]
        float[] verticesArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }

        // Convert indices List<Integer> to int[]
        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        System.out.println("Uploading " + verticesArray.length / 5 + " vertices and " + indicesArray.length / 6 + " faces");
        
        // Allocate memory for buffers and transfer data
        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(verticesArray.length);
        verticesBuffer.put(verticesArray).flip();
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indicesArray.length);
        indicesBuffer.put(indicesArray).flip();

        // Generate and bind VAO (Vertex Array Object)
        if (vaoID == 0) vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        // Generate and bind VBO (Vertex Buffer Object) to store vertices
        if (vboID == 0) vboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);

        // Set vertex attribute pointers for position (3 floats) and texture coords (2 floats)
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0); // Position
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES); // Texture coordinates
        GL20.glEnableVertexAttribArray(1);

        // Generate and bind EBO (Element Buffer Object) to store indices
        if (eboID == 0) eboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        vertexCount = indices.size() * 2; // Set the number of vertices to the size of indices

        // Unbind VAO to avoid accidental modification
        GL30.glBindVertexArray(0);

        // Free up memory used by buffers
        MemoryUtil.memFree(verticesBuffer);
        MemoryUtil.memFree(indicesBuffer);

        System.out.println("Mesh built with vertex count: " + vertexCount);
    }

    // Adds the faces of a voxel to the mesh if needed
    private int addVoxelFaces(Chunk chunk, int x, int y, int z, int blockID, int indexCount) {
        // Calculate the voxel's position in world space
        float xPos = x * VOXEL_SIZE;
        float yPos = y * VOXEL_SIZE;
        float zPos = z * VOXEL_SIZE;

        // Get the block type to retrieve its textures
        Block_Types blockType = Block_Types.getBlockTypeByID(blockID);
        if (blockType == null) {
            System.out.println("Block type not found for blockID: " + blockID);
            return indexCount; // Skip this voxel if no block type is found
        }

        // Get the textures associated with this block type (one for each face)
        String[] textures = blockType.getTextures();

        // Add a face if the adjacent voxel is empty or out of chunk bounds
        if (shouldAddFace(chunk, x, y, z, 0, 0, 1)) { // Front face
            addFaceForDirection(xPos, yPos, zPos, textures[0], 0, 0, 1);
            indexCount += 6; // Each face adds 6 indices (2 triangles)
        }
        if (shouldAddFace(chunk, x, y, z, 0, 0, -1)) { // Back face
        	System.out.println("Adding back face for voxel at: " + x + ", " + y + ", " + z);
            addFaceForDirection(xPos, yPos, zPos, textures[1], 0, 0, -1);
            indexCount += 6;
        }
        if (shouldAddFace(chunk, x, y, z, -1, 0, 0)) { // Left face
            addFaceForDirection(xPos, yPos, zPos, textures[2], -1, 0, 0);
            indexCount += 6;
        }
        if (shouldAddFace(chunk, x, y, z, 1, 0, 0)) { // Right face
        	System.out.println("Adding right face for voxel at: " + x + ", " + y + ", " + z);
            addFaceForDirection(xPos, yPos, zPos, textures[3], 1, 0, 0);
            indexCount += 6;
        }
        if (shouldAddFace(chunk, x, y, z, 0, -1, 0)) { // Bottom face
            addFaceForDirection(xPos, yPos, zPos, textures[4], 0, -1, 0);
            indexCount += 6;
        }
        if (shouldAddFace(chunk, x, y, z, 0, 1, 0)) { // Top face
        	System.out.println("Adding top face for voxel at: " + x + ", " + y + ", " + z);
            addFaceForDirection(xPos, yPos, zPos, textures[5], 0, 1, 0);
            indexCount += 6;
        }

        return indexCount; // Return the updated index count
    }

    // Checks if a face should be added (if the adjacent voxel is empty or outside chunk bounds)
    private boolean shouldAddFace(Chunk chunk, int x, int y, int z, int dx, int dy, int dz) {
        int nx = x + dx;
        int ny = y + dy;
        int nz = z + dz;

        if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE && nz >= 0 && nz < CHUNK_SIZE) {
            Voxel adjacentVoxel = chunk.getVoxel(nx, ny, nz);
            System.out.println("Checking adjacent voxel at: " + nx + ", " + ny + ", " + nz);
            return adjacentVoxel == null || !adjacentVoxel.isSolid();
        } else {
            return true; // Out of bounds means the face should be visible
        }
    }



    // Adds a face in the specified direction, with texture and position
    private void addFaceForDirection(float xPos, float yPos, float zPos, String texturePath, int dx, int dy, int dz) {
        int textureID = TextureLoader.loadTexture(texturePath); // Load the texture for this face
        textureIDs.add(textureID); // Store the texture ID

        // Get the vertices for this face and the default texture coordinates
        float[] faceVertices = getFaceVertices(xPos, yPos, zPos, dx, dy, dz);
        float[] texCoords = getDefaultTextureCoords();

        // Calculate the current starting index for this face's vertices
        int currentIndex = vertices.size() / 5; // 5 floats per vertex (3 position + 2 texture coordinates)

        // Add vertex data (position and texture coordinates)
        for (int i = 0; i < 4; i++) {
            vertices.add(faceVertices[i * 3]);     // x coordinate
            vertices.add(faceVertices[i * 3 + 1]); // y coordinate
            vertices.add(faceVertices[i * 3 + 2]); // z coordinate
            vertices.add(texCoords[i * 2]);        // texture u coordinate
            vertices.add(texCoords[i * 2 + 1]);    // texture v coordinate
        }

        // Add indices for the two triangles that form this face
        indices.add(currentIndex);
        indices.add(currentIndex + 1);
        indices.add(currentIndex + 2);
        indices.add(currentIndex + 2);
        indices.add(currentIndex + 3);
        indices.add(currentIndex);
    }

    // Get the vertices for a face based on direction (dx, dy, dz)
    private float[] getFaceVertices(float xPos, float yPos, float zPos, int dx, int dy, int dz) {
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

    // Get default texture coordinates (a fixed square)
    private float[] getDefaultTextureCoords() {
        return new float[]{
            0.0f, 0.0f, // Bottom-left
            1.0f, 0.0f, // Bottom-right
            1.0f, 1.0f, // Top-right
            0.0f, 1.0f  // Top-left
        };
    }

    // Render the chunk mesh
    public void render() {
        if (vertexCount == 0) {
            System.out.println("No vertex to render");
            return;
        }

        // Enable face culling (to optimize rendering by not rendering back faces)
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL30.glBindVertexArray(vaoID); // Bind VAO to render the mesh

        // Render each face using its texture
        int faceCount = indices.size() / 6; // Each face has 6 indices (2 triangles)
        for (int i = 0; i < faceCount; i++) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureIDs.get(i)); // Bind appropriate texture
            GL11.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, i * 6 * Integer.BYTES); // Draw face
        }

        GL30.glBindVertexArray(0); // Unbind VAO
        GL11.glDisable(GL11.GL_CULL_FACE); // Disable face culling after rendering
    }

    // Clean up OpenGL resources
    public void cleanup() {
        GL15.glDeleteBuffers(vboID); // Delete VBO
        GL15.glDeleteBuffers(eboID); // Delete EBO
        GL30.glDeleteVertexArrays(vaoID); // Delete VAO
    }

    // Update a voxel in the chunk and rebuild the mesh
    public void updateVoxel(Chunk chunk, int x, int y, int z, int blockID) {
        // Set the voxel in the chunk (code commented out here)
        // chunk.setVoxel(x, y, z, new Voxel(blockID));

        // Rebuild the entire chunk mesh to update it with the new voxel
        buildChunkMesh(chunk);
    }
}
