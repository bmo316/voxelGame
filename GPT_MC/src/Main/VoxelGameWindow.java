package Main;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import Block_Entities.Block_Types;
import ChunkUtilities.Chunk;
import ChunkUtilities.ChunkMesh;

import java.io.IOException;
import java.nio.FloatBuffer;

public class VoxelGameWindow {

    private long window;
    public int windowX = 800;
    public int windowY = 500;
    
    // Create a camera object
    private Camera camera = new Camera(new Vector3f(0.0f, 0.0f, 5.0f));  // Start a bit further back
    
    // CubeTest instance
    private CubeTest cubeTest;
    
    public static Block_Types BlockTypes;
    
    //Chunk Test
    
    private Chunk chunk;
    private ChunkMesh chunkMesh;
    
    // Shader program
    public ShaderProgram shaderProgram;
    
    //Skysphere program
    private SkySphere skySphere;
    private ShaderProgram skySphereShaderProgram;

    // Mouse movement tracking
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    public static void main(String[] args) {
    	BlockTypes = Block_Types.registerDefaultBlocks();
        new VoxelGameWindow().run();
        
    }

    public void run() {
        System.out.println("Starting LWJGL 3 Voxel Game...");
        
        try {
            init();
        } catch (Exception e) {
            System.err.println("Exception in init(): " + e.getMessage());
            e.printStackTrace();
            return;
        }

		
        //cubeTest = new CubeTest();  // Initialize CubeTest
        chunk = new Chunk();
        chunkMesh = chunk.generateMesh();
        
        loop();

        // Clean up
        //cubeTest.cleanup();
        shaderProgram.cleanup();  // Clean up shader resources
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private void init() throws IOException {
        System.out.println("Initializing GLFW...");
        

        // Set up an error callback to print to System.err
        GLFWErrorCallback.createPrint(System.err).set();
        GLFW.glfwSetErrorCallback((error, description) -> {
            System.err.println("GLFW Error [" + error + "]: " + MemoryUtil.memUTF8(description));
        });

        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        System.out.println("GLFW initialized successfully.");

        // Configure GLFW: set OpenGL version to 3.3 core profile
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);  // Required for macOS

        // Create the window
        window = GLFW.glfwCreateWindow(windowX, windowY, "Voxel Game", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        System.out.println("GLFW window created successfully.");

        // Set the current context
        GLFW.glfwMakeContextCurrent(window);
        System.out.println("OpenGL context made current.");

        // Enable v-sync
        GLFW.glfwSwapInterval(1);

        // Make the window visible
        GLFW.glfwShowWindow(window);
        System.out.println("GLFW window made visible.");

        // Initialize OpenGL bindings
        GL.createCapabilities();
        System.out.println("OpenGL capabilities created.");

        // Enable depth testing for 3D
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        System.out.println("Depth testing enabled.");

        // Set mouse position callback for camera control
        GLFW.glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            float xOffset = (float) (xpos - lastMouseX);
            float yOffset = (float) (lastMouseY - ypos);  // reversed since y-coordinates go from bottom to top

            lastMouseX = xpos;
            lastMouseY = ypos;

            camera.handleMouseInput(xOffset, yOffset);
        });
        System.out.println("Cursor position callback set.");

        // Capture the mouse (hide cursor and lock it to the window)
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        System.out.println("Cursor input mode set.");

        // Create the shader program
        shaderProgram = new ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl");

        // Create the sky sphere
        skySphere = new SkySphere(32, 32, 50.0f); // Large enough to encompass the scene

        // Load sky sphere shaders
        skySphereShaderProgram = new ShaderProgram("shaders/sky_sphere_vertex.glsl", "shaders/sky_sphere_fragment.glsl");
        
        GLFW.glfwSetFramebufferSizeCallback(window, (win, width, height) -> {
            // Update window dimensions
            windowX = width;
            windowY = height;

            // Adjust the OpenGL viewport to the new window dimensions
            GL11.glViewport(0, 0, windowX, windowY);

            // Optionally, update the projection matrix to maintain the correct aspect ratio
            updateProjectionMatrix();
        });
        
