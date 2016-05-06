/*
 * Copyright SlowFrog 2011
 *
 * License granted to anyone for any kind of purpose as long as you don't sue me.
 */
package com.slowfrog.qwop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.lang.*;

/**
 * This class will try to play QWOP and evolve some way to play well...
 * hopefully. Game at {@link http://foddy.net/Athletics.html}
 * 
 * @author SlowFrog
 */
public class Qwopper {

  /** Tolerance for color comparison. */
  private static final int RGB_TOLERANCE = 3;

  /** Unit delay in milliseconds when playing a 'string' 150*/ 
  private static final int DELAY = 50;

  /** Interval between two speed checks. */
  private static final int CHECK_INTERVAL = 1000;

  /** All possible 'notes' */
  private static final String NOTES = "QWOPqwop++";

  /**
   * Number of consecutive runs before we trigger a reload of the browser to
   * keep CPU and memory usage reasonable.
   */
  private static final int MAX_RUNS_BETWEEN_RELOAD = 10;

  /** Distance between two colors. */
  private static int colorDistance(int rgb1, int rgb2) {
    int dr = Math.abs(((rgb1 & 0xff0000) >> 16) - ((rgb2 & 0xff0000) >> 16));
    int dg = Math.abs(((rgb1 & 0xff00) >> 8) - ((rgb2 & 0xff00) >> 8));
    int db = Math.abs((rgb1 & 0xff) - (rgb2 & 0xff));
    return dr + dg + db;
  }

  /** Checks if a color matches another within a given tolerance. */
  private static boolean colorMatches(int ref, int other) {
    return colorDistance(ref, other) < RGB_TOLERANCE;
  }

  /**
   * Checks if from a given x,y position we can find the pattern that identifies
   * the blue border of the message box.
   */
  private static boolean matchesBlueBorder(BufferedImage img, int x, int y) {
    int refColor = 0x9dbcd0;
    return ((y > 4) && (y < img.getHeight() - 4) && (x < img.getWidth() - 12) &&
            colorMatches(img.getRGB(x, y), refColor) &&
            colorMatches(img.getRGB(x + 4, y), refColor) &&
            colorMatches(img.getRGB(x + 8, y), refColor) &&
            colorMatches(img.getRGB(x + 12, y), refColor) &&
            colorMatches(img.getRGB(x, y + 4), refColor) &&
            !colorMatches(img.getRGB(x, y - 4), refColor) && !colorMatches(
        img.getRGB(x + 4, y + 4), refColor));
  }

  /**
   * From a position that matches the blue border, slide left and top until the
   * corner is found.
   */
  private static int[] slideTopLeft(BufferedImage img, int x, int y) {
    int ax = x;
    int ay = y;

    OUTER_LOOP:

    while (ax >= 0) {
      --ax;
      if (matchesBlueBorder(img, ax, ay)) {
        continue;
      } else {
        ++ax;
        while (ay >= 0) {
          --ay;
          if (matchesBlueBorder(img, ax, ay)) {
            continue;
          } else {
            ++ay;
            break OUTER_LOOP;
          }
        }
      }
    }
    return new int[] { ax, ay };
  }

  /**
   * Move the mouse cursor to a given screen position and click with the left
   * mouse button.
   */
  private static void clickAt(Robot rob, int x, int y) {
    rob.mouseMove(x, y);
    rob.mousePress(InputEvent.BUTTON1_MASK);
    rob.mouseRelease(InputEvent.BUTTON1_MASK);
  }

  /**
   * Simulates a key 'click' by sending a key press followed by a key release
   * event.
   */
  private static void clickKey(Robot rob, int keycode) {
    rob.keyPress(keycode);
    rob.keyRelease(keycode);
  }

