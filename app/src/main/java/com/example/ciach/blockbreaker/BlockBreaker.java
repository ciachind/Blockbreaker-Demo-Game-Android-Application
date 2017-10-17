/** Dave Ciachin Block Breaker Asgn 12
*/

package com.example.ciach.blockbreaker;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;

public class BlockBreaker extends Activity {


    BlockBreakerView blockBreakerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the blockBreakerView
        blockBreakerView = new BlockBreakerView(this);
        setContentView(blockBreakerView);

    }

    class BlockBreakerView extends SurfaceView implements Runnable {

        Thread gameThread = null;

        SurfaceHolder ourHolder;

        // set for playing
        volatile boolean playing;

        // Keep paused at the start
        boolean paused = true;

        Canvas canvas;
        Paint paint;

        // Keep track of the frame rate
        long fps;

        // Calculate the fps
        private long timeThisFrame;

        // Initialize screen size
        int screenX;
        int screenY;

        // Initialize the game paddle
        Paddle paddle;

        // Initialize the ball
        Ball ball;

        // Set number of blocks
        Block[] blocks = new Block[200];
        int numBlocks = 0;

        // Set sound effects
        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;

        // Initialize the score at 0
        int score = 0;

        // Set number of lives
        int lives = 3;

        // Method for the view
        public BlockBreakerView(Context context) {

            super(context);

            // Initialize holder and paint objects
            ourHolder = getHolder();
            paint = new Paint();

            // Get a Display object to access screen details
            Display display = getWindowManager().getDefaultDisplay();
            // Load the resolution into a Point object
            Point size = new Point();
            display.getSize(size);

            screenX = size.x;
            screenY = size.y;

            paddle = new Paddle(screenX, screenY);

            // Create a ball
            ball = new Ball(screenX, screenY);

            // Load the sounds

            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            try {
                // Create the objects
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                // Load sound effects
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);

            } catch (IOException e) {
                // Print an error message
                Log.e("error", "failed to load sound files");
            }

            createBlocksAndRestart();

        }

        public void createBlocksAndRestart() {

            // Reset ball position
            ball.reset(screenX, screenY);

            int blockWidth = screenX / 8;
            int blockHeight = screenY / 10;

            // Setup the blocks
            numBlocks = 0;
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 3; row++) {
                    blocks[numBlocks] = new Block(row, column, blockWidth, blockHeight);
                    numBlocks++;
                }
            }
            // if game over reset scores and lives
            if (lives == 0) {
                score = 0;
                lives = 3;
            }
        }

        @Override
        public void run() {
            while (playing) {
                // Capture the current time
                long startFrameTime = System.currentTimeMillis();
                // Update the frame
                if (!paused) {
                    update();
                }
                // Draw the frame
                draw();
                // Calculate fps
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }

            }

        }

        // Place for updates
        public void update() {

            // Move paddle if necessary
            paddle.update(fps);

            ball.update(fps);

            // Check for ball colliding with a block
            for (int i = 0; i < numBlocks; i++) {
                if (blocks[i].getVisibility()) {
                    if (RectF.intersects(blocks[i].getRect(), ball.getRect())) {
                        blocks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }
            // Check for ball colliding with paddle
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }
            // Bounce the ball back when it hits the bottom of screen
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 2);

                // Lose a life
                lives--;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                if (lives == 0) {
                    paused = true;
                    createBlocksAndRestart();
                }
            }

            // Bounce the ball back when it hits the top of screen
            if (ball.getRect().top < 0)

            {
                ball.reverseYVelocity();
                ball.clearObstacleY(12);

                soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }

            // bounce off left wall on collision
            if (ball.getRect().left < 0)

            {
                ball.reverseXVelocity();
                ball.clearObstacleX(2);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // bounce of right wall on collision
            if (ball.getRect().right > screenX - 10) {

                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - 22);

                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Pause if cleared screen
            if (score == numBlocks * 10)

            {
                paused = true;
                createBlocksAndRestart();
            }

        }

        // Draw a new scene
        public void draw() {

            // Check that drawing surface is valid
            if (ourHolder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();

                // Draw the background color
                canvas.drawColor(Color.argb(255, 26, 128, 182));

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the paddle
                canvas.drawRect(paddle.getRect(), paint);

                // Draw the ball
                canvas.drawRect(ball.getRect(), paint);

                // Change the brush color for drawing
                paint.setColor(Color.argb(255, 249, 129, 0));

                // Draw the blocks if visible
                for (int i = 0; i < numBlocks; i++) {
                    if (blocks[i].getVisibility()) {
                        canvas.drawRect(blocks[i].getRect(), paint);
                    }
                }

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the score
                paint.setTextSize(40);
                canvas.drawText("Score: " + score + "   Lives: " + lives, 10, 50, paint);

                // Check if all blocks are gone
                if (score == numBlocks * 10) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU WON!", 10, screenY / 2, paint);
                }

                // Check if lives left
                if (lives <= 0) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU LOST!", 10, screenY / 2, paint);
                }

                // Draw everything to the screen
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // Check if paused
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        // Check if started
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // The SurfaceView class implements onTouchListener
        // Override this method and detect screen touches.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:
                    paused = false;
                    if (motionEvent.getX() > screenX / 2) {

                        paddle.setMovementState(paddle.RIGHT);
                    } else

                    {
                        paddle.setMovementState(paddle.LEFT);
                    }

                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }

            return true;
        }

    }

    // Method executes when player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the resume method to execute
        blockBreakerView.resume();
    }

    // Executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tells the game to pause
        blockBreakerView.pause();
    }

    public static class Paddle {

        // Object that holds 4 coordinates
        private RectF rect;

        // Length and height of paddle
        private float length;
        private float height;

        // Left side of paddle
        private float x;

        // Top of paddle
        private float y;

        // Sets the speed the paddle will move
        private float paddleSpeed;

        // Paddle movements
        public final int STOPPED = 0;
        public final int LEFT = 1;
        public final int RIGHT = 2;

        // Check if paddle is moving
        private int paddleMoving = STOPPED;

        // Screen width and height
        public Paddle(int screenX, int screenY){
            // 130 pixels wide and 20 pixels high
            length = 130;
            height = 20;

            // Start paddle at center
            x = screenX / 2;
            y = screenY - 20;

            rect = new RectF(x, y, x + length, y + height);

            // Speed of the paddle in pixels per second
            paddleSpeed = 850;
        }

        // Creates the paddle rectangle
        public RectF getRect(){
            return rect;
        }

        // Set if the paddle is going left, right or nowhere
        public void setMovementState(int state){
            paddleMoving = state;
        }

        // Determines if the paddle needs to move and changes the coordinates
        // contained in rect
        public void update(long fps){
            if(paddleMoving == LEFT){
                x = x - paddleSpeed / fps;
            }

            if(paddleMoving == RIGHT){
                x = x + paddleSpeed / fps;
            }


            rect.left = x;
            rect.right = x + length;
        }

    }

    public static class Block {

        private RectF rect;

        private boolean isVisible;

        public Block(int row, int column, int width, int height){

            isVisible = true;

            int padding = 1;

            rect = new RectF(column * width + padding,
                    row * height + padding,
                    column * width + width - padding,
                    row * height + height - padding);
        }

        public RectF getRect(){
            return this.rect;
        }

        public void setInvisible(){
            isVisible = false;
        }

        public boolean getVisibility(){
            return isVisible;
        }
    }

    public static class Ball {
        RectF rect;
        float xVelocity;
        float yVelocity;
        float ballWidth = 10;
        float ballHeight = 10;

        public Ball(int screenX, int screenY){

            // Ball travelling straight up at 100 pixels per second
            xVelocity = 200;
            yVelocity = -400;

            // Place the ball in the centre of the screen at the bottom
            // 10 pixel x 10 pixel square
            rect = new RectF();

        }

        public RectF getRect(){
            return rect;
        }

        public void update(long fps){
            rect.left = rect.left + (xVelocity / fps);
            rect.top = rect.top + (yVelocity / fps);
            rect.right = rect.left + ballWidth;
            rect.bottom = rect.top - ballHeight;
        }

        public void reverseYVelocity(){
            yVelocity = -yVelocity;
        }

        public void reverseXVelocity(){
            xVelocity = - xVelocity;
        }

        public void setRandomXVelocity(){
            Random generator = new Random();
            int answer = generator.nextInt(2);

            if(answer == 0){
                reverseXVelocity();
            }
        }

        public void clearObstacleY(float y){
            rect.bottom = y;
            rect.top = y - ballHeight;
        }

        public void clearObstacleX(float x){
            rect.left = x;
            rect.right = x + ballWidth;
        }

        public void reset(int x, int y){
            rect.left = x / 2;
            rect.top = y - 20;
            rect.right = x / 2 + ballWidth;
            rect.bottom = y - 20 - ballHeight;
        }

    }
}
// End BlockBreaker
