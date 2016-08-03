/**
 * Interface to specify gameboard behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

interface GameboardModelInterface {
    boolean handleSwap(int[] coord1, int[] coord2);
    Rune[][] getBoard();
    int[][] getHintCoords();
}