  /** Wait for a few milliseconds, without fear of an InterruptedException. */
  private static void doWait(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // Don't mind
    }
  }

  /**
   * Play a string. Interpret a string of QWOPqwop+ as a music sheet.
   * <ul>
   * <li>QWOP means press the key Q, W, O or P</li>
   * <li>qwop means release the key</li>
   * <li>+ means wait for a small delay</li>
   * </ul>
   */
  private void playString(String str) {
    this.string = str;
    long lastTick = System.currentTimeMillis();
    for (int i = 0; i < str.length(); ++i) {
      if (stop) {
        return;
      }
      char c = str.charAt(i);
      switch (c) {
      case 'Q':
        rob.keyPress(KeyEvent.VK_Q);
        break;

      case 'W':
        rob.keyPress(KeyEvent.VK_W);
        break;

      case 'O':
        rob.keyPress(KeyEvent.VK_O);
        break;

      case 'P':
        rob.keyPress(KeyEvent.VK_P);
        break;

      case 'q':
        rob.keyRelease(KeyEvent.VK_Q);
        break;

      case 'w':
        rob.keyRelease(KeyEvent.VK_W);
        break;

      case 'o':
        rob.keyRelease(KeyEvent.VK_O);
        break;

      case 'p':
        rob.keyRelease(KeyEvent.VK_P);
        break;

      case '+':
        if (System.currentTimeMillis() > this.nextCheck) {
          checkSpeed();
        }

        int waitTime = (int) ((lastTick + delay) - System.currentTimeMillis());
        if (waitTime > 0) {
          doWait(waitTime);
        }
        long newTick = System.currentTimeMillis();
        // log.logf("w=%03d d=%03d\n", waitTime, newTick - lastTick);
        lastTick = newTick;
        if ((this.timeLimit != 0) && (newTick > this.timeLimit)) {
          this.stop = true;
          return;
        }
        // After each delay, check the screen to see if it's finished
        if (isFinished()) {
          return;
        }
        break;

      default:
        System.out.println("Unkown 'note': " + c);
      }
    }
  }

  private void checkSpeed() {
    this.nextCheck += CHECK_INTERVAL;
    String distStr = captureDistance();
    float dist = Float.parseFloat(distStr);
    long dur = System.currentTimeMillis() - this.start;
    if (dur == 0) {
      dur = 1;
    }
    float speed = (dist * 1000) / dur;
    log.logf("%.1fm in %ds: speed=%.3f\n", dist, (dur / 1000), speed);
  }

  private static int keyIndex(char key) {
    switch (Character.toLowerCase(key)) {
    case 'q':
      return 0;
    case 'w':
      return 1;
    case 'o':
      return 2;
    case 'p':
      return 3;
    default:
      throw new IllegalArgumentException("Invalid key: " + key);
    }
  }

  private static String indexKey(int index) {
    return "qwop".substring(index, index + 1);
  }

  /**
   * A realistic random string is one where:
   * <ul>
   * <li>a key press is always followed by a key release for the same key</li>
   * <li>there is some time between a press and a release of a key</li>
   * <li>there is also some time between a release and a press of a key.</li>
   * </ul>
   * 
   * @param duration
   *          duration of the sequence in 'ticks'
   */
  public static String makeRealisticRandomString(int duration) {
    Random random = new Random(System.currentTimeMillis());
    String str = "";
    boolean[] down = { false, false, false, false };
    boolean[] justDown = { false, false, false, false };
    boolean[] justUp = { false, false, false, false };
    int cur = 0;
    while (cur < duration) {
      int rnd = random.nextInt(NOTES.length());
      String k = NOTES.substring(rnd, rnd + 1);
      char kc = k.charAt(0);
      if (kc == '+') { // delay
        ++cur;
        for (int i = 0; i < 4; ++i) {
          justDown[i] = false;
          justUp[i] = false;
        }
      } else if (Character.isUpperCase(kc)) { // key press
        int ki = keyIndex(kc);
        if (!(down[ki] || justUp[ki])) {
          down[ki] = true;
          justDown[ki] = true;
        } else {
          continue;
        }

      } else { // Lower case: key release
        int ki = keyIndex(kc);
        if (down[ki] && !justDown[ki]) {
          down[ki] = false;
          justUp[ki] = true;
        } else {
          continue;
        }
      }

      str += kc;
    }

    // Make sure all keys are released at the end (maybe without a delay)
    for (int i = 0; i < down.length; ++i) {
      if (down[i]) {
        str += indexKey(i);
      }
    }
    return str;
  }

  //this is functioning correctly
  public static ArrayList<String> initiate(int populationSize) {
    Random random = new Random(System.currentTimeMillis());
    ArrayList population = new ArrayList();

    for (int i=0; i<populationSize; ++i) {
      String str = "";
      for (int j=0;j<4;++j) {
        int aNum = random.nextInt(127);
		String theNum = String.format("%7s", Integer.toBinaryString(aNum)).replace(' ', '0');
        str += theNum;
		aNum = random.nextInt(255);
		theNum = String.format("%8s", Integer.toBinaryString(aNum)).replace(' ', '0');
        str += theNum;
		aNum = random.nextInt(127);
		theNum = String.format("%7s", Integer.toBinaryString(aNum)).replace(' ', '0');
        str += theNum;
      }
      population.add(str);
    }
    return population;
  }
  
  //this is functioning correctly
  public static ArrayList<Double> findScore(ArrayList<String> population, ArrayList<String> data) {
    ArrayList<Double> result = new ArrayList();

    for (int i=0;i<population.size();++i) {
      String str = data.get(i);
      String[] lst = str.split("\\|");
      result.add(Double.parseDouble(lst[3]));
    }
    return result;
  }
  
  //this is functioning correctly
  private static ArrayList<String> crossover(ArrayList<String> population, double crossoverRate, ArrayList<Double> result) {
    Random random = new Random(System.currentTimeMillis());
    ArrayList<String> newPopulation = new ArrayList();

    for (int i=0; i<population.size()/2; ++i) {
	  int x1 = random.nextInt(population.size());
	  int x2 = random.nextInt(population.size());
	  int y1 = random.nextInt(population.size());
	  int y2 = random.nextInt(population.size());
	  while (x1==x2) {
		x2 = random.nextInt(population.size());
	  }
	  while (y1==y2) {
		y2 = random.nextInt(population.size());
	  }
	  String a; 
	  String b; 
	  if (result.get(x1) > result.get(x2)) {
		a = population.get(x1);
	  } else {
		a = population.get(x2);
	  }
	  if (result.get(y1) > result.get(y2)) {
		b = population.get(y1);
	  } else {
		b = population.get(y2);
	  }
	  if (random.nextFloat() < crossoverRate) {
		int start = random.nextInt(64);
		int end = start + random.nextInt(64-start);
		String sub = a.substring(start,end);
		a = a.substring(0,start)+b.substring(start,end)+a.substring(end,64);
		b = b.substring(0,start)+sub+b.substring(end,64);
	  }
	  newPopulation.add(a);
	  newPopulation.add(b);
    }
    return newPopulation;
  }
  
  //this is functioning correctly
  private static ArrayList<String> mutation(ArrayList<String> population, double mutationRate) {
    Random random = new Random(System.currentTimeMillis());

    for (int i=0;i<population.size();++i) {
      for (int index=0;index<population.get(i).length();++index) {
        if (random.nextFloat() < mutationRate) {
          String str;
          if (population.get(i).charAt(index) == '0') {
            str = population.get(i).substring(0,index)+'1'+population.get(i).substring(index+1);
          } else {
            str = population.get(i).substring(0,index)+'0'+population.get(i).substring(index+1);
          }
          population.remove(i);
          population.add(i,str);
        }
      }
    }
    return population;
  }
  
  //this is functioning correctly
  //this code is TERRIBLE!!! I know so DO NOT tell me how bad it is.
  public static String convertChromosomeToQWOP (String Chromosome) {
    String QWOP = "";
	System.out.println(Chromosome);
	int Qwait1 = Integer.parseInt(Chromosome.substring(0, 7), 2)*10;
    int Qpress = Integer.parseInt(Chromosome.substring(7, 15),2)*10;
    int Qwait2 = Integer.parseInt(Chromosome.substring(15,22),2)*10;
	int Wwait1 = Integer.parseInt(Chromosome.substring(22,29),2)*10;
    int Wpress = Integer.parseInt(Chromosome.substring(29,37),2)*10;
    int Wwait2 = Integer.parseInt(Chromosome.substring(37,44),2)*10;
	int Owait1 = Integer.parseInt(Chromosome.substring(44,51),2)*10;
    int Opress = Integer.parseInt(Chromosome.substring(51,59),2)*10;
    int Owait2 = Integer.parseInt(Chromosome.substring(59,66),2)*10;
	int Pwait1 = Integer.parseInt(Chromosome.substring(66,73),2)*10;
    int Ppress = Integer.parseInt(Chromosome.substring(73,81),2)*10;
	int Pwait2 = Integer.parseInt(Chromosome.substring(81,88),2)*10;
	int Q = Qwait1+Qwait2+Qpress;
	int W = Wwait1+Wwait2+Wpress;
	int O = Owait1+Owait2+Opress;
	int P = Pwait1+Pwait2+Ppress;
	int max;
	if (Q>=W && Q>=O && Q>=P) {
		max = Q;
	} else if (W>=Q && W>=O && W>=P) {
		max = W;
	} else if (O>=Q && O>=W && O>=P) {
		max = O;
	} else {
		max = P;
	}
	int num = max/DELAY+1;
	for (int i=0;i<=num;++i) {
		QWOP += "+";
	}
	int i = 1;
	//Q
	QWOP = QWOP.substring(0,Qwait1/DELAY)+"Q"+QWOP.substring(Qwait1/DELAY+1,Qwait1/DELAY+Qpress/DELAY)+"q"+QWOP.substring(Qwait1/DELAY+Qpress/DELAY+1);
	//W
	if (QWOP.charAt(Wwait1/DELAY) == '+') {
		QWOP = QWOP.substring(0,Wwait1/DELAY)+"W"+QWOP.substring(Wwait1/DELAY+1);
	} else {
		while (QWOP.charAt(Wwait1/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Wwait1/DELAY+i)+"W"+QWOP.substring(Wwait1/DELAY+i+1);
		i = 1;
	}
	if (QWOP.charAt(Wwait1/DELAY+Wpress/DELAY) == '+') {
		QWOP = QWOP.substring(0,Wwait1/DELAY+Wpress/DELAY)+"w"+QWOP.substring(Wwait1/DELAY+Wpress/DELAY+1);
	} else {
		while (QWOP.charAt(Wwait1/DELAY+Wpress/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Wwait1/DELAY+Wpress/DELAY+i)+"w"+QWOP.substring(Wwait1/DELAY+Wpress/DELAY+i+1);
		i = 1;
	}
	//O
	if (QWOP.charAt(Owait1/DELAY) == '+') {
		QWOP = QWOP.substring(0,Owait1/DELAY)+"O"+QWOP.substring(Owait1/DELAY+1);
	} else {
		while (QWOP.charAt(Owait1/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Owait1/DELAY+i)+"O"+QWOP.substring(Owait1/DELAY+i+1);
		i = 1;
	}
	if (QWOP.charAt(Owait1/DELAY+Opress/DELAY) == '+') {
		QWOP = QWOP.substring(0,Owait1/DELAY+Opress/DELAY)+"o"+QWOP.substring(Owait1/DELAY+Opress/DELAY+1);
	} else {
		while (QWOP.charAt(Owait1/DELAY+Opress/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Owait1/DELAY+Opress/DELAY+i)+"o"+QWOP.substring(Owait1/DELAY+Opress/DELAY+i+1);
		i = 1;
	}
	//P
	if (QWOP.charAt(Pwait1/DELAY) == '+') {
		QWOP = QWOP.substring(0,Pwait1/DELAY)+"P"+QWOP.substring(Pwait1/DELAY+1);
	} else {
		while (QWOP.charAt(Pwait1/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Pwait1/DELAY+i)+"P"+QWOP.substring(Pwait1/DELAY+i+1);
		i = 1;
	}
	if (QWOP.charAt(Pwait1/DELAY+Ppress/DELAY) == '+') {
		QWOP = QWOP.substring(0,Pwait1/DELAY+Ppress/DELAY)+"p"+QWOP.substring(Pwait1/DELAY+Ppress/DELAY+1);
	} else {
		while (QWOP.charAt(Pwait1/DELAY+Ppress/DELAY+i) != '+') {
			i++;
		}
		QWOP = QWOP.substring(0,Pwait1/DELAY+Ppress/DELAY+i)+"p"+QWOP.substring(Pwait1/DELAY+Ppress/DELAY+i+1);
		i = 1;
	}
	

    return QWOP;
  }

  public static ArrayList<String> doGA (ArrayList<String> population,ArrayList<String> previousP, ArrayList<Double> result, ArrayList<Double> previousR) {
    int populationSize = population.size();
    double crossoverRate = 0.8;
    double mutationRate = 0.05;
    Random random = new Random(System.currentTimeMillis());
	ArrayList<String> newPopulation = new ArrayList<String>();
	ArrayList<Double> copy_result = new ArrayList<Double>();
	ArrayList<Double> copy_previousR = new ArrayList<Double>();
	
	if (previousP.size() == populationSize && previousR.size() == populationSize) {
	  for(String chromosome : population) {
		newPopulation.add(chromosome);
	  }
	  for(double score : result) {
		copy_result.add(score);
	  }
	  for(double score : previousR) {
		copy_previousR.add(score);
	  }
	  Collections.sort(copy_result);
	  Collections.sort(copy_previousR);
	  Collections.reverse(copy_previousR);
		
	  for (int i=0;i<populationSize/5;++i) {
		if (copy_previousR.get(i)>copy_result.get(i)) {
		  int index = result.indexOf(copy_result.get(i));
			newPopulation.remove(index);
			newPopulation.add(index,previousP.get(i));
		  }
		}
	  newPopulation = crossover(newPopulation,crossoverRate,result);
	  newPopulation = mutation(newPopulation,mutationRate);
		
	} else {
	  newPopulation = crossover(population,crossoverRate,result);
	  newPopulation = mutation(newPopulation,mutationRate);
	}
    

    return newPopulation;
  }

  private Robot rob;

  private int[] origin;

  private boolean finished;

  private Log log;

  private long start;

  private long nextCheck;

  private long timeLimit;

  private boolean stop;

  private String string;

  private int delay = DELAY;

  private int nbRuns;

  private BufferedImage capture;

  private BufferedImage transformed;

  public Qwopper(Robot rob, Log log) {
    this.rob = rob;
    this.log = log;
  }

  public int[] getOrigin() {
    return this.origin;
  }

  public String getString() {
    return this.string;
  }

  public BufferedImage getLastCapture() {
    return this.capture;
  }

  public BufferedImage getLastTransformed() {
    return this.transformed;
  }

  /** Look for the origin of the game area on screen. */
  private static int[] findOrigin(Robot rob) {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    BufferedImage shot = rob.createScreenCapture(new Rectangle(dim));
    for (int x = 0; x < dim.width; x += 4) {
      for (int y = 0; y < dim.height; y += 4) {
        if (matchesBlueBorder(shot, x, y)) {
          int[] corner = slideTopLeft(shot, x, y);
          return new int[] { corner[0] - 124, corner[1] - 103 };
        }
      }
    }
    throw new RuntimeException(
        "Origin not found. Make sure the game is open and fully visible.");
  }

  /** Checks if the game is finished by looking at the two yellow medals. */
  public boolean isFinished() {
    Color col1 = rob.getPixelColor(origin[0] + 157, origin[1] + 126);
    if (colorMatches(
        (col1.getRed() << 16) | (col1.getGreen() << 8) | col1.getBlue(),
        0xffff00)) {
      Color col2 = rob.getPixelColor(origin[0] + 482, origin[1] + 126);
      if (colorMatches(
          (col2.getRed() << 16) | (col2.getGreen() << 8) | col2.getBlue(),
          0xffff00)) {
        finished = true;
        return true;
      }

    }
    finished = false;
    return false;
  }

  public boolean isRunning() {
    return !(this.stop || this.finished);
  }

  /** Find the real origin of the game. */
  public int[] findRealOrigin() {
    origin = findOrigin(rob);
    if (isFinished()) {
      origin = new int[] { origin[0] - 5, origin[1] + 4 };
    }

    return origin;
  }

  /**
   * Start a game, either by clicking on it (at first load) or pressing space
   * for next games.
   */
  public void startGame() {
    stop = false;
    clickAt(rob, origin[0], origin[1]);
    if (isFinished()) {
      clickKey(rob, KeyEvent.VK_SPACE);
    } else {
      // Press 'R' for restart
      rob.keyPress(KeyEvent.VK_R);
      rob.keyRelease(KeyEvent.VK_R);
    }
  }

  public void stop() {
    this.stop = true;
  }

  private void stopRunning() {
    Point before = MouseInfo.getPointerInfo().getLocation();

    // Restore focus to QWOP (after a button click on QwopControl)
    clickAt(rob, origin[0], origin[1]);
    // Make sure all possible keys are released
    rob.keyPress(KeyEvent.VK_Q);
    rob.keyPress(KeyEvent.VK_W);
    rob.keyPress(KeyEvent.VK_O);
    rob.keyPress(KeyEvent.VK_P);
    doWait(20);
    rob.keyRelease(KeyEvent.VK_Q);
    rob.keyRelease(KeyEvent.VK_W);
    rob.keyRelease(KeyEvent.VK_O);
    rob.keyRelease(KeyEvent.VK_P);

    // Return the mouse cursor to its initial position...
    rob.mouseMove(before.x, before.y);
  }

  public void refreshBrowser() {
    // Click out of the flash rectangle to give focus to the browser
    clickAt(rob, origin[0] - 5, origin[1] - 5);
    
    // Reload (F5)
    rob.keyPress(KeyEvent.VK_F5);
    doWait(20);
    rob.keyRelease(KeyEvent.VK_F5);

    // Wait some time and try to find the window again
    for (int i = 0; i < 10; ++i) {
      doWait(2000);
      try {
        this.findRealOrigin();
        return;
      } catch (RuntimeException e) {
        // Probably not available yet
      }
    }
    throw new RuntimeException("Could not find origin after browser reload");
  }

  public String captureDistance() {
    Rectangle distRect = new Rectangle();
    distRect.x = origin[0] + 200;
    distRect.y = origin[1] + 20;
    distRect.width = 200;
    distRect.height = 30;
    this.capture = rob.createScreenCapture(distRect);

    BufferedImage thresholded = ImageReader.threshold(this.capture);
    List<Rectangle> parts = ImageReader.segment(thresholded);
    this.transformed = ImageReader.drawParts(thresholded, parts);
    return ImageReader.readDigits(thresholded, parts);
  }

  public RunInfo playOneGame(String str, long maxDuration) {
    
    log.log("Playing " + str);
    doWait(500); // 0.5s wait to be sure QWOP is ready to run
    this.start = System.currentTimeMillis();
    this.nextCheck = this.start + CHECK_INTERVAL;
    if (maxDuration > 0) {
      this.timeLimit = this.start + maxDuration;
    } else {
      this.timeLimit = 0;
    }
    while (!(isFinished() || stop)) {
      playString(str);
    }
    stopRunning();
    checkSpeed();

    if (++nbRuns == MAX_RUNS_BETWEEN_RELOAD) {
      nbRuns = 0;
      refreshBrowser();
      log.log("Refreshing browser");
    }
    
    long end = System.currentTimeMillis();
    doWait(1000);
    float distance = Float.parseFloat(captureDistance());
    RunInfo info;
    if (stop) {
      info = new RunInfo(str, this.delay, false, true, end - this.start,
          distance);
    } else {
      info = new RunInfo(str, this.delay, distance < 100, false, end -
                                                                 this.start,
          distance);
    }
    return info;
  }
}
