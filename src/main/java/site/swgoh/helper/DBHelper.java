package site.swgoh.helper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;

import org.apache.log4j.Logger;

import site.swgoh.beans.MemberData;

public class DBHelper {
	
	private static DBHelper instance = null;
	private static final Logger logger = Logger.getLogger(DBHelper.class);
	private static final String DB_SERVER = System.getenv("DB_SERVER");
	private static final String DB_PORT = System.getenv("DB_PORT");
	private static final String DB_NAME = System.getenv("DB_NAME");
	private static final String DB_USER = System.getenv("DB_USER");
	private static final String DB_PWD = System.getenv("DB_PWD");
	
	
	private Connection conn;
	
	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	private static final String SRV_NAME = System.getenv("");
	
	private DBHelper(){
		initialize();
	}
	
	// assume that conn is an already created JDBC connection (see previous examples)
	
	public static DBHelper getInstance(){
		if (instance == null){
			logger.info("Creating new instance of DBHelper.");
			instance = new DBHelper();
		}
		return instance;
	}

	private void initialize(){
		String dbConnectionString = "jdbc:mysql://" + DB_SERVER
								  + ":" + DB_PORT
								  + "/" + DB_NAME;
								 // + "?user=" + DB_USER
								 // + "&password=" + DB_PWD;
		try {
			logger.info("Creating DB connection. " + dbConnectionString);
		    conn = DriverManager.getConnection(dbConnectionString,DB_USER,DB_PWD);
		    logger.info("DB Connection created.");
		} catch (SQLException ex) {
		    logger.error(ex.getMessage());
		}
	}
	public void close() throws SQLException{
		conn.close();
		conn = null;
	}
	
	
	public static void writeRecord(MemberData md){
		Connection conn = DBHelper.getInstance().getConn();
		Timestamp timestamp = new Timestamp(md.getDate().getTime());
		
		int tickets = 0;
		try{
			tickets = Integer.parseInt(md.getTickets());
		} catch (NumberFormatException e){
			logger.error(e.getMessage());
		}
		
		String sql = "INSERT INTO swgohticketdb.TICKETS_RUN( GUILD_ID,RUN_ID,MEMBER_NAME,TICKETS,M_DATE,SOURCE_FILE) VALUES (1, 0, ?, ?, ?, ?);";
		
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,md.getMemberName());
			statement.setInt(2, tickets);
			statement.setTimestamp(3, timestamp);
			statement.setString(4,md.getSourceFile());
			logger.info("Executing prepared statement. " + sql);
			statement.executeUpdate();
			logger.info("Row updated.");
			//conn.close();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		} finally {
			
		}
	}
	
	/*
	public static void main( String[] args )
    {
		MemberData md = new MemberData("MemberTest", "666", "imaginary.png");
		DBHelper.writeRecord(md);
    }
    */
	
}
