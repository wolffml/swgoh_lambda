package site.swgoh.swgoh_lambda.myHandler;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

//public class S3Test implements RequestHandler<S3Event, String> {
public class LambdaS3Handler {
	
	private static final int START_X = 678;
	private static final int START_Y = 388;
	private static final int END_X = 1183;
	private static final int END_Y = 537;
	
	//private static final float MAX_WIDTH = 100;
    //private static final float MAX_HEIGHT = 100;
    private final String JPG_TYPE = (String) "jpg";
    private final String JPG_MIME = (String) "image/jpeg";
    private final String PNG_TYPE = (String) "png";
    private final String PNG_MIME = (String) "image/png";
    private final String DST_BUCKET = "";
 

    public String handleRequest(S3Event s3event, Context context) {
        try {
        	LambdaLogger logger = context.getLogger();
            S3EventNotificationRecord record = s3event.getRecords().get(0);
            

            String srcBucket = record.getS3().getBucket().getName();
            logger.log("Processing in bucket: " + srcBucket);
            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getKey().replace('+', ' ');
            srcKey = URLDecoder.decode(srcKey, "UTF-8");

            String dstBucket = srcBucket + "-cropped";
            String dstKey = "cropped-" + srcKey;

            // Sanity check: validate that source and destination are different buckets.
            if (srcBucket.equals(dstBucket)) {
                logger.log("Destination bucket must not match source bucket.");
                return "";
            }

            // Infer the image type.
            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
            if (!matcher.matches()) {
                logger.log("Unable to infer image type for key " + srcKey);
                return "";
            }
            String imageType = matcher.group(1);
            if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                logger.log("Skipping non-image " + srcKey);
                return "";
            }

            // Download the image from S3 into a stream
            //AmazonS3 s3Client = new AmazonS3Client();
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
            logger.log("Getting object from S3 bucket.");
            InputStream objectData = s3Object.getObjectContent();
            logger.log("Object retrieved from S3");
            // Read the source image
            BufferedImage srcImage = ImageIO.read(objectData);
            //srcImage.
            BufferedImage croppedSrcImage = srcImage.getSubimage(START_X, START_Y, END_X, END_Y);
            BufferedImage copyOfImage = new BufferedImage(croppedSrcImage.getWidth(), croppedSrcImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = copyOfImage.createGraphics();
            g.drawImage(croppedSrcImage, 0, 0, null);
            g.dispose();
            
            // Re-encode image to target format
            logger.log("Preparing to save to bucket.");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(copyOfImage, imageType, os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            // Set Content-Length and Content-Type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(os.size());
            if (JPG_TYPE.equals(imageType)) {
                meta.setContentType(JPG_MIME);
            }
            if (PNG_TYPE.equals(imageType)) {
                meta.setContentType(PNG_MIME);
            }

            // Uploading to S3 destination bucket
            logger.log("Writing to: " + dstBucket + "/" + dstKey);
            s3Client.putObject(dstBucket, dstKey, is, meta);
            logger.log("Successfully cropped " + srcBucket + "/"
                    + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
            return "Ok";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}