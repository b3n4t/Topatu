package com.example.topatu;


import android.content.Context;
import android.os.Bundle;
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
    private ArrayList<miataruLocation> friends = new ArrayList<miataruLocation>();

    public static fragmentFriendView  newInstance() {
        fragmentFriendView f = new fragmentFriendView();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }

    private void getFakeData(){
        friends = new ArrayList<miataruLocation>();
        miataruLocation loc;
        loc = new miataruLocation("BF0160F5-4138-402C-A5F0-DEB1AA1F4216","Demo Device");
        loc.setAccuracy(10);
        loc.setAltitude(20);
        loc.setLongitude(20);
        loc.setTime(0);
        friends.add(loc);
        loc = new miataruLocation("45E41CC2-84E7-4258-8F75-3BA80CC0E652");
        loc.setAccuracy(100);
        loc.setAltitude(200);
        loc.setLongitude(200);
        loc.setTime(0);
        friends.add(loc);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOGTAG, "fragmentFriendView - onActivityCreated");

        getFakeData();

        ArrayAdapter<miataruLocation> adapter = new ArrayAdapter<miataruLocation>(getActivity(),android.R.layout.simple_list_item_1, friends);
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

    private class FriendArrayAdapter extends ArrayAdapter<miataruLocation> {
        private final Context context;
        private final miataruLocation[] values;

        public FriendArrayAdapter (Context context, miataruLocation[] values) {
            super(context, R.layout.friend_row_layout, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.friend_row_layout, parent, false);
            TextView update = (TextView) rowView.findViewById(R.id.lastupdate);
            TextView maintext = (TextView) rowView.findViewById(R.id.firstLine);
            TextView secondarytext = (TextView) rowView.findViewById(R.id.secondLine);

            return rowView;
        }
    }
}
