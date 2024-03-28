import swiftbot.*;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;

public class SwiftbotSurveyProgram {
    static SwiftBotAPI swiftBot;
    static int objectEncounters =0;
    static int imagesSaved =0;
    static boolean stopExecution = false;
    static boolean endExecution = false;
    static  boolean check = true;
    static boolean Bcheck = false;
    static boolean Ycheck = false;
    static boolean looper = true;
    public static void main(String[] args) {
        try {
            swiftBot = new SwiftBotAPI();
        } catch (Exception e) {
            System.out.println("\nI2C disabled!");
            System.out.println("Run the following command:");
            System.out.println("sudo raspi-config nonint do_i2c 0\n");
            System.exit(5);
        }
        int counter = 1;
        while(true)
        {
        	
        	if(counter ==1)
        	{
        	System.out.println("Welcome to Swift Bot");
        	System.out.println("Press Y on Bot to start wandering");
        	System.out.println("Press B to exit");
        	counter++;
        	}
        	while(looper==true)
        	{
        	swiftBot.disableButton(Button.Y);
            swiftBot.enableButton(Button.Y, () -> {
                swiftBot.disableButton(Button.Y);
                Ycheck = true ;
                looper = false;
            });
  
            swiftBot.disableButton(Button.B);
            swiftBot.enableButton(Button.B, () -> {
                swiftBot.disableButton(Button.B); 
                Bcheck =true ;
                looper = false;
            });
        	}
        	if(Ycheck == true )
        	{
        	runSwiftbotSurveyProgram();
        	counter = 1;
        	if(endExecution==true)
            {
            	System.exit(0);
            }
        	resetState();
        	System.out.println("Restarting program.");
        	}
        	else if(Bcheck == true)
        	{System.exit(0);}	
            
            
        
        }
    }

    public static void runSwiftbotSurveyProgram() {
    	
    	simulateWandering(swiftBot);
	    
	    while (true) { // Check if the thread is interrupted before each iteration
	        double distanceToObject = swiftBot.useUltrasound();
	        System.out.println("Distance to Object while wandering: "+distanceToObject);
	        if (distanceToObject <= 100) {
	            swiftBot.stopMove();
	            swiftBot.disableUnderlights();
	            break; // Exit the thread when the condition is met
	        }
	    }

	    System.out.println("Press A on Bot to start qr scanning");
        // Using CountDownLatch to synchronize button press
        CountDownLatch latch = new CountDownLatch(1);

        // Enabling button and setting up its listener
        swiftBot.enableButton(Button.A, () -> {
            swiftBot.stopMove();
            swiftBot.disableButton(Button.A);
            latch.countDown(); // Countdown latch when Button A is pressed
        });

        // Waiting until the button is pressed or the condition is met
        try {
            latch.await(); // This will block until latch count becomes zero
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        }

        String mode = getModeFromQRCode();
        long startTime = System.currentTimeMillis();

        executeMode(mode);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Would you like to view the log of the execution?");
        System.out.println("Button Y on bot for Yes Button X for no");
        System.out.println("Button B to completly exit.");
        while (check==true) {
            swiftBot.disableButton(Button.Y);
            swiftBot.enableButton(Button.Y, () -> {
                displayExecutionLog(mode, duration);
                swiftBot.disableButton(Button.Y);
                check =false;
            });
            swiftBot.disableButton(Button.X);
            swiftBot.enableButton(Button.X, () -> {
                swiftBot.disableButton(Button.X);
                check =false;
            });
            swiftBot.disableButton(Button.B);
            swiftBot.enableButton(Button.B, () -> {
                swiftBot.disableButton(Button.B);
                endExecution=true;
                check =false;
            });
        }
        return;
    }

    private static void handleInterruptedException(InterruptedException e) {
        e.printStackTrace(); // or any other handling mechanism
    }

    private static void executeMode(String mode) {
        switch (mode) {
            case "Curious Swiftbot":
                executeCuriousMode();
                break;
            case "Scaredy Swiftbot":
                executeScaredyMode();

                break;
            case "Dubious Swiftbot":
                executeDubiousMode();
                break;
        }
    }

