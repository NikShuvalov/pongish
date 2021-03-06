package com.charlesdrews.pongish.game.objects;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.charlesdrews.pongish.game.GameEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Container for the other game objects. Includes logic to update the positions of those objects.
 *
 * Created by charlie on 9/10/16.
 */
public class PongScene implements GameObjects.Scene, Parcelable {

    // =================================== Constants =============================================

    private static final String TAG = "PongScene";

    private static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;
    private static final float HORIZONTAL_THUMB_MARGIN_AS_PERCENT_OF_SCREEN_WIDTH = 0.11f;

    private static final int SCORE_COLOR = Color.GREEN;
    private static final float SCORE_TOP_MARGIN_AS_PERCENT_OF_GAME_BOARD_HEIGHT = 0.02f;
    private static final float SCORE_MARGIN_FROM_CENTER_AS_PERCENT_OF_GAME_BOARD_WIDTH = 0.03f;
    private static final float SCORE_TEXT_SIZE_AS_PERCENT_OF_GAME_BOARD_HEIGHT = 0.1f;

    private static final int CENTER_LINE_COLOR = Color.WHITE;
    private static final int END_LINE_COLOR = Color.WHITE;

    private static final int PADDLE_COLOR = Color.WHITE;
    private static final float PADDLE_HEIGHT_AS_PERCENT_OF_GAME_BOARD_HEIGHT = 0.2f;
    private static final float PADDLE_WIDTH_AS_PERCENT_OF_GAME_BOARD_WIDTH = 0.015f;

    private static final int NORMAL_BALL_POINTS = 3;
    private static final int NORMAL_BALL_COLOR = Color.WHITE;
    private static final float NORMAL_BALL_RADIUS_AS_PERCENT_OF_GAME_BOARD_WIDTH = 0.022f;
    private static final float NORMAL_BALL_SPEED_AS_PERCENT_OF_GAME_BOARD_WIDTH_PER_SECOND = 0.509f;

    private static final int BONUS_BALL_POINTS = 1;
    private static final int[] BONUS_BALL_COLORS = { Color.YELLOW, Color.CYAN, Color.MAGENTA };
    private static final float BONUS_BALL_RADIUS_AS_PERCENT_OF_GAME_BOARD_WIDTH = 0.015f;
    private static final float BONUS_BALL_SPEED_AS_PERCENT_OF_GAME_BOARD_WIDTH_PER_SECOND = 0.436f;

    private static final int BONUS_BALLS_CONSECUTIVE_HITS_THRESHOLD = 10;

    private static final float BALL_SPEED_INCREASE_ON_PADDLE_HIT_AS_PERCENT_OF_CURRENT_SPEED = 0.04f;

    private static final int BALL_COLOR_ON_POINT_SCORED = Color.RED;
    private static final int END_LINE_COLOR_ON_POINT_SCORED = Color.RED;
    private static final long MS_BEFORE_LINE_COLOR_REVERTS_AFTER_SCORE = 1_000L;

    private static final double MIN_ABS_VAL_DEG_AFTER_PADDLE_COLLISION = 10d;
    private static final double HALF_ABS_VAL_RANGE_AFTER_PADDLE_COLLISION =
            (180d - 2d * MIN_ABS_VAL_DEG_AFTER_PADDLE_COLLISION) / 2d;


    // ================================= Member variables =======================================

    private float mGameBoardWidth, mGameBoardHeight, mGameBoardHorizontalMargin;
    private int mBackgroundColor, mComputerControlledPaddle;
    private GameObjects.Score mLeftPlayerScore, mRightPlayerScore;
    private GameObjects.VerticalLine mLeftEndLine, mRightEndLine, mCenterLine;
    private GameObjects.Paddle mLeftPaddle, mRightPaddle;
    private GameObjects.Ball mNormalBall;
    private List<GameObjects.Ball> mBonusBalls;
    private int mConsecutivePaddleHits = 0;
    private boolean mNeedToAddBonusBalls = false;
    private boolean mCountDownInProgress = false;
    private long mTimeLeftEndLineTurnedRed = 0, mTimeRightEndLineTurnedRed = 0;

    private List<GameEngine.ScoreToRender> mScoresToRender;
    private List<GameEngine.VerticalLineToRender> mVerticalLinesToRender;
    private List<GameEngine.CircleToRender> mCirclesToRender;
    private List<GameEngine.RectangleToRender> mRectanglesToRender;