        System.out.println("Shader program created.");
    }
    
    // WINDOW RESIZING
    private void updateProjectionMatrix() {
        float aspectRatio = (float) windowX / (float) windowY;
        Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(60.0f), aspectRatio, 0.1f, 100.0f);

        // Set the updated projection matrix uniform
        FloatBuffer projectionBuffer = MemoryUtil.memAllocFloat(16);
        projectionMatrix.get(projectionBuffer);
        shaderProgram.setUniformMatrix4fv("projection", projectionBuffer);
        MemoryUtil.memFree(projectionBuffer);
    }
    
    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
        	
        	
            // Clear the color and depth buffer
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            // Use the shader program

            GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f); // Set to a dark gray for contrast
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            skySphereShaderProgram.use();
            
            
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            

            
            
            // Set the camera view
            Matrix4f viewMatrix = camera.getViewMatrix();
            FloatBuffer viewBuffer = MemoryUtil.memAllocFloat(16);
            viewMatrix.get(viewBuffer);  // Store matrix into buffer
            shaderProgram.setUniformMatrix4fv("view", viewBuffer);
            MemoryUtil.memFree(viewBuffer);
            
            // Set the projection matrix
            Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(60.0f), windowX / windowY, 0.1f, 100.0f);
            FloatBuffer projectionBuffer = MemoryUtil.memAllocFloat(16);
            projectionMatrix.get(projectionBuffer);
            shaderProgram.setUniformMatrix4fv("projection", projectionBuffer);
            MemoryUtil.memFree(projectionBuffer);
            
            
            // Draw the background
           //drawBackground();
            
            //skySphere.renderWithoutShaders();

            renderSkySphere();
            
            // Use the shader program
            shaderProgram.use();

            
            renderChunk();

            // Render the cube
			//RenderCubetest();

            // Set the model matrix (identity, since we are not transforming the cube)
			//Matrix4f modelMatrix = new Matrix4f().scale(200.0f); // Scaling the cube by a factor of 2
			//FloatBuffer modelBuffer = MemoryUtil.memAllocFloat(16);
			//modelMatrix.get(modelBuffer);
			//cubeTest.shaderProgram.setUniformMatrix4fv("model", modelBuffer);
			//MemoryUtil.memFree(modelBuffer);

            // Set other uniforms (view, projection, model matrices)

            

            // Swap the color buffers
            GLFW.glfwSwapBuffers(window);

            // Poll for window events (e.g., key presses, mouse movements)
            GLFW.glfwPollEvents();

            // Handle camera movement
            camera.handleKeyboardInput(window, chunk);
            
            
            

            
        }
    }

    private void drawBackground() {
        // Bind the shader program
        shaderProgram.use();

        // Disable depth testing to ensure background is rendered first
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Define the two triangles to cover the screen in normalized device coordinates
        float[] vertices = {
            // Triangle 1 (Bottom - Black)
            -1.0f, -1.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
            -1.0f,  0.0f, 0.0f,

            -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f, 0.0f,
             1.0f,  0.0f, 0.0f,

            // Triangle 2 (Top - Blue)
            -1.0f, 0.0f, 0.0f,
             1.0f, 0.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,

            -1.0f, 1.0f, 0.0f,
             1.0f, 0.0f, 0.0f,
             1.0f, 1.0f, 0.0f
        };

        // Create a FloatBuffer to store vertices
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        // Generate and bind VAO and VBO for the triangles
        int vao = GL30.glGenVertexArrays();
        int vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        // Enable the vertex attribute
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        // Draw the bottom half (light gray)
        shaderProgram.setUniform4f("color", 0.3f, 0.3f, 0.3f, 1.0f); // Light gray color
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6); // Draw the first two triangles

        // Draw the top half (light blue)
        shaderProgram.setUniform4f("color", 0.5f, 0.7f, 1.0f, 1.0f); // Light blue color
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 6, 6); // Draw the second two triangles

        // Unbind and delete VAO and VBO
        GL20.glDisableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);

        MemoryUtil.memFree(vertexBuffer);

        // Re-enable depth testing for other objects
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void renderSkySphere() {
        // Disable depth writing for the sky sphere
        GL11.glDepthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);  // Draw the sky in the background

        // Use the sky sphere shader program
        skySphereShaderProgram.use();

        // Create view matrix without translation to keep the sky sphere stationary
        Matrix4f viewMatrix = new Matrix4f(camera.getViewMatrix());
        viewMatrix.m30(0).m31(0).m32(0);  // Remove translation components
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = stack.mallocFloat(16);
            viewMatrix.get(viewBuffer);
            skySphereShaderProgram.setUniformMatrix4fv("view", viewBuffer);
        }

        // Set the projection matrix
        Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(60.0f), windowX / windowY, 0.1f, 100.0f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projectionBuffer = stack.mallocFloat(16);
            projectionMatrix.get(projectionBuffer);
            skySphereShaderProgram.setUniformMatrix4fv("projection", projectionBuffer);
        }

        // Render the sky sphere (ensure skySphere.render() uses the correct VAO)
        skySphere.render();

        // Reset depth function and mask
        GL11.glDepthMask(true); // Re-enable depth writing
        GL11.glDepthFunc(GL11.GL_LESS); // Set depth function back to default
    }

    private void RenderCubetest() {
        // Enable depth writing for the cube
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS); // Use the default depth function

        // Use the cube shader program
        cubeTest.shaderProgram.use();

        // Create view matrix from the camera (do not modify it)
        Matrix4f viewMatrix = camera.getViewMatrix(); // Use the camera's full view matrix
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = stack.mallocFloat(16);
            viewMatrix.get(viewBuffer);
            cubeTest.shaderProgram.setUniformMatrix4fv("view", viewBuffer);
        }

        // Set the projection matrix (perspective projection)
        Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(60.0f), (float) windowX / (float) windowY, 0.1f, 100.0f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projectionBuffer = stack.mallocFloat(16);
            projectionMatrix.get(projectionBuffer);
            cubeTest.shaderProgram.setUniformMatrix4fv("projection", projectionBuffer);
        }

        // Set the model matrix (you can apply transformations to the cube here if needed)
        Matrix4f modelMatrix = new Matrix4f(); // Identity matrix, no transformation
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelBuffer = stack.mallocFloat(16);
            modelMatrix.get(modelBuffer);
            cubeTest.shaderProgram.setUniformMatrix4fv("model", modelBuffer);
        }

        // Render the cube
        try {
            cubeTest.render();
        } catch (IOException e) {
            System.out.println("Failed to render the cube");
            e.printStackTrace();
        }
    }


    private void renderChunk() {
        // Enable depth writing for the chunk
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS); // Use the default depth function

        // Use the chunk shader program (assuming you have set up a shader program for the chunk)
        shaderProgram.use();

        // Create view matrix from the camera (do not modify it)
        Matrix4f viewMatrix = camera.getViewMatrix(); // Use the camera's full view matrix
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer viewBuffer = stack.mallocFloat(16);
            viewMatrix.get(viewBuffer);
            shaderProgram.setUniformMatrix4fv("view", viewBuffer);
        }

        // Set the projection matrix (perspective projection)
        Matrix4f projectionMatrix = new Matrix4f().perspective((float) Math.toRadians(60.0f), (float) windowX / (float) windowY, 0.1f, 100.0f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer projectionBuffer = stack.mallocFloat(16);
            projectionMatrix.get(projectionBuffer);
            shaderProgram.setUniformMatrix4fv("projection", projectionBuffer);
        }

        // Set the model matrix (you can apply transformations to the chunk here if needed)
        Matrix4f modelMatrix = new Matrix4f(); // Identity matrix, no transformation
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer modelBuffer = stack.mallocFloat(16);
            modelMatrix.get(modelBuffer);
            shaderProgram.setUniformMatrix4fv("model", modelBuffer);
        }

        // Render the chunk
        chunkMesh.render();
    }


    
    private void cleanup() {
    	chunkMesh.cleanup();
    }
    
}
