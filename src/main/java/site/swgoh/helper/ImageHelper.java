package site.swgoh.helper;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

public class ImageHelper {
	
	private static final Logger logger = Logger.getLogger(ImageHelper.class);
	
	private static final int START_X = 678;
	private static final int START_Y = 388;
	private static final int END_X = 1183;
	private static final int END_Y = 537;
	
	public static BufferedImage resizeFile(BufferedImage srcImage){
		logger.info("Beginning Image Crop Process.");
		long beginTime = System.currentTimeMillis();
		
		//Crop the image and make a duplicate
		BufferedImage croppedSrcImage = srcImage.getSubimage(START_X, START_Y, END_X, END_Y);
        BufferedImage copyOfImage = new BufferedImage(croppedSrcImage.getWidth(), croppedSrcImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = copyOfImage.createGraphics();
        g.drawImage(croppedSrcImage, 0, 0, null);
        g.dispose();
		
		long endTime = System.currentTimeMillis();
		logger.info("Ending Image crop operations which took: " + (endTime -beginTime) + " milliseconds");
		return copyOfImage;
	}

}