    // =================================== Constructor ==========================================

    /**
     * Instantiate a new Scene with the specified characteristics.
     *
     * @param availableWidth is the width in pixels available for the game board.
     * @param availableHeight is the height in pixels available for the game board.
     * @param computerControlledPaddle must be GameObjects.Scene.LEFT_PADDLE,
     *                                 GameObjects.Scene.RIGHT_PADDLE, or
     *                                 GameObjects.Scene.NEITHER_PADDLE.
     * @param gameBoardColor is the int representation of the scene's background color.
     */
    public PongScene(final int availableWidth, final int availableHeight,
                     final int computerControlledPaddle, final int gameBoardColor) {

        mGameBoardHorizontalMargin = availableWidth *
                HORIZONTAL_THUMB_MARGIN_AS_PERCENT_OF_SCREEN_WIDTH;

        mGameBoardWidth = availableWidth - (2 * mGameBoardHorizontalMargin);

        mGameBoardHeight = availableHeight;
        mBackgroundColor = gameBoardColor;

        float gameBoardCenterX = mGameBoardHorizontalMargin + (mGameBoardWidth / 2f);

        mLeftPlayerScore = new PongScore(SCORE_COLOR,
                gameBoardCenterX -
                        (SCORE_MARGIN_FROM_CENTER_AS_PERCENT_OF_GAME_BOARD_WIDTH * mGameBoardWidth),
                SCORE_TOP_MARGIN_AS_PERCENT_OF_GAME_BOARD_HEIGHT * mGameBoardHeight,
                SCORE_TEXT_SIZE_AS_PERCENT_OF_GAME_BOARD_HEIGHT * mGameBoardHeight,
                true);

        mRightPlayerScore = new PongScore(SCORE_COLOR,
                gameBoardCenterX +
                        (SCORE_MARGIN_FROM_CENTER_AS_PERCENT_OF_GAME_BOARD_WIDTH * mGameBoardWidth),
                SCORE_TOP_MARGIN_AS_PERCENT_OF_GAME_BOARD_HEIGHT * mGameBoardHeight,
                SCORE_TEXT_SIZE_AS_PERCENT_OF_GAME_BOARD_HEIGHT * mGameBoardHeight,
                false);

        mComputerControlledPaddle = computerControlledPaddle;
        initializeGameObjects();
    }

    /**
     * Instantiate a new Scene with the specified characteristics.
     *
     * @param availableWidth is the width in pixels available for the game board.
     * @param availableHeight is the height in pixels available for the game board.
     * @param computerControlledPaddle must be GameObjects.Scene.LEFT_PADDLE,
     *                                 GameObjects.Scene.RIGHT_PADDLE, or
     *                                 GameObjects.Scene.NEITHER_PADDLE.
     */
    public PongScene(final int availableWidth, final int availableHeight,
                     final int computerControlledPaddle) {
        this(availableWidth, availableHeight, computerControlledPaddle, DEFAULT_BACKGROUND_COLOR);
    }


    // ============================= GameObjects.PongScene methods ===============================

