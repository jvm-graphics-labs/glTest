/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package glTest.problems;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import common.BufferUtils;
import glTest.solutions.Solution;
import java.nio.FloatBuffer;

/**
 *
 * @author GBarbieri
 */
public abstract class Problem {

    protected Solution solution;
    private int solutionId = 0;
    protected FloatBuffer clearColor, clearDepth;

    public boolean init(GL4 gl4) {

        clearColor = GLBuffers.newDirectFloatBuffer(4);
        clearDepth = GLBuffers.newDirectFloatBuffer(1);

        return true;
    }

    public void render(GL4 gl4) {

    }

    public boolean shutdown(GL4 gl4) {

        if (solution != null) {
            solution.shutdown(gl4);
        }

        BufferUtils.destroyDirectBuffer(clearColor);
        BufferUtils.destroyDirectBuffer(clearDepth);

        return true;
    }

    public String getName() {
        return "";
    }

    public void setSolution(GL4 gl4, Solution solution) {
        this.solution = solution;
    }

    public Solution getSolution() {
        return solution;
    }

    public int getSolutionId() {
        return solutionId;
    }

    public void setSolutionId(int solutionId) {
        this.solutionId = solutionId;
    }
}
