package com.SQLiteDatabaseWrapper;

import test.Data.SQLLoad;
import act.QDF.QDFGUI;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
/**
 * Class acts as an Adapter to access the DB. It should hold a 
 * The primary reason that this is an independent service is to make it independent
 *  of any particular Activity. 
 *  
 *  Note: will have handshaking with 
 *  
 *  
 * FIXME Implement a Binder for the service ?
 * If this proves to be an issue, we can move to have the service be bound to what 
 * ever active GUI is present. 
 * 		Service Manager might be required at that point
 */
public class QDFDbAdapter{
    //Might be able to drop these into the R string xml since this DB will be static 
	
	/* Android// is most likely the root directory*/ 
	//Broadcast Receiver
	/*
	 Should cover all data transfers to and from DB:
	 -Sending Variables from GUI to the Settings Table in the DB to the C/C++ scripts
	 
	 -Once Polling service has broadcast that there is new data, 
	 	Read the new data in and pass it to the GUI to update the location
	 
	 */
    public static final String UPDATEGUIACTION = "com.Services.UpdateGUIValues";
    public static final String GETLOCATIONACTION = "com.Services.GetLocationValues";	
	
    /* Defaults will be 
    *  .5 - sec dwell time
	*	18,525,000,000 kil - center freq. 18.525 Mega Hz 
    */
    
    private final int mDefaultCenterFreq = 18525000;//Hz
    private final int mDefaultDwellTime = 500;//mSec
		
	//Global DB Variables
    public static final String DBPATH="/data/data/act.QDF/databases/";
    public static final String DBNAME = "QDFDatabase";
    public static final int DBVERSION = 1;
    
    //Global Keys
    public static final String ID = "_id";
    public static final String TIMESTAMP = "tstamp";
    
    //Table 1:
    /*
     * the Settings table will need Dwell time as a double and 
     * Center frequency
     */
    public static final String SETTINGSTABLENAME= "settings";

    //Table 1 Keys
    public static final String DWELLTIME= "dwelltime";//assumed Seconds*Proabbly will need to change*
    public static final String CENTERFREQ= "centerfreq";
    public static final String READ = "read";
    
    
    //Table 2:
    /*Data should only have one meaning-full column which will be
    the integer value between 0 and 360(degrees)
    */
    public static final String DATATABLENAME = "data";

    //Table 2 Keys
    public static final String LOCATION= "location";//location given in Degrees(0 = left, EAST = Cartesian Coordinate plan)
    public static final String POWERLEVEL= "powerlevel";//This should be the average power 
    
    /**
     * Commands
     * 
     * We only need a limited number of SQLcommands, most of which don't
     *  need to be generated dynamically.
     *  
     *  Dynamic
     *   	Write new values from GUI to settings table in the DB
     *   		*used with static labels*
     *  Static
     *  	Create tables(Settings+data)
     *  
     *  	Query for direction data, key of timestamp
     *  	 
     * 
     */
    public static final String COMMAND_CREATE_TABLE_SETTINGS= "CREATE TABLE "+SETTINGSTABLENAME+" ( "+
    		//ID +" INTEGER NOT NULL, "+
    		TIMESTAMP + " DATE PRIMARY KEY, "+
    		DWELLTIME + " INTEGER NOT NULL, "+
    		CENTERFREQ + " INTEGER NOT NULL, " +
    		READ +" INTEGER NOT NULL);";
    
    public static final String COMMAND_CREATE_TABLE_DATA= "CREATE TABLE "+DATATABLENAME+ " ( "+
			//TIMESTAMP + " INTEGER PRIMARY KEY, "+
    		TIMESTAMP + " DATE PRIMARY KEY, "+
   			LOCATION + " INTEGER NOT NULL, " +
   			POWERLEVEL + " INTEGER NOT NULL);";
    
    public static final String COMMAND_CREATE_TRIGGER_SETTINGS=
    "CREATE TRIGGER "+SETTINGSTABLENAME+"_"+TIMESTAMP+" AFTER  INSERT ON " + SETTINGSTABLENAME+
    " BEGIN "+
    "UPDATE "+ SETTINGSTABLENAME+" SET " +TIMESTAMP+" = STRFTIME('%s','now') WHERE rowid = new.rowid; "+
    "END;"; 
    
