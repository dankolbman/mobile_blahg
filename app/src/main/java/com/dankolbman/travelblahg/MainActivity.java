package com.dankolbman.travelblahg;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

    private EditText urlEditText;
    private EditText apiEditText;
    private String endpoint_string;
    private String api_string;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get elements
        urlEditText = (EditText) findViewById(R.id.url_endpoint);
        apiEditText = (EditText) findViewById(R.id.api_key);
        statusTextView = (TextView) findViewById(R.id.status_text);
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

    public void testAPI(View view) {
        endpoint_string = urlEditText.getText().toString();
        api_string = apiEditText.getText().toString();
        String url_string = "http://" + endpoint_string + "/api/dan/" + api_string + "/ping";

        statusTextView.setText(url_string);

        // Begin the upload
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(url_string);
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
                jsonobj.put("description", "Real");
                jsonobj.put("enable", "true");
            } catch (JSONException ex) {
                statusTextView.setText("Error Occurred while building JSON");
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
                        sb.append(line + "\n");
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
            statusTextView.setText("Preparing for upload...");
        }

        @Override
        protected void onProgressUpdate(String... text) {
            statusTextView.setText(text[0]);
        }
    }
}
