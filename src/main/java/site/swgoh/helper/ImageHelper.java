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
	
	//X,Y  	0.3531250	0.3592593
	//W,H	0.6161458	0.4972222

	private static final float X_RATIO = 0.3531250F;
	private static final float Y_RATIO = 0.3592593F;
	private static final float W_RATIO = 0.6161458F;
	private static final float H_RATIO = 0.4972222F;
	
	
	
	
	public static BufferedImage resizeFile(BufferedImage srcImage){
		logger.info("Beginning Image Crop Process.");
		long beginTime = System.currentTimeMillis();
		
		int start_x = Math.round(X_RATIO * srcImage.getWidth());
		int start_y = Math.round(Y_RATIO * srcImage.getHeight());
		int end_x = Math.round(W_RATIO * srcImage.getWidth());
		int end_y = Math.round(H_RATIO * srcImage.getHeight());
		
		logger.info("X: " + start_x +
					"Y: " + start_y +
					"W: " + end_x +
					"H: " + end_y);
		
		//Crop the image and make a duplicate
		//BufferedImage croppedSrcImage = srcImage.getSubimage(START_X, START_Y, END_X, END_Y);
		BufferedImage croppedSrcImage = srcImage.getSubimage(start_x, start_y, end_x, end_y);
		
        BufferedImage copyOfImage = new BufferedImage(croppedSrcImage.getWidth(), croppedSrcImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = copyOfImage.createGraphics();
        g.drawImage(croppedSrcImage, 0, 0, null);
        g.dispose();
		
		long endTime = System.currentTimeMillis();
		logger.info("Ending Image crop operations which took: " + (endTime -beginTime) + " milliseconds");
		return copyOfImage;
	}

}
