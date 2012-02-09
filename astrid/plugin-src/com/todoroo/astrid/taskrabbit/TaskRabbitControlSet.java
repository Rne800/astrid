package com.todoroo.astrid.taskrabbit;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.RestClient;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.actfm.EditPeopleControlSet.AssignedChangedListener;
import com.todoroo.astrid.actfm.OAuthLoginActivity;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.ui.FragmentPopover;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitControlSet extends PopupControlSet implements AssignedChangedListener, LocationListener {


    public interface TaskRabbitSetListener {
        public void readFromModel(JSONObject json, String key);
        public void saveToJSON(JSONObject json, String key) throws JSONException;
        public void writeToJSON(JSONObject json, String key) throws JSONException;
    }

    /** task model */
    Task model = null;

    @Autowired
    private RestClient restClient;


    GeoPoint[] supportedLocations =
    {
            new GeoPoint(42358430, -71059770),
            new GeoPoint(37739230, -122439880),
            new GeoPoint(40714350, -74005970),
            new GeoPoint(41878110, -8762980),
            new GeoPoint(34052230, -118243680),
            new GeoPoint(33717470, -117831140)};

    /** true if editing started with a new task */
    boolean isNewTask = false;
    private EditText taskDescription;
    private EditText taskTitle;
    private Button  taskButton;
    private LinearLayout taskControls;
    private Location currentLocation;
    private ImageButton pictureButton;
    private Bitmap pendingCommentPicture = null;
    private int cameraButton;
    private FragmentPopover menuPopover;
    private TextView menuTitle;
    private ListView menuList;
    private ListAdapter adapter;
    private ScrollView scrollView;


    private View menuNav;
    private ImageView menuNavDisclosure;

    private final Fragment fragment;
    private final List<TaskRabbitSetListener> controls = Collections.synchronizedList(new ArrayList<TaskRabbitSetListener>());

    private Spinner spinnerMode;
    private LinearLayout row;

    public static final int REQUEST_CODE_TASK_RABBIT_OAUTH = 5;
    /** Act.fm current user name */

    public static final String TASK_RABBIT_TOKEN = "task_rabbit_token"; //$NON-NLS-1$
    //public static final String TASK_RABBIT_URL = "http://www.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_URL = "http://rs-astrid-api.taskrabbit.com"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_ID = "fDTmGeR0uNCvoxopNyqsRWae8xOvbOBqC7jmHaxv"; //$NON-NLS-1$
    public static final String TASK_RABBIT_CLIENT_APPLICATION_ID = "XBpKshU8utH5eaNmhky9N8aAId5rSLTh04Hi60Co"; //$NON-NLS-1$

    public static final String CITY_NAME = "task_rabbit_city_name"; //$NON-NLS-1$
    private TaskRabbitTaskContainer taskRabbitTask;



    public TaskRabbitControlSet(Fragment fragment, int viewLayout, int displayViewLayout) {
        super(fragment.getActivity(), viewLayout, displayViewLayout, 0);
        this.fragment = fragment;
        DependencyInjectionService.getInstance().inject(this);
        loadLocation();

    }











    @Override
    protected void refreshDisplayView() {
        JSONObject remoteData = taskRabbitTask.getRemoteTaskData();
        if (remoteData != null)
            updateDisplay(remoteData);
    }


    @Override
    public void readFromTask(Task task) {
        model = task;
        taskRabbitTask = TaskRabbitDataService.getInstance().getContainerForTask(model);
        updateTaskRow(taskRabbitTask);

    }

    @Override
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpUIComponents();
                dialog.show();
            }
        };
    }

    @Override
    public String writeToModel(Task task) {
        //        TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
        return null;
    }


    private void updateTaskRow(TaskRabbitTaskContainer container) {
        displayText.setText("Task Rabbit Status");
        JSONObject remoteData = container.getRemoteTaskData();
        if (remoteData != null) {
            updateDisplay(remoteData);
            updateStatus(remoteData);
        }
    }

    private int getSelectedItemPosition() {
        return (menuList.getSelectedItemPosition() >= 0) ? menuList.getSelectedItemPosition() : 0;
    }


    private void setUpControls() {
        TypedArray arrays = activity.getResources().obtainTypedArray(R.array.tr_default_set);
        TypedArray arrayType = activity.getResources().obtainTypedArray(R.array.tr_default_array);
        for (int i = 0; i < arrays.length(); i++) {

            int titleID = arrays.getResourceId(i, 0);
            int arrayID = arrayType.getResourceId(i, 0);
            if (arrayID == R.string.tr_set_key_location) {
                TaskRabbitLocationControlSet set = new TaskRabbitLocationControlSet(fragment, R.layout.task_rabbit_row, titleID, i);
                controls.add(set);
            }
            else if(arrayID == R.string.tr_set_key_deadline) {

                TaskRabbitDeadlineControlSet deadlineControl = new TaskRabbitDeadlineControlSet(
                        activity, R.layout.control_set_deadline,
                        R.layout.task_rabbit_row);
                controls.add(deadlineControl);
                deadlineControl.readFromTask(model);
            }
            else if(arrayID == R.string.tr_set_key_name) {
                TaskRabbitNameControlSet nameControlSet = new TaskRabbitNameControlSet(activity,
                        R.layout.control_set_notes, R.layout.task_rabbit_row, titleID, i);
                controls.add(nameControlSet);
            }
            else {
                TaskRabbitSpinnerControlSet set = new TaskRabbitSpinnerControlSet(fragment, R.layout.task_rabbit_spinner, titleID, i);
                controls.add(set);
            }
        }
        if(TextUtils.isEmpty(taskDescription.getText())){
            taskDescription.setText(model.getValue(Task.NOTES));

        }
        if(TextUtils.isEmpty(taskTitle.getText())) {
            taskTitle.setText(model.getValue(Task.TITLE));
        }
        populateFields(taskRabbitTask);

        displayViewsForMode(getSelectedItemPosition());
    }
    private void displayViewsForMode(int mode) {


        taskControls.removeAllViews();

        if (row == null) {
            row = new LinearLayout(activity);
            row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
        }
        else {
            row.removeAllViews();
        }

        menuTitle.setText(activity.getResources().getStringArray(R.array.tr_preset_types)[mode]);
        int[] presetValues = getPresetValues(mode);
        TypedArray keys = activity.getResources().obtainTypedArray(R.array.tr_default_set_key);
        JSONObject parameters = defaultValuesToJSON(keys, presetValues);
        for (int i = 1; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            TaskRabbitSetListener set = controls.get(i);
            int arrayID = keys.getResourceId(i, 0);
            if (arrayID == R.string.tr_set_key_cost_in_cents || arrayID == R.string.tr_set_key_named_price) {
                if(row.getParent() == null)
                    taskControls.addView(row);
                else {
//                    View separator = activity.getLayoutInflater().inflate(R.layout.tea_separator, row);
//                    separator.setLayoutParams(new LayoutParams(1, LayoutParams.FILL_PARENT));

                }
                LinearLayout displayRow = (LinearLayout)((TaskEditControlSet)set).getDisplayView();
                displayRow.findViewById(R.id.TEA_Separator).setVisibility(View.GONE);
                LinearLayout.LayoutParams layoutParams= null;
                if(arrayID == R.string.tr_set_key_named_price) {
                    layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1);
                    displayRow.findViewById(R.id.display_row_body).setPadding(5, 0, 10, 0);
                    displayRow.setMinimumWidth(130);
                }
                else {
                    layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1);
                    displayRow.findViewById(R.id.display_row_body).setPadding(10, 0, 5, 0);
                }
                row.addView(displayRow, layoutParams);
            }
            else {
                taskControls.addView(((TaskEditControlSet)set).getDisplayView());
            }
            ((TaskRabbitSetListener) set).readFromModel(parameters, activity.getString(arrayID));
        }
    }
    private JSONObject defaultValuesToJSON (TypedArray keys, int[] presetValues) {

        JSONObject parameters = new JSONObject();
        for(int i = 0; i < keys.length(); i++) {
            try {
                int arrayID = keys.getResourceId(i, 0);
                parameters.put(activity.getString(arrayID), (presetValues[i]));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return parameters;
    }

    /** Initialize UI components */
    private void setUpUIComponents() {
        if (taskDescription == null){
            scrollView = (ScrollView) getView().findViewById(R.id.edit_scroll);
            taskDescription = (EditText) getView().findViewById(R.id.task_description);
            taskDescription.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
//                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }
            });




            taskTitle = (EditText) getView().findViewById(R.id.task_title);

            taskControls = (LinearLayout)getView().findViewById(R.id.task_controls);

            taskButton = (Button) getView().findViewById(R.id.task_button);
            taskButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    submitTaskRabbit();
                }
            });

            final ImageView mainMenu = (ImageView) getView().findViewById(R.id.main_menu);
            mainMenu.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });



            spinnerMode = (Spinner) getView().findViewById(R.id.task_type);
            String[] listTypes = activity.getResources().getStringArray(
                    R.array.tr_preset_types);

            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    activity, android.R.layout.simple_spinner_item, listTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinnerMode.setAdapter(adapter);
                }
            });
            spinnerMode.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int arg2, long arg3) {
                    displayViewsForMode(getSelectedItemPosition());
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    return;
                }
            });





            menuNav = getView().findViewById(R.id.menu_nav);
            menuNavDisclosure = (ImageView) getView().findViewById(R.id.menu_disclosure_arrow);

            menuTitle = (TextView) getView().findViewById(R.id.task_rabbit_title);
            menuNav.setOnClickListener(menuClickListener);
            createMenuPopover();


            final ClearImageCallback clearImage = new ClearImageCallback() {
                @Override
                public void clearImage() {
                    pendingCommentPicture = null;
                    pictureButton.setImageResource(R.drawable.camera_button_gray);
                }
            };
            pictureButton = (ImageButton) getView().findViewById(R.id.picture);
            pictureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (pendingCommentPicture != null)
                        ActFmCameraModule.showPictureLauncher(fragment, clearImage);
                    else
                        ActFmCameraModule.showPictureLauncher(fragment, null);
                }
            });


            setUpControls();
        }
    }



    private void populateFields(TaskRabbitTaskContainer container) {
        if (container == null) {
            return;
        }

        if(taskRabbitTask.getTaskID() > 0)
            taskButton.setText("Update task!");
        JSONObject jsonData = container.getLocalTaskData();
        synchronized (controls) {
            if(jsonData != null) {
                String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);
                menuList.setSelection(jsonData.optInt(keys[0]));
                String title = jsonData.optString(keys[1]);
                if (!TextUtils.isEmpty(title)) {
                    taskTitle.setText(title);
                }
                for (int i = 0; i < controls.size(); i++) {
                    TaskRabbitSetListener set = (TaskRabbitSetListener) controls.get(i);
                    set.readFromModel(jsonData, keys[i]);
                }
            }
        }
    }


    /* saving/converting task rabbit data */
    private JSONObject localParamsToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();

        int[] presetValues = getPresetValues(getSelectedItemPosition());
        String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);


        String category = "Category: " + taskTitle.getText().toString() + "\n";
        parameters.put(activity.getString(R.string.tr_set_key_description), category + taskDescription.getText().toString());
        for (int i = 0; i < controls.size(); i++) {
            if (presetValues[i] == -1) continue;
            TaskRabbitSetListener set = controls.get(i);
            set.writeToJSON(parameters, keys[i]);
        }
        if (parameters.optJSONArray("other_locations_attributes") == null) {
            parameters.put(activity.getString(R.string.tr_attr_city_id),  Preferences.getInt("task_rabbit_city_id", 1));
            parameters.put(activity.getString(R.string.tr_attr_city_lat), true);
        }

        parameters.put(activity.getString(R.string.tr_set_key_name), taskTitle.getText().toString());
        //        parameters.put(activity.getString(R.string.tr_set_key_type), menuList.getSelectedItem().toString());

        Log.d("localParamsToJSON", parameters.toString());

        if (pendingCommentPicture != null) {
            String picture = buildPictureData(pendingCommentPicture);
            JSONObject pictureArray = new JSONObject();
            pictureArray.put("image", picture);

            parameters.put("uploaded_photos_attributes", new JSONObject().put("1", pictureArray));
            Log.d("The task json", parameters.toString());
        }
        return new JSONObject().put("task", parameters);
    }


    private int[] getPresetValues(int mode) {
        Log.d("fjdskfjdslfjds", "dsfdsf" + mode);
        TypedArray arrays = activity.getResources().obtainTypedArray(R.array.tr_default_type_array);
        int[] presetValues = activity.getResources().getIntArray(arrays.getResourceId(mode, 0));
        return presetValues;
    }


    private String serializeToJSON () throws JSONException {

        JSONObject parameters = new JSONObject();
        String[] keys = activity.getResources().getStringArray(R.array.tr_default_set_key);
        for (int i = 0; i < controls.size(); i++) {
            TaskRabbitSetListener set = controls.get(i);
            set.saveToJSON(parameters, keys[i]);
        }
        parameters.put(activity.getString(R.string.tr_set_key_type), getSelectedItemPosition());
        parameters.put(activity.getString(R.string.tr_set_key_name), taskTitle.getText().toString());
        parameters.put(activity.getString(R.string.tr_set_key_description), taskDescription.getText().toString());
        return parameters.toString();
    }
    private HttpEntity getTaskBody()  {

        try {
            return new StringEntity(localParamsToJSON().toString());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String buildPictureData(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(bitmap.getWidth() > 512 || bitmap.getHeight() > 512) {
            float scale = Math.min(512f / bitmap.getWidth(), 512f / bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(scale * bitmap.getWidth()),
                    (int)(scale * bitmap.getHeight()), false);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    protected void submitTaskRabbit(){

        if(!Preferences.isSet(TASK_RABBIT_TOKEN)){
            loginTaskRabbit();
        }
        else {


            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        String urlCall = "tasks/";
                        if (taskRabbitTask.getTaskID() > 0) urlCall += (taskRabbitTask.getTaskID() > 0);
                        Log.d("Tasks url:", taskRabbitURL(urlCall));
                        Header authorization = new BasicHeader("Authorization", "OAuth " + Preferences.getStringValue(TASK_RABBIT_TOKEN));  //$NON-NLS-1$
                        Header contentType = new BasicHeader("Content-Type",  //$NON-NLS-1$
                        "application/json");   //$NON-NLS-1$

                        String response = restClient.post(taskRabbitURL(urlCall), getTaskBody(), contentType, authorization);
                        Log.d("Task rabbit response", response);
                        JSONObject taskResponse = new JSONObject(response);
                        if(taskResponse.has("id")){
                            taskRabbitTask.setRemoteTaskData(response);
                            taskRabbitTask.setTaskID(taskResponse.optString("id"));
                            Message successMessage = new Message();
                            successMessage.what = 1;
                            handler.sendMessage(successMessage);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Message failureMessage = new Message();
                        failureMessage.what = -1;
                        handler.sendMessage(failureMessage);
                    }
                }
            }).start();

        }
        try {
            taskRabbitTask.setLocalTaskData(serializeToJSON().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //submit!
    }


    /* message callbacks */
    /**
     * Show toast for task edit canceling
     */
    private void showSuccessToast() {
        Toast.makeText(activity, "Task posted to Task Rabbit successfully!",
                Toast.LENGTH_SHORT).show();
    }

    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case -1:

                if(dialog.isShowing()) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                    adb.setTitle("Error posting task");
                    adb.setMessage("Please try again");
                    adb.setPositiveButton("Close",null);
                    adb.show();
                }
                break;
            case 0: break;
            case 1:
                pendingCommentPicture = null;
                showSuccessToast();
                TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
                updateDisplay(taskRabbitTask.getRemoteTaskData());
                dialog.dismiss();
                break;
            case 2:
                TaskRabbitDataService.getInstance().saveTaskAndMetadata(taskRabbitTask);
                updateDisplay(taskRabbitTask.getRemoteTaskData());
                dialog.dismiss();
                break;
            }
        }
    };


    /* login methods */
    protected void loginTaskRabbit() {
        Intent intent = new Intent(activity,
                OAuthLoginActivity.class);
        try {
            String url = TASK_RABBIT_URL + "/api/authorize?client_id=" + TASK_RABBIT_CLIENT_ID;
            intent.putExtra(OAuthLoginActivity.URL_TOKEN, url);
            fragment.startActivityForResult(intent, REQUEST_CODE_TASK_RABBIT_OAUTH);
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_LOGIN_GL_START);
        } catch (Exception e) {
            //            handleError(e);
            e.printStackTrace();
        }
    }
    private void loadLocation() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) || !locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER )) {
            buildAlertMessageNoGps();
        }

        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 500.0f, this);
        updateControlSetLocation(currentLocation);
    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Yout GPS seems to be disabled, do you want to enable it?")
        .setCancelable(false)
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    protected void saveUserInfo(String response) throws Exception {
        JSONObject userObject = new JSONObject(response);
        JSONObject cityObject = userObject.getJSONObject("city");
        if (cityObject.has("name")){
            Preferences.setString("task_rabbit_city_name", cityObject.getString("name"));
        }
        if (cityObject.has("id")){
            Preferences.setInt("task_rabbit_city_id", cityObject.getInt("id"));
        }
        if (cityObject.has("lat")){
            //            currentLocation.setLatitude(cityObject.getDouble("lat"));
            Preferences.setString("task_rabbit_city_lat", String.valueOf(cityObject.getDouble("lat")));
        }
        if (cityObject.has("lng")){
            //            currentLocation.setLongitude(cityObject.getDouble("lng"));
            Preferences.setString("task_rabbit_city_lng", String.valueOf(cityObject.getDouble("lng")));
        }
    }

    private String taskRabbitURL(String method) {
        return TASK_RABBIT_URL + "/api/v1/"+ method;

    }

    /** Fire task rabbit if assigned **/
    @Override
    public boolean showTaskRabbitForUser(String name, JSONObject json) {
        // TODO Auto-generated method stub
        if (name.equals(activity.getString(R.string.actfm_EPA_task_rabbit))) {
            setUpUIComponents();
            dialog.show();
            return true;
        }
        return false;
    }

    public boolean activityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_TASK_RABBIT_OAUTH && resultCode == Activity.RESULT_OK){
            String result = data.getStringExtra(OAuthLoginActivity.DATA_RESPONSE);
            if(result.contains("access_token=")) {
                try {
                    result = result.substring(result.indexOf("access_token=")+"access_token=".length());
                    Preferences.setString(TASK_RABBIT_TOKEN, result);
                    String response = restClient.get(taskRabbitURL("account"));
                    Log.d("Task rabbit response", response);
                    saveUserInfo(response);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            return true;
        }
        else {
            for (TaskRabbitSetListener set : controls) {
                if (set.getClass().equals(TaskRabbitLocationControlSet.class)) {
                    if (((TaskRabbitLocationControlSet) set).activityResult(requestCode, resultCode, data)) {
                        return true;
                    }
                }
            }
        }
        if (dialog.isShowing()) {
            CameraResultCallback callback = new CameraResultCallback() {
                @Override
                public void handleCameraResult(Bitmap bitmap) {
                    pendingCommentPicture = bitmap;
                    pictureButton.setImageBitmap(pendingCommentPicture);
                }
            };

            return (ActFmCameraModule.activityResult(activity,
                    requestCode, resultCode, data, callback));
        }
        return false;
    }


    /*
     * (non-Javadoc)
     * @see android.location.LocationListener#onLocationChanged(android.location.Location)
     */
    @Override
    public void onLocationChanged(Location location) {

        currentLocation = location;
        updateControlSetLocation(currentLocation);

    }
    public void updateControlSetLocation (Location location) {
        for (TaskRabbitSetListener controlSet : controls) {
            if (TaskRabbitLocationControlSet.class.isAssignableFrom(controlSet.getClass())) {
                ((TaskRabbitLocationControlSet) controlSet).updateCurrentLocation(location);
            }
        }
    }
    @Override
    public void onProviderDisabled(String provider) {
        return;
    }
    @Override
    public void onProviderEnabled(String provider) {
        return;
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        return;
    }


    /*
     *
     */

    public void updateDisplay(JSONObject json) {
        if (json != null && json.has("state_label")) {
            String status = json.optString(activity.getString(R.string.tr_attr_state_label));
            TextView statusText = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
            statusText.setText(status);
            getDisplayView().setVisibility(View.VISIBLE);
        }
        else {
            getDisplayView().setVisibility(View.GONE);
        }
    }


    protected void updateStatus(JSONObject json){

        final int taskID = json.optInt("id"); //$NON-NLS-1$
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Log.d("Tasks url:", taskRabbitURL("tasks/" + taskID));
                    String response = restClient.get(taskRabbitURL("tasks/" + taskID));
                    Log.d("Task rabbit response", response);
                    JSONObject taskResponse = new JSONObject(response);
                    if(taskResponse.has("id")){
                        taskRabbitTask.setRemoteTaskData(response);
                        taskRabbitTask.setTaskID(taskResponse.optString("id"));
                        Message successMessage = new Message();
                        successMessage.what = 2;
                        handler.sendMessage(successMessage);
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                    Message failureMessage = new Message();
                    failureMessage.what = 0;
                    handler.sendMessage(failureMessage);
                }
            }
        }).start();
        //submit!
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(Preferences.getStringValue(TASK_RABBIT_TOKEN));
    }
    @Override
    public boolean shouldShowTaskRabbit() {
        if(Locale.getDefault().getCountry().equals("US")) return true; //$NON-NLS-1$
        return false;
    }

    public boolean supportsCurrentLocation() {

        //TODO test this
        if(true) return true;
        if (currentLocation == null) return false;
        for (GeoPoint point : supportedLocations){
            Location city = new Location(""); //$NON-NLS-1$
            city.setLatitude(point.getLatitudeE6()/1E6);
            city.setLongitude(point.getLongitudeE6()/1E6);
            float distance = currentLocation.distanceTo(city);
            if (distance < 400000) { //250 mi radius
                return true;
            }
        }
        return false;
    }




    /* Menu Popover */
    private void setMenuDropdownSelected(boolean selected) {
        int oldTextColor = menuTitle.getTextColors().getDefaultColor();
        int textStyle = (selected ? R.style.TextAppearance_ActionBar_ListsHeader_Selected :
            R.style.TextAppearance_ActionBar_ListsHeader);

        TypedValue listDisclosure = new TypedValue();
        activity.getTheme().resolveAttribute(R.attr.asListsDisclosure, listDisclosure, false);
        menuTitle.setTextAppearance(activity, textStyle);
        menuNav.setBackgroundColor(selected ? oldTextColor : android.R.color.transparent);
        menuNavDisclosure.setSelected(selected);
    }


    private final OnClickListener menuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setMenuDropdownSelected(true);
            menuPopover.show(v);
        }
    };


    private void createMenuPopover() {
        menuPopover = new FragmentPopover(activity, R.layout.task_rabbit_menu_popover);
        menuPopover.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                setMenuDropdownSelected(false);
            }
        });
        setupListView();
        menuPopover.setContent(menuList);
    }


    private void setupListView() {
        String[] keys = activity.getResources().getStringArray(R.array.tr_preset_types);
        if (!supportsCurrentLocation()) {
            keys = new String[]{ activity.getResources().getString(R.string.tr_type_virtual)};
        }
        adapter = new ArrayAdapter<String>(activity, R.layout.task_rabbit_menu_row, keys);
        menuList = new ListView(activity);
        menuList.setAdapter(adapter);
        menuList.setCacheColorHint(Color.TRANSPARENT);

        menuList.setSelection(0);
        menuList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                displayViewsForMode(position);
                menuPopover.dismiss();
            }
        });
    }



}
