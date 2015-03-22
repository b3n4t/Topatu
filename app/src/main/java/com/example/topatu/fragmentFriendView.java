package com.example.topatu;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentFriendView extends ListFragment implements persistentFriends.friendEvents, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

    private static String LOGTAG = "TopatuLog";
    private OnFriendSelected listener;
    private ArrayList<miataruFriend> friends = null;
    private adapterFriendArray adapter = null;
    private persistentFriends friendData = null;
    private miataruFriend selectedFriend;
    private int selectedItem = -1;
    private Menu myMenu = null;

    public static fragmentFriendView  newInstance() {
        fragmentFriendView f = new fragmentFriendView();
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOGTAG, "fragmentFriendView - onActivityCreated");

        setHasOptionsMenu(true);

        //friendData = new persistentFriends();
        //friendData.registerCallback(this);
        //friends = persistentFriends.getFriends();

        friends = persistentFriends.getFriends();

        if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG,"fragmentFriendView - Number of friends "+friends.size()); }

        //getFakeData();

        //ArrayAdapter<miataruFriend> adapter = new ArrayAdapter<miataruFriend>(getActivity(),android.R.layout.simple_list_item_1, friends);
        adapter = new adapterFriendArray(getActivity(),friends);
        this.getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        this.getListView().setOnItemClickListener(this);
        this.setListAdapter(adapter);
        //this.getListView().setOnItemLongClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFriendSelected) {
            listener = (OnFriendSelected) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet fragmentFriendView.OnFriendSelected");
        }
    }

    //
    //
    // GUI to add new friend
    //
    //
    private void setMenuStatus ( ) {
        boolean menuStatus;
        int menuAlpha;

        if (selectedItem == -1) {
            menuStatus = false;
            menuAlpha = 0x40;
        } else {
            menuStatus = true;
            menuAlpha = 0xFF;
        }

        MenuItem menuEntry;
        menuEntry = myMenu.findItem(R.id.show_on_map);
        menuEntry.setEnabled(menuStatus);
        menuEntry.getIcon().setAlpha(menuAlpha);
        menuEntry = myMenu.findItem(R.id.edit_friend);
        menuEntry.setEnabled(menuStatus);
        menuEntry.getIcon().setAlpha(menuAlpha);
        menuEntry = myMenu.findItem(R.id.delete_friend);
        menuEntry.setEnabled(menuStatus);
        menuEntry.getIcon().setAlpha(menuAlpha);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.v(LOGTAG,"fragmentFriendView - onItemClick - Old friend (" + selectedItem + ") and new (" + position +")");
        if ( selectedItem == position ) {
            selectedItem = -1;
            selectedFriend = null;
        } else {
            selectedItem = position;
            selectedFriend = friends.get(selectedItem);
        }
        setMenuStatus();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_friend_view, menu);
        myMenu = menu;
        setMenuStatus();
        Log.v(LOGTAG,"fragmentFriendView - onCreateOptionsMenu");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_friend:
                showFriendDialog(null);
                return true;
            case R.id.show_on_map:
                listener.onFriendSelected(selectedItem);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFriendDialog (miataruFriend friend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if ( friend == null ) {
            builder.setTitle("New Friend:");
        } else {
            builder.setTitle("Edit Friend:");
        }
        builder.setMessage("Alias:");
        final EditText newAlias = new EditText (getActivity());
        newAlias.setHint("Alias to identify this device");
        if ( friend != null && friend.getAlias() != null && friend.getAlias().length() > 0 ) {
            newAlias.setText(friend.getAlias());
        }

        builder.setView(newAlias);
        builder.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if ( MainActivity.Debug > 0 ) { Log.v(LOGTAG,"***** Set '" + newAlias.getText() + "' to '" + selectedFriend.getUUID() + "'"); }
                //String value1 = servername.getText().toString();
                //String value2 = username.getText().toString();
                //String value3 = password.getText().toString();
                // Do something with value!
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        AlertDialog editDialog = builder.create();
        editDialog.show();


    }

    //
    //
    // Click listeners
    //
    //
    public boolean onItemLongClick(AdapterView parent, View view, final int position, long id) {
        selectedFriend = friends.get(position);
        // if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG,"LongClickMenu - Item " + position); }
        //final CharSequence[] items = { "View in Map", "Edit alias", "Delete" };
        final CharSequence[] items = MainActivity.getAppContext().getResources().getStringArray(R.array.showtext_menu_dialog_content);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.showtext_menu_dialog_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ( selectedFriend == null ) { return; }
                //if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG,"Popup Menu Item selected "+item); }
                switch (item ) {
                    case 0:
                        Log.v(LOGTAG, "Action: To Map ("+selectedFriend.getAlias()+")");
                        listener.onFriendSelected(position);
                        break;
                    case 1:
                        //if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "Action: Edit ("+selectedFriend.getAlias()+")"); }
                        showFriendDialog(selectedFriend);
                        break;
                    case 2:
                        if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "Action: Delete ("+selectedFriend.getAlias()+")"); }
                        friendData.removeFriend(selectedFriend);
                        //Toast toast = Toast.makeText(getActivity(), "Item deleted "+selectedFriend.getUUID(), Toast.LENGTH_SHORT);
                        //toast.show();
                        break;
                    default: if ( MainActivity.Debug > 2 ) { Log.v(LOGTAG, "Action: Wrong selection");break; }
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

        return false;
    }

    //
    //
    // Callback to refresh friend list
    //
    //
    @Override
    public void onRefreshFriendInfo() {
        adapter.notifyDataSetChanged();
    }
    //
    //
    // onPause and onResume
    //
    //
    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "fragmentFriendView - onPause");
        friendData.close();
        friendData = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "fragmentFriendView - onResume");
        //if ( friendData == null ) {
        friendData = new persistentFriends(this);
        //friendData.registerCallback(this);
    }

    //
    //
    // Adapter for friend ListView
    //
    //
    private class adapterFriendArray extends ArrayAdapter<miataruFriend> {
        private final Context context;
        private final ArrayList<miataruFriend> values;

        public adapterFriendArray(Context context, ArrayList<miataruFriend> values) {
            super(context, R.layout.friend_row_layout, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View rowView = convertView;

            //Log.v(LOGTAG,"fragmentFriendView - Adapter - Selected friend is " + selectedItem +" ("+position+")");

            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.friend_row_layout, parent, false);
            }

            TextView update = (TextView) rowView.findViewById(R.id.lastupdate);
            TextView maintext = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondarytext = (TextView) rowView.findViewById(R.id.secondLine);

            //if ( position == fragmentFriendView.this.getListView().getSelectedItemPosition() ) {
            if ( position == selectedItem ) {
                rowView.setBackgroundResource(R.color.topatu_backgroundselected);
                update.setBackgroundResource(R.color.topatu_backgroundselected);
                maintext.setBackgroundResource(R.color.topatu_backgroundselected);
                secondarytext.setBackgroundResource(R.color.topatu_backgroundselected);
            } else {
                rowView.setBackgroundResource(R.color.topatu_backgroundnormal);
                update.setBackgroundResource(R.color.topatu_backgroundnormal);
                maintext.setBackgroundResource(R.color.topatu_backgroundnormal);
                secondarytext.setBackgroundResource(R.color.topatu_backgroundnormal);
            }
            miataruFriend friend = values.get(position);

            maintext.setText(friend.getShowText());
            secondarytext.setText(friend.getSecondaryText());

            update.setText(friend.getUpdateTime());

            return rowView;
        }
    }

    //
    //
    // Callback interface
    //
    //
    public interface OnFriendSelected {
        public void onFriendSelected (int friendPos);
    }

}
