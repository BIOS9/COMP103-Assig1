// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2018T2, Assignment 1
 * Name: Matthew Corfiatis
 * Username: CorfiaMatt
 * ID: 300447277
 */

import java.time.Instant;
import java.util.*;
import ecs100.*;
import java.awt.Color;

/**
 * This class contains the main method of the program. 
 * 
 * A SlideShow object represents the slideshow application and sets up the buttons in the UI. 
 * 
 * @author pondy
 */
public class SlideShow {

    public static final int LARGE_SIZE = 450;   // size of images during slide show
    private int smallSize = 100;   // size of images when editing list
    public static final int MARGIN = 20;        // Distance of the editor slides from the edge of the canvas
    public static final int GAP = 10;           // gap between images when editing
    public static final int COLUMNS = 6;        // Number of columns of thumbnails
    private int slideDelay = 1000; //1 second

    private long startMillis = 0; //Timer point for the slideshow timer
    private List<String> images = new ArrayList<>();//  List of image file names.
    private int currentImage = -1;     // index of currently selected image.
    private int lastImage = -1;
    private boolean showRunning;      // flag signalling whether the slideshow is running or not


    /**
     * Constructor 
     */
    public SlideShow() {
        setupGUI();
    }

    /**
     * Initialises the UI window, and sets up the buttons. 
     */
    public void setupGUI() {
        UI.initialise();

        UI.addButton("Run show (enter)",   this::runShow);
        UI.addButton("Edit show (enter)",    this::editShow);
        UI.addSlider("Preview size", 10, 100, 50, this::doPreviewSize);
        UI.addSlider("Slide delay", 1, 20, 2, this::doSlideDelay);
        UI.addButton("remove (r)",       this::doRemove);
        UI.addButton("remove all",   this::doRemoveAll);
        UI.addButton("reverse",      this::doReverse);
        UI.addButton("shuffle",      this::doShuffle);
        UI.addButton("move left (<)",      this:: doMoveLeft);
        UI.addButton("move right (>)",     this:: doMoveRight);
        UI.addButton("move to start (s)",  this:: doMoveStart);
        UI.addButton("move to end (e)",    this:: doMoveEnd);
        UI.addButton("add before (b)",   this::doAddBefore);
        UI.addButton("add after (a)",    this::doAddAfter);

        UI.addButton("Testing",      this::setTestList);
        UI.addButton("Quit",         UI::quit);

        UI.setKeyListener(this::doKey);
        UI.setMouseListener(this::doMouse);
        //UI.setMouseMotionListener(this::doMouseMotion);
        UI.setDivider(0);
        UI.printMessage("Mouse must be over graphics pane to use the keys");

    }



    // RUNNING

    /**
     * As long as the show isn't already running, and there are some
     * images to show, start the show running from the currently selected image.
     * The show should keep running indefinitely, as long as the
     * showRunning field is still true.
     * Cycles through the images, going back to the start when it gets to the end.
     * The currentImage field should always contain the index of the current image.
     */
    public void runShow(){
        if(showRunning) return;
        showRunning = true;
        while(showRunning)
        {
            if(Instant.now().toEpochMilli() - startMillis > slideDelay) {
                startMillis = Instant.now().toEpochMilli();
                display();
                currentImage++;
                if (currentImage >= images.size())
                    currentImage = 0;
            }
            UI.sleep(10); //To avoid using too many cpu cycles
        }
    }

    /**
     * Stop the show by changing showRunning to false.
     * Redisplay the list of images, so they can be edited
     */
    public void editShow(){
        if(!showRunning) return;
        showRunning = false;
        if(currentImage > 0)
            currentImage--; //Put the image selection on the last image displayed in the slideshow
        display();
    }

    /**
     * Display just the current slide if the show is running.
     * If the show is not running, display the list of images
     * (as thumbnails) highlighting the current image
     */
    public void display(){
        UI.clearGraphics();
        if(showRunning)
        {
            UI.drawImage(images.get(currentImage), MARGIN, MARGIN, LARGE_SIZE, LARGE_SIZE);
        }
        else //Edit mode
        {
            for(int i = 0; i < images.size(); i++)
            {
                int column = i % COLUMNS; // Calculate what column to place image in
                int row = i / COLUMNS; // Calculate what row to place image in

                double x = (column * (smallSize + GAP)) + MARGIN;
                double y = (row * (smallSize + GAP)) + MARGIN;
                UI.drawImage(images.get(i), x, y, smallSize, smallSize);
            }
            drawSelectRect();
        }
    }



    // Other Methods (you will need quite a lot of additional methods).

    /**
     * Select the image at the specified index.
     * @param index Index of the image to select
     */
    public void selectImage(int index)
    {
        if(index < 0 || index >= images.size() || !checkAllowAction()) return;;
        int oldIndex = currentImage;
        currentImage = index;
        eraseSelectRect(oldIndex);
        drawSelectRect();
    }

