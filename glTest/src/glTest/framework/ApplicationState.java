/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package glTest.framework;

import common.TimeHack6435126;
import common.GlDebugOutput;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.util.GLBuffers;
import glm.vec._2.i.Vec2i;
import java.util.ArrayList;
import glTest.problems.Problem;
import glTest.solutions.Solution;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 *
 * @author GBarbieri
 */
public class ApplicationState implements GLEventListener, KeyListener {

    public static final Vec2i RESOLUTION = new Vec2i(1024, 768);

    public static void main(String[] args) throws InterruptedException {

        TimeHack6435126.enableHighResolutionTimer();
        
        ApplicationState app = new ApplicationState();
        app.setup();
    }

    private ProblemFactory factory;
    private ArrayList<Problem> problems;
    private Solution[] solutions;
    private Problem problem;
    private Solution solution;
    public GLWindow glWindow;
    public static Animator animator;
    private final boolean DEBUG = true;
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1), queryName = GLBuffers.newDirectIntBuffer(1);
    private LongBuffer gpuTime = GLBuffers.newDirectLongBuffer(1);
    private final String rootTitle = "gltest";
    private int offsetProblem = 0, offsetSolution = 0, frames = 0;
    private long cpuStart, cpuTotal, gpuTotal, updateCountersStart, updateTick = 1_000;

    public void setup() {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(1024, 768);
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle(rootTitle);
        if (DEBUG) {
            glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        }

        glWindow.setVisible(true);

        if (DEBUG) {
            glWindow.getContext().addGLDebugListener(new GlDebugOutput());
        }

        glWindow.addGLEventListener(this);
        glWindow.addKeyListener(this);

        System.out.println("GL created successfully! Info follows.");

        animator = new Animator();
        animator.setRunAsFastAsPossible(true);
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator.add(glWindow);
        animator.setExclusiveContext(true);

        glWindow.setExclusiveContextThread(animator.getExclusiveContextThread());
        animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL4 gl4 = drawable.getGL().getGL4();
        System.out.println("" + GLContext.getCurrent().getGLVersion());
        System.out.println("Vendor: " + gl4.glGetString(GL_VENDOR));
        System.out.println("Renderer: " + gl4.glGetString(GL_RENDERER));
        System.out.println("Version: " + gl4.glGetString(GL_VERSION));
        System.out.println("Shading Language Version: " + gl4.glGetString(GL_SHADING_LANGUAGE_VERSION));

        gl4.setSwapInterval(0);

        // Default GL State
        gl4.glCullFace(GL_FRONT);
        gl4.glEnable(GL_CULL_FACE);
        gl4.glDisable(GL_SCISSOR_TEST);
        gl4.glEnable(GL_DEPTH_TEST);
        gl4.glDepthMask(true);
        gl4.glDepthFunc(GL_LESS);
        gl4.glDisable(GL_BLEND);
        gl4.glColorMask(true, true, true, true);

        if (DEBUG) {
            gl4.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, null, false);
            gl4.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_HIGH, 0, null, true);
            gl4.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_MEDIUM, 0, null, true);
        }

        gl4.glGetQueryiv(GL_TIME_ELAPSED, GL_QUERY_COUNTER_BITS, queryName);
        System.out.println("GL_QUERY_COUNTER_BITS: " + queryName.get(0));

        gl4.glGenQueries(1, queryName);

        // Now that we have something valid, create our VAO and bind it. Ugh! So lame that this is required.
        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));

        factory = new ProblemFactory();
        problems = factory.getProblems();
        assert (problems.size() > 0);

        setInitialProblemAndSolution(gl4, "NullProblem", "NullSolution");

        updateCountersStart = System.currentTimeMillis();
    }

    private void setInitialProblemAndSolution(GL4 gl4, String probName, String solnName) {

        for (int i = 0; i < problems.size(); i++) {
            if (problems.get(i).getName().equals(probName)) {
                problem = problems.get(i);
                break;
            }
        }

        solutions = factory.getSolutions(problem);
        for (Solution sol : solutions) {
            if (sol.getName().equals(solnName)) {
                solution = sol;
                break;
            }
        }

        initProblem(gl4);

        initSolution(gl4, 0);

        onProblemOrSolutionSet();
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL4 gl4 = drawable.getGL().getGL4();

        if (offsetProblem != 0) {
            changeProblem(gl4);
        }
        if (offsetSolution != 0) {
            changeSolution(gl4);
        }

        if (problem == null) {
            return;
        }

        gl4.glBeginQuery(GL_TIME_ELAPSED, queryName.get(0));
        {
            cpuStart = System.nanoTime();
            {
                // This is the main entry point shared by all tests. 
                problem.render(gl4);
                frames++;
            }
            cpuTotal += System.nanoTime() - cpuStart;
        }
        gl4.glEndQuery(GL_TIME_ELAPSED);
        gl4.glGetQueryObjectui64v(queryName.get(0), GL_QUERY_RESULT, gpuTime);
        gpuTotal += gpuTime.get(0);

        // Present the results.
//        if (frames == problem.getSolution().updateFps) {
        if (System.currentTimeMillis() - updateCountersStart > updateTick) {
//            System.out.println("cpuTotal: " + cpuTotal / 1_000_000 + ", gpuTotal: " + gpuTotal + ", frames: " + frames);
            String cpu = String.format("%,.3f", ((double) cpuTotal / 1_000_000 / frames)) + " ms";
            String gpu = String.format("%,.3f", ((double) gpuTotal / 1_000_000 / frames)) + " ms";
            String fps = String.format("%,.2f", (1_000 / ((double) gpuTotal / 1_000_000 / frames)));
//            String fps = String.format("%,.2f", frames / ((double) gpuTotal / 1_000_000));
            System.out.println("CPU time: " + cpu + ", GPU time: " + gpu + ", theor. FPS: " + fps);
            resetCounter();
        }
    }

    private void resetCounter() {
        frames = 0;
        cpuTotal = 0;
        gpuTotal = 0;
        updateCountersStart = System.currentTimeMillis();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL4 gl4 = drawable.getGL().getGL4();

        // Must cleanup before we call base class.
        gl4.glBindVertexArray(0);
        gl4.glDeleteVertexArrays(1, vertexArrayName);

        System.exit(0);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.stop();
                break;
            case KeyEvent.VK_LEFT:
                offsetProblem = -1;
                break;
            case KeyEvent.VK_RIGHT:
                offsetProblem = 1;
                break;
            case KeyEvent.VK_UP:
                offsetSolution = -1;
                break;
            case KeyEvent.VK_DOWN:
                offsetSolution = 1;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private void changeProblem(GL4 gl4) {

        shutdownSolution(gl4);

        shutdownProblem(gl4);

        int problemCount = problems.size();
        int problemId = problems.indexOf(problem);
        problemId = (problemId + problemCount + offsetProblem) % problemCount;

        problem = problems.get(problemId);

        initProblem(gl4);

        solutions = factory.getSolutions(problem);

        solution = solutions[problem.getSolutionId()];

        initSolution(gl4, problem.getSolutionId());

        offsetProblem = 0;

        onProblemOrSolutionSet();
    }

    private void changeSolution(GL4 gl4) {

        shutdownSolution(gl4);

        int solutionCount = solutions.length;
        if (solutionCount == 0) {
            return;
        }

        int solutionId = problem.getSolutionId();
        solutionId = (solutionId + solutionCount + offsetSolution) % solutionCount;

        solution = solutions[solutionId];

        initSolution(gl4, solutionId);

        offsetSolution = 0;

        onProblemOrSolutionSet();
    }

    private void initSolution(GL4 gl4, int solutionId) {

        System.out.print("Solution " + solution.getName() + " init... ");
        System.out.println(solution.init(gl4) ? "Ok" : "Fail");

        problem.setSolution(gl4, solution);
        problem.setSolutionId(solutionId);

        resetCounter();
    }

    private void shutdownSolution(GL4 gl4) {

        System.out.print("Solution " + solution.getName() + " shutdown... ");
        System.out.println(solution.shutdown(gl4) ? "Ok" : "Fail");

        problem.setSolution(gl4, null);
    }

    private void initProblem(GL4 gl4) {

        System.out.print("Problem " + problem.getName() + " - init... ");
        System.out.println(problem.init(gl4) ? "Ok" : "Fail");
    }

    private void shutdownProblem(GL4 gl4) {

        System.out.print("Problem " + problem.getName() + " shutdown... ");
        System.out.println(problem.shutdown(gl4) ? "Ok" : "Fail");
    }

    private void onProblemOrSolutionSet() {

        System.gc();

        String newTitle = rootTitle + " - " + problem.getName();

        if (solution != null) {
            newTitle += " - " + solution.getName();
        }
        glWindow.setTitle(newTitle);

        System.gc();
    }
}
