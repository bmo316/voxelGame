#version 330 core

layout(location = 0) in vec3 position;

out vec3 fragPosition;  // Pass the vertex position to the fragment shader

uniform mat4 projection;
uniform mat4 view;

void main() {
    fragPosition = position;  // Pass the vertex position to the fragment shader
    gl_Position = projection * view * vec4(position, 1.0);
}