    /**
     * Draw a red rectangle around the image to indicate that its selected.
     */
    private void drawSelectRect()
    {
        if(currentImage == -1) return;

        int column = currentImage % COLUMNS; // Calculate what column to place image in
        int row = currentImage / COLUMNS; // Calculate what row to place image in

        double x = (column * (smallSize + GAP)) + MARGIN;
        double y = (row * (smallSize + GAP)) + MARGIN;

        UI.setColor(Color.red);
        UI.setLineWidth(2);
        UI.drawRect(x - 2, y  - 2, smallSize + 4, smallSize + 4); //Draw red rect around image
    }

    /**
     * Erase the selection rectangle
     * @param index Index of the selection rectangle to remove
     */
    private void eraseSelectRect(int index)
    {
        int column = index % COLUMNS; // Calculate what column to place image in
        int row = index / COLUMNS; // Calculate what row to place image in

        double x = (column * (smallSize + GAP)) + MARGIN;
        double y = (row * (smallSize + GAP)) + MARGIN;

        UI.setColor(Color.white);
        UI.setLineWidth(2);
        UI.drawRect(x - 2, y  - 2, smallSize + 4, smallSize + 4); //Draw white rect in place of red one
    }

    /**
     * Callback for moving the UI slider for slide delay.
     * Changes the delay between slides
     * @param val Value of the slider, from 1-100
     */
    private void doSlideDelay(double val)
    {
        slideDelay = (int)val * 1000; //Set delay
        startMillis = Instant.now().toEpochMilli() - slideDelay; //Reset time
        display();
    }


    /**
     * Callback for moving the UI slider for preview size.
     * Changes the size of the image previews in the editor mode
     * @param val Value of the slider, from 10-100
     */
    private void doPreviewSize(double val)
    {
        smallSize = 2 * (int)val;
        display();
    }

    /**
     * Remove all images from the slideshow and clear the selection
     */
    private void doRemoveAll() {
        if(!checkAllowAction()) return;
        currentImage = -1;
        images.clear();
        display();
    }

    /**
     * Remove the currently selected image from the slideshow
     */
    private void doRemove() {
        if(!checkAllowAction() || !isValidSelection()) return;

        images.remove(currentImage);

        if(images.size() == 0) //If no more images
            currentImage = -1; //Clear selection
        if(currentImage >= images.size()) //If the selection is past the end of the image list
            currentImage = images.size() - 1; //Select the last element
        display();
    }

    /**
     * Reverse the order of the slideshow images
     */
    private void doReverse()
    {
        if(!checkAllowAction()) return;
        currentImage = 0; //Clear selection
        Collections.reverse(images); //Reverse images
        display(); //Redraw
    }

    /**
     * Shuffle order of the slideshow images
     */
    private void doShuffle()
    {
        if(!checkAllowAction()) return;
        currentImage = 0; //Clear selection
        Collections.shuffle(images); //Shuffle images
        display(); //Redraw
    }

    /**
     * Move the current image one space to the left in the slideshow
     */
    private void doMoveLeft()
    {
        if(!checkAllowAction() || !isValidSelection()) return; //Check parameters are valid
        if(currentImage == 0) //Ensure an out of bounds exception doesn't occur
        {
            UI.printMessage("Cannot move first image left.");
            return;
        }
        String tempImage = images.get(currentImage); //Get the image path
        images.remove(currentImage); //Remove it from the list
        images.add(--currentImage, tempImage); //Add it at the new location and update the selection
        display();
    }

    /**
     * Move the current image one space to the right in the slideshow
     */
    private void doMoveRight()
    {
        if(!checkAllowAction() || !isValidSelection()) return; //Check parameters are valid
        if(currentImage == images.size() - 1) //Ensure an out of bounds exception doesn't occur
        {
            UI.printMessage("Cannot move last image right.");
            return;
        }
        String tempImage = images.get(currentImage); //Get the image path
        images.remove(currentImage); //Remove it from the list
        images.add(++currentImage, tempImage); //Add it at the new location and update the selection
        display();
    }

    /**
     * Move the current image to the start of the list in the slideshow
     */
    private void doMoveStart()
    {
        if(!checkAllowAction() || !isValidSelection()) return; //Check parameters are valid
        String tempImage = images.get(currentImage); //Get the image path
        images.remove(currentImage); //Remove it from the list
        images.add(currentImage = 0, tempImage); //Add it at the new location and update the selection
        display();
    }

    /**
     * Move the current image to the end of the list in the slideshow
     */
    private void doMoveEnd()
    {
        if(!checkAllowAction() || !isValidSelection()) return; //Check parameters are valid
        String tempImage = images.get(currentImage); //Get the image path
        images.remove(currentImage); //Remove it from the list
        images.add(currentImage = images.size(), tempImage); //Add it at the new location and update the selection
        display();
    }

    /**
     * Opens file chooser dialogue and adds the new image after the current image
     */
    private void doAddAfter()
    {
        if(showRunning || (!isValidSelection() && currentImage != -1)) return; //Check parameters are valid
        String file = UIFileChooser.open();
        if(file == null) return;
        images.add(currentImage == -1 ? 0 : ++currentImage, file);
        display();
    }

