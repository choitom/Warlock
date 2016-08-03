Warlock
A game by Kiya Govek, Tom Choi, Ryan Gorey, and Kiran Tomlinson inspired by PopCap Games' Bejeweled.
See the in-game instructions page for the rules of play.


To run from the command line, simply execute the run.sh script:
$ bash run.sh

Or, if the permissions are appropriately set, you can just use:
$ ./run.sh

Or, if you don't mind having lots of .class files crowding the warlock directory, navigate just outside the warlock directory, then:
$ javac warlock/*.java
$ java warlock.Controller


Known bugs:
* The game may display poorly on lower-resolution monitors. Due to JavaFX limitations, this issue cannot be fully addressed within available time.
  Please set your computer to its maximum possible resolution for optimal displaying. The timer may not be visible on smaller screens.
  Displays on which the game is confirmed to display well:
      * 27-inch iMac (Weitz lab computers) on default resolution
      * 15-inch MacBook Pro on max resolution
  Displays on which the game is confirmed to display poorly:
      * 15-inch MacBook Pro on default resolution
      * MacBook Air on any resolution
* On new Macs, the game window minimizes and then resumes fullscreen when a new game is started. This is due to the interaction between JavaFX fullscreen and OS X's Spaces feature, and is beyond our control.
* On Macs, use of the JavaFX sound library triggers this compilation warning: WARNING 140: This application, or a library it uses, is using the deprecated Carbon Component Manager for hosting Audio Units.


Credits:
* All sound effects from www.freesound.org
    * Error.wav by Ryan Smith, unchanged and licenced under http://creativecommons.org/licenses/by/3.0/
    * Success.wav by bertrof, renamed from "Game Sound Correct.wav" and licenced under http://creativecommons.org/licenses/by/3.0/
    * Countdown.wav by qubodup, renamed from "Cyber Countdown", converted to .wav, and licenced under http://creativecommons.org/licenses/by/3.0/
    * Shuffle.wav by FoolBoyMedia, renamed from "Notification Up II" and licenced under http://creativecommons.org/licenses/by-nc/3.0/
    * MenuHover.wav is in the public domain
* Music: "Overworld" by Kevin MacLeod (incompetech.com) Licensed under Creative Commons: By Attribution 3.0 License http://creativecommons.org/licenses/by/3.0/
* Metal Macabre font licenced under http://www.1001fonts.com/licenses/ffc.html
* Rune icons are the original work of Ryan Gorey


Notes on project structure:
* Warlock makes use of three important design patterns:
    * Model View Controller
        Our implementation of MVC is fairly standard. View handles all UI details, including sounds and input collection. Our controller is fairly small, and is only used when communication between the view and the models is absolutely necessary.
        The controller also contains the main method of the program. We have two models, one for the gameboard and one for the timer, which take commands from the controller and relay information to the view.
    * Observer
        We use the observer pattern to relay information from the models to the view. This information most notably includes timer updates, board state updates, score updates, and shuffle notifications.
    * Mediator
        In order to be able to create new games from within one application, we use the mediator pattern. Controller acts as the mediator, and creates new views as necessary. This pattern is why the controller extends Application.
        It stores the stage on which each new view displays.
* We use interfaces extensively to keep ourselves honest and help us understand what methods should be public-facing.