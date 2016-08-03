/**
 * Class to represent a single rune.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

import javafx.scene.image.Image;
import java.util.Random;

public class Rune {
    private static final String[] possibleTypes = {"Fire", "Water", "Air", "Metal", "Ice", "Thunder", "Plant"};

    /**
     * INSTANCE VARIABLES
     */
    private final String typeName;
    private final Image image;
    private boolean isMatched;

    /**
     * CONTRUCTOR
     * Randomly generates a rune
     */
    public Rune() {
        Random random = new Random();
        typeName = possibleTypes[random.nextInt(possibleTypes.length)];
        image = new Image(getClass().getResourceAsStream("resources/images/" + typeName + ".png"));
    }

    /**
     * CONTRUCTOR
     * Copies another rune's settings
     * @param runeToCopy the rune to copy
     */
    public Rune(Rune runeToCopy) {
        typeName = runeToCopy.getTypeName();
        image = runeToCopy.getImage();
    }

    /**
     * Mutator for isMatched
     * @param isMatched
     */
    public void setIsMatched(boolean isMatched) {this.isMatched = true;}

    /**
     * Accessor for isMatched
     * @return a rune's matched value
     */
    public boolean getIsMatched() {return isMatched;}

    /**
     * Accessor for typeName
     * @return rune type
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Accessor for image
     * @return rune image
     */
    public Image getImage() {return image;}
}