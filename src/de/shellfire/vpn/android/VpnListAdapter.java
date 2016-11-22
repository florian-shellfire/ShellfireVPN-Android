package de.shellfire.vpn.android;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VpnListAdapter extends ArrayAdapter<Vpn> {

	HashMap<Vpn, Integer> mIdMap = new HashMap<Vpn, Integer>();
	private List<Vpn> mObjects;
	private Context mContext;

	public VpnListAdapter(Context context, int textViewResourceId, List<Vpn> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
		for (int i = 0; i < objects.size(); ++i) {
			mIdMap.put(objects.get(i), i);
			mObjects = objects;
		}
	}



	@Override
	public long getItemId(int position) {
		Vpn item = getItem(position);
		return mIdMap.get(item);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Vpn vpn = mObjects.get(position);

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.vpn_list_item, parent, false);

		setVpnValues(rowView, vpn);

		return rowView;
	}

	void setVpnValues(View rowView, Vpn vpn) {
		// assign vpn name
		TextView nameView = (TextView) rowView.findViewById(R.id.vpnName);
		nameView.setText(vpn.getName().trim());


		// assign vpnAccountType (Free, Premium, PremiumPlus) + stars
		TextView vpnAccountTypeView = (TextView) rowView.findViewById(R.id.vpnAccountType);
		vpnAccountTypeView.setText(Util.getServerTypeResId(vpn.getAccountType()));

		int numStars;
		if (vpn.getAccountType() == ServerType.Free)
			numStars = 1;
		else if (vpn.getAccountType() == ServerType.Premium)
			numStars = 3;
		else
			numStars = 5;

		for (int i = 5 - numStars; i >= 1; i--) {
			int id;
			if (i == 1)
				id = R.id.star_1;
			else if (i == 2)
				id = R.id.star_2;
			else if (i == 3)
				id = R.id.star_3;
			else if (i == 4)
				id = R.id.star_4;
			else
				id = R.id.star_5;

			ImageView starView = (ImageView) rowView.findViewById(id);
			starView.setVisibility(android.view.View.INVISIBLE);
		}

	}

}
