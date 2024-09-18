#version 330 core

in vec3 fragPosition;  // Interpolated vertex position from the vertex shader

out vec4 FragColor;

void main() {
    // Adjust this threshold to control where the transition happens
    float threshold = -.005;

    // Determine the color based on the y-coordinate of the position
    float yNormalized = fragPosition.y / length(fragPosition);
    
    // Use step to create a sharp transition
    float mixFactor = step(threshold, yNormalized);

    // Interpolate between black and light blue with a sharp cutoff
    vec3 color = mix(vec3(0.0, 0.0, 0.0), vec3(0.5, 0.7, 1.0), mixFactor);

    FragColor = vec4(color, 1.0);
}
