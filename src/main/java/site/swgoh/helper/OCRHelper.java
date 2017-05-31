package site.swgoh.helper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;

import site.swgoh.beans.ResultRecord;
import site.swgoh.lists.OCRRecordList;

public class OCRHelper {
	
	static final Logger logger = Logger.getLogger(OCRHelper.class);
	
	//String for the OCR Call
	private static final String baseURI = System.getenv("ocr_uri");
	private static final String apiKey = System.getenv("ocr_apikey");
	private static final String lang = "en";
	

	
	public static OCRRecordList getOCRData(InputStream sourceIS, String fileName){
		logger.info("OCR URI: " + baseURI);
		VisionServiceRestClient c = new VisionServiceRestClient(apiKey, baseURI);
		Gson gson = new Gson();
	    // Put the image into an input stream for detection.
	    //File file = new File(strFile);
	    OCRRecordList resultLines = new OCRRecordList(fileName);
		try {
		    //ByteArrayInputStream baInputStream = new ByteArrayInputStream(inputStream);
		    //ByteArrayInputStream inputStream = new ByteArrayInputStream(IOUtils.toByteArray(sourceIS));
		    
			OCR ocr = c.recognizeText(sourceIS, lang, false);
			//OCR ocr = c.recognizeText("https://static1.squarespace.com/static/53e8d221e4b00c61990b52a4/t/54c69295e4b05c3509c1b6c5/1422299824465/image.jpg?format=750w", "en",false);
			
			String result = gson.toJson(ocr);
			logger.info(result);
			int i=0;
			for (Region region : ocr.regions) {
				//logger.debug("Bounds: " + region.boundingBox);
				//for (int i=0; i < region.lines.size(); i++) {
				for (Line line: region.lines) {
					i++;
				    String strLine = "";
					for (Word word: line.words) {
						strLine += word.text + " ";
						//logger.debug("Word: " + word.text);
					}
					ResultRecord rec = new ResultRecord(i, strLine.trim());
					resultLines.add(rec);
				}
			}
			for (ResultRecord rec: resultLines){
				logger.info(rec);
	    	}
			resultLines.setJson(result);
		} catch (Exception e) {
			logger.error(e.toString());
			//e.printStackTrace();
		}		
		return resultLines;
	}
	
}
