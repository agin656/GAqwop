package com.slowfrog.qwop;

import java.util.*;
import java.lang.*;

public class GA {
	public static Random random = new Random();
	//initiate the first generation
	public static ArrayList initiate(int populationSize) {
		ArrayList population = new ArrayList();
		
		for (int i=0; i<populationSize; i++) {
			String str = "";
			for (int j=0;j<8;j++) {
				int aNum = random.nextInt(255);
				String theNum = aNum.toBinaryString(aNum);
				str.concat(theNum);
			}
			population.add(str);
		}
		return population;
	}
	
	public static ArrayList fitness(ArrayList population, ArrayList data) {
		ArrayList result = new ArrayList();
		
		for (i=0;i<population.size();i++) {
			String str = data.get(i);
			String[] lst = str.split("\\|");
			result.add(lst[3]);
		}
		return result;
	}
	
	public static ArrayList crossover(ArrayList population, int crossoverRate) {
		ArrayList newPopulation = new ArrayList();
		
		for (int i=0; i<population.size()/2; i++) {
			String a = population.remove(int random.nextInt(population.size()));
			String b = population.remove(int random.nextInt(population.size()));
			if (float random.nextFloat() < crossoverRate) {
				int start = random.nextInt(64);
				int end = start + random.nextInt(64-start);
				String sub = a.substring(start,end);
				a = a.substring(start)+b.substring(start,end)+a.substring(end,64);
				b = b.substring(start)+sub+b.substring(end,64);
			}
			newPopulation.add(a);
			newPopulation.add(b);
		}
		return newPopulation;
	}
	
	public static ArrayList mutation(ArrayList population, int mutationRate) {
		for (int i=0;i<population.size();i++) {
			for (int index=0;index<population[i].length();index++) {
				if (float random.nextFloat() < mutationRate) {
					String str;
					if (population.get(i).charAt(index) == '0') {
						str = population.get(i).substring(index)+'1'+population.get(i).substring(index+1,population.get(i).length());
					} else {
						str = population.get(i).substring(index)+'0'+population.get(i).substring(index+1,population.get(i).length());
					}
					population.remove(i);
					population.add(i,str);
				}
			}
		}
		return population;
	}
	
	public static ArrayList ga(int populationSize, int crossoverRate, int mutationRate) {
		this.populationSize = populationSize;
		this.crossoverRate = crossoverRate;
		this.mutationRate = mutationRate;
		//use aScore to determine the top chromosomes
		this.aScore = 30;
		//determine when consider finding the solution
		this.endPoint = 100;
	}
	
	
}