    protected PongScene(Parcel in) {
        mGameBoardWidth = in.readFloat();
        mGameBoardHeight = in.readFloat();
        mGameBoardHorizontalMargin = in.readFloat();
        mBackgroundColor = in.readInt();
        mComputerControlledPaddle = in.readInt();
        mLeftPlayerScore = in.readParcelable(GameObjects.Score.class.getClassLoader());
        mRightPlayerScore = in.readParcelable(GameObjects.Score.class.getClassLoader());
        mLeftEndLine = in.readParcelable(GameObjects.VerticalLine.class.getClassLoader());
        mRightEndLine = in.readParcelable(GameObjects.VerticalLine.class.getClassLoader());
        mCenterLine = in.readParcelable(GameObjects.VerticalLine.class.getClassLoader());
        mLeftPaddle = in.readParcelable(GameObjects.Paddle.class.getClassLoader());
        mRightPaddle = in.readParcelable(GameObjects.Paddle.class.getClassLoader());
        mNormalBall = in.readParcelable(GameObjects.Ball.class.getClassLoader());
        mBonusBalls = in.createTypedArrayList(PongBall.CREATOR);
        mConsecutivePaddleHits = in.readInt();
        mNeedToAddBonusBalls = in.readByte() != 0;
        mCountDownInProgress = in.readByte() != 0;
        mTimeLeftEndLineTurnedRed = in.readLong();
        mTimeRightEndLineTurnedRed = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mGameBoardWidth);
        dest.writeFloat(mGameBoardHeight);
        dest.writeFloat(mGameBoardHorizontalMargin);
        dest.writeInt(mBackgroundColor);
        dest.writeInt(mComputerControlledPaddle);
        dest.writeParcelable(mLeftPlayerScore, flags);
        dest.writeParcelable(mRightPlayerScore, flags);
        dest.writeParcelable(mLeftEndLine, flags);
        dest.writeParcelable(mRightEndLine, flags);
        dest.writeParcelable(mCenterLine, flags);
        dest.writeParcelable(mLeftPaddle, flags);
        dest.writeParcelable(mRightPaddle, flags);
        dest.writeParcelable(mNormalBall, flags);
        dest.writeTypedList(mBonusBalls);
        dest.writeInt(mConsecutivePaddleHits);
        dest.writeByte((byte) (mNeedToAddBonusBalls ? 1 : 0));
        dest.writeByte((byte) (mCountDownInProgress ? 1 : 0));
        dest.writeLong(mTimeLeftEndLineTurnedRed);
        dest.writeLong(mTimeRightEndLineTurnedRed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PongScene> CREATOR = new Creator<PongScene>() {
        @Override
        public PongScene createFromParcel(Parcel in) {
            return new PongScene(in);
        }

        @Override
        public PongScene[] newArray(int size) {
            return new PongScene[size];
        }
    };

    @Override
    public void movePaddle(final int paddle, final float deltaY, final long millisSinceLastUpdate) {
        if (!mCountDownInProgress) {
            if (paddle == LEFT_PADDLE) {
                mLeftPaddle.move(deltaY, mGameBoardHeight, millisSinceLastUpdate);
            } else if (paddle == RIGHT_PADDLE) {
                mRightPaddle.move(deltaY, mGameBoardHeight, millisSinceLastUpdate);
            }
        }
    }

    @Override
    public boolean updateGameObjects(final long millisSinceLastUpdate) {

        // If enough time has elapsed, reset colors for end lines
        if (mLeftEndLine.getColor() != END_LINE_COLOR &&
                System.currentTimeMillis() - mTimeLeftEndLineTurnedRed >
                        MS_BEFORE_LINE_COLOR_REVERTS_AFTER_SCORE) {
            mLeftEndLine.setColor(END_LINE_COLOR);
        }

        if (mRightEndLine.getColor() != END_LINE_COLOR &&
                System.currentTimeMillis() - mTimeRightEndLineTurnedRed >
                        MS_BEFORE_LINE_COLOR_REVERTS_AFTER_SCORE) {
            mRightEndLine.setColor(END_LINE_COLOR);
        }

        // Move normal ball (update direction if paddle hit, otherwise check if side wall hit)
        boolean pointScored = moveBallAndCheckResult(mNormalBall, millisSinceLastUpdate, true);

        // Do the same for each bonus ball
        for (GameObjects.Ball ball : mBonusBalls) {
            pointScored = pointScored || moveBallAndCheckResult(ball, millisSinceLastUpdate, false);
        }

        // If a point was not yet scored, bonus balls were not yet added, and the # of consecutive
        // hits exceeds threshold, then add bonus balls!
        if (!pointScored && mNeedToAddBonusBalls) {

            // If 2x the threshold is reached, release 2x the bonus balls, etc.
            for (int i = 0; i < mConsecutivePaddleHits / BONUS_BALLS_CONSECUTIVE_HITS_THRESHOLD; i++) {
                addBonusBalls();
            }
        }

        // Move computer controlled paddle
        switch (mComputerControlledPaddle) {

            case LEFT_PADDLE:
                moveComputerControlledPaddle(mLeftPaddle, LEFT_PADDLE, millisSinceLastUpdate);
                break;

            case RIGHT_PADDLE:
                moveComputerControlledPaddle(mRightPaddle, RIGHT_PADDLE, millisSinceLastUpdate);
                break;

            case BOTH_PADDLES:
                moveComputerControlledPaddle(mLeftPaddle, LEFT_PADDLE, millisSinceLastUpdate);
                moveComputerControlledPaddle(mRightPaddle, RIGHT_PADDLE, millisSinceLastUpdate);
                break;
        }

        return pointScored;
    }

    @Override
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    @Override
    public List<GameEngine.ScoreToRender> getScoresToRender() {
        return mScoresToRender;
    }

    @Override
    public List<GameEngine.CircleToRender> getCirclesToRender() {

        // Clear list of circles to render, in case it contains any expired bonus balls.
        mCirclesToRender.clear();

        // Add normal ball and bonus balls (if any exist).
        mCirclesToRender.add(mNormalBall);
        for (GameObjects.Ball ball : mBonusBalls) {
            mCirclesToRender.add(ball);
        }

        return mCirclesToRender;
    }

    @Override
    public List<GameEngine.RectangleToRender> getRectanglesToRender() {
        return mRectanglesToRender;
    }

    @Override
    public List<GameEngine.VerticalLineToRender> getVerticalLinesToRender() {
        return mVerticalLinesToRender;
    }

    @Override
    public void resetAfterPointScored() {
        initializeGameObjects();
    }

    @Override
    public void setCountdownInProgress(boolean countdownInProgress) {
        mCountDownInProgress = countdownInProgress;
    }


    // =========================== Parcelable methods & constant ==================================


    // ================================ Helper methods ===========================================

    /**
     * Add a left paddle, a right paddle, and the normal ball to the scene. If any bonus balls
     * exist, remove them. Also prepare the lists of circles and rectangles to return to the
     * renderer.
     */
    private void initializeGameObjects() {

        mConsecutivePaddleHits = 0;

        // Add left, right, and center line.
        mLeftEndLine = new PongLine(mGameBoardHorizontalMargin, 0, mGameBoardHeight,
                END_LINE_COLOR, false);

        mRightEndLine = new PongLine(mGameBoardHorizontalMargin + mGameBoardWidth, 0,
                mGameBoardHeight, END_LINE_COLOR, false);

        mCenterLine = new PongLine(mGameBoardHorizontalMargin + (mGameBoardWidth / 2f), 0,
                mGameBoardHeight, CENTER_LINE_COLOR, true);

        // Add left & right paddles and the normal ball.
        switch (mComputerControlledPaddle) {
            case LEFT_PADDLE:
                mLeftPaddle = getNewPaddle(true, LEFT_PADDLE);
                mRightPaddle = getNewPaddle(false, RIGHT_PADDLE);
                break;

            case RIGHT_PADDLE:
                mLeftPaddle = getNewPaddle(false, LEFT_PADDLE);
                mRightPaddle = getNewPaddle(true, RIGHT_PADDLE);
                break;

            case NEITHER_PADDLE:
                mLeftPaddle = getNewPaddle(false, LEFT_PADDLE);
                mRightPaddle = getNewPaddle(false, RIGHT_PADDLE);
                break;

            case BOTH_PADDLES:
                mLeftPaddle = getNewPaddle(true, LEFT_PADDLE);
                mRightPaddle = getNewPaddle(true, RIGHT_PADDLE);
                break;

            default:
                throw new IllegalArgumentException("Computer controlled paddle must be either " +
                        "GameObjects.Scene.LEFT_PADDLE, ...RIGHT_PADDLE, or ...NEITHER_PADDLE.");
        }

        mNormalBall = new PongBall(mGameBoardWidth, mGameBoardHeight, mGameBoardHorizontalMargin,
                NORMAL_BALL_RADIUS_AS_PERCENT_OF_GAME_BOARD_WIDTH * mGameBoardWidth,
                NORMAL_BALL_SPEED_AS_PERCENT_OF_GAME_BOARD_WIDTH_PER_SECOND *
                        mGameBoardWidth / 1000f,
                NORMAL_BALL_COLOR);

        // Instantiate an empty list for bonus balls, or if one exists, empty it.
        if (mBonusBalls == null) {
            mBonusBalls = new CopyOnWriteArrayList<>();
        }
        else {
            mBonusBalls.clear();
        }
        mNeedToAddBonusBalls = false;

        // Instantiate and initialize a list of scores to return to the renderer.
        mScoresToRender = new ArrayList<>(2);
        mScoresToRender.add(mLeftPlayerScore);
        mScoresToRender.add(mRightPlayerScore);

        // Instantiate and initialize a list of vertical lines to return to the renderer.
        mVerticalLinesToRender = new ArrayList<>(3);
        mVerticalLinesToRender.add(mLeftEndLine);
        mVerticalLinesToRender.add(mRightEndLine);
        mVerticalLinesToRender.add(mCenterLine);

        // Instantiate an empty list to hold balls as circles to return to the renderer.
        mCirclesToRender = new ArrayList<>();

        // Instantiate and initialize a list of paddles as rectangles to return to the renderer.
        mRectanglesToRender = new ArrayList<>(2);
        mRectanglesToRender.add(mLeftPaddle);
        mRectanglesToRender.add(mRightPaddle);
    }

    private GameObjects.Paddle getNewPaddle(boolean isComputerControlled, int paddlePosition) {
        return new PongPaddle(isComputerControlled, paddlePosition,
                PADDLE_WIDTH_AS_PERCENT_OF_GAME_BOARD_WIDTH * mGameBoardWidth,
                PADDLE_HEIGHT_AS_PERCENT_OF_GAME_BOARD_HEIGHT * mGameBoardHeight,
                mGameBoardWidth, mGameBoardHeight, mGameBoardHorizontalMargin, PADDLE_COLOR);
    }

    /**
     * Calculate the new direction for a ball that has struck the specified paddle in the given
     * location.
     *
     * @param paddlePosition is either GameObjects.Scene.LEFT_PADDLE or
     *                       GameObjects.Scene.RIGHT_PADDLE.
     * @param collisionLocation ranges from -1.0, bottom of paddle, to 1.0, top of paddle, with
     *                          0.0 being the exact center of the paddle.
     * @return a new ball direction in degrees.
     */
    private double getDirectionAfterPaddleCollision(final int paddlePosition,
                                                    final float collisionLocation) {

        double absoluteValueNewDirection = 90d +
                (-collisionLocation) * HALF_ABS_VAL_RANGE_AFTER_PADDLE_COLLISION;

        if (paddlePosition == GameObjects.Scene.LEFT_PADDLE) {
            return absoluteValueNewDirection;
        }
        else if (paddlePosition == GameObjects.Scene.RIGHT_PADDLE) {
            return -absoluteValueNewDirection;
        }
        else {
            throw new IllegalStateException("Paddle's position is neither " +
                    "GameObjects.Scene.LEFT_PADDLE nor GameObjects.Scene.RIGHT_PADDLE");
        }
    }

    /**
     * Check whether the given ball has hit either paddle. If so, update the ball's direction.
     *
     * @param ball whose position will be checked against the paddle positions.
     * @return true if ball hit a paddle, else false.
     */
    private boolean checkForPaddleCollisionsAndUpdateBall(GameObjects.Ball ball,
                                                          boolean isNormalBall) {

        // Check for collision with left paddle
        float collisionLocation = mLeftPaddle.getRelativeCollisionLocation(ball);

        if (collisionLocation != NO_PADDLE_HIT) {

            if (isNormalBall) {
                incrementConsecutiveHitsCounter();
            }

            ball.setDirection(getDirectionAfterPaddleCollision(LEFT_PADDLE, collisionLocation));
            ball.changeSpeed(BALL_SPEED_INCREASE_ON_PADDLE_HIT_AS_PERCENT_OF_CURRENT_SPEED);
            return true;
        }
        else {
            // If no collision with left paddle, check right paddle
            collisionLocation = mRightPaddle.getRelativeCollisionLocation(ball);
            if (collisionLocation != NO_PADDLE_HIT) {

                if (isNormalBall) {
                    incrementConsecutiveHitsCounter();
                }

                ball.setDirection(getDirectionAfterPaddleCollision(RIGHT_PADDLE, collisionLocation));
                ball.changeSpeed(BALL_SPEED_INCREASE_ON_PADDLE_HIT_AS_PERCENT_OF_CURRENT_SPEED);
                return true;
            }
        }
        return false;
    }

    private void incrementConsecutiveHitsCounter() {
        mConsecutivePaddleHits += 1;
        if (mConsecutivePaddleHits > 0
                && mConsecutivePaddleHits % BONUS_BALLS_CONSECUTIVE_HITS_THRESHOLD == 0) {
            mNeedToAddBonusBalls = true;
        }
    }

    /**
     * Take the following steps:
     *   1) Move the given Ball. Its move() method handles collisions with top/bottom walls.
     *   2) Check if it hit a paddle and if so, change it's direction accordingly.
     *   3) If no paddle hit, check if a point was scored; return true if yes.
     * @param ball is the Ball whose position will be updated.
     * @param millisSinceLastUpdate is the time in milliseconds since the ball was last moved.
     * @return true if a point was scored, else false.
     */
    private boolean moveBallAndCheckResult(GameObjects.Ball ball, long millisSinceLastUpdate,
                                           boolean isNormalBall) {

        // Start by updating the ball's position
        ball.move(millisSinceLastUpdate, mGameBoardHeight);

        // Then check if it hit a paddle, and update its direction if yes
        if (!checkForPaddleCollisionsAndUpdateBall(ball, isNormalBall)) {

            // If the ball hasn't hit either paddle, check if it hit the left or right wall
            int hit = ball.checkIfPointScored(mGameBoardWidth, mGameBoardHorizontalMargin);

            // If a side wall was hit, return true so the game engine knows to pause the loop
            switch (hit) {
                case GameObjects.Scene.LEFT_WALL_HIT: {
                    ball.setColor(BALL_COLOR_ON_POINT_SCORED);

                    mLeftEndLine.setColor(END_LINE_COLOR_ON_POINT_SCORED);
                    mTimeLeftEndLineTurnedRed = System.currentTimeMillis();

                    if (isNormalBall) {
                        mRightPlayerScore.increaseScore(NORMAL_BALL_POINTS);
                        mConsecutivePaddleHits = 0;
                    }
                    else {
                        mRightPlayerScore.increaseScore(BONUS_BALL_POINTS);
                        mBonusBalls.remove(ball);
                    }

                    return isNormalBall;
                }

                case GameObjects.Scene.RIGHT_WALL_HIT: {
                    ball.setColor(BALL_COLOR_ON_POINT_SCORED);

                    mRightEndLine.setColor(END_LINE_COLOR_ON_POINT_SCORED);
                    mTimeRightEndLineTurnedRed = System.currentTimeMillis();

                    if (isNormalBall) {
                        mLeftPlayerScore.increaseScore(NORMAL_BALL_POINTS);
                        mConsecutivePaddleHits = 0;
                    }
                    else {
                        mLeftPlayerScore.increaseScore(BONUS_BALL_POINTS);
                        mBonusBalls.remove(ball);
                    }

                    return isNormalBall;
                }

                default:
                    return false;
            }
        }
        else {
            return false;
        }
    }

    private void addBonusBalls() {
        for (int color : BONUS_BALL_COLORS) {
            mBonusBalls.add(new PongBall(mGameBoardWidth, mGameBoardHeight,
                    mGameBoardHorizontalMargin,
                    BONUS_BALL_RADIUS_AS_PERCENT_OF_GAME_BOARD_WIDTH * mGameBoardWidth,
                    BONUS_BALL_SPEED_AS_PERCENT_OF_GAME_BOARD_WIDTH_PER_SECOND *
                            mGameBoardWidth / 1000f,
                    color));
        }
        mNeedToAddBonusBalls = false;
    }

    private void moveComputerControlledPaddle(GameObjects.Paddle paddle, int paddlePosition,
                                              long millisSinceLastUpdate) {

        float closestBallX = mNormalBall.getCenterX();
        float closestBallY = mNormalBall.getCenterY();

        for (GameObjects.Ball ball : mBonusBalls) {
            if (paddlePosition == LEFT_PADDLE && mNormalBall.getDirection() > 0) {
                if (ball.getCenterX() < closestBallX) {
                    closestBallX = ball.getCenterX();
                    closestBallY = ball.getCenterY();
                }
            }
            else if (paddlePosition == RIGHT_PADDLE && mNormalBall.getDirection() < 0) {
                if (ball.getCenterX() > closestBallX) {
                    closestBallX = ball.getCenterX();
                    closestBallY = ball.getCenterY();
                }
            }
        }
        paddle.move(closestBallY - paddle.getCenterY(), mGameBoardHeight,
                millisSinceLastUpdate);
    }
}
