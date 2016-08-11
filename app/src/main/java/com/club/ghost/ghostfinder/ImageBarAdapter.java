package com.club.ghost.ghostfinder;

/**
 * Created by tpfaff on 8/7/16.
 */

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageBarAdapter extends BaseAdapter {

    private Activity activity;
    private ArrayList<Integer> data = new ArrayList<Integer>();
    private static LayoutInflater inflater=null;

    public ImageBarAdapter(Activity a) {
        activity = a;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void rebuild (ArrayList<Integer> d) {
        data = d;
        notifyDataSetChanged();
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public int getID(int position) { return data.get(position); }

    public String getName(int id) {
        switch (id) {
            case 1: return "Normale Anomalie";
            case 2: return "Paranormalaxiom";
            case 3: return "Sphärenkringel";
            case 4: return "Ekto Schmekto";
            case 5: return "Üble Zerscheinung";
            default: return "";
        }
    }

    public String getFile(int id) {
        switch (id) {
            case 1: return "normale_anomalie.pdf";
            case 2: return "paranormalaxiom";
            case 3: return "sphaerenkringel";
            case 4: return "ekto_schmekto";
            case 5: return "ueble_zerscheinung";
            default: return "";
        }
    }

    public int getResourceID(int id) {
        switch (id) {
            case 1: return R.drawable.psi0;
            case 2: return R.drawable.psi1;
            case 3: return R.drawable.psi2;
            case 4: return R.drawable.psi3;
            case 5: return R.drawable.psi4;
            default: return 0;
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row, null);

        TextView title = (TextView)vi.findViewById(R.id.title); // title
        ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image

        int idx = data.get(position);
        title.setText(getName(idx));
        thumb_image.setImageResource(getResourceID(idx));
        return vi;
    }
}
