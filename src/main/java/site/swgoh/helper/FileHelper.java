package site.swgoh.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;


public class FileHelper {

	static final Logger logger = Logger.getLogger(FileHelper.class);

	public static Date getDateFromFileD(String fileName) {
		
		//LambdaLogger logger = LambdaLoggers
		SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ssSSS");
		SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM-dd");
		String datePart= fileName.substring(fileName.indexOf("_") + 1, 
				fileName.indexOf("."));
		
		String strReturn = "";
		Date dateFromString = new Date();
		try{
			dateFromString = sdfIn.parse(datePart);
			strReturn = sdfOut.format(dateFromString);
		} catch(ParseException e){
			logger.error(e.getMessage());
		}
		logger.info("String version:" + strReturn);
		
		return dateFromString;
	}
	
}
