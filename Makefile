# Makfile
# Ncube Manelisi

.SUFFIXES: .java .class
BIN = bin/
SRC = src/

#Match bin/.class with dependency src/.java
$(BIN)%.class: $(SRC)%.java
	@javac -d $(BIN) -cp $(BIN) $<

#Class files
Median_Filter_Parallel_Class = bin/MedianFilterParallel.class
Mean_Filter_Parallel_Class = bin/MeanFilterParallel.class

default: $(Median_Filter_Serial_Class) $(Mean_Filter_Serial_Class) $(Median_Filter_Parallel_Class) $(Mean_Filter_Parallel_Class)

# Getting arguments from console 
args = `arg="$(filter-out $@,$(MAKECMDGOALS))" && echo $${arg:-${1}}`

clean:
	rm $(BIN)*.class

runMeanFilter: $(Mean_Filter_Parallel_Class)
	@java -cp bin MeanFilterParallel $(call args)

runMedianFilter: $(Median_Filter_Parallel_Class)
	@java -cp bin MedianFilterParallel $(call args)
