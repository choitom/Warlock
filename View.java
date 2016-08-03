/**
 * Implementation of the view. Plays the role of view in MVC and of Observer in the Observer pattern.
 * View displays the game and accepts user input. It passes information to controller when communication with the models is required.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Queue;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class View extends Application implements ViewInterface, GameboardObserver, TimerObserver {


    /**
     * CONSTANTS
     */

    public static final boolean DISPLAY_MENU = true;
    private static final boolean DISPLAY_GAME = false;
    private static final boolean DISPLAY_BACK_BUTTON = true;
    private static final boolean HIDE_BACK_BUTTON = false;
    private static final String TEXT_ALIGN_CENTER = "-fx-text-alignment: center;";
    private static final String TEXT_ALIGN_JUSTIFY = "-fx-text-alignment: justify;";
    private static final String BORDER_COLOR_BLACK = "-fx-border-color: black;";
    private static final String BORDER_COLOR_TRANSPARENT = "-fx-border-color: transparent;";
    private static final String BORDER_COLOR_RED = "-fx-border-color: red;";
    private static final String BORDER_COLOR_BLUE = "-fx-border-color: blue;";


    /**
     * INSTANCE VARIABLES
     */

    // Sections of the view
    private BorderPane gameInterface;
    private GridPane gamePane;
    private VBox gameMenu;
    private HBox timerPane;

    // Buttons
    private Button menuButton;
    private Button pauseButton;
    private Button hintButton;
    private Button seeHighScoreButton;
    private Button newGameButton;
    private Button instructionButton;
    private Button backButton;
    private Button resumeButton;
    private Button exitButton;
    private Button backToMenuButton;

    // Timer items
    private Label clockLabel;
    private ProgressBar timerBar;

    // Score display
    private Label scoreLabel;

    // The current state of the board as displayed in the view
    private Rune[][] board;
    private int[][] hintCoords;
    private boolean hintIsDisplayed;
    private boolean inAnimation;

    // The controller
    private final ControllerInterface controller;

    // The current score
    private int score;

    // Display settings
    private boolean menuDisplaySetting;
    private boolean backButtonDisplaySetting;

    // The screen dimensions
    private double screenHeight;
    private double screenWidth;

    // Large chunks of text!
    private String instructions;

    // The thread that contains the rune fill in animation
    private Thread animationLoop;

    // Sound effects
    private final AudioClip errorClip;
    private final AudioClip menuHoverClip;
    private final AudioClip successClip;
    private final AudioClip countdownClip;
    private final AudioClip shuffleClip;

    /**
     * CONSTRUCTOR
     * Creates a new view with a board and a pointer to the controller
     * @param board the board to display
     * @param controller a pointer to the controller
     * @param displaySetting either DISPLAY_MENU or DISPLAY_GAME
     */
    public View(Rune[][] board, Controller controller, boolean displaySetting) {
        this.board = board;
        this.controller = controller;
        this.menuDisplaySetting = displaySetting;
        hintCoords = new int[2][2];
        hintIsDisplayed = false;
        inAnimation = false;
        score = 0;

        instructions =
            "The goal of the game is to make as many rune matches as possible before time runs out. " +
            "A rune match is a series of at least three adjacent runes of the same type, either vertically or horizontally. " +
            "Click on a rune to select it. Then click on an adjacent rune to swap it with the previously selected rune. The runes will only swap if doing so " +
            "creates a valid match. You get more points for making longer matches! New runes will fill the empty spaces left by matched runes. If there are no possible matches " +
            "on the board, the board will randomly reshuffle. If you need a hint, click on the Hint button. You get three hints per game.\n" +
            "Good luck!";

        errorClip = new AudioClip(getClass().getResource("resources/sounds/Error.wav").toString());
        menuHoverClip = new AudioClip(getClass().getResource("resources/sounds/MenuHover.wav").toString());
        successClip = new AudioClip(getClass().getResource("resources/sounds/Success.wav").toString());
        countdownClip = new AudioClip(getClass().getResource("resources/sounds/Countdown.wav").toString());
        shuffleClip = new AudioClip(getClass().getResource("resources/sounds/Shuffle.wav").toString());
        countdownClip.setVolume(0.4);
        successClip.setVolume(0.4);
    }


    /**
     * PUBLIC METHODS
     */

    /**
     * JavaFX method called when the view is launched. Creates and displays the game interface
     * @param primaryStage the stage on which the view is displayed
     */
    public void start(Stage primaryStage) {
        // Get screen dimensions
        screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        screenWidth = Screen.getPrimary().getVisualBounds().getWidth();

        // Create game interface
        gameInterface = getGameInterface();
        if (menuDisplaySetting == DISPLAY_MENU){
            backButtonDisplaySetting = HIDE_BACK_BUTTON;
            showMenu();
        } else {
            backButtonDisplaySetting = DISPLAY_BACK_BUTTON;
        }
        Scene scene = new Scene(gameInterface);
        scene.setFill(Color.rgb(255, 255, 255));

        // Add css stylesheet
        String stylesheet = View.class.getResource("resources/viewStyle.css").toExternalForm();
        assert stylesheet != null;
        scene.getStylesheets().add(stylesheet);

        // Set up and display window
        primaryStage.setTitle("Warlock");
        primaryStage.setScene(scene);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreen(true);
        primaryStage.show();

        // Resize game items to fit on screen
        gamePane.setScaleX(0.67 * screenHeight / gamePane.getHeight());
        gamePane.setScaleY(0.67 * screenHeight / gamePane.getHeight());
        gameInterface.setScaleX(0.8 * screenWidth / gameInterface.getWidth());
        gameInterface.setScaleY(0.8 * screenHeight / gameInterface.getHeight());
    }

    /**
     * Kills the animation thread when the program is closed.
     */
    public void stop() {
        try {
            animationLoop.join();
        } catch (InterruptedException ignored) {}
    }


    /**
     * Displays the menu page.
     */
    private void showMenu(){
        VBox menuPage = createMenuPage();

        gameInterface.setLeft(null);
        gameInterface.setBottom(null);
        gameInterface.setCenter(menuPage);

        controller.pause();
    }

    /**
     * Show that a rune is selected.
     * @param selectedRuneCoord the coordinates of the selected rune
     */
    public void select(int[] selectedRuneCoord) {
        getRuneLabelAtCoord(selectedRuneCoord).setStyle(BORDER_COLOR_BLACK);
    }

    /**
     * Remove the selection box around a rune.
     * @param selectedRuneCoord the coordinates of the rune to deselect
     */
    public void deselect(int[] selectedRuneCoord) {
        if (hintIsDisplayed && (isSameCoordinate(selectedRuneCoord, hintCoords[0]) || isSameCoordinate(selectedRuneCoord, hintCoords[1]))) {
            getRuneLabelAtCoord(selectedRuneCoord).setStyle(BORDER_COLOR_RED);
        } else {
            getRuneLabelAtCoord(selectedRuneCoord).setStyle(BORDER_COLOR_TRANSPARENT);
        }
    }

    /**
     * Observer method that updates the board display
     * @param highlightStages a queue of rune matches to be highlighted, in order
     * @param boardStages a queue of board states to be displayed, in order
     */
    public void updateBoard(Queue<Stack<int[]>> highlightStages, Queue<Rune[][]> boardStages) {

        // The animation runs in an independent thread
        animationLoop = new Thread(() -> {

            // Get ready to display the fill in animation. Remove all highlighting
            Platform.runLater(this::removeRuneHighlighting);
            hintIsDisplayed = false;
            inAnimation = true;

            // Loop while there are board states to be displayed
            while (!boardStages.isEmpty()) {

                Stack<int[]> highlightStage = highlightStages.poll();
                Rune[][] boardStage = boardStages.poll();

                // Display the board with matches
                Platform.runLater(() -> {
                    board = boardStage;
                    updateGamePane();
                });

                // Highlight each matched rune
                if (highlightStage != null) {
                    successClip.play();
                    while (!highlightStage.isEmpty()) {
                        int[] runeToHighlightCoord = highlightStage.pop();
                        Platform.runLater(() -> getRuneLabelAtCoord(runeToHighlightCoord).setStyle(BORDER_COLOR_BLUE));
                    }
                }

                // Pause the animation thread for 1 second so the player can see the matches
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

                // Now, resolve the matches and display the runes that fill in
                Rune[][] resolvedBoardStage = boardStages.poll();
                if (resolvedBoardStage != null) {
                    Platform.runLater(() -> {
                        removeRuneHighlighting();
                        board = resolvedBoardStage;
                        updateGamePane();
                    });
                }

                // Pause the animation thread for 0.25 seconds before repeating the loop
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }

            inAnimation = false;
        });

        animationLoop.start();
    }

    /**
     * Update the score display to show the new score.
     * @param score the new score
     */
    public void updateScore(int score) {
        if (score == this.score) {
            return;
        }

        // Display the added score
        int addedScore = score - this.score;
        scoreLabel.setText("Score: " + this.score + "\n+" + addedScore);
        this.score = score;

        // Schedule the score display to update to the new score
        Timer timer = new Timer();
        timer.schedule(
            new TimerTask() {
                public void run() {
                    Platform.runLater(() -> scoreLabel.setText("Score: " + score));
                    timer.cancel();
                }
            }
        , 1000);
    }

    /**
     * Observer method that updates the timer
     * @param time the new time
     */
    public void updateTimer(int time) {
        Platform.runLater(() -> {
            if (clockLabel != null) {
                clockLabel.setText("Timer: " + time);
            }

            if (timerBar != null) {
                timerBar.setProgress(time / Controller.GAME_TIME);
            }

            if (time == 10) {
                countdownClip.play();
            }

        });
    }

    /**
     * Displays the shuffle message for short period of time
     */
    public void displayShuffleMessage() {
        Platform.runLater(() -> {
            StackPane tempGamePane = new StackPane();
            tempGamePane.setMaxSize(gamePane.getWidth(), gamePane.getHeight());

            // Display the shuffle message
            Label shuffleMessage = new Label("Board reshuffled!");
            shuffleMessage.setTranslateY(-1 * screenHeight / 2);
            shuffleMessage.setId("shuffle-label");
            tempGamePane.getChildren().addAll(gamePane, shuffleMessage);
            gameInterface.setCenter(tempGamePane);

            shuffleClip.play();

            // Removes the message after two seconds
            Timer timer = new Timer();
            timer.schedule(
                new TimerTask() {
                    public void run() {
                        Platform.runLater(() -> gameInterface.setCenter(gamePane));
                        timer.cancel();
                    }
                }, 2000);
        });
    }

    /**
     * Ends the current game and displays the score.
     */
    public void terminateGame() {
        Platform.runLater(() -> {
            backButtonDisplaySetting = HIDE_BACK_BUTTON;
            controller.pause();
            boolean isHighScore = controller.recordScore();
            displayFinalScore(isHighScore);
        });
    }

    /**
     * Plays the error sound
     */
    public void playErrorSound() {
        errorClip.play();
    }

    /**
     * PRIVATE METHODS
     */

    /**
     * Creates the game interface. Menu buttons, title, board, etc
     * @return a BorderPane containing the game interface
     */
    private BorderPane getGameInterface(){
        initializeButtons();

        HBox titleBar = createTitleBar();
        VBox menuBar = createInGameMenuBar();
        timerPane = createTimer();
        gamePane = createGamePane();

        BorderPane gameInterface = new BorderPane();
        gameInterface.setId("game-interface");
        gameInterface.setTop(titleBar);
        gameInterface.setLeft(menuBar);
        gameInterface.setCenter(gamePane);
        gameInterface.setBottom(timerPane);

        return gameInterface;
    }

    /**
     * Creates the HBox that hold the game's title and the current score.
     * @return an HBox with the title bar elements
     */
    private HBox createTitleBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_CENTER);
        topBar.setMinWidth(screenWidth);

        // Create the title
        Label title = new Label("WarlocK");
        title.setId("title");
        title.setMinWidth(Region.USE_PREF_SIZE);
        title.setTranslateY(-100);
        title.setAlignment(Pos.CENTER_LEFT);
        title.setTranslateX(20);

        // Create the score display
        scoreLabel = new Label("Score: " + score);
        scoreLabel.setId("score");
        scoreLabel.setMinWidth(Region.USE_PREF_SIZE);
        scoreLabel.setAlignment(Pos.CENTER_RIGHT);
        scoreLabel.setTranslateX(-20);

        Node space = new Pane();
        topBar.getChildren().addAll(title, space,  scoreLabel);
        HBox.setHgrow(space, Priority.ALWAYS);

        return topBar;
    }

    /**
     * Creates a VBox that holds the in-game menu
     * @return the VBox containing the menu buttons
     */
    private VBox createInGameMenuBar() {
        VBox menu = new VBox();
        menu.setId("menu");
        menu.setSpacing(screenHeight / 12);
        menu.setTranslateX(screenWidth / 12);
        menu.setTranslateY(screenHeight / 6);

        // Size the buttons correctly
        double buttonWidth = screenWidth / 6;
        double buttonHeight = screenHeight / 10;
        menuButton.setMinSize(buttonWidth, buttonHeight);
        pauseButton.setMinSize(buttonWidth, buttonHeight);
        resumeButton.setMinSize(buttonWidth, buttonHeight);
        hintButton.setMinSize(buttonWidth, buttonHeight);

        menu.getChildren().addAll(menuButton, pauseButton, hintButton);
        menu.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        gameMenu = menu;

        return menu;
    }

    /**
     * Creates the timer items.
     * @return An HBox containing the timer items.
     */
    private HBox createTimer() {
        HBox timerPane = new HBox();

        // Create the time number display
        clockLabel = new Label();
        clockLabel.setId("clock-label");
        clockLabel.setTranslateX(screenWidth / 20);
        clockLabel.setTranslateY(40);


        // Create the timer bar
        timerBar = new ProgressBar(1);
        timerBar.setMinSize(screenWidth / 1.5, screenHeight / 15);
        timerBar.setTranslateY(40);
        timerBar.setTranslateX(screenWidth / -12);

        Node space = new Pane();
        timerPane.getChildren().addAll(clockLabel, space,  timerBar);
        HBox.setHgrow(space, Priority.ALWAYS);
        timerPane.setAlignment(Pos.TOP_CENTER);

        return timerPane;
    }

    /**
     * Creates the GridPane that holds the game board
     * @return the GridPane containing the board
     */
    private GridPane createGamePane() {
        GridPane gameBoard = new GridPane();
        gameBoard.setId("rune-grid-pane");


        // Create labels for each rune with the rune image
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Label label = new Label();
                label.setGraphic(new ImageView(board[row][col].getImage()));
                label.setId("rune-label");

                label.setOnMouseClicked(event -> {
                    int[] coord = {GridPane.getRowIndex((Node)event.getSource()), GridPane.getColumnIndex((Node)event.getSource())};
                    if (!inAnimation) {
                        controller.runeClick(coord);
                    }
                });

                gameBoard.add(label, col, row);
            }
        }

        gameBoard.setAlignment(Pos.CENTER);
        gameBoard.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        return gameBoard;
    }

    /**
     * Creates the menu page.
     * @return a BorderPane containing the menu page
     */
    private VBox createMenuPage(){
        VBox menuPage = new VBox();
        menuPage.setSpacing(screenHeight / 40);
        menuPage.setAlignment(Pos.CENTER);
        menuPage.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label pageName = new Label("menu");
        pageName.setId("menu-page-name");

        menuPage.getChildren().addAll(pageName, newGameButton, instructionButton, seeHighScoreButton, exitButton);

        if (backButtonDisplaySetting == DISPLAY_BACK_BUTTON) {
            menuPage.getChildren().add(1, backButton);
        }

        // Size all buttons correctly
        double buttonHeight = screenHeight / 12;
        double buttonWidth = screenWidth / 4;
        for (Node node : menuPage.getChildren()) {
            if (node instanceof Button) {
                ((Button) node).setPrefSize(buttonWidth, buttonHeight);
                node.setId("menu-page-button");
            }
        }


        return menuPage;
    }

    /**
     * Creates a page that displays a large chunk of text. Used for instructions and high scores.
     * @param text the text to display
     * @param style any style string to apply to the text, like TEXT_ALIGN_CENTER or TEXT_ALIGN_JUSTIFY
     * @return a VBox containing all the textPage items
     */
    private VBox createTextPage(String text, String style) {
        VBox textPage = new VBox();
        textPage.setAlignment(Pos.CENTER);
        textPage.setSpacing(screenHeight / 8);

        // Create the text label
        Label textLabel = new Label();
        textLabel.setText(text);
        textLabel.setWrapText(true);
        textLabel.setMinHeight(screenHeight / 1.5);
        textLabel.setId("long-text-label");
        textLabel.setStyle(style);

        // Configure the back to menu button
        backToMenuButton.setId("menu-page-button");
        backToMenuButton.setMinSize(screenWidth / 4, screenHeight / 12);

        // Put the pieces together!
        textPage.getChildren().addAll(textLabel, backToMenuButton);
        return textPage;
    }

    /**
     * Create all the buttons we need with their event handlers
     */
    private void initializeButtons(){
        menuButton = new Button("Menu");
        menuButton.setOnAction(event -> showMenu());

        pauseButton = new Button("Pause");
        pauseButton.setOnAction(event -> pauseGame());

        hintButton = new Button("Hints: " + controller.getHintsRemaining());
        hintButton.setOnAction(event -> getHint());

        seeHighScoreButton = new Button("high scores");
        seeHighScoreButton.setOnAction(event -> showHighScoresPage());

        newGameButton = new Button("new game");
        newGameButton.setOnAction(event -> newGame());

        instructionButton = new Button("instructions");
        instructionButton.setOnAction(event -> showInstructionsPage());

        backButton = new Button("back");
        backButton.setOnAction(event -> backToGame());

        resumeButton = new Button("Resume");
        resumeButton.setOnAction(event -> resumeGame());

        exitButton = new Button("exit");
        exitButton.setOnAction(event -> System.exit(0));

        backToMenuButton = new Button("back to menu");
        backToMenuButton.setOnAction(event -> showMenu());


        Button[] allButtons = {menuButton, pauseButton, hintButton, seeHighScoreButton, newGameButton, instructionButton, backButton, resumeButton, exitButton, backToMenuButton};
        for (Button button : allButtons) {
            button.setOnMouseEntered(event -> menuHoverClip.play());
        }
    }

    /**
     * Asks the controller to make a new game.
     */
    private void newGame(){
        controller.newGame(DISPLAY_GAME);
    }

    /**
     * Show the score to the play and show if its a high score
     * @param isHighScore true if it is, or false
     */
    private void displayFinalScore(boolean isHighScore) {
        gameInterface.setBottom(null);
        scoreLabel.setVisible(false);
        pauseButton.setDisable(true);
        hintButton.setDisable(true);

        Label finalScoreLabel = new Label("Final Score: " + score);
        if (isHighScore) {
            finalScoreLabel.setText("New High Score: " + score);
        }
        finalScoreLabel.setId("final-score-label");

        gameInterface.setCenter(finalScoreLabel);
    }

    /**
     * Pauses the game. Notifies the controller that the game has been paused.
     */
    private void pauseGame(){
        menuButton.setDisable(true);
        hintButton.setDisable(true);

        Label pauseLabel = new Label("You have Paused the game");
        pauseLabel.setMinHeight(gamePane.getHeight());
        pauseLabel.setId("pause-label");

        gameInterface.setCenter(pauseLabel);
        gameMenu.getChildren().set(1, resumeButton);

        controller.pause();
    }

    /**
     * Resumes the game. Notifies the controller that the game has been resumed.
     */
    private void resumeGame(){
        menuButton.setDisable(false);
        if (controller.getHintsRemaining() > 0) {
            hintButton.setDisable(false);
        }
        gameMenu.getChildren().set(1, pauseButton);
        gameInterface.setCenter(gamePane);

        controller.resume();
    }

    /**
     * Exits the menu page and returns to the game.
     */
    private void backToGame(){
        gameInterface.setBottom(timerPane);
        gameInterface.setLeft(gameMenu);
        gameInterface.setCenter(gamePane);
        controller.resume();
    }

    /**
     * Update the game board to display the current runes.
     */
    private void updateGamePane() {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                int[] coord = {row, col};
                getRuneLabelAtCoord(coord).setGraphic(new ImageView(board[row][col].getImage()));
            }
        }
    }

    /**
     * Display the instructions page.
     */
    private void showInstructionsPage() {
        VBox instructionsPage = createTextPage(instructions, TEXT_ALIGN_JUSTIFY);
        gameInterface.setCenter(instructionsPage);
    }

    /**
     * Displays the high score page.
     */
    private void showHighScoresPage(){
        int[] highScores = controller.getHighScores();
        String scoreText = "HIGH SCORES:\n\n" + highScores[0] + "\n\n" + highScores[1]+"\n\n" + highScores[2] +"\n\n" + highScores[3] + "\n\n" +highScores[4];
        VBox scorePage = createTextPage(scoreText, TEXT_ALIGN_CENTER);
        gameInterface.setCenter(scorePage);
    }

    /**
     * Returns a pointer to the label at a given coordinate
     * @param coord the coordinates of the rune label to get
     * @return The label object at those coordinates
     */
    private Label getRuneLabelAtCoord(int[] coord) {
        int row = coord[0];
        int col = coord[1];

        Node result = null;
        ObservableList<Node> children = gamePane.getChildren();
        
        for (Node rune : children) {
            if(GridPane.getRowIndex(rune) == row && GridPane.getColumnIndex(rune) == col) {
                result = rune;
                break;
            }
        }

        return (Label)result;
    }

    /**
     * Asks the controller for a hint to display.
     */
    private void getHint(){
        if (inAnimation) {
            return;
        }

        hintCoords = controller.hint();
        hintButton.setText("Hints: " + controller.getHintsRemaining());
        if (controller.getHintsRemaining() == 0) {
            hintButton.setDisable(true);
        }
        if (hintCoords != null) {
            displayHint();
        }
    }

    /**
     * Display the hint
     */
    private void displayHint() {
        hintIsDisplayed = true;
        for (int[] runeCoord : hintCoords) {
            if (runeCoord != null) {
                getRuneLabelAtCoord(runeCoord).setStyle(BORDER_COLOR_RED);
            }
        }
    }

    /**
     * Checks if two coordinates are identical
     * @param coordA the first coordinate
     * @param coordB the second coordinate
     * @return true if they are, else false
     */
    private boolean isSameCoordinate(int[] coordA, int[] coordB) {
        return coordA[0] == coordB[0] && coordA[1] == coordB[1];
    }

    /**
     * Removes all rune highlighting
     */
    private void removeRuneHighlighting() {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                getRuneLabelAtCoord(new int[] {row, col}).setStyle(BORDER_COLOR_TRANSPARENT);
            }
        }
    }
}