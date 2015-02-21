package com.example.topatu;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentFriendView extends ListFragment implements persistentFriends.friendEvents, AdapterView.OnItemLongClickListener {

    private static String LOGTAG = "TopatuLog";
    private ArrayList<miataruFriend> friends = null;
    private FriendArrayAdapter adapter = null;
    private persistentFriends friendData = null;

    public static fragmentFriendView  newInstance() {
        fragmentFriendView f = new fragmentFriendView();
        return f;
    }

    private void getFakeData(){
        friends = new ArrayList<miataruFriend>();
        miataruFriend loc;

        loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("my_id", "No own UUID!!!!!!"),"Myself");
        loc.setLocation(1,1,5,System.currentTimeMillis());
        friends.add(loc);

        loc = new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216","Demo Miataru device");
        loc.setLocation(1,1,5,System.currentTimeMillis() - 60*60*3*1000);
        friends.add(loc);

        loc = new miataruFriend("45E41CC2-84E7-4258-8F75-3BA80CC0E652");
        loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis() - 60*3*1000 );
        friends.add(loc);

        loc = new miataruFriend("3dcfbbe1-8018-4a88-acec-9d2aa6643e13","Test handy");
        loc.setLocation(2.0,2.0,50.0,System.currentTimeMillis() - 93*1000);
        friends.add(loc);

        loc = new miataruFriend("99999999-9999-9999-9999-999999999999",Long.toString(System.currentTimeMillis()));
        loc.setLocation(2.0,2.0,50.0,System.currentTimeMillis() - 10*1000);
        friends.add(loc);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOGTAG, "fragmentFriendView - onActivityCreated");

        //friendData = new persistentFriends();
        //friendData.registerCallback(this);
        //friends = friendData.getFriends();

        friends = persistentFriends.getFriends();

        Log.v(LOGTAG,"fragmentFriendView - Number of friends "+friends.size());

        //getFakeData();

        //ArrayAdapter<miataruFriend> adapter = new ArrayAdapter<miataruFriend>(getActivity(),android.R.layout.simple_list_item_1, friends);
        adapter = new FriendArrayAdapter((Context)getActivity(),friends);
        setListAdapter(adapter);

        getListView().setOnItemLongClickListener(this);
    }

    //
    //
    // Click listeners
    //
    //
    public boolean onItemLongClick(AdapterView parent, View view, final int position, long id) {
        final miataruFriend selectedFriend = friends.get(position);
        //Log.v(LOGTAG,"LongClickMenu - Item " + position);
        final CharSequence[] items = { "View in Map", "Edit alias", "Delete" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Action:");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ( selectedFriend == null ) { return; }
                //Log.v(LOGTAG,"Popup Menu Item selected "+item);
                switch (item ) {
                    case 0: Log.v(LOGTAG, "Action: To Map ("+selectedFriend.getAlias()+")");break;
                    case 1:
                        //Log.v(LOGTAG, "Action: Edit ("+selectedFriend.getAlias()+")");
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Edit:");
                        builder.setMessage("Alias:");
                        final EditText newAlias = new EditText (getActivity());
                        newAlias.setText(selectedFriend.getAlias());

                        builder.setView(newAlias);
                        builder.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.v(LOGTAG,"***** Set '" + newAlias.getText() + "' to '" + selectedFriend.getUUID() + "'");
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
                        break;
                    case 2:
                        //Log.v(LOGTAG, "Action: Delete ("+selectedFriend.getAlias()+")");
                        Toast toast = Toast.makeText(getActivity(), "Item deleted "+selectedFriend.getUUID(), Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                    default: Log.v(LOGTAG, "Action: Wrong selection");break;
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
    public void refreshFriendInfo () {
        adapter.notifyDataSetChanged();
    }
    @Override
    public void refreshFriendLocation () {}
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
        friendData = new persistentFriends();
        friendData.registerCallback(this);
    }

    //
    //
    // Adapter for the ListView
    //
    //
    private class FriendArrayAdapter extends ArrayAdapter<miataruFriend> {
        private final Context context;
        private final ArrayList<miataruFriend> values;

        public FriendArrayAdapter (Context context, ArrayList<miataruFriend> values) {
            super(context, R.layout.friend_row_layout, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;

            if ( rowView == null ) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.friend_row_layout, parent, false);
            }

            TextView update = (TextView) rowView.findViewById(R.id.lastupdate);
            TextView maintext = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondarytext = (TextView) rowView.findViewById(R.id.secondLine);

            miataruFriend friend = values.get(position);

            if (friend.getAlias() !=  null && friend.getAlias().length() > 0) {
                maintext.setText(friend.getAlias());
                secondarytext.setText(friend.getUUID());
            } else {
                maintext.setText(friend.getUUID());
                secondarytext.setText("");
            }

            update.setText(friend.getUpdateTime());

            return rowView;
        }
    }
}
