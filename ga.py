import random

def initiate(popSize):
    population = []
    for i in range(popSize):
        population.append( ##represent second of pressing a button, and second of releasing it, in the order of QWOP
                           '{0:08b}'.format(random.randint(0,255)) + '{0:08b}'.format(random.randint(0,255)) +
                           '{0:08b}'.format(random.randint(0,255)) + '{0:08b}'.format(random.randint(0,255)) +
                           '{0:08b}'.format(random.randint(0,255)) + '{0:08b}'.format(random.randint(0,255)) +
                           '{0:08b}'.format(random.randint(0,255)) + '{0:08b}'.format(random.randint(0,255)))
##                           ##represent starting either pressing or releasing a button, in the order of QWOP, 1 is press
##                           [random.randint(0,1),random.randint(0,1),random.randint(0,1),random.randint(0,1)]
##                           ])
    return population

##number represent how far it can run
def fitness(pop, data):
    result = []
    for i in pop:
##placeholder of fitness function
        result.append(random.randint(0,100))
    return result
        

def crossover(population, crossoverRate):
    newPopulation = []
    for i in range(int(len(population)/2)):
        a = population.pop(random.randint(0,len(population)-1))
        b = population.pop(random.randint(0,len(population)-1))
        if random.random() < crossoverRate:
            start = random.randint(0,91)
            end = random.randint(start,91)
            new = a[start:end]
            a = a[:start] + b[start:end] + a[end:]
            b = b[:start] + new + a[end:]
        newPopulation.append(a)
        newPopulation.append(b)
    return newPopulation

##mutation now mutates all digits
def mutation(population, mutationRate):
    for i in range(len(population)):
        for index in range(len(population[i])):
            if random.random() < mutationRate:
                if population[i][index] == '0':
                    population[i] = population[i][:index] + '1' + population[i][index+1:]
                else:
                    population[i] = population[i][:index] + '0' + population[i][index+1:]
    return population

def main():
    populationSize = int(input("Population Size is => "))
    crossoverRate = float(input("Crossover Rate is => "))
    mutationRate = float(input("Mutation Rate is => "))
##use aScore to determine the top chromosomes
    aScore = 30
##determine when consider finding the solution
    endPoint = 99

    
##initialize population
    population = initiate(populationSize)
    data = []
##save the top chromosomes in the first generation
    saved0 = []
    result = fitness(population, data)
    for i in range(len(result)):
        if result[i] > aScore:
            saved0.append(population[i])
    result_copy = result[:]
    result_copy.sort()
    result_copy.reverse()

##if find a solution (this happens rarely from generation1)
    if result_copy[0] > endPoint:
        print("Hey, I found it")
        best_index = result.index(result_copy[0])
        print("the gene is:")
        print(population[best_index])
        print("It can run " + str(result_copy[0]) + "meters.")
        pop = []
        pop.append(population[best_index])
        fitness(pop,data)
        return pop
    
    end = "no"
    while end == "no":
        print("a generation is passed")
##mutation and crossover
        population = mutation(population, mutationRate)    
        population = crossover(population, crossoverRate)
##save the top chromosomes in the first generation
        saved1 = []
        result = fitness(population, data)
        result_copy = result[:]
        result_copy.sort()
        result_copy.reverse()
##save the top 10% if the top 10% is better than aScore
        if result_copy[int(len(result_copy)/10)] > aScore:
            for i in range(len(result)):
                if result[i] > result_copy[int(len(result_copy)/10)]:
                    saved1.append(population[i])
##save the ones that is better than aScore if the top 10% is not all better than aScore
        else:
            for i in range(len(result)):            
                if result[i] > aScore:
                    saved1.append(population[i])
        result_copy.sort()
##replace the bad chromosomes in the current generation with the good ones from the parent generation
        for i in range(len(population)):
            if saved0 == []:
                break
            if result[i] <= result_copy[len(saved0)-1]:
                population[i] = saved0.pop()
##save the good chromosome in the current generation as the good ones from parent 
        saved0 = saved1[:]
        result_copy.reverse()

##if finds a solution
        if result_copy[0] > endPoint:
            print("Hey, I found it")
            best_index = result.index(result_copy[0])
            print("the gene is:")
            print(population[best_index])
            print("It can run " + str(result_copy[0]) + " meters.")
            pop = []
            pop.append(population[best_index])
            fitness(pop,data)
            end = "yes"
    return pop
    
main()
