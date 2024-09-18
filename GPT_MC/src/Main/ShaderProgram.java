package Main;

import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class ShaderProgram {
    public int programID;
    private final int vertexShaderID;
    private final int fragmentShaderID;

    public ShaderProgram(String vertexFile, String fragmentFile) throws IOException {
        vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER);
        fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER);
        programID = GL20.glCreateProgram();
        GL20.glAttachShader(programID, vertexShaderID);
        GL20.glAttachShader(programID, fragmentShaderID);
        GL20.glLinkProgram(programID);
        
        // Check for linking errors
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Program linking failed: " + GL20.glGetProgramInfoLog(programID));
        }
    }

    private int loadShader(String filePath, int type) throws IOException {
        // Read shader source from classpath
        String shaderSource = readShaderFile(filePath);
        int shaderID = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderID, shaderSource);
        GL20.glCompileShader(shaderID);

        // Check for compilation errors
        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Shader compilation failed for " + filePath + ": " + GL20.glGetShaderInfoLog(shaderID));
        }
        return shaderID;
    }

    private String readShaderFile(String filePath) throws IOException {
        // Use class loader to load the resource
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IOException("Shader file not found: " + filePath);
        }

        StringBuilder shaderSource = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
        }
        return shaderSource.toString();
    }

    public void use() {
        GL20.glUseProgram(programID);
    }

    // Set a matrix4fv uniform
    public void setUniformMatrix4fv(String name, FloatBuffer matrixBuffer) {
        // Ensure the shader program is in use before setting the uniform
        use();
        
        // Retrieve the location of the uniform variable from the shader
        int location = GL20.glGetUniformLocation(programID, name);
        if (location == -1) {
            System.err.println("Uniform variable not found: " + name);
            return;
        }
        
        // Pass the matrix data to the shader
        GL20.glUniformMatrix4fv(location, false, matrixBuffer);
    }

    // Set a 4-float vector uniform
    public void setUniform4f(String name, float x, float y, float z, float w) {
        // Ensure the shader program is in use before setting the uniform
        use();
        
        int location = GL20.glGetUniformLocation(programID, name);
        if (location == -1) {
            System.err.println("Uniform variable not found: " + name);
            return;
        }
        
        GL20.glUniform4f(location, x, y, z, w);
    }

    public void cleanup() {
        GL20.glUseProgram(0); // Unbind the program
        GL20.glDetachShader(programID, vertexShaderID);
        GL20.glDetachShader(programID, fragmentShaderID);
        GL20.glDeleteShader(vertexShaderID);
        GL20.glDeleteShader(fragmentShaderID);
        GL20.glDeleteProgram(programID);
    }
}
