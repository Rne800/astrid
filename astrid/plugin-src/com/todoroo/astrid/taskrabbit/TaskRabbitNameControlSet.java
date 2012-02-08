package com.todoroo.astrid.taskrabbit;

import java.io.ByteArrayOutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet.ActivityResultSetListener;
import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet.TaskRabbitSetListener;
import com.todoroo.astrid.ui.PopupControlSet;

public class TaskRabbitNameControlSet extends PopupControlSet implements TaskRabbitSetListener, ActivityResultSetListener{

    protected final EditText editText;
    protected final TextView notesPreview;
    private final LinearLayout notesBody;
    private final Fragment fragment;


    private final ImageButton pictureButton;
    private Bitmap pendingCommentPicture = null;
    private int cameraButton;


    public TaskRabbitNameControlSet(Fragment f, int viewLayout,
            int displayViewLayout, int titleID, int i) {
        super(f.getActivity(), viewLayout, displayViewLayout, titleID);
        this.fragment = f;
        editText = (EditText) getView().findViewById(R.id.notes);
        notesPreview = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        notesBody = (LinearLayout) getDisplayView().findViewById(R.id.notes_body);
        dialog.getWindow()
              .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        displayText.setText(activity.getString(titleID));;
        editText.setMaxLines(Integer.MAX_VALUE);



        pictureButton = (ImageButton) getView().findViewById(R.id.picture);
        if (pictureButton != null) {

        final ClearImageCallback clearImage = new ClearImageCallback() {
            @Override
            public void clearImage() {
                pendingCommentPicture = null;
                pictureButton.setImageResource(R.drawable.camera_button_gray);
            }
        };
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingCommentPicture != null)
                    ActFmCameraModule.showPictureLauncher(fragment, clearImage);
                else
                    ActFmCameraModule.showPictureLauncher(fragment, null);
            }
        });
        }

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

        if (pendingCommentPicture != null) {
            String picture = buildPictureData(pendingCommentPicture);
            JSONObject pictureArray = new JSONObject();
            pictureArray.put("image", picture);

            json.put("uploaded_photos_attributes", new JSONObject().put("1", pictureArray));
            Log.d("The task json", json.toString());
        }
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

    @Override
    public void readFromModel(JSONObject json, String key) {
        if (json.optInt(key, -1) == 0) {
            editText.setHint(displayText.getText().toString());
            return;
        }
        editText.setTextKeepState(json.optString(key, ""));
        notesPreview.setText(json.optString(key, ""));

    }


    @Override
    public boolean activityResult (int requestCode, int resultCode, Intent data) {
    if (dialog.isShowing() && pictureButton != null) {
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