    private static void displayExecutionLog(String mode, long duration) {
        System.out.println("Mode: " + mode);
        System.out.println("Duration of execution: " + duration + " milliseconds");
        System.out.println("Number of times the Swiftbot encountered an object: " + objectEncounters);
        System.out.println("Number of images saved: " + imagesSaved);
    }

    public static String getModeFromQRCode() {
        try {
            System.out.println("Scanning QR code to select mode...");
            
            BufferedImage img = swiftBot.getQRImage();
            String decodedMessage = swiftBot.decodeQRImage(img);

            if (decodedMessage.isEmpty()) {
                System.out.println("No QR Code was found. Please try again.");
                return getModeFromQRCode(); // Retry scanning QR code
            } else {
                System.out.println("QR code found. Decoded message: " + decodedMessage);
                return decodedMessage;
            }
        } catch (Exception e) {
            System.out.println("ERROR: Unable to scan for QR code.");
            e.printStackTrace();
            System.exit(5);
            return ""; // In case of error, return an empty string
        }
    }


    public static void executeCuriousMode() {
        try {
            boolean objectDetected = false;
            boolean objectMoved = false;
            double lastObjectDistance = 0;
            long lastObjectDetectedTime = 0;

            while (stopExecution==false) {
                // Continuously monitor the environment
                double distanceToObject = swiftBot.useUltrasound();
                System.out.println("distanceToObject: "+distanceToObject);
                if (distanceToObject > 16) {
                    // Object beyond buffer zone, turn underlights green and move towards it
                    swiftBot.fillUnderlights(new int[]{0, 0, 255}); // Turn underlights green
                    swiftBot.move(100, 100, 1000); // Move forward
                    objectDetected = true; // Object detected beyond buffer zone
                    objectMoved = true; // Swiftbot is moving towards the object
                } else if (distanceToObject < 14 && distanceToObject > 0) {
                    // Object within buffer zone, move backward to create gap
                    swiftBot.move(-100, -100, 1000); // Move backward
                    objectDetected = true; // Object detected within buffer zone
                    objectMoved = true; // Swiftbot is moving to create gap
                } else if (distanceToObject >= 14 && distanceToObject <= 16) {
                	swiftBot.stopMove();
                    // Object at required gap, blink underlights and remain stationary
                    for (int i = 0; i < 3; i++) {
                        swiftBot.fillUnderlights(new int[]{0, 0, 255}); // Green
                        Thread.sleep(500);
                        swiftBot.disableUnderlights();
                        Thread.sleep(500);
                    }
                    objectDetected = true; // Object detected at required gap
                    objectMoved = false; // Swiftbot is stationary
                }



                // Take an image of the object in front and save it
                BufferedImage objectImage = swiftBot.takeStill(ImageSize.SQUARE_720x720);
                String filename = "/home/pi/objectImage_" + imagesSaved + ".png";
                File outputFile = new File(filename);
                try {
                    ImageIO.write(objectImage, "png", outputFile);
                    imagesSaved++; // Increment imagesSaved counter
                    System.out.println("Image saved: " + filename);
                } catch (IOException e) {
                    System.err.println("Error saving image: " + e.getMessage());
                }


                // Wait for 2 seconds
                Thread.sleep(2000);

                // Check if the object has moved within the last 5 seconds
                if (System.currentTimeMillis() - lastObjectDetectedTime > 5000) {
                    if (!objectMoved) {
                        // Object has not moved, wait for a second and start moving again
                        Thread.sleep(1000);
                        // Move in a slightly different direction
                        swiftBot.move(50, 50, 2000); // Example: move left for 2 seconds
                    }
                }

                // Update object detection status and distance
                lastObjectDetectedTime = System.currentTimeMillis();

                // Check if Button X is pressed to stop execution
            	swiftBot.disableButton(Button.X);
                swiftBot.enableButton(Button.X, () -> {
                    stopExecution = true;
                    swiftBot.disableButton(Button.X);
                });

                // Increment objectEncounters
                objectEncounters++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public static void executeScaredyMode() {
        try {
            boolean objectDetected = false;
            double lastObjectDistance = 0;
            long lastObjectDetectedTime = 0;

            while (stopExecution==false) {
                // Enable the button for stopping execution
            	swiftBot.disableButton(Button.X);
                swiftBot.enableButton(Button.X, () -> {
                    stopExecution = true;
                    swiftBot.disableButton(Button.X);
                });
                
                // Continuously monitor the environment
                double distanceToObject = swiftBot.useUltrasound();

                if (distanceToObject <= 100) { // 1 meter = 100 centimeters
                    // Object within 1 meter, take necessary actions
                	// Take an image of the object and save it
                	BufferedImage objectImage = swiftBot.takeStill(ImageSize.SQUARE_720x720);
                	String filename = "/home/pi/objectImage_" + imagesSaved + ".png";
                	File outputFile = new File(filename);
                	try {
                	    ImageIO.write(objectImage, "png", outputFile);
                	    imagesSaved++; // Increment imagesSaved counter
                	    System.out.println("Image saved: " + filename);
                	} catch (IOException e) {
                	    System.err.println("Error saving image: " + e.getMessage());
                	}


                    // Blink the underlights
                    for (int i = 0; i < 3; i++) {
                        swiftBot.fillUnderlights(new int[]{0, 255, 0}); // Blue
                        Thread.sleep(500);
                        swiftBot.disableUnderlights();
                        Thread.sleep(500);
                    }
                    // Set the underlights to red
                    swiftBot.fillUnderlights(new int[]{255, 0, 0}); // Red
                    // Back up and turn in the opposite direction to move away from the object for three seconds
                    swiftBot.move(-100, -100, 1000); // Move backward
                    Thread.sleep(1000);
                    
                    
                    int timeToChange = 3000;
                    
                    swiftBot.move(100, -100, timeToChange); // Turn right while moving backward
                    Thread.sleep(timeToChange);
                    
                    swiftBot.move(-100, -100, 2000); // Move backward
                    Thread.sleep(2000);
                    

                    
                    objectDetected = true;
                } else {     
                    // Simulate wandering behavior with blue lights
                	objectDetected = false;
                    simulateWandering(swiftBot);
                }


                // Check if the Swiftbot has not encountered an object for five seconds
                if (System.currentTimeMillis() - lastObjectDetectedTime > 5000) {
                    if (!objectDetected) {
                        // Object not detected, wait for a second and start moving again in a slightly different direction
                        Thread.sleep(1000);
                        // Move in a slightly different direction
                        swiftBot.move(50, 50, 2000); // Example: move left for 2 seconds
                    }
                }

                // Update object detection status and distance
                lastObjectDetectedTime = System.currentTimeMillis();
                lastObjectDistance = distanceToObject;
                objectDetected = false;

                // Check if Button X is pressed to stop execution
            	swiftBot.disableButton(Button.X);
                swiftBot.enableButton(Button.X, () -> {
                    stopExecution = true;
                    swiftBot.disableButton(Button.X);
                });
                
                // Increment objectEncounters
                objectEncounters++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void executeDubiousMode() {
        try {
            // Generate a random number to choose between Curious and Scaredy modes
            Random random = new Random();
            int modeChoice = random.nextInt(2); // Randomly choose between 0 and 1

            // Execute the chosen mode
            if (modeChoice == 0) {
                // Curious mode

                    executeCuriousMode();

            } else {
                // Scaredy mode
            		executeScaredyMode();
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void simulateWandering(SwiftBotAPI swiftBot) {
    	
    	swiftBot.fillUnderlights(new int[]{0, 255, 0}); // Blue
        // Simulate random movement for wandering
        int randomDirection = (int) (Math.random() * 200) - 100; // Generate a random velocity between -100 and 100
        int randomDuration = (int) (Math.random() * 4000) + 1000; // Generate a random duration between 1 and 5 seconds

            // Move with the randomly generated velocity for the randomly generated duration
            swiftBot.move(randomDirection, randomDirection, randomDuration);
        } 
    public static void resetState() {
        objectEncounters = 0;
        imagesSaved = 0;
        stopExecution = false;
        endExecution = false;
        check = true;
        Bcheck = false;
        Ycheck = false;
        looper = true;
    }

}
