package com.charlesdrews.pongish.game;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;

import com.charlesdrews.pongish.game.objects.GameObjects;

import java.util.Locale;

/**
 * Provide game engine functionality, including the main update/draw loop.
 *
 * Created by charlie on 9/10/16.
 */
public class PongEngine implements GameEngine.Engine {

    // ==================================== Constants ============================================
    private static final String TAG = "PongEngine";

    private static final long PAUSE_AFTER_SCORE_IN_MS = 2_500L;

    private static final String FPS_TEMPLATE = "FPS: %d";
    private static final int FPS_TEXT_COLOR = Color.WHITE;
    private static final float FPS_TEXT_SIZE = 40f;
    private static final float FPS_X_COORDINATE = 40f;
    private static final float FPS_Y_COORDINATE = 80f;


    // ================================== Member variables =====================================

    private GameEngine.Renderer mRenderer;
    private GameObjects.Scene mScene;

    private Thread mGameThread;
    private Runnable mRunnable;
    private volatile boolean mExecuteGameLoop = false;
    private long mLastFrameRenderTimeInMillis;


    // ==================================== Constructor =========================================

    public PongEngine() {
        // This class implements Runnable, so it can be used as the game thread's target.
        // Keep mRunnable as a variable in case the Runnable implementation is ever moved to
        // another class.
        mRunnable = this;
    }


    // ============================== GameEngine.Engine methods ==================================
    @Override
    public void bindRenderer(@NonNull GameEngine.Renderer renderer) {
        mRenderer = renderer;
    }

    @Override
    public void unbindRenderer() {
        mRenderer = null;
    }

    @Override
    public void setScene(@NonNull GameObjects.Scene scene) {
        mScene = scene;
    }

    @Override
    public long getLastFrameRenderTimeInMillis() {
        return mLastFrameRenderTimeInMillis;
    }

    @Override
    public void startGameExecution() {
        mExecuteGameLoop = true;

        mGameThread = new Thread(mRunnable);
        mGameThread.start();
    }

    @Override
    public void stopGameExecution() {
        mExecuteGameLoop = false;

        if (mGameThread != null) {
            try {
                // Wait for the game thread to complete.
                mGameThread.join();
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Main thread interrupted while waiting to join the game thread", e);

                // Reset the interrupt flag that was cleared when InterruptedException was thrown.
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void drawFrame() {

        // Lock the canvas. If not successful, do not proceed.
        if (!mRenderer.beginDrawing()) {
            Log.d(TAG, "drawFrame: unable to lock canvas!");
            return;
        }

        // Wipe everything by re-drawing the background color.
        mRenderer.drawBackground(mScene.getBackgroundColor());

        // Draw each game item.
        for (GameEngine.CircleToRender circle : mScene.getCirclesToRender()) {
            mRenderer.drawCircle(circle.getCenterX(), circle.getCenterY(), circle.getRadius(),
                    circle.getColor());
        }

        for (GameEngine.RectangleToRender rect : mScene.getRectanglesToRender()) {
            mRenderer.drawRect(rect.getLeftX(), rect.getTopY(), rect.getRightX(), rect.getBottomY(),
                    rect.getColor());
        }

        for (GameEngine.VerticalLineToRender line : mScene.getVerticalLinesToRender()) {
            mRenderer.drawVerticalLine(line.getX(), line.getTopY(), line.getBottomY(),
                    line.getColor(), line.isDashed());
        }

        // Draw the frames per second as text.
        long framesPerSecond = 0L;
        if (mLastFrameRenderTimeInMillis > 0L) {
            framesPerSecond = 1_000L / mLastFrameRenderTimeInMillis;
        }

        mRenderer.drawFramesPerSecond(
                String.format(Locale.getDefault(), FPS_TEMPLATE, framesPerSecond),
                FPS_X_COORDINATE, FPS_Y_COORDINATE, FPS_TEXT_SIZE, FPS_TEXT_COLOR);

        // Unlock the canvas and post the drawings.
        mRenderer.commitDrawing();
    }

    @Override
    public void run() {
        while (mExecuteGameLoop) {

            // Save frame render start time.
            long renderStartTimeInMillis = System.currentTimeMillis();

            // Update item positions. Use the last frame's rendering time as an estimate for how
            // long it will take to render this frame.
            boolean pointScored = mScene.updateGameObjectPositions(mLastFrameRenderTimeInMillis);

            // Draw the frame.
            drawFrame();

            // Track frame rendering time.
            mLastFrameRenderTimeInMillis = System.currentTimeMillis() - renderStartTimeInMillis;

            // If a point was scored, pause the game temporarily.
            if (pointScored) {
                mScene.resetAfterPointScored();

                // TODO - replace this with a countdown to the new ball
                try {
                    Thread.sleep(PAUSE_AFTER_SCORE_IN_MS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Game thread interrupted while sleeping after point scored", e);

                    // Reset the interrupt flag that was cleared when InterruptedException
                    // was thrown, then end thread by returning.
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
