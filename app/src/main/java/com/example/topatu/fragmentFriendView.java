package com.example.topatu;


//import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
//import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
//import android.widget.Button;
//import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
//import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.GridLayout;
import android.location.Location;

import java.util.ArrayList;

/**
 * Created by Ixa on 09/02/2015.
 */
public class fragmentFriendView extends Fragment{

    private static String LOGTAG = "TopatuLog";
    private ArrayList<Location> Friends = new ArrayList<Location>();

    private void getFakeData(){
        Friends = new ArrayList<Location>();
        Location loc;
        Bundle params;
        loc = new Location("Miataru");
        params = new Bundle();
        params.putString("Alias", "Test");
        params.putString("UUID", "7c2281c9-d49a-4742-b2d9-c72635f7cbfd");
        loc.setExtras(params);
        loc.setAccuracy(10);
        loc.setAltitude(20);
        loc.setLongitude(20);
        loc.setTime(0);
        Friends.add(loc);
        loc = new Location("Miataru");
        params = new Bundle();
        params.putString("UUID", "c8048414-4cc8-4a31-883d-3439d5bb5ba1");
        loc.setExtras(params);
        loc.setAccuracy(100);
        loc.setAltitude(200);
        loc.setLongitude(200);
        loc.setTime(0);
        Friends.add(loc);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOGTAG, "fragmentFriendView - onCreateView");
        View  myWidget;

        //myWidget = new TextView ( getActivity());
        //myWidget.setText("FriendView");
        myWidget = inflater.inflate(R.layout.fragment_friendview, container, false);

        //Fill the list
        LinearLayout list = (LinearLayout) myWidget.findViewById(R.id.FriendList);
        if ( list.getChildCount() > 0 ) {
            list.removeAllViews();
        }

        for(int i = 0; i < Friends.size(); i++) {
            Location friendloc = Friends.get(i);
            Bundle friendparams = friendloc.getExtras();
            if ( friendparams != null ) {
                final String uuid = friendparams.getString("UUID") ;
                String text;
                if ( uuid == null ) {
                    Log.v(LOGTAG, "Friend "+ i+" with missing UUID");

                    Friends.remove(friendloc);
                } else {
                    //Log.v(LOGTAG, "Friend "+i);

                    if ( friendparams.getString("Alias") != null ) {
                        text = friendparams.getString("Alias");
                    } else {
                        text = uuid;
                    }
                    Log.v(LOGTAG, "Friend "+ i + " - " + text);
                    TextView view = new TextView ( getActivity());
                    view.setText(text);
                    view.setPadding(0,10,0,10);

                    view.setOnLongClickListener(new View.OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View v) {
                            // TODO Auto-generated method stub
                            Log.v(LOGTAG, "Long tap on " + uuid);

                            final CharSequence[] items = { "Show on map", "Delete" };

                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                            builder.setTitle("Action:");
                            builder.setItems(items, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int item) {
                                    //cart = cartList.get(position);
                                    //db.removeProductFromCart(context, cart);

                                    Log.v(LOGTAG, "Action chooseb on " + uuid + ":");
                                    switch (item) {
                                        case 0:
                                            Log.v(LOGTAG, "   To map");
                                            break;
                                        case 1:
                                            Log.v(LOGTAG, "   Delete");
                                            break;
                                        default:
                                            Log.v(LOGTAG, "   Unkown");
                                            break;
                                    }
                                    /*
                                    new AlertDialog.Builder(context)
                                            .setTitle(getString(R.string.success))
                                            .setMessage(getString(R.string.item_removed))
                                            .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(CartDetailsActivity.this, HomeScreen.class);
                                                    startActivity(intent);
                                                }
                                            })
                                            .show();*/

                                }

                            });

                            AlertDialog alert = builder.create();

                            alert.show();

                            return true;
                        }
                    });
                    list.addView(view);
                }
            }
        }

        return myWidget;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "fragmentFriendView - onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "fragmentFriendView - onResume");
    }

    public static fragmentFriendView  newInstance() {
        fragmentFriendView f = new fragmentFriendView();
        //Bundle b = new Bundle();
        //b.putString("msg", text);
        //f.setArguments(b);

        return f;
    }
}
