package Main;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import ChunkUtilities.Chunk;
import ChunkUtilities.Voxel;

public class Camera {
    private Vector3f position;
    private float pitch;  // Up and down rotation
    private float yaw;    // Left and right rotation
    private Vector3f up;

    private float speed = 0.25f;   // Camera movement speed
    private float sensitivity = 0.1f;  // Mouse sensitivity
    private float reachDistance = 5.0f; // Maximum distance for placing/removing blocks

    public Camera(Vector3f position) {
        this.position = position;
        this.pitch = 0.0f;
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.yaw = -90.0f;  // Facing forward along -Z axis
    }

    // Move camera based on key input
    public void handleKeyboardInput(long window, Chunk chunk) {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            moveForward();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            moveBackward();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            moveLeft();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            moveRight();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS) {
            moveDown();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_E) == GLFW.GLFW_PRESS) {
            moveUp();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) {
            moveDown();
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            moveUp();
        }

        // Add or remove blocks
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            deleteBlock(chunk);
        }
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {
            addBlock(chunk);
        }
    }

    // Update camera rotation based on mouse input
    public void handleMouseInput(float xOffset, float yOffset) {
        xOffset *= sensitivity;
        yOffset *= sensitivity;

        yaw += xOffset;
        pitch += yOffset;

        // Constrain pitch (up/down rotation)
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
    }

    // Get the view matrix (transforms the world from the camera's perspective)
    public Matrix4f getViewMatrix() {
        Vector3f front = new Vector3f();
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));

        Vector3f cameraTarget = position.add(front, new Vector3f());
        return new Matrix4f().lookAt(position, cameraTarget, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    // Movement methods
    private void moveForward() {
        position.add(new Vector3f((float) Math.cos(Math.toRadians(yaw)), 0, (float) Math.sin(Math.toRadians(yaw))).mul(speed));
    }

    private void moveBackward() {
        position.add(new Vector3f(-(float) Math.cos(Math.toRadians(yaw)), 0, -(float) Math.sin(Math.toRadians(yaw))).mul(speed));
    }

    private void moveLeft() {
        position.add(new Vector3f((float) Math.sin(Math.toRadians(yaw)), 0, -(float) Math.cos(Math.toRadians(yaw))).mul(speed));
    }

    private void moveRight() {
        position.add(new Vector3f(-(float) Math.sin(Math.toRadians(yaw)), 0, (float) Math.cos(Math.toRadians(yaw))).mul(speed));
    }

    private void moveUp() {
        position.add(new Vector3f(up).mul(speed));
    }

    private void moveDown() {
        position.sub(new Vector3f(up).mul(speed));
    }

    // Improved raycast to delete a block
    private void deleteBlock(Chunk chunk) {
        Vector3f rayDirection = getViewDirection().normalize(); // Ensure direction is normalized
        Vector3f rayStart = new Vector3f(position);
        Vector3f rayStep = new Vector3f();

        for (float i = 0; i < reachDistance; i += 0.01f) { // Smaller step size for higher precision
            // Compute the current step along the ray
            rayStep.set(rayDirection).mul(i).add(rayStart);

            // Convert to voxel coordinates
            int x = (int) Math.floor(rayStep.x);
            int y = (int) Math.floor(rayStep.y);
            int z = (int) Math.floor(rayStep.z);

            // Check if the voxel is within chunk bounds
            if (x >= 0 && x < Chunk.getChunkSize() && y >= 0 && y < Chunk.getChunkSize() && z >= 0 && z < Chunk.getChunkSize()) {
                Voxel voxel = chunk.getVoxel(x, y, z);

                // If the voxel is solid, delete it
                if (voxel != null && voxel.isSolid()) {
                    System.out.println("Voxel ID Removed = " + voxel.getBlockID() + " Voxel Coords = " + x + " " + y + " " + z);
                    chunk.setVoxel(x, y, z, Voxel.AIR); // Set to air (assuming Voxel.AIR is the empty state)

                    // Update this voxel and its neighbors in the chunk mesh
                    //updateNeighboringBlocks(chunk, x, y, z);
                    break; // Stop after deleting the block
                }
            }
        }
    }

    // Improved raycast to add a block
    private void addBlock(Chunk chunk) {
        Vector3f rayDirection = getViewDirection().normalize(); // Ensure direction is normalized
        Vector3f rayStart = new Vector3f(position);
        Vector3f rayStep = new Vector3f();
        Vector3f lastAirPosition = new Vector3f();

        for (float i = 0; i < reachDistance; i += 0.01f) { // Smaller step size for higher precision
            // Compute the current step along the ray
            rayStep.set(rayDirection).mul(i).add(rayStart);

            // Convert to voxel coordinates
            int x = (int) Math.floor(rayStep.x);
            int y = (int) Math.floor(rayStep.y);
            int z = (int) Math.floor(rayStep.z);

            // Check if the voxel is within chunk bounds
            if (x >= 0 && x < Chunk.getChunkSize() && y >= 0 && y < Chunk.getChunkSize() && z >= 0 && z < Chunk.getChunkSize()) {
                Voxel voxel = chunk.getVoxel(x, y, z);

                // Track the last air position before hitting a solid voxel
                if (voxel != null && !voxel.isSolid()) {
                    lastAirPosition.set(x, y, z);
                }

                // If the voxel is solid, place a block at the last air position
                if (voxel != null && voxel.isSolid()) {
                    int addX = (int) lastAirPosition.x;
                    int addY = (int) lastAirPosition.y;
                    int addZ = (int) lastAirPosition.z;

                    // Ensure we are within chunk bounds
                    if (addX >= 0 && addX < Chunk.getChunkSize() && addY >= 0 && addY < Chunk.getChunkSize() && addZ >= 0 && addZ < Chunk.getChunkSize()) {
                        if (chunk.getVoxel(addX, addY, addZ).getBlockID() == 0) { // Check if it's air
                            chunk.setVoxel(addX, addY, addZ, new Voxel(2)); // Place the block (ID 4)
                            //updateNeighboringBlocks(chunk, addX, addY, addZ);
                            System.out.println("Voxel ID Set = 2 at Voxel Coords = " + addX + " " + addY + " " + addZ);
                        }
                    }
                    break; // Stop after placing the block
                }
            }
        }
    }

    // Update the visibility of neighboring blocks' faces after placing a block
    private void updateNeighboringBlocks(Chunk chunk, int x, int y, int z) {
        // Offsets for the 6 cardinal neighbors in 3D space
        int[][] neighbors = {
            {1, 0, 0}, // Positive X
            {-1, 0, 0}, // Negative X
            {0, 1, 0}, // Positive Y
            {0, -1, 0}, // Negative Y
            {0, 0, 1}, // Positive Z
            {0, 0, -1} // Negative Z
        };

        // Update the block itself first
        if (x >= 0 && x < Chunk.getChunkSize() && y >= 0 && y < Chunk.getChunkSize() && z >= 0 && z < Chunk.getChunkSize()) {
            chunk.getChunkMesh().updateVoxel(chunk, x, y, z, chunk.getVoxel(x, y, z).getBlockID());
        }

        // Update the mesh for each immediate neighbor
        for (int[] offset : neighbors) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            int nz = z + offset[2];

            // Ensure the neighbor is within chunk bounds
            if (nx >= 0 && nx < Chunk.getChunkSize() && ny >= 0 && ny < Chunk.getChunkSize() && nz >= 0 && nz < Chunk.getChunkSize()) {
                // Update the chunk mesh for the neighbor
                chunk.getChunkMesh().updateVoxel(chunk, nx, ny, nz, chunk.getVoxel(nx, ny, nz).getBlockID());
            }
        }
    }


    // Get the direction the camera is facing
    private Vector3f getViewDirection() {
        Vector3f front = new Vector3f();
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        return front.normalize();
    }
}
