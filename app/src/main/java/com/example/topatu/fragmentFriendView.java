package com.example.topatu;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import android.widget.TextView;


/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentFriendView extends ListFragment {

    private static String LOGTAG = "TopatuLog";
    private ArrayList<miataruFriend> friends = new ArrayList<miataruFriend>();

    public static fragmentFriendView  newInstance() {
        fragmentFriendView f = new fragmentFriendView();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }

    private void getFakeData(){
        friends = new ArrayList<miataruFriend>();
        miataruFriend loc;
        //System.currentTimeMillis()

        loc = new miataruFriend(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("my_id", "No own UUID!!!!!!"),"Myself");
        loc.setLocation(1,1,5,System.currentTimeMillis());
        friends.add(loc);

        loc = new miataruFriend("BF0160F5-4138-402C-A5F0-DEB1AA1F4216","Demo Miataru device");
        loc.setLocation(1,1,5,System.currentTimeMillis());
        friends.add(loc);

        loc = new miataruFriend("45E41CC2-84E7-4258-8F75-3BA80CC0E652");
        loc.setLocation(2.0, 2.0, 50.0, System.currentTimeMillis());
        friends.add(loc);

        loc = new miataruFriend("3dcfbbe1-8018-4a88-acec-9d2aa6643e13","Test handy");
        loc.setLocation(2.0,2.0,50.0,System.currentTimeMillis());
        friends.add(loc);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOGTAG, "fragmentFriendView - onActivityCreated");

        getFakeData();

        //ArrayAdapter<miataruFriend> adapter = new ArrayAdapter<miataruFriend>(getActivity(),android.R.layout.simple_list_item_1, friends);
        FriendArrayAdapter adapter = new FriendArrayAdapter((Context)getActivity(),friends);
        setListAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "fragmentFriendView - onPause");
        persistentFriendList.remove("fragmentFriendView");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "fragmentFriendView - onResume");
        persistentFriendList.add("fragmentFriendView");
    }

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

            return rowView;
        }
    }
}
