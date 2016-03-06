/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package glTest.framework;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

/**
 *
 * @author elect
 */
public class GLUtilities {

    private static final String SHADERS_ROOT = "src/shaders/";

    public static int createProgram(GL3 gl3, String vsFilename, String fsFilename, String[] uniformNames,
            int[] uniformLocations) {

        assert (uniformNames != null);
        assert (uniformLocations != null);

        // Ensure that the sizes match, otherwise there is a parameter mismatch.
        assert (uniformNames.length == uniformLocations.length);

        int retProg = createProgram(gl3, vsFilename, fsFilename);

        if (retProg != 0) {
            for (int i = 0; i < uniformNames.length; i++) {
                uniformLocations[i] = gl3.glGetUniformLocation(retProg, uniformNames[i]);
            }
        }
        return retProg;
    }

    public static int createProgram(GL3 gl3, String vsFilename, String fsFilename) {

        ShaderCode vs = ShaderCode.create(gl3, GL_VERTEX_SHADER, 1, GLUtilities.class,
                new String[]{SHADERS_ROOT + vsFilename}, true);
        ShaderCode fs = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, 1, GLUtilities.class,
                new String[]{SHADERS_ROOT + fsFilename}, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vs);
        shaderProgram.add(fs);
        shaderProgram.link(gl3, System.out);

        // Flag these now, they're either attached (linked in) and will be cleaned up with the link, or the
        // link failed and we're about to lose track of them anyways.
        vs.destroy(gl3);
        fs.destroy(gl3);

        return shaderProgram.program();
    }
}
