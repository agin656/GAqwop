import random

def initiate(popSize):
    population = []
    for i in range(popSize):
        population.append(['{0:08b}'.format(random.randint(0,255)), '{0:08b}'.format(random.randint(0,255)),
                           '{0:08b}'.format(random.randint(0,255)), '{0:08b}'.format(random.randint(0,255)),
                           '{0:08b}'.format(random.randint(0,255)), '{0:08b}'.format(random.randint(0,255)),
                           '{0:08b}'.format(random.randint(0,255)), '{0:08b}'.format(random.randint(0,255))])
    print(population)
    return population

def fitness(pop, data):
    return random.randint(0, 100)
        

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


def mutation(population, mutationRate):
    for i in range(len(population)):
        if random.random() < mutationRate:
            index = random.randint(0,91)
            if population[i][index] == '0':
                population[i] = population[i][:index] + '1' + population[i][index+1:]
            else:
                population[i] = population[i][:index] + '0' + population[i][index+1:]
    return population

def main():
    populationSize = int(input("Population Size is => "))
    crossoverRate = float(input("Crossover Rate is => "))
    mutationRate = float(input("Mutation Rate is => "))


    population = initiate(populationSize)
    data = readData()

    saved0 = []
    result = fitness(population, data)
    for i in range(len(result)):
        if result[i] > 30:
            saved0.append(population[i])
    result.sort()
    result.reverse()
    print(result[0])
    print(result.index(0))

    population = mutation(population, mutationRate)
    population = crossover(population, crossoverRate)
    saved1 = []
    result = fitness(population, data)
    result_copy = result[:]
    result_copy.sort()
    result_copy.reverse()
    if result_copy[int(len(result_copy)/20)] > 30:
        for i in range(len(result)):
            if result[i] > result_copy[int(len(result_copy)/20)]:
                saved1.append(population[i])
    else:
        for i in range(len(result)):            
            if result[i] > 30:
                saved1.append(population[i])
    result_copy.sort()
    for i in range(len(population)):
        if saved0 == []:
            break
        if result[i] <= result_copy[len(saved0)]:
            population[i] = saved0.pop()
    saved0 = saved1[:]
    result_copy.reverse()
    print(result_copy[0])
    print(result_copy.index(0))

    
    end = input("Continue?(no) => ")
    while end != "no":
        
        population = mutation(population, mutationRate)    
        population = crossover(population, crossoverRate)
        saved1 = []
        result = fitness(population, data)
        result_copy = result[:]
        result_copy.sort()
        result_copy.reverse()
        if result_copy[int(len(result_copy)/20)] > 30:
            for i in range(len(result)):
                if result[i] > result_copy[int(len(result_copy)/20)]:
                    saved1.append(population[i])
        else:
            for i in range(len(result)):            
                if result[i] > 30:
                    saved1.append(population[i])
        result_copy.sort()
        for i in range(len(population)):
            if saved0 == []:
                break
            if result[i] <= result_copy[len(saved0)]:
                population[i] = saved0.pop()
        saved0 = saved1[:]
        result_copy.reverse()
        print(result_copy[0])
        print(result_copy.index(0))
        if result[0] > 130:
            input("Hey, I found it")
            best_index = result.index(result_copy[0])
            print("the code is:")
            print(population[best_index])
            print("with " + str(result_copy.index(0)) + " correct out of 150")
            end = "no"

## for debugging of 96
        if result_copy[0] == 96:
            end = "no"

## prints the best classifier
    print(population[result.index(result_copy[0])])
##        end = input("Continue?(no) => ")
    
main()
