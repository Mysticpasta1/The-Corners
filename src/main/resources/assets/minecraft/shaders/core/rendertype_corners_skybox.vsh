#version 150

#moj_import <projection.glsl>

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 texProj0;
out vec4 glPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    glPos = gl_Position;
    texProj0 = projection_from_position(gl_Position);
}
