package com.todoroo.astrid.taskrabbit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSet;
import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet.TaskRabbitSetListener;

public class TaskRabbitSpinnerControlSet extends TaskEditControlSet implements TaskRabbitSetListener{

    private final Spinner spinner;
    private final int setID;
    private final int titleID;
    private final TextView displayText;
    private final TextView displayEdit;
    private final Fragment fragment;
    private ArrayAdapter adapter;

    public  TaskRabbitSpinnerControlSet(final Fragment fragment, int viewLayout, int title, int setID) {
        super(fragment.getActivity(), viewLayout);
        this.setID = setID;
        this.fragment = fragment;
        this.titleID = title;
        //        DependencyInjectionService.getInstance().inject(this);

//        spinner = new Spinner(fragment.getActivity());
        spinner = (Spinner) getDisplayView().findViewById(R.id.spinner);
        spinner.setPrompt(getActivity().getString(title));

        displayEdit = (TextView) getDisplayView().findViewById(R.id.display_row_edit);

        displayText = (TextView) getDisplayView().findViewById(R.id.display_row_title);
        displayText.setText(title);



        String[] listTypes = getStringDefaultArray(setID, R.array.tr_default_array);



        adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, listTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(adapter);
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                Log.d("SELECTED ITEM", "SLECTED :" + arg2 + " ACTUAL: " + spinner.getSelectedItemPosition() + " STRING: " + spinner.getSelectedItem().toString());

                displayEdit.setText(spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        Log.d("SELECTED ITEM", " ACTUAL: " + spinner.getSelectedItemPosition());
        getView().setOnClickListener(getDisplayClickListener());

    }


    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.performClick();
            }
        };
    }
    public String[] getStringDefaultArray(int position, int arrayType) {
            TypedArray arrays = getActivity().getResources().obtainTypedArray(arrayType);
            int arrayID = arrays.getResourceId(position, -1);
             return getActivity().getResources().getStringArray(arrayID);
    }
    public void resetAdapter(String[] items) {
        if (adapter == null) return;
        adapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setAdapter(adapter);
                spinner.setSelection(0); // plus 1 for the no selection item
            }
        });

        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                Log.d("SELECTED ITEM", "SLECTED :" + arg2 + " ACTUAL: " + spinner.getSelectedItemPosition() + " STRING: " + spinner.getSelectedItem().toString());

                displayEdit.setText(spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
    }

    private Activity getActivity() {
        return fragment.getActivity();
    }

    public int selectedPosition() {
        return spinner.getSelectedItemPosition();
    }

    @Override
    public void readFromModel(JSONObject json, String key) {

        if (titleID == R.string.tr_set_named_price && json.has(fragment.getActivity().getString(R.string.tr_set_key_type))){
            String[] listTypes = getStringDefaultArray(json.optInt(fragment.getActivity().getString(R.string.tr_set_key_type), 0), R.array.tr_default_price_type_array);
            resetAdapter(listTypes);
        }
        int intValue = json.optInt(key, 0);

        Log.d("iii", "" + spinner.getCount());
        if (intValue < spinner.getCount()) {
            spinner.setSelection(intValue);
            displayEdit.setText(spinner.getSelectedItem().toString());


        }
        Log.d("dfhjskhfds", ""+intValue);
    }

    @Override
    public void saveToJSON(JSONObject json, String key) throws JSONException {
        json.put(key, spinner.getSelectedItemPosition());

    }

    public int parseToDollars (String conversion){
        int index = conversion.lastIndexOf('$');
        String cents = conversion.substring(index+1);
        if(TextUtils.isEmpty(cents)) return 0;
        Log.d("PARSING TO CHANGE", cents);
        return Integer.parseInt(cents);
    }

    @Override
    public void writeToJSON(JSONObject json, String key) throws JSONException {

        if(spinner.getSelectedItem() != null){
            String spinnerString = spinner.getSelectedItem().toString();
            if (titleID == R.string.tr_set_cost_in_cents || titleID == R.string.tr_set_named_price) {
                int cents = parseToDollars(spinnerString) *100;
                json.put(key, cents);
            }
            else if (key.contains("description")) {
                String description = json.optString("description", "");
                description += String.format("\n%S %S", key, spinner.getSelectedItem().toString()); //$NON-NLS-1$
            }
            else {
                json.put(key, spinnerString);
            }
        }
    }


    @Override
    public void readFromTask(Task task) {
        return;
    }

    @Override
    public String writeToModel(Task task) {
        return null;
    }

}
