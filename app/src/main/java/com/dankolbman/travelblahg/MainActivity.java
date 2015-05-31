package com.dankolbman.travelblahg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends ActionBarActivity {

    private GpsTracker tracker;

    private EditText urlEditText;
    private EditText apiEditText;
    private String endpoint_string;
    private String api_string;
    private TextView statusTextView;
    private TextView latitudeText;
    private TextView longitudeText;
    private TextView filenameText;
    private ProgressBar progressBarBar;
    private TextView progressText;

    private ListView fileListView;

    private File[] files;
    private Boolean isUploading = false;
    private Boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Base on the current time
        String filename = System.currentTimeMillis()+".dat";
        // Set up tracker
        tracker = new GpsTracker(this, filename);
        // Get elements
        urlEditText = (EditText) findViewById(R.id.url_endpoint);
        apiEditText = (EditText) findViewById(R.id.api_key);
        statusTextView = (TextView) findViewById(R.id.status_text);
        latitudeText = (TextView) findViewById(R.id.latitude);
        longitudeText = (TextView) findViewById(R.id.longitude);
        filenameText = (TextView) findViewById(R.id.filename);
        fileListView = (ListView) findViewById(R.id.file_list);
        progressBarBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressText = (TextView) findViewById(R.id.progress_text);
        this.getFilesDir().mkdirs();
        filenameText.setText("Saving to: " + this.getFilesDir().getAbsolutePath() + "/" + filename);

        File f = new File(this.getFilesDir().getAbsolutePath());
        files = f.listFiles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    Pings server with user and auth
     */
    public void testAPI(View view) {
        endpoint_string = urlEditText.getText().toString();
        api_string = apiEditText.getText().toString();
        String url_string = "http://" + endpoint_string + "/" + api_string;

        // Begin the upload
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(url_string);
    }

    /*
    Get current location from tracker
     */
    public void getLoc(View view) {
        if(tracker.canGetLocation() && tracker.getLocation() != null) {
            Location loc = tracker.getLocation();
            latitudeText.setText("Latitude: "+Double.toString(loc.getLatitude()));
            longitudeText.setText("Longitude: "+Double.toString(loc.getLongitude()));
        } else {
            CharSequence msg = "Must be recording";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, msg, duration);
            toast.show();
        }
    }

    /*
    Start gps service
     */
    public void startRec(View view) {
        if(!isRecording) {
            isRecording = true;
            // Base on the current time
            String filename = System.currentTimeMillis() + ".dat";
            // Set up tracker
            tracker = new GpsTracker(this, filename);
            filenameText.setText("Saving to: " + this.getFilesDir().getAbsolutePath() + "/" + filename);
            tracker.startRec();
        }
    }

    /*
    Stop gps service
     */
    public void stopRec(View view) {
        if(isRecording) {
            isRecording = false;
            tracker.stopRec();
        }
    }

    /*
    Get files in data directory and display in list view
     */
    public void getFiles(View view) {

        File f = new File(this.getFilesDir().getAbsolutePath());
        files = f.listFiles();

        // Add filenames to list view
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = files.length-1; i >= 0; --i) {
            String filename = files[i].getName();
            if(filename.substring(filename.lastIndexOf('.'),filename.length()).equals(".dat")) {
                // Remove file extension
                filename = filename.substring(0, filename.lastIndexOf("."));
                // Get time in UTC from the file name
                Date date = new Date(Long.parseLong(filename));
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss z");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                int numlines = 0;
                try {
                    numlines = countLines(files[i]);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                list.add(sdf.format(date) + " - " + numlines + " Pings");
            }
        }


        final ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        fileListView.setAdapter(adapter);

        /*
        Click listener for file list
         */
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if (!isUploading) {
                    final String item = (String) parent.getItemAtPosition(position);
                    view.setBackgroundColor(Color.parseColor("#55EE55"));
                    //list.remove(item);
                    //adapter.notifyDataSetChanged();

                    isUploading = true;
                    // Begin the upload
                    endpoint_string = urlEditText.getText().toString();
                    api_string = apiEditText.getText().toString();
                    // Files were added in reverse order (newest first)
                    File file = files[files.length - position - 1];

                    UploadTask runner = new UploadTask();
                    runner.execute(file);
                }
            }

        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, long id) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(view.getContext());
                alertDialogBuilder.setTitle("Delete file");
                alertDialogBuilder
                        .setMessage("Click yes to delete").setCancelable(false)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                final String item = (String) parent.getItemAtPosition(position);
                                File file = files[files.length - position - 1];
                                Boolean deleted = file.delete();
                                list.remove(item);
                                adapter.notifyDataSetChanged();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("No",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
        });

    }

    /*
    For counting number of pings in a data file
    http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
     */
    public static int countLines(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        byte[] c = new byte[1024];
        int count = 0;
        int readChars = 0;
        boolean empty = true;
        try {
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            is.close();
        }
        return (count == 0 && !empty) ? 1 : count;
    }

    /*
    Uploader
     */
    private class UploadTask extends AsyncTask<File, Integer, String> {

        private String resp;
        private int total;

        @Override
        protected String doInBackground(File... params) {
            publishProgress(0);

            File file = params[0];
            String url_string = "http://" + endpoint_string + "/" + api_string + "/ping";

            total = 1;
            try{
                total = countLines(file);
            } catch (IOException e){
                e.printStackTrace();
            }

            int attempted = 0;
            int good = 0;

            // Read each line into json and post it
            try {
                URL url = new URL(url_string);
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null && attempted-good < 10) {

                    // Construct JSON data object
                    JSONObject jsonobj;
                    jsonobj = new JSONObject();
                    try {
                        String[] parts = line.split(", ");
                        jsonobj.put("timestamp", Long.parseLong(parts[0]));
                        jsonobj.put("accuracy", Float.parseFloat(parts[1]));
                        jsonobj.put("latitude", Double.parseDouble(parts[2]));
                        jsonobj.put("longitude", Double.parseDouble(parts[3]));
                    } catch (JSONException ex) {
                        resp = "Error Occurred while building JSON";
                        ex.printStackTrace();
                    }
                    // Connect to the api
                    try {
                        // Needs to be done every request
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(10000);
                        conn.setConnectTimeout(15000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.connect();
                        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"UTF-8");
                        // Write json
                        out.write(jsonobj.toString());
                        out.flush();
                        out.close();

                        // Get response
                        int HttpResult = conn.getResponseCode();

                        StringBuilder sb = new StringBuilder();
                        if(HttpResult == HttpURLConnection.HTTP_OK){
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    conn.getInputStream(),"utf-8"));
                            String inresp = null;
                            while ((inresp = in.readLine()) != null) {
                                sb.append(line);
                            }
                            in.close();
                            resp = sb.toString();
                            Log.d("", resp);
                            if(resp.substring(0,3).equals("143")) {
                                good++;
                            }
                        } else {
                            resp =  Integer.toString(HttpResult);
                        }
                        attempted++;

                        // Disconnect
                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        resp = e.getMessage();
                    }
                    publishProgress((int) (attempted));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(attempted-good >= 10) {
                resp = "Stopped Upload. Too many failed POSTs";
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            isUploading = false;
            statusTextView.setText(result);
        }

        @Override
        protected void onPreExecute() {
            statusTextView.setText("Uploading...");
            progressBarBar.setProgress(0);
        }

        @Override
        protected void onProgressUpdate(Integer... prog) {
            progressBarBar.setProgress((int)(prog[0]*100.0/total));
            progressText.setText(prog[0].toString() + "/" + Integer.toString(total));
        }
    }

    /*
    Task runner for network threads
     */
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String resp;

        @Override
        protected String doInBackground(String... params) {
            publishProgress("Uploading...");

            // Construct JSON data object
            JSONObject jsonobj;
            jsonobj = new JSONObject();
            try {
                jsonobj.put("status", "testing");
            } catch (JSONException ex) {
                resp = "Error Occurred while building JSON";
                ex.printStackTrace();
            }

            // Connect to the api
            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.connect();

                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"UTF-8");

                out.write(jsonobj.toString());
                out.flush();
                out.close();

                // Get response
                int HttpResult = conn.getResponseCode();

                StringBuilder sb = new StringBuilder();
                if(HttpResult == HttpURLConnection.HTTP_OK){
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            conn.getInputStream(),"utf-8"));
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();
                    resp = sb.toString();
                } else {
                    resp =  Integer.toString(HttpResult);
                }

                // Disconnect
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {
            statusTextView.setText(result);
        }

        @Override
        protected void onPreExecute() {
            statusTextView.setText("Uploading...");
        }

        @Override
        protected void onProgressUpdate(String... text) {
            statusTextView.setText(text[0]);
        }
    }
}
