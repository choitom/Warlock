/**
 * Interface to specify controller behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

interface ControllerInterface {
    void newGame(boolean displaySetting);
    void pause();
    void resume();

    void runeClick(int[] coord);

    int[][] hint();
    int getHintsRemaining();

    boolean recordScore();
    int[] getHighScores();
}