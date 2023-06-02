import java.util.concurrent.RecursiveAction;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.io.File;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Defines an object which can be used to apply the mean filter algorithm to an image
 * Does so using parallel programming utilizing the ForkJoin framework
 * @author Ncube Manelisi
 */
public class MedianFilterParallel extends RecursiveAction{
    int[][] redArray = null, greenArray = null, blueArray = null, alphaArray= null;
    File imageFile = null;
    BufferedImage image = null;
    int width, height;
    int windowWidth, windowSize;    //windowSize is the size of the box around each pixel e.g when width is 3, size of box is 9
    String inputFile, outputFile;
    final int SEQUENTIAL_CUTOFF = 400;

    /**
     * Constructor to create MedianFilterParallel object to apply the Median filter using ForkJoin framework
     * @param in is the input filename
     * @param width is the width of the sliding square window
     */
    public MedianFilterParallel(String in, int width){
        inputFile = in;
        windowWidth = width;
        windowSize = windowWidth*windowWidth;
        readInFile();
    }

    /**
     * Method to read the specified file and create a BufferedImage object
     */
    public void readInFile(){
        //Reading in image file
        try {
            imageFile = new File(inputFile);
            image = ImageIO.read(imageFile);
            //Image dimensions
            width = image.getWidth();
            height = image.getHeight();

            //Creating arrays for the ARGB components
            alphaArray = new int[width][height];
            redArray = new int[width][height];
            greenArray = new int[width][height];
            blueArray = new int[width][height];
        } catch (IOException e) {
            System.out.println("An error has occured: "+e.getMessage());
            System.exit(1);
        }
    }


    /**
     * RecursiveAction class to read the picture's pixels into arrays recursively row by row
     */
    public class ReadPixels extends RecursiveAction{
        int startY, endY;

        ReadPixels(int startY, int endY){
            this.startY = startY;
            this.endY = endY;
        }

        protected void compute(){
            if (endY-startY <= SEQUENTIAL_CUTOFF){     //Cutoff when 12 columns or less remain
                for (int y = startY; y<=endY; y++){
                    for (int x = 0; x<width; x++){
                        int pixelValue = image.getRGB(x, y);     //Getting pixel value
                        // Bitwise opertations to extract ARGB components
                        alphaArray[x][y] = (pixelValue >> 24) & 0xff;
                        redArray[x][y] = (pixelValue >> 16) & 0xff;
                        greenArray[x][y] = (pixelValue >> 8) & 0xff;
                        blueArray[x][y] = pixelValue & 0xff;
                    }
                }
            }
            else{
                int mid = (endY + startY)/2 ;
                ReadPixels top = new ReadPixels(startY, mid);
                ReadPixels bottom = new ReadPixels(mid+1, endY);
                top.fork();
                bottom.compute();
                top.join();
                //invokeAll(top,bottom);
            }
        }
    }

    /**
     * Recursively divides the ARGB arrays into half and applies the median filter algorithm
     */
    public class ApplyFilter extends RecursiveAction{
        int startY, endY;

        ApplyFilter(int startY, int endY){
            this.startY = startY;
            this.endY = endY;
        }

        /**
         * Finds the median in the slding square window centered at (xCo, yCo) coordinate
         * @param xCo is the x-coordinate of the target pixel
         * @param yCo is the y-coordinate of the target pixel
         * @param array is the target array
         * @return the median value in the sliding square window
         */
        public int findMedian(int xCo, int yCo, int[][] array){
            //Extracting all the numbers in the square window
            int[] square = new int[windowSize];
            int xStart = xCo - windowWidth/2;    //Starting x-value for the sliding square window
            int yStart = yCo - windowWidth/2;    //Starting y-value for the sliding square window
            int i=0;

            for (int y = yStart; y<yStart+windowWidth; y++){
                for (int x = xStart; x<xStart+windowWidth; x++){
                    square[i] = array[x][y];
                    i++;
                }
            }
            //Sorting elements from the square window
            Arrays.sort(square);
            return square[windowSize/2];    //Get middle value
        }

        protected void compute(){
            if (endY-startY <= SEQUENTIAL_CUTOFF){     
                int distToSide = windowWidth/2;    
                for (int y = startY; y<=endY; y++){
                    for (int x = 0; x<width; x++){
                        //Make sure the pixel has a complete surrounding box !!!
                        if ((x-distToSide>=0) && (y-distToSide>=0) && (x+distToSide < width) && (y+distToSide < height)){ 
                            int newRPixel = findMedian(x, y, redArray); //Replaces red pixels with the median of the surrounding pixels
                            int newGPixel = findMedian(x, y, greenArray); //Replaces green pixels with the median of the surrounding pixels
                            int newBPixel = findMedian(x, y, blueArray);  //Replaces blue pixels with the median of the surrounding pixels

                            int pixel = (alphaArray[x][y] << 24) | (newRPixel << 16) | (newGPixel << 8) | (newBPixel); //Convert back to pixel value
                            image.setRGB(x, y, pixel);  //Sets new pixel
                        }
                    }
                }
            }
            else{
                int mid = (endY + startY)/2 ;
                ApplyFilter top = new ApplyFilter(startY, mid);
                ApplyFilter bottom = new ApplyFilter(mid+1, endY);
                top.fork();
                bottom.compute();
                top.join();
                //invokeAll(top,bottom);
            }
        }
    }

protected void compute(){   
        //Does nothing, All the work is done by the inner classes ReadPixels and ApplyFilter 
    }

    public static void main(String[] args){

        if (args.length != 3){
            System.out.println("Please enter 3 command-line arguments in the form: \t <inputImageName> <outputImageName> <windowWidth>");
            System.exit(0);
        }

        final int windWidth = Integer.parseInt(args[2]);

        if ((windWidth % 2 != 1) && windWidth >= 3){
            System.out.println("{Error}: Width of the filter window must be an odd positive integer >= 3");
            System.exit(0);
        }
        final ForkJoinPool pool= new ForkJoinPool();   
        MedianFilterParallel filter = new MedianFilterParallel(args[0], windWidth);
        MedianFilterParallel.ReadPixels readPixels = filter.new ReadPixels(0, filter.height-1);
        MedianFilterParallel.ApplyFilter applyFilter = filter.new ApplyFilter(0, filter.height-1);
        pool.invoke(readPixels);

        /**Test to validate correctness: Print red values before and after filtering
        System.out.println("RedArray Before filtering\n==========================================");
        for (int row = 0; row<10; row++){
            for (int col = 0; col<10; col++){
                System.out.print(filter.redArray[col][row]+",");
            }
            System.out.println();
        }*/

        long start = System.currentTimeMillis();
        pool.invoke(applyFilter);
        float time = (System.currentTimeMillis() - start)/1000.0f;
        System.out.println("Time taken to apply filter="+time+" seconds");

        /*System.out.println("==========================================\nAfter filtering\n==========================================");
        for (int row = 0; row<10; row++){
            for (int col = 0; col<10; col++){
                int pixelValue = filter.image.getRGB(col, row);   
                int red = (pixelValue >> 16) & 0xff;
                System.out.print(red+",");
            }
            System.out.println();
        }*/

       //Save image file
        try {
            File outputFile = new File(args[1]);
            ImageIO.write(filter.image, "jpg", outputFile); 
            System.out.println("...Parallel Median Filter completed");
        } catch (IOException e) {
            System.out.println("An error occured: "+e.getMessage());
            System.exit(1);
        }
    }
}