    /**
     * Opens file chooser dialogue and adds the new image before the current image
     */
    private void doAddBefore()
    {
        if(!checkAllowAction() || !isValidSelection()) return; //Check parameters are valid
        String file = UIFileChooser.open();
        if(file == null) return;
        images.add(currentImage, file);
        display();
    }

    /**
     * Check that the show is not running and the user is allowed to perform an edit action.
     * @return Boolean to indicate whether the action is allowed.
     */
    private boolean checkAllowAction()
    {
        if(images.size() == 0)
        {
            UI.printMessage("No images available.");
            return false;
        }
        return !showRunning;
    }

    /**
     * Check if there is a valid image selection.
     * @return Valid or not
     */
    private boolean isValidSelection()
    {
        if(currentImage < 0 || currentImage >= images.size()) //Ensure selection is within range of images
        {
            UI.printMessage("Invalid selection!");
            return false;
        }
        return true;
    }

    // More methods for the user interface: keys (and mouse, for challenge)
    /**
     * Interprets key presses.
     * works in both editing the list and in the slide show.
     */  
    public void doKey(String key)
    {
        boolean isSelectKey = false; //If the key pressed was a valid show/edit key
        int originalSelection = currentImage; //Old selection, needed if selection is changed

        //Match key and conditions to choose what action to execute
        if (key.equals("Left") && currentImage > 0)
        {
            currentImage--;
            isSelectKey = true;
        }
        else if (key.equals("Right") && currentImage < images.size() - 1)
        {
            currentImage++;
            isSelectKey = true;
        }
        if (key.equals("Down") && currentImage < images.size() - COLUMNS && !showRunning) //Dont allow user to use down while show is running
        {
            currentImage+=COLUMNS; //Shift the selection one row by adding or removing a column
            isSelectKey = true;
        }
        else if (key.equals("Up") && currentImage >= COLUMNS && !showRunning) //Dont allow user to use up while show is running
        {
            currentImage-=COLUMNS; //Shift the selection one row by adding or removing a column
            isSelectKey = true;
        }
        else if (key.equals("Home"))
        {
            currentImage = 0;
            isSelectKey = true;
        }
        else if (key.equals("End"))
        {
            currentImage = images.size() - 1;
            isSelectKey = true;
        }
        else if(key.equals("Comma")) //Lower case <
            doMoveLeft();
        else if(key.equals("Period")) //Lower case <
            doMoveRight();
        else if(key.equals("a"))
            doAddAfter();
        else if(key.equals("b"))
            doAddBefore();
        else if(key.equals("e"))
            doMoveEnd();
        else if(key.equals("s"))
            doMoveStart();
        else if(key.equals("r"))
            doRemove();
        else if(key.equals("Enter"))
        {
            if(showRunning)
                editShow();
            else
                runShow();
        }

        if (isSelectKey && !showRunning) { //If the show is not running and the user pressed a valid edit key
            //Move selection
            eraseSelectRect(originalSelection);
            drawSelectRect();
        }
        else if(isSelectKey && showRunning) //If the show is running and the user pressed a valid show key
        {
            startMillis = Instant.now().toEpochMilli(); //Reset timer
            display(); //Redisplay show image
        }
    }

    /**
     * Mouse action callback
     * @param action Mouse action
     * @param x Mouse position X
     * @param y Mouse position Y
     */
    private void doMouse(String action, double x, double y)
    {
        if(!checkAllowAction()) return;
        if(x > MARGIN + (COLUMNS * (smallSize + GAP))) return; //Ensure mouse is over images in the x direction, y direction is not important because images fill downwards
        int xIndex = (int)(x - MARGIN) / (smallSize + GAP); //Index offset for the x position of the mouse
        int yIndex = (int)(y - MARGIN) / (smallSize + GAP) * COLUMNS; //Index offset for the y position of the mouse
        int index = xIndex + yIndex;

        if(index < 0 || index >= images.size()) return;

        switch(action)
        {
            case "pressed":
                lastImage = index; //Save for later use
                selectImage(index); //Select new image
                break;
            case "released":
                if(lastImage != index)
                {
                    String tempImage = images.get(lastImage); //Get the image path
                    images.remove(lastImage); //Remove it from the list
                    images.add(index, tempImage); //Add it at the new location and update the selection
                    selectImage(index);
                    display();
                }
                break;
        }
    }

    /**
     * A method that adds a bunch of names to the list of images, for testing.
     */
    public void setTestList(){
        if (showRunning) return;
        String[] names = new String[] {"Atmosphere.jpg", "BachalpseeFlowers.jpg",
                                       "BoraBora.jpg", "Branch.jpg", "DesertHills.jpg",
                                       "DropsOfDew.jpg", "Earth_Apollo17.jpg",
                                       "Frame.jpg", "Galunggung.jpg", "HopetounFalls.jpg",
                                       "Palma.jpg", "Sky.jpg", "SoapBubble.jpg",
                                       "Sunrise.jpg", "Winter.jpg"};
        
        for(String name : names){
            images.add(name);
        }
        currentImage = 0;
        display();
    }
            



    public static void main(String[] args) {
        new SlideShow();
    }

}
