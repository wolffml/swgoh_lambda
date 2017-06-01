package site.swgoh.handlers;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import site.swgoh.beans.MemberData;
import site.swgoh.helper.DBHelper;
import site.swgoh.helper.ImageHelper;
import site.swgoh.helper.OCRHelper;
import site.swgoh.lists.DailyTrackingList;
import site.swgoh.lists.OCRRecordList;

//public class S3Test implements RequestHandler<S3Event, String> {
public class LambdaS3Handler {
	
	
	static final Logger logger = Logger.getLogger(LambdaS3Handler.class);
	
	//private static final float MAX_WIDTH = 100;
    //private static final float MAX_HEIGHT = 100;
    private final String JPG_TYPE = (String) "jpg";
    private final String JPG_MIME = (String) "image/jpeg";
    private final String PNG_TYPE = (String) "png";
    private final String PNG_MIME = (String) "image/png";
    private final String TXT_MINE = (String) "txt/plain";
    private final String DST_BUCKET = System.getenv("output_bucket");
 

    public String handleRequest(S3Event s3event, Context context) {
    	//LambdaLogger logger = context.getLogger();
        try {
        	
            S3EventNotificationRecord record = s3event.getRecords().get(0);
            
            String srcBucket = record.getS3().getBucket().getName();
            logger.info("Processing in bucket: " + srcBucket);
            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getKey().replace('+', ' ');
            srcKey = URLDecoder.decode(srcKey, "UTF-8");

            String dstBucket = DST_BUCKET; //srcBucket + "-cropped";
            String dstKey = "cropped-" + srcKey;

            // Sanity check: validate that source and destination are different buckets.
            if (srcBucket.equals(dstBucket)) {
                logger.info("Destination bucket must not match source bucket.");
                return "";
            }

            // Infer the image type.
            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
            if (!matcher.matches()) {
                logger.debug("Unable to infer image type for key " + srcKey);
                return "";
            }
            String imageType = matcher.group(1);
            if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                logger.debug("Skipping non-image " + srcKey);
                return "";
            }

            // Download the image from S3 into a stream
            //AmazonS3 s3Client = new AmazonS3Client();
            logger.info("Getting instance of S3Client.");
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();
            logger.info("S3 client instatiated.  Geting Object info.");
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
            logger.info("Getting object from S3 bucket.");
            InputStream objectData = s3Object.getObjectContent();
            logger.info("Object retrieved from S3");
            // Read the source image
            BufferedImage srcImage = ImageIO.read(objectData);
            BufferedImage copyOfImage = ImageHelper.resizeFile(srcImage);
            
            // *********************************Save Cropped image to a bucket******************************
            // Re-encode image to target format
            logger.info("Preparing to save to bucket.");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(copyOfImage, imageType, os);
            
            //File tmpFile = new File("tmp.png");
            //ImageIO.write(copyOfImage, imageType, tmpFile);
            
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
            
            
          //Process copyOfImage for OCR
            InputStream is2 = new ByteArrayInputStream(os.toByteArray());
            OCRRecordList ocrReturnDataList = OCRHelper.getOCRData(is2, srcKey);
            
            // Uploading to S3 destination bucket
            logger.info("Writing to: " + dstBucket + "/" + dstKey);
            s3Client.putObject(dstBucket, dstKey, is, meta);
            logger.info("Successfully cropped " + srcBucket + "/"
                    + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
            
            //Here we write the response JSON to a file
            InputStream streamJSON = new ByteArrayInputStream(ocrReturnDataList.getJson().getBytes());
            ObjectMetadata metaJSON = new ObjectMetadata();
            metaJSON.setContentLength(streamJSON.available());
            metaJSON.setContentType(TXT_MINE);
            dstKey = dstKey.replace("png","txt");
            s3Client.putObject(dstBucket, dstKey, streamJSON, metaJSON);
            
            //Map the ocrReturnDataList to a DailyTracking List of MemberData beans 
            DailyTrackingList dailyList = new DailyTrackingList();
            dailyList.addFileContents(ocrReturnDataList);
            logger.info(dailyList.toString());
            
            //Need to write the MemberData elements to a CSVfile
            InputStream streamCSV = new ByteArrayInputStream(dailyList.toString().getBytes());
            ObjectMetadata metaCSV = new ObjectMetadata();
            metaCSV.setContentLength(streamCSV.available());
            metaCSV.setContentType(TXT_MINE);
            dstKey = dstKey.replace("txt","csv");
            s3Client.putObject(dstBucket, dstKey, streamCSV, metaCSV);
            s3Client.shutdown();
            
            
            
            
            //Write the MemberData elements to the database
            for (MemberData md: dailyList){
            	DBHelper.writeRecord(md);
            }
            DBHelper.getInstance().close();
            
            
            return "Ok";
        } catch (IOException e) {
            throw new RuntimeException(e);
            
        } catch (SQLException e) {
			
			e.printStackTrace();
			return "Error";
		}
    }
}