package ChunkUtilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class TextureLoader {
    // Store already loaded textures and their coordinates
    private static final Map<String, Integer> loadedTextures = new HashMap<>();
    private static final Map<Integer, float[]> textureCoordinates = new HashMap<>(); // Store texture coordinates

    public static int loadTexture(String filePath) {
        // Check if the texture has already been loaded
        if (loadedTextures.containsKey(filePath)) {
            return loadedTextures.get(filePath);
        }

        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // Set texture parameters to avoid blurring
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST); // Use GL_NEAREST for minifying
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST); // Use GL_NEAREST for magnification

        // Load texture
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load(filePath, width, height, channels, 4);
            if (image != null) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                STBImage.stbi_image_free(image);
            } else {
                System.err.println("Failed to load texture: " + filePath);
                return -1; // Return an invalid ID if texture loading failed
            }
        }

        // Store the texture ID to avoid reloading the same texture
        loadedTextures.put(filePath, textureID);

        // Assign default texture coordinates for this texture (this can be modified for atlas usage)
        textureCoordinates.put(textureID, new float[]{
            0.0f, 0.0f, // Bottom-left
            1.0f, 0.0f, // Bottom-right
            1.0f, 1.0f, // Top-right
            0.0f, 1.0f  // Top-left
        });

        System.out.println("Texture Loaded Successfully: " + filePath + " with ID: " + textureID);
        return textureID;
    }

    // Get texture coordinates for the given texture ID
    public static float[] getTextureCoords(int textureID) {
        return textureCoordinates.getOrDefault(textureID, new float[]{
            0.0f, 0.0f, // Bottom-left
            1.0f, 0.0f, // Bottom-right
            1.0f, 1.0f, // Top-right
            0.0f, 1.0f  // Top-left
        });
    }
}
