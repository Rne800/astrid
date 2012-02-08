package com.todoroo.astrid.taskrabbit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet.TaskRabbitSetListener;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitNameControlSet extends PopupControlSet implements TaskRabbitSetListener{

    protected final EditText editText;
    protected final TextView notesPreview;
    private final LinearLayout notesBody;

    public TaskRabbitNameControlSet(Activity activity, int viewLayout,
            int displayViewLayout, int titleID, int i) {
        super(activity, viewLayout, displayViewLayout, titleID);
        editText = (EditText) getView().findViewById(R.id.notes);
        notesPreview = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        notesBody = (LinearLayout) getDisplayView().findViewById(R.id.notes_body);
        dialog.getWindow()
              .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        displayText.setText(activity.getString(titleID));;
    }

    @Override
    public void saveToJSON(JSONObject json, String key) throws JSONException {
        json.put(key, editText.getText().toString());
    }

    @Override
    public void writeToJSON(JSONObject json, String key) throws JSONException {
        String nameKey = activity.getString(R.string.tr_set_key_description);
        if (json.has(nameKey)) {
            json.put(nameKey, json.optString(nameKey, "") + "\nRestaurant Name: " + editText.getText().toString());
        }

    }
    @Override
    public void readFromModel(JSONObject json, String key) {
        if (json.optInt(key) == 1) {
            editText.setHint("Enter a restaurant");
            return;
        }
        editText.setTextKeepState(json.optString(key, ""));
        notesPreview.setText(json.optString(key, ""));

    }




    @Override
    protected void refreshDisplayView() {
        notesPreview.setText(editText.getText());
    }

    @Override
    public void readFromTask(Task task) {
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }

    @Override
    protected void onOkClick() {
        super.onOkClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    protected void onCancelClick() {
        super.onCancelClick();
        InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public boolean hasNotes() {
        return !TextUtils.isEmpty(editText.getText());
    }

}
