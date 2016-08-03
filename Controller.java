/**
 * Implementation of the controller. This file contains the main method of the program. This class plays the role of
 * the controller in the MVC design pattern and the role of the mediator in the Mediator pattern.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;

public class Controller extends Application implements ControllerInterface {

    /**
     * CONSTANTS
     */

    public static final double GAME_TIME = 120;


    /**
     * INSTANCE VARIABLES
     */

    // The other MVC components
    private View view;
    private GameboardModel gameboardModel;
    private TimerModel timerModel;

    private int[] selectedRuneCoord;
    private boolean runeIsSelected;

    private Stage newGame;
    private MediaPlayer overworldPlayer;

    /**
     * Main method of Warlock
     * @param args unused command line arguments
     */
    public static void main(String[] args){
        launch(args);
    }


    /**
     * PUBLIC METHODS
     */

    /**
     * Starts a new game with its starting interface. This is a method from the Application superclass
     * This method is called only once, at the beginning of the program
     */
    public void start(Stage stage) {
        overworldPlayer = new MediaPlayer(new Media(new File("warlock/resources/sounds/Overworld.mp3").toURI().toString()));
        overworldPlayer.setVolume(0.5);
        overworldPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        overworldPlayer.play();

        newGame = stage;
        newGame(View.DISPLAY_MENU);
    }

    /**
     * When the application is exited, end all processes.
     * This stops the animation thread, and all timer threads.
     */
    public void stop() {
        System.exit(0);
    }

    /**
     * Changes the selected status of the runes
     * If the runes selected are adjacent to each other, ask model to swap them
     * @param coord the coordinates of the rune clicked on
     */
    public void runeClick(int[] coord) {
        if (runeIsSelected) {
            if (coordsAreAdjacent(coord, selectedRuneCoord)) {
                view.deselect(selectedRuneCoord);
                runeIsSelected = false;

                boolean swapResultsInMatch = gameboardModel.handleSwap(coord, selectedRuneCoord);

                if (!swapResultsInMatch) {
                    view.playErrorSound();
                }

            } else if (coord[0] == selectedRuneCoord[0] && coord[1] == selectedRuneCoord[1]) {
                view.deselect(selectedRuneCoord);
                runeIsSelected = false;
            } else {
                view.deselect(selectedRuneCoord);
                selectedRuneCoord = coord;
                view.select(selectedRuneCoord);
            }
        } else {
            runeIsSelected = true;
            selectedRuneCoord = coord;
            view.select(selectedRuneCoord);
        }
    }

    /**
     * Tells the model to stop the timer of the game
     */
    public void pause() {
        timerModel.stopTimer();
    }

    /**
     * Tells the model to start the timer of the game
     */
    public void resume() {
        timerModel.startTimer();
    }

    /**
     * Asks the model for a hint for View to display
     * @return a 2D int array containing the coords of hint runes, or null if no hints remain
     */
    public int[][] hint() {
        if (getHintsRemaining() > 0) {
            return gameboardModel.getHintCoords();
        }
        return null;
    }

    /**
     * Returns the number of hints remaining
     * @return the number of hints
     */
    public int getHintsRemaining() {
        return gameboardModel.getHintsRemaining();
    }

    /**
     * Refresh and starts a new game
     * @param displaySetting either View.DISPLAY_MENU or View.DISPLAY_GAME
     */
    public void newGame(boolean displaySetting){
        // Create and set up new MVC components
        gameboardModel = new GameboardModel();
        timerModel = new TimerModel((int) GAME_TIME);
        view = new View(gameboardModel.getBoard(), this, displaySetting);
        gameboardModel.addGameboardObserver(view);
        timerModel.addTimerObserver(view);

        // Initialize variables and start the game!
        selectedRuneCoord = new int[2];
        runeIsSelected = false;
        view.start(newGame);
    }

    /**
     * Tells the model to record the score
     * @return true if its a highscore, else false
     */
    public boolean recordScore() {
        return gameboardModel.recordScore();
    }

    /**
     * Tells view what the current high scores are
     */
    public int[] getHighScores(){
        return gameboardModel.getHighScores();
    }


    /**
     * PRIVATE METHODS
     */

    /**
     * Determines if two rune coordinates are adjacent to each other
     * @param coordA one coordinate
     * @param coordB the other coordinate
     * @return whether the runes are adjacent to each other or not
     */
    private boolean coordsAreAdjacent(int[] coordA, int[] coordB) {
        if (coordA[0] == coordB[0]) {
            if (coordA[1] == coordB[1] + 1 || coordA[1] == coordB[1] - 1) {
                return true;
            }
        } else if (coordA[1] == coordB[1]) {
            if (coordA[0] == coordB[0] + 1 || coordA[0] == coordB[0] - 1) {
                return true;
            }
        }

        return false;
    }

}
