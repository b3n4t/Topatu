package com.example.topatu;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentFriendView extends ListFragment implements persistentFriends.friendEvents, AdapterView.OnItemClickListener {

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
        if ( selectedItem != -1 && selectedFriend.getServer().compareTo(MainActivity.getAppContext().getString(R.string.settings_my_server_local)) != 0 ) {
            menuEntry = myMenu.findItem(R.id.edit_friend);
            menuEntry.setEnabled(menuStatus);
            menuEntry.getIcon().setAlpha(menuAlpha);
            menuEntry = myMenu.findItem(R.id.delete_friend);
            menuEntry.setEnabled(menuStatus);
            menuEntry.getIcon().setAlpha(menuAlpha);
        } else {
            menuEntry = myMenu.findItem(R.id.edit_friend);
            menuEntry.setEnabled(false);
            menuEntry.getIcon().setAlpha(0x40);
            menuEntry = myMenu.findItem(R.id.delete_friend);
            menuEntry.setEnabled(false);
            menuEntry.getIcon().setAlpha(0x40);
        }
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
            case R.id.edit_friend:
                if ( selectedFriend.getServer().compareTo(MainActivity.getAppContext().getString(R.string.settings_my_server_local)) != 0 ) {
                    showFriendDialog(selectedFriend, false);
                    return false;
                }
                return false;
            case R.id.delete_friend:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.showtext_friendinfo_confirm_deletion);
                builder.setNegativeButton(R.string.showtext_friendinfo_no,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
                builder.setPositiveButton(R.string.showtext_friendinfo_yes,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        friendData.removeFriend(selectedFriend);
                    }
                });
                AlertDialog editDialog = builder.create();
                editDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFriendDialog (miataruFriend friend) {
        if ( friend != null ) {
            showFriendDialog(friend, false);
        } else {
            showFriendDialog(null, true);
        }
    }

    private void showFriendDialog (miataruFriend friend, boolean isNew ) {
        final EditText viewID;
        final EditText viewServer;
        final EditText viewAlias;
        final Button okButton;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_friend,null);
        builder.setView(v);


        viewID = (EditText)v.findViewById(R.id.friend_uuid);
        viewServer = (EditText)v.findViewById(R.id.friend_server);
        viewAlias = (EditText)v.findViewById(R.id.friend_alias);

        if ( isNew ) {
            builder.setTitle(R.string.showtext_friendinfo_new_friend);

            builder.setPositiveButton(R.string.showtext_friendinfo_accept,new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if ( MainActivity.Debug > 0 ) { Log.v(LOGTAG,"***** New friend '" + viewID.getText().toString() + "' '" + viewServer.getText().toString() + "' '" + viewAlias.getText().toString() + "'"); }
                    friendData.addFriend(viewID.getText().toString(),
                            viewServer.getText().toString(),
                            viewAlias.getText().toString() );
                }
            });

        } else {
            builder.setTitle(R.string.showtext_friendinfo_edit_friend);
            viewID.setText(friend.getUUID());
            viewServer.setText(friend.getServer());
            if ( friend.getUUID() != null && friend.getUUID().length() > 0 ) {
                viewAlias.setText(friend.getAlias());
            }

            builder.setPositiveButton(R.string.showtext_friendinfo_accept,new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    friendData.editFriend(selectedFriend, viewID.getText().toString(),
                            viewServer.getText().toString(),
                            viewAlias.getText().toString());
                }
            });
        }
        builder.setNegativeButton(R.string.showtext_friendinfo_cancel,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        AlertDialog editDialog = builder.create();
        editDialog.show();
        okButton = editDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if ( isNew ) {
            okButton.setEnabled(false);
        }


        TextWatcher textChanged = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (viewID.getText().toString().length() == 0 || viewServer.getText().toString().length() == 0) {
                    okButton.setEnabled(false);
                } else {
                    okButton.setEnabled(true);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        viewID.addTextChangedListener(textChanged);
        viewServer.addTextChangedListener(textChanged);
        viewAlias.addTextChangedListener(textChanged);
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

            miataruFriend friend = values.get(position);

            maintext.setText(friend.getShowText());
            secondarytext.setText(friend.getSecondaryText());
            update.setText(friend.getUpdateTime());

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
