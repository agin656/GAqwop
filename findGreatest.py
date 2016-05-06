def findGreatest(filename):
    readFile = open(filename, 'r')
    lines = readFile.readlines()
    greatest = 0
    greatest_rundata = ''
    for line in lines:
        info = line.split('|')
        if float(info[3]) > greatest:
            greatest = float(info[3])
            greatest_rundata = info[1]
    print(greatest)
    print(greatest_rundata)

findGreatest('./qwopper-master/runs.txt')
