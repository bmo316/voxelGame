package Main;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class SkySphere {
    private int vaoID;
    private int vertexCount;

    public SkySphere(int stacks, int slices, float radius) {
        generateSphere(stacks, slices, radius);
    }

    public void generateSphere(int stacks, int slices, float radius) {
    	
    	System.out.println("Generating Sphere");
        // Create lists to hold vertices and indices
        float[] vertices = new float[(stacks + 1) * (slices + 1) * 3];
        int[] indices = new int[stacks * slices * 6];
        
        int vertexPointer = 0;
        int indexPointer = 0;
        
        for (int i = 0; i <= stacks; i++) {
            double lat = Math.PI / stacks * i - Math.PI / 2;
            double sinLat = Math.sin(lat);
            double cosLat = Math.cos(lat);

            for (int j = 0; j <= slices; j++) {
                double lon = 2 * Math.PI / slices * j;
                double sinLon = Math.sin(lon);
                double cosLon = Math.cos(lon);

                // Vertex position
                vertices[vertexPointer++] = (float) (radius * cosLat * cosLon);
                vertices[vertexPointer++] = (float) (radius * sinLat);
                vertices[vertexPointer++] = (float) (radius * cosLat * sinLon);
            }
        }

        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int first = (i * (slices + 1)) + j;
                int second = first + slices + 1;

                // Triangle 1
                indices[indexPointer++] = first;
                indices[indexPointer++] = second;
                indices[indexPointer++] = first + 1;

                // Triangle 2
                indices[indexPointer++] = second;
                indices[indexPointer++] = second + 1;
                indices[indexPointer++] = first + 1;
            }
        }

        // Create VAO and VBOs
        vaoID = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoID);

        // Vertex VBO
        int vboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);

        // Index VBO
        int eboID = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboID);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        GL30.glBindVertexArray(0);

        vertexCount = indices.length;
        
        System.out.println("Total vertices: " + vertices.length / 3);
        System.out.println("Total indices: " + indices.length);

    }

    public void render() {
    	//System.out.println("Rendering Sphere");
    	GL30.glBindVertexArray(vaoID);
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }
    
    public void renderWithoutShaders() {
        // Bind the VAO
        GL30.glBindVertexArray(vaoID);
        
        // Disable shaders for fixed-function pipeline rendering
        GL20.glUseProgram(0);

        // Set color for the sphere (light blue for debugging)
        GL11.glColor3f(0.5f, 0.7f, 1.0f);

        // Draw the sphere using GL_TRIANGLES (using indices)
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);

        // Unbind the VAO
        GL30.glBindVertexArray(0);
    }

}