    public static final String COMMAND_CREATE_TRIGGER_DATA=
        "CREATE TRIGGER "+DATATABLENAME+"_"+TIMESTAMP+" AFTER  INSERT ON " + DATATABLENAME+
        " BEGIN "+
        "UPDATE "+ DATATABLENAME+" SET " +TIMESTAMP+" = STRFTIME('%s','now') WHERE rowid = new.rowid; "+
        "END;"; 
    ////////////////////////Local variables//////
	
    private static SQLiteDatabase mDb;//we only want this class manipulating the database
    private final Context mContext;
    private static QDFDbHelper mDbHelper;
    
    /*
     * Serives
     * @see android.app.Service#onCreate()
     */

    
///////////////////Class
    public QDFDbAdapter(Context context){
    	mContext = context;
    }
//Functions- for database access and creation
//Void since we dont want to return  
    //public QDFDbService open() throws SQLException {

    
    public QDFDbAdapter open() throws SQLException {        
    	mDbHelper = new QDFDbHelper(mContext);    	
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    
    public void close() {
        mDbHelper.close();
        mDb.close();
    }
    
    
    
    /////Test functions
//    public Cursor fetchAllData() {//Test
//        return mDb.query(this.DATATABLENAME, new String[] {this.TIMESTAMP,this.LOCATION},
//        		null, null, null, null, null);
//    }
    @Deprecated
    public Cursor readSettings(){
        return mDb.query(QDFDbAdapter.SETTINGSTABLENAME, new String[] {QDFDbAdapter.TIMESTAMP,QDFDbAdapter.DWELLTIME,QDFDbAdapter.CENTERFREQ,QDFDbAdapter.READ},
        		null, null, null, null, null);
    }
    @Deprecated
    public long updateData(	){ 	
    	return SQLLoad.loadData(mDb);
    }
    /*
     * Enter a blank entry in the database to set-up a 
     */
    public long InitializeData(	){
    	
    	return SQLLoad.loadData(mDb);
    }
   public void loadTestData(){
	   if(mDb!=null&&mDb.isOpen()){
		  SQLLoad.loadData(mDb);
	   }
   }
   /*TEST- to simulated the data handshake*/
   public static void simHandShake(int count){
	   if(count % 8==0){//Every 4 sseconds		   
		   ContentValues values = new ContentValues();
		   values.put(QDFDbAdapter.READ, 1);		   
		   mDb.update(QDFDbAdapter.SETTINGSTABLENAME, values, null, null);
		   //replaceOrThrow(this.SETTINGSTABLENAME, null, initialValues)
	   }	   
   }
   
   /*
    * Simulate loading the data in the first thing
    * 
    * Add  New record every .5 seconds, fastest we can add
    * this produces a shit tone of error s 
    */
   public static void simFirstData(int count)
   {
	   if((count%2)==0){
		   try{   
		   SQLLoad.loadData(mDb);
		   }catch(Exception e){
			   Log.e(QDFGUI.QDFTAG, e.getMessage());
			   //FIXME should pause the polling service when the Activity is not up to up
			   //Occurs more when the 
		   }
	   }
   }
	
/**/
    public boolean purgeAll(){//clear the Database
    	long temp = -1;
    	long temp2 = -1;
    	
    	temp = this.purgeData();
    	temp2 = this.purgeSettings();
    		
    	return  (temp>-1&&temp2>-1);
    	
    }
    
    //////Table specific functions
    
/**
	-----------Data Table--------------    
*/
    public long purgeData(){
    	long temp = -1;
    	
    	try{
    	if(mDb !=null){
    		temp = mDb.delete(DATATABLENAME, null, null);
    	}
    	}catch(Exception e){
    		Log.e(QDFGUI.QDFTAG," Database closed");
    	}
    	return temp;
    }
    
    public static long delOldData(long currentTimeStamp){
    	long temp = -1;
    	
    	try{
    	if(mDb !=null&& mDb.isOpen()){
    		temp = mDb.delete(DATATABLENAME, "tstamp < "+currentTimeStamp, null);
    	}
    	}catch(Exception e){
    		Log.e(QDFGUI.QDFTAG,e.getMessage());
    	}
    	return temp;
    }

    /**
     * pollDataTable will only look at the currect time stamp of the data table
     */
    public static long pollDataTable(){
    	long temp = 0;
    	if(mDb!= null && mDb.isOpen()){
    		Cursor c = mDb.query(QDFDbAdapter.DATATABLENAME, new String[] {QDFDbAdapter.TIMESTAMP},
    				null, null, null, null, null);
    		if(c.moveToLast()){
    			temp = Long.parseLong(c.getString(0));
    		}
    		c.close();
    	return temp;
    	}
    	return -1;
    }
    public static int pollSettingsTable(){
    	int temp = -1;
    	/*FIXME Needed for abd push and pulls, obviously creates a fair amount of lag
    	mDbHelper.close();
    	mDb = mDbHelper.getWritableDatabase();
    	*/
    	if(mDb!= null && mDb.isOpen()){
    		Cursor c = mDb.query(QDFDbAdapter.SETTINGSTABLENAME, new String[] {QDFDbAdapter.READ},
    				null, null, null, null, null);
    		if(c.moveToLast()){
    			temp = Integer.parseInt(c.getString(0));
    		}
    		c.close();
    	return temp;
    	}
    	return -1;
    }
     
    //Read data table
    public static Cursor readData(){
    	if(mDb!=null&& mDb.isOpen()){
    		return mDb.query(QDFDbAdapter.DATATABLENAME, new String[] {QDFDbAdapter.TIMESTAMP,QDFDbAdapter.LOCATION, QDFDbAdapter.POWERLEVEL},
    		null, null, null, null, null);
    	}else{
    		return null;
    	}
    	
}
    /**
    					-----------Settings Table--------------    
    */
    /**
     * Removes previous entries,and place new data in the settings table 
     */
    /**
     * Initialize the settings table with the default values for the Dwell time and center frequency.
     * this also will work at  
     *
     */
    public long initializeSettings(){
    	return updateSettings(this.mDefaultDwellTime,this.mDefaultCenterFreq);
    }
    public long updateSettings(int dwellTime, int centerFreque) {
        ContentValues initialValues = new ContentValues();
          
        initialValues.put(QDFDbAdapter.DWELLTIME, dwellTime);
        initialValues.put(QDFDbAdapter.CENTERFREQ, centerFreque);
        initialValues.put(QDFDbAdapter.READ, 0);

        return mDb.insert(QDFDbAdapter.SETTINGSTABLENAME, null, initialValues);
    }
    
    public long purgeSettings(){
    	long temp = -1;
    	
    	try{
    	if(mDb !=null){
    		temp =mDb.delete(SETTINGSTABLENAME, null, null);
    	}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	return temp;
    }
    

        
///////////////////Helper Class to perform all the SQLite DB creates and opens
	private static class QDFDbHelper extends SQLiteOpenHelper {

		public QDFDbHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, DBNAME, null, DBVERSION);
			
		}
		
		public QDFDbHelper(Context context){
			super(context, DBNAME, null, DBVERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase sqlDB) {//called by getWriteableDatabase()           
			try{
            	sqlDB.execSQL(COMMAND_CREATE_TABLE_SETTINGS);
            	sqlDB.execSQL(COMMAND_CREATE_TRIGGER_SETTINGS);
            }catch(Exception e){
            	e.printStackTrace();
            }
            
            try{
            	sqlDB.execSQL(COMMAND_CREATE_TABLE_DATA);
            	sqlDB.execSQL(COMMAND_CREATE_TRIGGER_DATA);
        	}catch(Exception e){
        		e.printStackTrace();
            }
		}
		@Override
		public void onOpen(SQLiteDatabase sqlDb) {
			super.onOpen(sqlDb);
		}
		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			//Nothing at this point
		}
	}//Helper
}//Service
