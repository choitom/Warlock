/**
 * Interface to specify view behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */
package warlock;

import javafx.stage.Stage;

interface ViewInterface {
    void start(Stage primaryStage);
    void select(int[] selectedRuneCoord);
    void deselect(int[] selectedRuneCoord);
    void playErrorSound();
}
