# image-processing
Uses filters for smoothing RGB color images

## Java Fork/Join Framework
Implements parallel versions of the mean and median filter algorithms to smooth images

## Running the programs
A Makefile has been provided to compile the programs, open a terminal and run "make" to compile\
Alternatively run "javac" on the programs inside the "src" folder\
Each of the programs takes in command line arguments in the form: "input output windowWidth" 
where input is the image to apply the filters on (sample file provided input.jpg) 
Output is the name of the file you wish to save the changes to
WindowWidth is the size of the sliding window used in the algorithm, must be odd number >= 3
Using the Makefile run the command, "make runMeanFilter args="input.jpg output.jpg 3" to run the mean filter
Input the command "make runMeanFilter args="input.jpg output.jpg 3" to run the median filter
  
Alternatively, without the use of the Makefile, run the command "java MeanFilterParallel input.jpg output.jpg 3" to run the mean filter\
Run "java MedianFilterParallel input.jpg output.jpg 3" to run the median filter
