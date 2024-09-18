package Main;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CubeTest {
    private int vaoID;
    private int vboID;
    private int eboID;
    private int textureID;
    // Shader program
    public ShaderProgram shaderProgram;

    public CubeTest() {
        // Initialize the shader program
        initializeShader();

        // Initialize cube with texture
        float[] vertices = {
            // Positions          // Texture Coords
            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f, 1.0f,

            -0.5f, -0.5f,  0.5f,  0.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 0.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f,

            -0.5f,  0.5f,  0.5f,  0.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
            -0.5f, -0.5f, -0.5f,  1.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,

             0.5f,  0.5f,  0.5f,  0.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  1.0f, 1.0f,
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f,

            -0.5f, -0.5f, -0.5f,  0.0f, 0.0f,
             0.5f, -0.5f, -0.5f,  1.0f, 0.0f,
             0.5f, -0.5f,  0.5f,  1.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,  0.0f, 1.0f,

            -0.5f,  0.5f, -0.5f,  0.0f, 0.0f,
             0.5f,  0.5f, -0.5f,  1.0f, 0.0f,
             0.5f,  0.5f,  0.5f,  1.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0,    // Back face
            4, 5, 6, 6, 7, 4,    // Front face
            8, 9, 10, 10, 11, 8, // Left face
            12, 13, 14, 14, 15, 12, // Right face
            16, 17, 18, 18, 19, 16, // Bottom face
            20, 21, 22, 22, 23, 20  // Top face
        };

        // Set up VAO, VBO, and EBO
        vaoID = GL30.glGenVertexArrays();
        vboID = GL15.glGenBuffers();
        eboID = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoID);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);

        // Load texture
        textureID = loadTexture("res/textures/Stone.png");

        // Free allocated memory
        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    private void initializeShader() {
        try {
            shaderProgram = new ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int loadTexture(String filePath) {
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // Set texture parameters to GL_NEAREST for a sharper look
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST); // Use GL_NEAREST
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST); // Use GL_NEAREST

        // Load texture
        try (MemoryStack stack = MemoryStack.stackPush()) {
            System.out.println("Loading Textures...");
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load(filePath, width, height, channels, 4);
            if (image != null) {
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width.get(), height.get(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            } else {
                System.err.println("Failed to load texture: " + filePath);
            }
            STBImage.stbi_image_free(image);
        }
        System.out.println("Texture Loaded Successfully " + textureID);
        return textureID;
    }


    public void render() throws IOException {
        // Check for OpenGL errors before rendering
        checkGLError("Before rendering cube");

        // Use shader program here
        shaderProgram.use();

        // Bind the texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Ensure the shader has a texture sampler uniform set to use texture unit 0
        int textureSamplerLocation = GL20.glGetUniformLocation(shaderProgram.programID, "textureSampler");
        GL20.glUniform1i(textureSamplerLocation, 0);

        // Bind the VAO
        GL30.glBindVertexArray(vaoID);

        // Draw the cube with the correct number of indices
        GL11.glDrawElements(GL11.GL_TRIANGLES, 36, GL11.GL_UNSIGNED_INT, 0);
        // Unbind the VAO
        GL30.glBindVertexArray(0);

        // Check for OpenGL errors after rendering
        checkGLError("After rendering cube");
    }

    // Utility method to check for OpenGL errors
    private void checkGLError(String stage) {
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            System.err.println("OpenGL Error at " + stage + ": " + error);
            switch (error) {
                case GL11.GL_INVALID_ENUM: System.err.println("GL_INVALID_ENUM"); break;
                case GL11.GL_INVALID_VALUE: System.err.println("GL_INVALID_VALUE"); break;
                case GL11.GL_INVALID_OPERATION: System.err.println("GL_INVALID_OPERATION"); break;
                case GL11.GL_STACK_OVERFLOW: System.err.println("GL_STACK_OVERFLOW"); break;
                case GL11.GL_STACK_UNDERFLOW: System.err.println("GL_STACK_UNDERFLOW"); break;
                case GL11.GL_OUT_OF_MEMORY: System.err.println("GL_OUT_OF_MEMORY"); break;
                default: System.err.println("Unknown Error"); break;
            }
        }
    }

    public void cleanup() {
        GL15.glDeleteBuffers(vboID);
        GL15.glDeleteBuffers(eboID);
        GL30.glDeleteVertexArrays(vaoID);
        GL11.glDeleteTextures(textureID);
    }
}
