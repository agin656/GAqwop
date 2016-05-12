package com.slowfrog.qwop.ui;

import java.awt.Robot;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import com.slowfrog.qwop.ConsoleLog;
import com.slowfrog.qwop.Log;
import com.slowfrog.qwop.Qwopper;
import com.slowfrog.qwop.RunInfo;

public class Main {

  private static final Log LOG = new ConsoleLog();

  private static ArrayList<String> data = new ArrayList<String>();

  public static void main(String[] args) {

    //using tries to represent population size
    int tries = 2;
    //count is how many times it runs
    //using count to represent max run time
    int count = 10;
    String str = null;
    if (args.length > 0) {
      try {
        tries = Integer.parseInt(args[0]);
        if (args.length > 1) {
          count = Integer.parseInt(args[1]);
        }

      } catch (NumberFormatException e) {
        // First arg is not a number: probably a code string
        str = args[0];
      }
    }

    try {
      Robot rob = new Robot();
      Qwopper qwop = new Qwopper(rob, LOG);
      qwop.findRealOrigin();
/*      for (int round = 0; round < count; ++round) {
        if (count > 1) {
          str = Qwopper.makeRealisticRandomString(30);
        }
        testString(qwop, str, tries, round);
      }*/

      ArrayList<String> population;
	  ArrayList<String> populationSave = new ArrayList<String>();
	  ArrayList<String> previousP = new ArrayList<String>();
      ArrayList<Double> result;
	  ArrayList<Double> previousR = new ArrayList<Double>();
/*	  
	  for (int i=0;i<5;++i) {
		  str = "++W++O++++oP++++w++++Q+p++++q+++++++++++";
		  testString(qwop,str,1,i);
	  }
*/

      //runs count times
      for (int generation = 0; generation < count; ++generation) {
        //initialize population
        population = Qwopper.initiate(tries);
        //iterate through each chromosome within population
        for (int index = 0; index < tries; ++index) {
          //find each chromosome and convert it to a playable string
          str = Qwopper.convertChromosomeToQWOP(population.get(index));
          //play the string
		  System.out.println("the chromosome is:");
		  System.out.println(population.get(index));
		  System.out.println("the string is:");
		  System.out.println(str);
		  if(str.isEmpty() || str == null) {
			  str = "++++++++++";
		  }
          testString(qwop, str, 1, index);
        }
        //find the score of each chromosome in the population
        result = Qwopper.findScore(population,data);
		populationSave.clear();
		//trying to make a deep copy
		for(String chromosome : population) {
			populationSave.add(chromosome);
		}
		//populationSave = population;
        population = Qwopper.doGA(population,previousP,result,previousR);
		previousP.clear();
		//trying to make a deep copy
		for(String chromosome : populationSave) {
			previousP.add(chromosome);
		}
		//previousP = populationSave;
		previousR.clear();
		//trying to make a deep copy
		for(Double score : result) {
			previousR.add(score);
		}
		//previousR = result
        data.clear();

      }


    } catch (Throwable t) {
      LOG.log("Error", t);
    }
  }

  private static void testString(Qwopper qwop, String str, int count, int round) {
    for (int i = 0; i < count; ++i) {
      LOG.logf("Run #%d.%d\n", round, i);
      qwop.startGame();
      RunInfo info = qwop.playOneGame(str, 60000);
      LOG.log(info.toString());
      LOG.log(info.marshal());
      saveRunInfo("runs.txt", info);
      //add the run info to data
      data.add(info.marshal());
    }
  }

  private static void saveRunInfo(String filename, RunInfo info) {
    try {
      PrintStream out = new PrintStream(new FileOutputStream(filename, true));
      try {
        out.println(info.marshal());
      } finally {
        out.flush();
        out.close();
      }
    } catch (IOException ioe) {
      LOG.log("Error marshalling", ioe);
    }
  }

}
