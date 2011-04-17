package act.QDF;

import java.sql.Timestamp;

import com.SQLiteDatabaseWrapper.QDFDbAdapter;
import com.Services.PollingService;

import act.QDF.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AnalogClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

public class QDFGUI extends Activity {

    public static final String POLLINGACTION = "act.QDF.QDFGUI.Polling";
    public static final String UPDATEACTION = "act.QDF.QDFGUI.UpdateSettingsDone";
    public static final String TOGTOCONACTION = "act.QDF.QDFGUI.ToggleToConsole";
    

	
    private BroadcastReceiver mBR;
	IntentFilter mConsoleIf;
	
    QDFDbAdapter mAdapter;
    
    String mFreqScale;
    String mTimeScale;
    
    int mDegree;
    String mDirection;
    int intID;
        
    //Progress Dialog
    ProgressDialog mProgress;
    
    
/*	
 * 
   public QDFGUI(){  
    super();    
    }
    */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qdfgui); 
        
        intID =-1;
        
        mConsoleIf = new IntentFilter();
        mConsoleIf.addAction(this.POLLINGACTION);
        mConsoleIf.addAction(this.UPDATEACTION);       
        
        mBR = new BroadcastReceiver(){
            public void onReceive(Context context, Intent intent) {
        		//TextView status = (TextView) findViewById(R.id.StatusText);        		
            	String action =  intent.getAction();
            	
            	if(QDFGUI.UPDATEACTION.equals(action))
            		{mProgress.dismiss();}
            	else{	
            	if(QDFGUI.POLLINGACTION.equals(action))
            		
            		new UIUpdateTask().execute();
            	}

        		
            }
        };
        
        
      
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Changeing setting for Angle of Arrival...");
        mProgress.setCancelable(false);

        mAdapter = new QDFDbAdapter(this);
        
        /*Spinner - Time*/
        Spinner spinnerTime = (Spinner) findViewById(R.id.spinnerTime);
        ArrayAdapter<CharSequence> adapterTime = ArrayAdapter.createFromResource(
                this, R.array.time_array, android.R.layout.simple_spinner_item);
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(adapterTime);
        
        spinnerTime.setOnItemSelectedListener(TimeOnItemSelectedListener);
        
        /*Spinner - Freq*/
        Spinner spinnerFreq = (Spinner) findViewById(R.id.spinnerFreq);
        ArrayAdapter<CharSequence> adapterFreq = ArrayAdapter.createFromResource(
                this, R.array.freq_array, android.R.layout.simple_spinner_item);
        adapterFreq.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFreq.setAdapter(adapterFreq);
        
        spinnerFreq.setOnItemSelectedListener(FreqOnItemSelectedListener);
        
        //Buttons
        Button toggleToConButton = (Button)findViewById(R.id.toggleToConButton);        
        toggleToConButton.setOnClickListener(mToggleListener);
        
        Button updateSettingsButton = (Button)findViewById(R.id.updateSettingsButton);        
        updateSettingsButton.setOnClickListener(mUpdateSettingsListener);
    }//OnCreate
	  
    @Override
    public void onStart(){
    	super.onStart();	
/*        this.registerReceiver(mBR, mConsoleIf);
        
    	this.startService(new Intent(this,com.Services.PollingService.class));        
 */
    }
    @Override
    public void  onResume(){
    	super.onResume();
    	
        this.registerReceiver(this.mBR, mConsoleIf);
        if(!PollingService.isRunning()){
        	this.startService(new Intent(this,com.Services.PollingService.class));        
        }     	
        
        mAdapter.open();
        mAdapter.purgeAll();//testing
        mAdapter.loadTestData();
    }
    @Override
    public void onPause(){//called when new activity started
    	super.onPause();
    	//this.unregisterReceiver(consoleBR);
    this.unregisterReceiver(this.mBR);
    }
    @Override
    public void onStop(){
    	super.onStop();
    	//mAdapter.close();

    }
    @Override
    public void onDestroy(){
    	//this.unregisterReceiver(receiver)
    	super.onDestroy();
    	if(PollingService.isRunning()){
    		this.stopService(new Intent(this,com.Services.PollingService.class));
    	}
    	
    	mAdapter.close();
    }
    ////////////////Helper function
    
    public void updateGUI(String newValue){
    	//TextView status = (TextView) findViewById(R.id.StatusText);
		//Simulated update GUI Algorithm
        //	status.setText("Status: "+"\n"+newValue);
    	//Turn on the corresponding radio button based on the feed back we got formt he functions  	
    	//getButton();
    	//int intID;
    	if(intID!=-1){
        RadioButton oldButton = (RadioButton)findViewById(intID);        
        oldButton.setChecked(false);
        //oldButton.setSelected(false);
    	}
    	
    	int index = newValue.indexOf(',');// substring(start)
    	mDirection = newValue.substring(0, index);
    	intID = Integer.parseInt(newValue.substring(index+1));
    	
        RadioButton newButton = (RadioButton)findViewById(intID);        
        newButton.setChecked(true);
        //newButton.set
        //newButton.setSelected(true);
        EditText degreeText = (EditText)findViewById(R.id.editTextDegree);        
        degreeText.setText("Degree: "+mDegree);
        
        EditText dirText = (EditText)findViewById(R.id.editTextDirection);        
        dirText.setText("Direction: "+mDirection);
        
        //AnalogClock clock=(AnalogClock)findViewById(R.id.analogClock1);
        
        //clock.se
    }
    private int getButton(String state){
    	return R.id.radioButtonENE;
    }
    ///----------Actions
    
    
    protected void toggleActivities(){
    	Intent temp = new Intent(this,act.QDF.DebugConsole.class);
		temp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	//this.stopService(new Intent(this,com.Services.PollingService.class));
		startActivity(temp);
		//this.finish();
		}
    
    
       //Listeners 
    private OnClickListener mUpdateSettingsListener = new OnClickListener() {
        public void onClick(View v) {
        	//ProgressDialog dialog = ProgressDialog.show(thisOne, "", "Changing ");//TODO Cancelable listener?
        	//dailog.
        	//mAdapter.updateSettings(0, 0);
        	
        	
        	//mAdapter.purgeSettings();
        	mAdapter.loadTestData();
        	//mProgress.show();
        	//adapter.updateData();
        }
    };
    private OnClickListener mToggleListener = new OnClickListener() {
        public void onClick(View v) {
        	toggleActivities();
        }
    };
    public OnItemSelectedListener TimeOnItemSelectedListener = new OnItemSelectedListener() {

        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
          //Toast.makeText(parent.getContext()), "The planet is " +
              mTimeScale = parent.getItemAtPosition(pos).toString();//, Toast.LENGTH_LONG).show();
        }

        public void onNothingSelected(AdapterView parent) {
          // Do nothing.
        }
    };//Listenr
    public OnItemSelectedListener FreqOnItemSelectedListener = new OnItemSelectedListener() {

        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
          //Toast.makeText(parent.getContext()), "The planet is " +
              mFreqScale=parent.getItemAtPosition(pos).toString();//, Toast.LENGTH_LONG).show();
        }

        public void onNothingSelected(AdapterView parent) {
          // Do nothing.
        }
    };//Listenr
    public class UIUpdateTask extends AsyncTask<CharSequence,Void, String>{
    	//<parameters,progress, result>
    		public UIUpdateTask() {
    			super();

    		}
    		
    		@Override
       		protected String doInBackground(CharSequence... arg0) {
    	    	//TextView status = (TextView) findViewById(R.id.StatusText);
    			//Simulated Algorithm
    	    	//String text = (String)status.getText();
    	        /*
    	         *  * TODO
    	         * Defaults will be 
    	         *  .5 - sec dwell time
    				18,525,000,000 kil - center freq. 18.525 Mega Hz 
    	         * True directions:
    	         * 
    	         * East = 0
    	         * North = 90
    	         * West = 180
    	         * South = 270
    	         * 
    	         * 16 buttons
    	         * 16 states
    	         * 
    	         * 360 Degress total/16=22.5 Degrees per button
    	         * *+or- 11.25
    	         * 
    	         * 
    	         * 
    	         * Thus the primary function of this is to return the
    	         *  coordinate/state we are in. This returns the ID
    	         */
    			String results = "N";//represents the state, Defualt to north 
    			
    			int intID = R.id.radioButtonN;
    			//FIXME-better try catch support
    			Cursor cursor = (SQLiteCursor) mAdapter.readData();
    	        cursor.moveToLast();
    	        
    			mDegree=Integer.parseInt(cursor.getString(1));
    			cursor.close();
    			
    			//Enforce degree range
    			if(mDegree>360 || mDegree<0)
    				{
    				return results+","+String.valueOf(intID);}
    			
    			/*16 possible states depending on the degree
    			 *	
    			 *E
    			 *ENE
    			 *NE
    			 *NNE
    			 *N
    			 *NNW
    			 *NW
    			 *WNW
    			 *W
    			 *WSW
    			 *SW
    			 *SSW
    			 *S
    			 *SSE
    			 *SE
    			 *ESE
    			 *	
    			 **/
    			
      			if(mDegree >=348.7||mDegree<11.25)
      				{
      				 results = "E";
      				 intID = R.id.radioButtonE;
      				}
      			else if(mDegree >=11.25&&mDegree<33.75)
  					{results = "ENE";
  					intID = R.id.radioButtonENE;}
      			else if(mDegree >=33.75&&mDegree<56.25)
					{results = "NE";
					intID = R.id.radioButtonNE;}      				
      			else if(mDegree >=56.25&&mDegree<78.75)
					{results = "NNE";
					intID = R.id.radioButtonNNE;}
      			else if(mDegree >=78.75&&mDegree<101.25)
					{results = "N";
					intID = R.id.radioButtonN;}
      			else if(mDegree >=101.25&&mDegree<123.75)
					{results = "NNW";
					intID = R.id.radioButtonNNW;}
      			else if(mDegree >=123.75&&mDegree<146.25)
					{results = "NW";}
      			else if(mDegree >=146.25&&mDegree<168.75)
					{results = "WNW";
					intID = R.id.radioButtonWNW;}
      			else if(mDegree >=168.75&&mDegree<191.25)
					{results = "W";
					intID = R.id.radioButtonW;}
      			else if(mDegree >=191.25&&mDegree<213.75)
					{results = "WSW";
					intID = R.id.radioButtonWSW;}
      			else if(mDegree >=213.75&&mDegree<236.25)
					{results = "SW";
					intID = R.id.radioButtonSW;}
      			else if(mDegree >=236.25&&mDegree<258.75)
					{results = "SSW";
					intID = R.id.radioButtonSSW;}
      			else if(mDegree >=258.75&&mDegree<281.25)
      				{results = "S";}
      			else if(mDegree >=281.25&&mDegree<303.75)
      				{results = "SSE";
      				intID = R.id.radioButtonSSE;}
      			else if(mDegree >=303.75&&mDegree<326.25)
      				{results = "SE";
      				intID = R.id.radioButtonSE;}
      			else if(mDegree >=326.25&&mDegree<348.75)
      				{results = "ESE";
      				intID = R.id.radioButtonESE;}
    			//Now we have state
      			
      			
      			return results+","+String.valueOf(intID);

    		}

    		@Override
    		public void onPostExecute(String result){
    			updateGUI(result);
    			this.cancel(true);
    			
    		}
    }//Asynch Task
}//QDF Activity
